package com.rtbc.orders.application;

import com.rtbc.orders.domain.DomainException;
import com.rtbc.orders.domain.ErrorCode;
import java.util.UUID;

public class OrderNotFoundException extends DomainException {

	public OrderNotFoundException(UUID id) {
		super(ErrorCode.ORDEN_NO_ENCONTRADA, "No existe la orden " + id);
	}
}
