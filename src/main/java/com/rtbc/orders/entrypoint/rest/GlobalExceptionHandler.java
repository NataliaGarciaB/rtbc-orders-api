package com.rtbc.orders.entrypoint.rest;

import com.rtbc.orders.domain.DomainException;
import com.rtbc.orders.domain.ErrorCode;
import com.rtbc.orders.domain.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;

/**
 * Traduce errores a ProblemDetail (RFC 9457) con {@code codigo} estable (ADR-005, ADR-009).
 */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	static final String CODE = "codigo";
	static final String ERRORS = "errores";
	private static final String INVALID_REQUEST = "La solicitud contiene errores de validación";

	@ExceptionHandler(DomainException.class)
	ProblemDetail handleDomain(DomainException e, HttpServletRequest request) {
		ProblemDetail problem = problem(statusOf(e.code()), e.getMessage(), e.code(), request.getRequestURI());
		if (e instanceof ValidationException validation) {
			problem.setProperty(ERRORS, List.of(new FieldErrorResponse(validation.field(), validation.getMessage())));
		}
		return problem;
	}

	/**
	 * R08 / ADR-016: otra solicitud modificó la orden entre la lectura y la escritura.
	 */
	@ExceptionHandler(OptimisticLockingFailureException.class)
	ProblemDetail handleConcurrentModification(HttpServletRequest request) {
		return problem(HttpStatus.CONFLICT,
				"La orden fue modificada por otra solicitud; consulte su estado e intente de nuevo",
				ErrorCode.TRANSICION_INVALIDA, request.getRequestURI());
	}

	/**
	 * Bean Validation de los DTOs: se reportan todos los campos inválidos a la vez.
	 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldErrorResponse> errors = e.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
				.toList();
		return validationError(INVALID_REQUEST, errors, request);
	}

	/**
	 * JSON mal formado, body ausente, tipo incorrecto o valor de enum desconocido (p. ej. estado "PERDIDA").
	 */
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldErrorResponse> errors = e.getMostSpecificCause() instanceof JacksonException jackson
				&& !jackson.getPath().isEmpty()
						? List.of(new FieldErrorResponse(pathOf(jackson), "valor no permitido"))
						: List.of();
		return validationError("El cuerpo de la solicitud no es un JSON válido o tiene valores no permitidos",
				errors, request);
	}

	/**
	 * Parámetro de ruta con formato inválido, p. ej. un id que no es UUID.
	 */
	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException e, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		String field = e instanceof MethodArgumentTypeMismatchException mismatch ? mismatch.getName() : e.getPropertyName();
		return validationError("El parámetro " + field + " tiene un formato inválido",
				List.of(new FieldErrorResponse(field, "formato inválido")), request);
	}

	private ResponseEntity<Object> validationError(String detail, List<FieldErrorResponse> errors,
			WebRequest request) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, detail, ErrorCode.VALIDACION, uriOf(request));
		problem.setProperty(ERRORS, errors);
		return ResponseEntity.badRequest().body(problem);
	}

	private static String pathOf(JacksonException e) {
		StringBuilder path = new StringBuilder();
		for (JacksonException.Reference reference : e.getPath()) {
			if (reference.getPropertyName() != null) {
				path.append(path.isEmpty() ? "" : ".").append(reference.getPropertyName());
			} else if (reference.getIndex() >= 0) {
				path.append('[').append(reference.getIndex()).append(']');
			}
		}
		return path.toString();
	}

	private static String uriOf(WebRequest request) {
		return ((ServletWebRequest) request).getRequest().getRequestURI();
	}

	private static HttpStatus statusOf(ErrorCode code) {
		return switch (code) {
			case VALIDACION, ORDEN_NO_PAGADA -> HttpStatus.BAD_REQUEST;
			case ORDEN_NO_ENCONTRADA -> HttpStatus.NOT_FOUND;
			case TRANSICION_INVALIDA -> HttpStatus.CONFLICT;
		};
	}

	private static ProblemDetail problem(HttpStatus status, String detail, ErrorCode code, String uri) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setInstance(URI.create(uri));
		problem.setProperty(CODE, code.name());
		return problem;
	}
}
