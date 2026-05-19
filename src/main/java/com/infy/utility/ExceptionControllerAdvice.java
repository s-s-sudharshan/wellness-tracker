package com.infy.utility;

import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.infy.exception.WellnessTrackerException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ExceptionControllerAdvice
{

    private static final Log LOGGER = LogFactory.getLog(ExceptionControllerAdvice.class);

    @Autowired
    private Environment environment;
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorInfo> authenticationExceptionHandler(
            AuthenticationException exception) {
        LOGGER.warn("AuthenticationException: " + exception.getMessage());
        ErrorInfo errorInfo = new ErrorInfo();
        errorInfo.setErrorCode(HttpStatus.UNAUTHORIZED.value());
        errorInfo.setErrorMessage(
                environment.getProperty("General.UNAUTHORIZED_MESSAGE"));
        return new ResponseEntity<>(errorInfo, HttpStatus.UNAUTHORIZED);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorInfo> accessDeniedExceptionHandler(
            AccessDeniedException exception) {
        LOGGER.warn("AccessDeniedException: " + exception.getMessage());
        ErrorInfo errorInfo = new ErrorInfo();
        errorInfo.setErrorCode(HttpStatus.FORBIDDEN.value());
        errorInfo.setErrorMessage(
                environment.getProperty("General.ACCESS_DENIED_MESSAGE"));
        return new ResponseEntity<>(errorInfo, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(WellnessTrackerException.class)
    public ResponseEntity<ErrorInfo> wellnessTrackerExceptionHandler(
            WellnessTrackerException exception) {
        LOGGER.error(exception.getMessage(), exception);
        ErrorInfo errorInfo = new ErrorInfo();
        errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
        errorInfo.setErrorMessage(environment.getProperty(exception.getMessage()));
        return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorInfo> generalExceptionHandler(Exception exception) {
		LOGGER.error(exception.getMessage(), exception);
		ErrorInfo errorInfo = new ErrorInfo();
		errorInfo.setErrorCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
		errorInfo.setErrorMessage(environment.getProperty("General.EXCEPTION_MESSAGE"));
		return new ResponseEntity<>(errorInfo, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ErrorInfo> validatorExceptionHandler(Exception exception) {
		LOGGER.error(exception.getMessage(), exception);
		String errorMsg;
		if (exception instanceof MethodArgumentNotValidException) {
		    MethodArgumentNotValidException manvException = (MethodArgumentNotValidException) exception;
		    errorMsg = manvException.getBindingResult()
					    .getAllErrors()
					    .stream()
					    .map(ObjectError::getDefaultMessage)
					    .collect(Collectors.joining(", "));
		} else {
		    ConstraintViolationException cvException = (ConstraintViolationException) exception;
		    errorMsg = cvException.getConstraintViolations()
					  .stream()
					  .map(ConstraintViolation::getMessage)
					  .collect(Collectors.joining(", "));
		}
		ErrorInfo errorInfo = new ErrorInfo();
		errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
		errorInfo.setErrorMessage(errorMsg);
		return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
    }
}
