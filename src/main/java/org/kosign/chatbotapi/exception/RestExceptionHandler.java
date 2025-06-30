package org.kosign.chatbotapi.exception;



import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import org.kosign.chatbotapi.components.common.api.ApiResponse;
import org.kosign.chatbotapi.components.common.api.ApiStatus;
import org.kosign.chatbotapi.components.common.api.Common;
import org.kosign.chatbotapi.components.common.api.EmptyJsonResponse;
import org.kosign.chatbotapi.logging.AppLogManager;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@Component
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return buildResponseEntity(new ApiStatus(BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgumentException(IllegalArgumentException ex) {
        AppLogManager.error(ex);

        return buildResponseEntity(new ApiStatus(BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public Object handleTimeoutException(IllegalStateException ex) {
        AppLogManager.error(ex);

        if(ex.getMessage().contains("Timeout on blocking")){
            return buildResponseEntity(new ApiStatus(HttpStatus.REQUEST_TIMEOUT.value(), "Request timeout"));
        }

        return buildResponseEntity(new ApiStatus(BAD_REQUEST.value(), "Unexpected error occurred"));
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return buildResponseEntity(new ApiStatus(BAD_REQUEST.value(), ex.getMessage()));
    }





//    @ExceptionHandler(MaxUploadSizeExceededException.class)
//    public ResponseEntity<Object> handleMaxSizeException(MaxUploadSizeExceededException ex) {
//        return buildResponseEntity(new ApiStatus(BAD_REQUEST.value(), ex.getMessage()));
//    }

    /**
     * Handle HttpMediaTypeNotSupportedException. This one triggers when JSON is invalid as well.
     *
     * @param ex      HttpMediaTypeNotSupportedException
     * @param headers HttpHeaders
     * @param status  HttpStatus
     * @param request WebRequest
     * @return the ApiError object
     */
    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        AppLogManager.error(ex);

        StringBuilder builder = new StringBuilder();

        builder.append(ex.getContentType());
        builder.append(" media type is not supported. Supported media types are ");

        ex.getSupportedMediaTypes().forEach(t -> builder.append(t).append(", "));

        return buildResponseEntity(new ApiStatus(status.value(),builder.toString()));
    }

    /**
     * Handle MethodArgumentNotValidException. Triggered when an object fails @Valid validation.
     *
     * @param ex      the MethodArgumentNotValidException that is thrown when @Valid validation fails
     * @param headers HttpHeaders
     * @param status  HttpStatus
     * @param request WebRequest
     * @return the ApiError object
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        AppLogManager.error(ex);

        StringBuilder sb = new StringBuilder();
        for (var error : ex.getBindingResult().getFieldErrors()) {
            sb.append(error.getField()).append(": ").append(error.getDefaultMessage());
            break;
        }

        return buildResponseEntity(new ApiStatus(status.value(), sb.toString()));
    }

    /**
     * Handles jakarta.validation.ConstraintViolationException. Thrown when @Validated fails.
     *
     * @param ex the ConstraintViolationException
     * @return the ApiError object
     */
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<Object> handleConstraintViolation(
            ConstraintViolationException ex) {

        AppLogManager.error(ex);

//        String errorMessage = MessageHelper.getMessage("Validation.error",MessageHelper.getLocale(requests));
//
//        apiError.setMessage(ex.getMessage());
//        apiError.addValidationErrors(ex.getConstraintViolations());

        return buildResponseEntity(new ApiStatus(BAD_REQUEST.value(),ex.getMessage()));
    }


    /**
     * Handles EntityNotFoundException. Created to encapsulate errors with more detail than javax.persistence.EntityNotFoundException.
     *
     * @param ex the EntityNotFoundException
     * @return the ApiError object
     */
    @ExceptionHandler(EntityNotFoundException.class)
    protected ResponseEntity<Object> handleEntityNotFound(
            EntityNotFoundException ex) {

        AppLogManager.error(ex);

        return buildResponseEntity(new ApiStatus(NOT_FOUND.value(), ex.getMessage()));

    }

    @ExceptionHandler(ValidationException.class)
    protected ResponseEntity<Object> handleValidationException(
            ValidationException ex) {

        AppLogManager.error(ex);
        return buildResponseEntity(new ApiStatus(BAD_REQUEST.value(),ex.getMessage()));

    }


    /**
     * Handle HttpMessageNotReadableException. Happens when request JSON is malformed.
     *
     * @param ex      HttpMessageNotReadableException
     * @param headers HttpHeaders
     * @param status  HttpStatus
     * @param request WebRequest
     * @return the ApiError object
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        AppLogManager.error(ex);

//        String error = "Malformed JSON request";
//        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, error, ex);

        return buildResponseEntity(new ApiStatus(status.value(),"Malformed JSON request"));
    }

    /**
     * Handle HttpMessageNotWritableException.
     *
     * @param ex      HttpMessageNotWritableException
     * @param headers HttpHeaders
     * @param status  HttpStatus
     * @param request WebRequest
     * @return the ApiError object
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotWritable(HttpMessageNotWritableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        AppLogManager.error(ex);
        String error = "Error writing JSON output";

        return buildResponseEntity(new ApiStatus(status.value(),error));
    }

//    @Override
//    protected ResponseEntity<Object> handleAsyncRequestTimeoutException(AsyncRequestTimeoutException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
//        return super.handleAsyncRequestTimeoutException(ex, headers, status, request);
//    }


    /**
     * Handle MissingServletRequestParameterException. Triggered when a 'required' request parameter is missing.
     *
     * @param ex      MissingServletRequestParameterException
     * @param headers HttpHeaders
     * @param status  HttpStatusCode
     * @param request WebRequest
     * @return the ApiError object
     */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String error = ex.getParameterName() + " parameter is missing";

        return buildResponseEntity(new ApiStatus(status.value(),error));
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
                AppLogManager.error(ex);
//        System.out.println(ex);
//
//        apiError.setMessage(String.format("Could not find the %s method for URL %s", ex.getHttpMethod(), ex.getRequestURL()));
//        apiError.setDebugMessage(ExceptionUtils.getStackTrace(ex));

        return buildResponseEntity(new ApiStatus(status.value(),String.format("Could not find the %s method for URL %s", ex.getHttpMethod(), ex.getRequestURL())));
    }

        /**
     * Handle DataIntegrityViolationException, inspects the cause for different DB causes.
     *
     * @param ex the DataIntegrityViolationException
     * @return the ApiError object
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        AppLogManager.error(ex);

        if (ex.getCause() instanceof ConstraintViolationException) {
            return buildResponseEntity(new ApiStatus(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage()));
        }

        return buildResponseEntity(new ApiStatus(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error"));

    }



    /**
     * Handle Exception, handle generic Exception.class
     *
     * @param ex the Exception
     * @return the ApiError object
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<Object> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {

        AppLogManager.error(ex);
//        System.out.println(ex);
//        ApiError apiError = new ApiError(BAD_REQUEST);
//
//        apiError.setMessage(String.format("The parameter '%s' of value '%s' could not be converted to type '%s'", ex.getName(), ex.getValue(), ex.getRequiredType().getSimpleName()));
//        apiError.setDebugMessage(ExceptionUtils.getStackTrace(ex));

        return buildResponseEntity(new ApiStatus(BAD_REQUEST.value(), String.format("The parameter '%s' of value '%s' could not be converted to type '%s'", ex.getName(), ex.getValue(), ex.getRequiredType().getSimpleName()) ));

    }

    /**
     * Handle HandleBusinessException
     *
     * @param ex BusinessException
     * @return the ApiError object
     */


    /**
     * Handle handleThrowable
     *
     * @param ex      Throwable
     * @return the ApiError object
     */
    @ExceptionHandler(Throwable.class)
    protected ResponseEntity<Object> handleThrowable(Throwable ex) {
        AppLogManager.error(ex);
//        apiError.setMessage(ex.getMessage());
//        apiError.setDebugMessage(ExceptionUtils.getStackTrace(ex));

        return buildResponseEntity(new ApiStatus(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error"));

    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        AppLogManager.error(ex);
        return buildResponseEntity(new ApiStatus(statusCode.value(), "Internal Server Error"));
    }

    public ResponseEntity<Object> buildResponseEntity(ApiStatus apiStatus) {
        ApiResponse<Object> apiResponse = new ApiResponse<>(apiStatus, new EmptyJsonResponse());

        return new ResponseEntity<>(apiResponse, HttpStatusCode.valueOf(apiStatus.getCode()));
    }

    public ResponseEntity<Object> buildResponseEntity(ApiStatus apiStatus, Object data, int httpCode, Common common) {
        ApiResponse<Object> apiResponse = new ApiResponse<>(apiStatus, data == null ? new EmptyJsonResponse() : data, common);

        return new ResponseEntity<>(apiResponse, HttpStatusCode.valueOf(httpCode));
    }
    public ResponseEntity<Object> buildResponseEntity(ApiStatus apiStatus, Object data, int httpCode) {
        ApiResponse<Object> apiResponse = new ApiResponse<>(apiStatus, data == null ? new EmptyJsonResponse() : data);

        return new ResponseEntity<>(apiResponse, HttpStatusCode.valueOf(httpCode));
    }
}
