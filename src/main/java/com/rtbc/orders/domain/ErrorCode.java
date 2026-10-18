package com.rtbc.orders.domain;

/**
 * Códigos estables de error expuestos en el contrato (ADR-009).
 */
public enum ErrorCode {
	VALIDACION,
	ORDEN_NO_PAGADA,
	TRANSICION_INVALIDA
}
