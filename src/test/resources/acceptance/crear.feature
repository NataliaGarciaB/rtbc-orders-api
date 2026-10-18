@crear
Feature: Crear orden (POST /orders)

  Background:
    * url baseUrl
    * def schemas = call read('classpath:acceptance/data/schemas.js')

  @obligatorio
  Scenario: Crear una orden válida
    Given path 'orders'
    And request read('classpath:acceptance/data/orden-valida.json')
    When method post
    Then status 201
    And match response == schemas.orden
    And match each response.productos == schemas.producto
    And match response.estado == 'CREADA'
    And match response.total == 200000
    And match karate.response.header('Location') contains '/orders/' + response.id
