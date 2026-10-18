package com.rtbc.orders.entrypoint.rest;

import com.rtbc.orders.application.CancelOrder;
import com.rtbc.orders.application.ChangeOrderStatus;
import com.rtbc.orders.application.CreateOrder;
import com.rtbc.orders.application.GetOrder;
import com.rtbc.orders.application.ListOrders;
import com.rtbc.orders.domain.Order;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Endpoints de órdenes según docs/api/openapi.yaml. Solo traduce HTTP ↔ casos de uso.
 */
@RestController
@RequestMapping("/orders")
class OrderController {

	private final CreateOrder createOrder;
	private final GetOrder getOrder;
	private final ListOrders listOrders;
	private final ChangeOrderStatus changeOrderStatus;
	private final CancelOrder cancelOrder;

	OrderController(CreateOrder createOrder, GetOrder getOrder, ListOrders listOrders,
			ChangeOrderStatus changeOrderStatus, CancelOrder cancelOrder) {
		this.createOrder = createOrder;
		this.getOrder = getOrder;
		this.listOrders = listOrders;
		this.changeOrderStatus = changeOrderStatus;
		this.cancelOrder = cancelOrder;
	}

	@PostMapping
	ResponseEntity<OrderResponse> create(@RequestBody CreateOrderRequest request) {
		Order order = createOrder.execute(request.cliente(), request.toProductLines());
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(order.id())
				.toUri();
		return ResponseEntity.created(location).body(OrderResponse.from(order));
	}

	@GetMapping
	List<OrderResponse> list() {
		return listOrders.execute().stream().map(OrderResponse::from).toList();
	}

	@GetMapping("/{id}")
	OrderResponse get(@PathVariable UUID id) {
		return OrderResponse.from(getOrder.execute(id));
	}
}
