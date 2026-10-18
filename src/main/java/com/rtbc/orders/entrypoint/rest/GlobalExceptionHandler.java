package com.rtbc.orders.entrypoint.rest;

import com.rtbc.orders.domain.DomainException;
import com.rtbc.orders.domain.ErrorCode;
import com.rtbc.orders.domain.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce errores a ProblemDetail (RFC 9457) con {@code codigo} estable (ADR-005, ADR-009).
 */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	static final String CODE = "codigo";
	static final String ERRORS = "errores";

	@ExceptionHandler(DomainException.class)
	ProblemDetail handleDomain(DomainException e, HttpServletRequest request) {
		ProblemDetail problem = problem(statusOf(e.code()), e.getMessage(), e.code(), request);
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
				ErrorCode.TRANSICION_INVALIDA, request);
	}

	private static HttpStatus statusOf(ErrorCode code) {
		return switch (code) {
			case VALIDACION, ORDEN_NO_PAGADA -> HttpStatus.BAD_REQUEST;
			case ORDEN_NO_ENCONTRADA -> HttpStatus.NOT_FOUND;
			case TRANSICION_INVALIDA -> HttpStatus.CONFLICT;
		};
	}

	private static ProblemDetail problem(HttpStatus status, String detail, ErrorCode code,
			HttpServletRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setInstance(URI.create(request.getRequestURI()));
		problem.setProperty(CODE, code.name());
		return problem;
	}
}
