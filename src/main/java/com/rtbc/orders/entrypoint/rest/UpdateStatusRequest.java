package com.rtbc.orders.entrypoint.rest;

import com.rtbc.orders.domain.OrderStatus;

record UpdateStatusRequest(OrderStatus estado) {
}
