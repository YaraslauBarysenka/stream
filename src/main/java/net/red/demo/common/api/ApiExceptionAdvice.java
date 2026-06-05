package net.red.demo.common.api;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.red.demo.common.exception.StreamBadRequestException;
import net.red.demo.remote.exception.StreamProviderException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionAdvice extends ResponseEntityExceptionHandler {
    private final ObjectMapper objectMapper;

    /**
     * Needs to convert body param to the {@link ErrorResponseDto} object according to REST API spec</a>.
     *
     * @param body the body to use for the response
     * @param headers the headers to use for the response
     * @param statusCode the status code to use for the response
     * @param request the current request
     * @return the {@link ErrorResponseDto} object
     */
    @Override
    protected @Nonnull ResponseEntity<Object> createResponseEntity(@Nullable Object body,
                                                                   @Nonnull HttpHeaders headers,
                                                                   @Nonnull HttpStatusCode statusCode,
                                                                   @Nonnull WebRequest request) {
        return super.createResponseEntity(createErrorDetails(body), headers, statusCode, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(@Nonnull MethodArgumentNotValidException ex,
                                                                  @Nonnull HttpHeaders headers,
                                                                  @Nonnull HttpStatusCode status,
                                                                  @Nonnull WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                Objects.requireNonNull(ex.getBody().getDetail()));
        ex.getBindingResult().getFieldErrors()
                .forEach(fieldError ->  problemDetail.setProperty(fieldError.getField(), fieldError.getDefaultMessage()));

        return super.createResponseEntity(createErrorDetails(problemDetail), headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponseDto handleInternalError(Throwable ex) {
        log.error("Unexpected error", ex);
        return new ErrorResponseDto(ex.getMessage());
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class,
            PropertyReferenceException.class,
            ConstraintViolationException.class,
            EntityNotFoundException.class,
            StreamBadRequestException.class,
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDto handleBadRequest(Throwable ex) {
        log.info("Bad request received. Exception {}, message {}", ex.getClass().getSimpleName(), ex.getMessage());
        return new ErrorResponseDto(ex.getMessage());
    }

    @ExceptionHandler({StreamProviderException.class})
    public ResponseEntity<ErrorResponseDto> handleProviderException(StreamProviderException ex) {
        FeignException feignException = ExceptionUtils.throwableOfType(ex, FeignException.class);
        return Optional.ofNullable(feignException)
                .map(fex -> ResponseEntity.badRequest().body(handleFeignException(fex)))
                .orElseGet(() -> ResponseEntity.internalServerError().body(handleInternalError(ex)));
    }

    @ExceptionHandler({FeignException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDto handleFeignException(FeignException ex) {
        if (HttpStatus.resolve(ex.status()) == null) {
            log.error(ex.getMessage(), ex);
            return new ErrorResponseDto(ex.getMessage());
        }
        log.error("Unexpected external feign error", ex);
        return ex.responseBody()
                .flatMap(body -> getErrorDetails(body, ex))
                .orElse(new ErrorResponseDto(ex.getMessage()));
    }

    private Optional<ErrorResponseDto> getErrorDetails(ByteBuffer body, FeignException ex) {
        try {
            ErrorResponseDto errorResponseDto = objectMapper.readValue(body.array(), ErrorResponseDto.class);
            errorResponseDto.setErrorDescription(ex.getMessage().replace(ex.contentUTF8(),
                    StringUtils.defaultString(errorResponseDto.getErrorDescription())));
            return Optional.of(errorResponseDto);
        } catch (IOException e) {
            log.warn("Deserialization ErrorDetails error", e);
        }
        return Optional.empty();
    }

    private @Nonnull ErrorResponseDto createErrorDetails(Object body) {
        String err = (body instanceof ProblemDetail problem) ? toErrorDescription(problem) :
                Optional.ofNullable(body).map(Object::toString).orElse(StringUtils.EMPTY);
        return new ErrorResponseDto(err);
    }

    private String toErrorDescription(ProblemDetail problem) {
        if (problem.getDetail() == null) {
            return null;
        }
        if (MapUtils.isEmpty(problem.getProperties())) {
            return problem.getDetail();
        }
        return MessageFormat.format("{0} {1}", problem.getDetail(), problem.getProperties());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorResponseDto {
        private String errorDescription;
    }
}