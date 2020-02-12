@regressionTest @promotionsShipping @shippingService
Feature: Shipping Service Level

  Background:
    Given I sign in to CM as admin user

  Scenario: Search shipping service level
    When I go to Promotions and Shipping
    And I click Search button in Shipping Service Levels tab
    Then Shipping Service Level Search Results should contain following service level codes
      | FedExExpress |
      | RM011        |

  Scenario Outline: Create, read, update and delete shipping service level
    When I go to Promotions and Shipping
    And I create shipping service level with following values
      | store   | shipping region  | carrier | name                  | property value |
      | <store> | <shippingRegion> | Fed Ex  | Test Shipping Service | 25             |
    Then I verify newly created shipping service level exists
      | <store> | <shippingRegion> |
    When I open the newly created shipping service level
    And I edit the shipping service level name to <UPDATED_NAME>
    And I click Search button in Shipping Service Levels tab
    Then Shipping Service Level Search Results should contain following service level names
      | <UPDATED_NAME> |
    When I delete the newly created shipping service level
      | <store> | <shippingRegion> |
    Then I verify shipping service level is deleted
      | <store> | <shippingRegion> |

    Examples:
      | UPDATED_NAME            | store       | shippingRegion |
      | Edited Shipping Service | SearchStore | USA            |

  Scenario Outline: Shipping Service Level codes must be unique
    Given I go to Promotions and Shipping
    And I create shipping service level with following values
      | store   | shipping region  | carrier | name                  | property value |
      | <store> | <shippingRegion> | Fed Ex  | Test Shipping Service | 10             |
    When I attempt to create a new shipping service level with the same code
    Then I should see following create shipping service level validation alert
      | A Shipping Service Level with the code you provided already exists. Enter another code. |
    When I delete the newly created shipping service level
      | <store> | <shippingRegion> |
    Then I can create a new shipping service level with the same code
    And I verify newly created shipping service level exists
      | <store> | <shippingRegion> |
    And I delete the newly created shipping service level
      | <store> | <shippingRegion> |

    Examples:
      | store       | shippingRegion |
      | SearchStore | USA            |
