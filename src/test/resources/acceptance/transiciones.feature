@transiciones
Feature: Transiciones de estado (PATCH /orders/{id}/status y DELETE /orders/{id})

  Background:
    * url baseUrl
    * def schemas = call read('classpath:acceptance/data/schemas.js')

  @obligatorio @RN-05
  Scenario: Intentar enviar una orden no pagada
    Given def orden = call read('classpath:acceptance/helpers/orden-en-estado.feature') { estado: 'CREADA' }
    And path 'orders', orden.orderId, 'status'
    And request { estado: 'ENVIADA' }
    When method patch
    Then status 400
    And match response == schemas.problema
    And match response.codigo == 'ORDEN_NO_PAGADA'
