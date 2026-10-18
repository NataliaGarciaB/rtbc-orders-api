package com.rtbc.orders.infrastructure.persistence;

import com.rtbc.orders.domain.OrderStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
class OrderEntity {

	@Id
	private UUID id;

	@Column(nullable = false, length = 120)
	private String customer;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "order_lines", joinColumns = @JoinColumn(name = "order_id"))
	@OrderColumn(name = "line_index")
	private List<OrderLineEmbeddable> lines = new ArrayList<>();

	protected OrderEntity() {
	}

	OrderEntity(UUID id, String customer, List<OrderLineEmbeddable> lines, OrderStatus status) {
		this.id = id;
		this.customer = customer;
		this.lines = new ArrayList<>(lines);
		this.status = status;
	}

	UUID id() {
		return id;
	}

	String customer() {
		return customer;
	}

	List<OrderLineEmbeddable> lines() {
		return lines;
	}

	OrderStatus status() {
		return status;
	}

	/**
	 * Cliente y productos son inmutables en el dominio: solo el estado cambia después de crear la orden.
	 */
	void changeStatus(OrderStatus status) {
		this.status = status;
	}
}
