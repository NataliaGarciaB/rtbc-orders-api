package com.rtbc.orders.entrypoint.rest;

import com.rtbc.orders.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

record UpdateStatusRequest(@NotNull(message = "es obligatorio") OrderStatus estado) {
}
