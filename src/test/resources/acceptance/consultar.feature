@consultar
Feature: Consultar órdenes (GET /orders/{id})

  Background:
    * url baseUrl
    * def schemas = call read('classpath:acceptance/data/schemas.js')

  @obligatorio
  Scenario: Consultar una orden existente
    Given def existente = call read('classpath:acceptance/helpers/crear-orden.feature')
    And path 'orders', existente.orderId
    When method get
    Then status 200
    And match response == schemas.orden
    And match response == existente.order
