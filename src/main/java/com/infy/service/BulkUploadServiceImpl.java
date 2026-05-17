package com.infy.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.infy.dto.BulkUploadResponseDTO;
import com.infy.dto.BulkUploadResultDTO;
import com.infy.entity.ActivityLog;
import com.infy.entity.BulkUpload;
import com.infy.entity.Department;
import com.infy.entity.User;
import com.infy.enums.ActivityType;
import com.infy.enums.BulkUploadStatus;
import com.infy.enums.BulkUploadType;
import com.infy.enums.Role;
import com.infy.enums.UserStatus;
import com.infy.exception.WellnessTrackerException;
import com.infy.repository.ActivityLogRepository;
import com.infy.repository.BulkUploadRepository;
import com.infy.repository.DepartmentRepository;
import com.infy.repository.UserRepository;

@Service
public class BulkUploadServiceImpl implements BulkUploadService {

    @Autowired
    private BulkUploadRepository bulkUploadRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    // -------------------------------------------------------------------------
    // Derives the authenticated user's email from the SecurityContext.
    // Throws Service.UNAUTHORIZED if no principal is present.
    // -------------------------------------------------------------------------
    private String getAuthenticatedEmail() throws WellnessTrackerException {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new WellnessTrackerException("Service.UNAUTHORIZED");
        }
        return authentication.getName(); // JWT subject — the user's email
    }

    // -------------------------------------------------------------------------
    // Loads the user for the supplied ID, verifies their email matches the JWT
    // principal, and confirms they are HR.
    // Throws Service.UNAUTHORIZED if the JWT email does not match the supplied ID.
    // Throws Service.NOT_HR if the user is not HR.
    // -------------------------------------------------------------------------
    private User resolveAuthenticatedHrUser(Integer suppliedUserId)
            throws WellnessTrackerException {
        String authenticatedEmail = getAuthenticatedEmail();

        Optional<User> userOptional = userRepository.findById(suppliedUserId);
        User user = userOptional.orElseThrow(
                () -> new WellnessTrackerException("Service.USER_NOT_FOUND"));

        // JWT principal must match the supplied ID — prevents any authenticated
        // user from passing a different HR user's ID to bypass the role check.
        if (!authenticatedEmail.equals(user.getEmail())) {
            throw new WellnessTrackerException("Service.UNAUTHORIZED");
        }

        if (!Role.HR.equals(user.getRole())) {
            throw new WellnessTrackerException("Service.NOT_HR");
        }

        return user;
    }

    // -------------------------------------------------------------------------
    // US 14 - Upload and process a CSV file.
    //
    // The upload is processed in one transaction. Valid rows are saved if the
    // upload completes without an unrecoverable IO error.
    //
    // Header validation:
    //   Row 1 is compared exactly against the expected header for the upload
    //   type. A mismatch marks the upload FAILED and returns immediately —
    //   no data rows are processed.
    //
    // Column count:
    //   EMPLOYEE        — exactly 8 columns per data row.
    //   BASELINE_METRIC — exactly 6 columns per data row.
    //   notes may be blank but the column must be present.
    //
    // Row processing:
    //   Each row is validated independently. Invalid rows are skipped and added
    //   to the errors list. Valid rows are saved. Processing continues regardless
    //   of row failures.
    // -------------------------------------------------------------------------
    @Override
    @Transactional
    public BulkUploadResultDTO uploadCsv(Integer uploadedBy, String uploadType,
            MultipartFile file) throws WellnessTrackerException {

        // 1. Authenticate and authorise
        User uploader = resolveAuthenticatedHrUser(uploadedBy);

        // 2. Validate uploadType
        BulkUploadType uploadTypeEnum;
        try {
            uploadTypeEnum = BulkUploadType.valueOf(uploadType.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new WellnessTrackerException("Service.INVALID_UPLOAD_TYPE");
        }

        // 3. Validate file
        if (file == null || file.isEmpty()) {
            throw new WellnessTrackerException("Service.INVALID_CSV_FILE");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null
                || !originalFilename.toLowerCase().endsWith(".csv")) {
            throw new WellnessTrackerException("Service.INVALID_CSV_FILE");
        }

        // 4. Persist upload record with PROCESSING status
        BulkUpload bulkUpload = new BulkUpload();
        bulkUpload.setUploadedBy(uploader);
        bulkUpload.setUploadType(uploadTypeEnum.name());
        bulkUpload.setFileName(originalFilename);
        bulkUpload.setStatus(BulkUploadStatus.PROCESSING.name());
        bulkUpload = bulkUploadRepository.save(bulkUpload);

        // 5. Expected header strings (case-sensitive, trimmed)
        String expectedHeader = BulkUploadType.EMPLOYEE.equals(uploadTypeEnum)
                ? "firstName,lastName,email,password,role,departmentId,managerId,status"
                : "userId,activityType,activityDate,activityValue,unit,notes";

        int importedRows = 0;
        int updatedRows  = 0;
        int failedRows   = 0;
        int totalRows    = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Row 1 — validate header exactly
                if (lineNumber == 1) {
                    if (!expectedHeader.equals(line.trim())) {
                        // Invalid header — fail the entire file, return immediately
                        bulkUpload.setStatus(BulkUploadStatus.FAILED.name());
                        bulkUpload.setTotalRows(0);
                        bulkUpload.setSuccessRows(0);
                        bulkUpload.setFailedRows(0);
                        bulkUploadRepository.save(bulkUpload);

                        BulkUploadResultDTO failed = new BulkUploadResultDTO();
                        failed.setBulkUploadId(bulkUpload.getBulkUploadId());
                        failed.setFileName(originalFilename);
                        failed.setUploadType(uploadTypeEnum.name());
                        failed.setStatus(BulkUploadStatus.FAILED.name());
                        failed.setTotalRows(0);
                        failed.setImportedRows(0);
                        failed.setUpdatedRows(0);
                        failed.setFailedRows(0);
                        failed.setErrors(List.of(
                                "Invalid CSV header. Expected: " + expectedHeader));
                        failed.setUploadedAt(bulkUpload.getUploadedAt());
                        return failed;
                    }
                    continue;
                }

                // Skip completely blank lines
                if (line.isBlank()) {
                    continue;
                }

                totalRows++;
                // -1 preserves trailing empty fields (e.g. blank notes column)
                String[] cols = line.split(",", -1);

                RowResult rowResult;
                if (BulkUploadType.EMPLOYEE.equals(uploadTypeEnum)) {
                    rowResult = processEmployeeRow(lineNumber, cols);
                } else {
                    rowResult = processBaselineMetricRow(lineNumber, cols);
                }

                if (rowResult.success) {
                    if (rowResult.wasUpdate) {
                        updatedRows++;
                    } else {
                        importedRows++;
                    }
                } else {
                    failedRows++;
                    errors.add(rowResult.error);
                }
            }

        } catch (Exception e) {
            bulkUpload.setStatus(BulkUploadStatus.FAILED.name());
            bulkUpload.setTotalRows(0);
            bulkUpload.setSuccessRows(0);
            bulkUpload.setFailedRows(0);
            bulkUploadRepository.save(bulkUpload);
            throw new WellnessTrackerException("General.EXCEPTION_MESSAGE");
        }

        // 6. Determine final status
        String finalStatus;
        if (failedRows == 0) {
            finalStatus = BulkUploadStatus.COMPLETED.name();
        } else if (importedRows + updatedRows > 0) {
            finalStatus = BulkUploadStatus.COMPLETED_WITH_ERRORS.name();
        } else {
            finalStatus = BulkUploadStatus.FAILED.name();
        }

        // 7. Update BulkUpload record with final counts
        bulkUpload.setStatus(finalStatus);
        bulkUpload.setTotalRows(totalRows);
        bulkUpload.setSuccessRows(importedRows + updatedRows);
        bulkUpload.setFailedRows(failedRows);
        bulkUploadRepository.save(bulkUpload);

        // 8. Build and return result DTO
        BulkUploadResultDTO result = new BulkUploadResultDTO();
        result.setBulkUploadId(bulkUpload.getBulkUploadId());
        result.setFileName(originalFilename);
        result.setUploadType(uploadTypeEnum.name());
        result.setStatus(finalStatus);
        result.setTotalRows(totalRows);
        result.setImportedRows(importedRows);
        result.setUpdatedRows(updatedRows);
        result.setFailedRows(failedRows);
        result.setErrors(errors);
        result.setUploadedAt(bulkUpload.getUploadedAt());
        return result;
    }

    // -------------------------------------------------------------------------
    // US 14 - Import history for a specific HR user, newest first.
    // Authenticated email must match the supplied userId.
    // Returns [] when no uploads exist — not an error (admin list rule).
    // -------------------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public List<BulkUploadResponseDTO> getUploadHistory(Integer userId)
            throws WellnessTrackerException {

        User user = resolveAuthenticatedHrUser(userId);

        List<BulkUpload> uploads =
                bulkUploadRepository.findByUploadedBy_UserIdOrderByUploadedAtDesc(
                        user.getUserId());

        List<BulkUploadResponseDTO> response = new ArrayList<>();
        for (BulkUpload upload : uploads) {
            response.add(mapToResponseDTO(upload));
        }
        return response;
    }

    // -------------------------------------------------------------------------
    // US 14 - Single upload record detail (ownership-guarded).
    // Authenticated email must match the supplied requestingUserId.
    // Returns summary only — row-level errors are not stored.
    // -------------------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public BulkUploadResponseDTO getUploadById(Integer bulkUploadId,
            Integer requestingUserId) throws WellnessTrackerException {

        User requestingUser = resolveAuthenticatedHrUser(requestingUserId);

        Optional<BulkUpload> uploadOptional =
                bulkUploadRepository.findById(bulkUploadId);
        BulkUpload upload = uploadOptional.orElseThrow(
                () -> new WellnessTrackerException("Service.BULK_UPLOAD_NOT_FOUND"));

        // Ownership guard — HR user can only view their own upload records
        if (!upload.getUploadedBy().getUserId().equals(requestingUser.getUserId())) {
            throw new WellnessTrackerException("Service.BULK_UPLOAD_ACCESS_DENIED");
        }

        return mapToResponseDTO(upload);
    }

    // -------------------------------------------------------------------------
    // EMPLOYEE row processor
    //
    // Expected columns (0-indexed, exactly 8):
    //   0=firstName, 1=lastName, 2=email, 3=password,
    //   4=role, 5=departmentId, 6=managerId, 7=status
    //
    // Upsert by email:
    //   Found     → update fields; blank password = keep existing hash.
    //   Not found → create new user; password required.
    // -------------------------------------------------------------------------
    private RowResult processEmployeeRow(int lineNumber, String[] cols) {
        String prefix = "Row " + lineNumber + ": ";

        // Exact column count — extra or missing columns are both rejected
        if (cols.length != 8) {
            return RowResult.fail(prefix + "expected exactly 8 columns "
                    + "(firstName,lastName,email,password,role,departmentId,managerId,status)"
                    + " but found " + cols.length + ".");
        }

        String firstName    = cols[0].trim();
        String lastName     = cols[1].trim();
        String email        = cols[2].trim();
        String password     = cols[3].trim();
        String roleStr      = cols[4].trim();
        String deptIdStr    = cols[5].trim();
        String managerIdStr = cols[6].trim();
        String statusStr    = cols[7].trim();

        // Required field presence checks
        if (firstName.isEmpty()) return RowResult.fail(prefix + "firstName is required.");
        if (lastName.isEmpty())  return RowResult.fail(prefix + "lastName is required.");
        if (email.isEmpty())     return RowResult.fail(prefix + "email is required.");
        if (roleStr.isEmpty())   return RowResult.fail(prefix + "role is required.");
        if (deptIdStr.isEmpty()) return RowResult.fail(prefix + "departmentId is required.");
        if (statusStr.isEmpty()) return RowResult.fail(prefix + "status is required.");

        // Role validation
        Role role;
        try {
            role = Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return RowResult.fail(prefix + "role '" + roleStr
                    + "' is invalid. Valid values: EMPLOYEE, MANAGER, HR.");
        }

        // UserStatus validation
        UserStatus userStatus;
        try {
            userStatus = UserStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return RowResult.fail(prefix + "status '" + statusStr
                    + "' is invalid. Valid values: ACTIVE, INACTIVE.");
        }

        // Department validation
        Integer departmentId;
        try {
            departmentId = Integer.parseInt(deptIdStr);
        } catch (NumberFormatException e) {
            return RowResult.fail(prefix + "departmentId '" + deptIdStr
                    + "' is not a valid integer.");
        }
        Optional<Department> deptOptional = departmentRepository.findById(departmentId);
        if (deptOptional.isEmpty()) {
            return RowResult.fail(prefix + "departmentId " + departmentId
                    + " does not exist.");
        }
        Department department = deptOptional.get();

        // Manager validation (optional — blank is allowed)
        User manager = null;
        if (!managerIdStr.isEmpty()) {
            Integer managerId;
            try {
                managerId = Integer.parseInt(managerIdStr);
            } catch (NumberFormatException e) {
                return RowResult.fail(prefix + "managerId '" + managerIdStr
                        + "' is not a valid integer.");
            }
            Optional<User> managerOptional = userRepository.findById(managerId);
            if (managerOptional.isEmpty()) {
                return RowResult.fail(prefix + "managerId " + managerId
                        + " does not exist.");
            }
            manager = managerOptional.get();
        }

        // Upsert by email
        Optional<User> existingOptional = userRepository.findByEmail(email);

        if (existingOptional.isPresent()) {
            // UPDATE path
            User existing = existingOptional.get();
            existing.setFirstName(firstName);
            existing.setLastName(lastName);
            existing.setRole(role);
            existing.setDepartment(department);
            existing.setManager(manager);
            existing.setStatus(userStatus);
            // Blank password on update = keep existing hash; non-blank = encode and update
            if (!password.isEmpty()) {
                existing.setPasswordHash(passwordEncoder.encode(password));
            }
            userRepository.save(existing);
            return RowResult.update();

        } else {
            // CREATE path — password required for new users
            if (password.isEmpty()) {
                return RowResult.fail(prefix
                        + "password is required when creating a new user.");
            }
            User newUser = new User();
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setEmail(email);
            newUser.setPasswordHash(passwordEncoder.encode(password));
            newUser.setRole(role);
            newUser.setDepartment(department);
            newUser.setManager(manager);
            newUser.setStatus(userStatus);
            userRepository.save(newUser);
            return RowResult.create();
        }
    }

    // -------------------------------------------------------------------------
    // BASELINE_METRIC row processor
    //
    // Expected columns (0-indexed, exactly 6):
    //   0=userId, 1=activityType, 2=activityDate, 3=activityValue, 4=unit, 5=notes
    //
    // notes (col 5) may be blank but the column must be present.
    // Always inserts a new ActivityLog row — no upsert.
    // -------------------------------------------------------------------------
    private RowResult processBaselineMetricRow(int lineNumber, String[] cols) {
        String prefix = "Row " + lineNumber + ": ";

        // Exact column count enforced
        if (cols.length != 6) {
            return RowResult.fail(prefix + "expected exactly 6 columns "
                    + "(userId,activityType,activityDate,activityValue,unit,notes)"
                    + " but found " + cols.length + ".");
        }

        String userIdStr       = cols[0].trim();
        String activityTypeStr = cols[1].trim();
        String activityDateStr = cols[2].trim();
        String activityValStr  = cols[3].trim();
        String unit            = cols[4].trim();
        String notes           = cols[5].trim();

        // Required field presence checks (notes is optional — blank allowed)
        if (userIdStr.isEmpty())       return RowResult.fail(prefix + "userId is required.");
        if (activityTypeStr.isEmpty()) return RowResult.fail(prefix + "activityType is required.");
        if (activityDateStr.isEmpty()) return RowResult.fail(prefix + "activityDate is required.");
        if (activityValStr.isEmpty())  return RowResult.fail(prefix + "activityValue is required.");
        if (unit.isEmpty())            return RowResult.fail(prefix + "unit is required.");

        // userId validation
        Integer userId;
        try {
            userId = Integer.parseInt(userIdStr);
        } catch (NumberFormatException e) {
            return RowResult.fail(prefix + "userId '" + userIdStr
                    + "' is not a valid integer.");
        }
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return RowResult.fail(prefix + "userId " + userId + " does not exist.");
        }
        User user = userOptional.get();

        // activityType validation
        ActivityType activityType;
        try {
            activityType = ActivityType.valueOf(activityTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return RowResult.fail(prefix + "activityType '" + activityTypeStr
                    + "' is invalid. Valid values: STEPS, WORKOUT, MEDITATION, WATER, SLEEP, OTHER.");
        }

        // activityDate validation
        java.time.LocalDate activityDate;
        try {
            activityDate = java.time.LocalDate.parse(activityDateStr);
        } catch (java.time.format.DateTimeParseException e) {
            return RowResult.fail(prefix + "activityDate '" + activityDateStr
                    + "' is not a valid date. Expected format: yyyy-MM-dd.");
        }

        // activityValue validation
        double activityValue;
        try {
            activityValue = Double.parseDouble(activityValStr);
        } catch (NumberFormatException e) {
            return RowResult.fail(prefix + "activityValue '" + activityValStr
                    + "' is not a valid number.");
        }
        if (activityValue <= 0) {
            return RowResult.fail(prefix + "activityValue must be greater than 0.");
        }

        // Insert new ActivityLog row
        ActivityLog log = new ActivityLog();
        log.setUser(user);
        log.setActivityType(activityType);
        log.setActivityDate(activityDate);
        log.setActivityValue(activityValue);
        log.setUnit(unit);
        log.setNotes(notes.isEmpty() ? null : notes);
        activityLogRepository.save(log);

        return RowResult.create();
    }

    // -------------------------------------------------------------------------
    // Private helper
    // -------------------------------------------------------------------------
    private BulkUploadResponseDTO mapToResponseDTO(BulkUpload upload) {
        BulkUploadResponseDTO dto = new BulkUploadResponseDTO();
        dto.setBulkUploadId(upload.getBulkUploadId());
        dto.setUploadedBy(upload.getUploadedBy().getUserId());
        dto.setUploadedByName(
                upload.getUploadedBy().getFirstName() + " "
                        + upload.getUploadedBy().getLastName());
        dto.setUploadType(upload.getUploadType());
        dto.setFileName(upload.getFileName());
        dto.setStatus(upload.getStatus());
        dto.setTotalRows(upload.getTotalRows());
        dto.setSuccessRows(upload.getSuccessRows());
        dto.setFailedRows(upload.getFailedRows());
        dto.setUploadedAt(upload.getUploadedAt());
        return dto;
    }

    // -------------------------------------------------------------------------
    // RowResult — value object carrying success/failure state for one CSV row.
    // Avoids throwing exceptions inside the row loop so one bad row never
    // stops the rest of the file from being processed.
    // -------------------------------------------------------------------------
    private static class RowResult {
        final boolean success;
        final boolean wasUpdate;
        final String error;

        private RowResult(boolean success, boolean wasUpdate, String error) {
            this.success   = success;
            this.wasUpdate = wasUpdate;
            this.error     = error;
        }

        static RowResult create() { return new RowResult(true, false, null); }
        static RowResult update() { return new RowResult(true, true,  null); }
        static RowResult fail(String error) { return new RowResult(false, false, error); }
    }
}
