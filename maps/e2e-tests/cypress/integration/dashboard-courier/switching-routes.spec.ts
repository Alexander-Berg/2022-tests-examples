import * as sidebarKeyset from '../../../../src/translations/sidebar';
import selectors from '../../../src/constants/selectors';
import { courierNameRecord } from '../../../src/constants/couriers';

// @see https://testpalm.yandex-team.ru/courier/testcases/339
context('Route switch inside courier', () => {
  let mapId: string | undefined;

  before(() => {
    cy.preserveCookies();
    cy.yandexLogin('manager');
    cy.get(selectors.sidebar.menu.monitoringGroup).click();
    cy.get(selectors.sidebar.menu.dashboard).click();
  });

  before(() => {
    cy.get(selectors.content.dashboard.couriers.table.courierNames)
      .contains(courierNameRecord.gumba)
      .click();
  });

  it('the data of the first route should be displayed', function () {
    cy.get(selectors.content.courier.route.map.common.container).should('be.visible');

    cy.get(selectors.content.courier.route.map.common.map)
      .invoke('attr', 'id')
      .then(id => {
        mapId = id;
      });

    cy.get(selectors.content.courier.route.selector.root)
      .should('be.visible')
      .find(selectors.content.courier.route.selector.routeButton)
      .eq(0)
      .should('have.class', selectors.content.courier.route.selector.activeRouteButton.slice(1));
  });

  it('the data of the second route should be displayed', function () {
    cy.get(selectors.content.courier.route.selector.root)
      .find(selectors.content.courier.route.selector.routeButton)
      .eq(1)
      .click();

    cy.get(selectors.content.courier.route.map.common.container).should('be.visible');

    cy.get(selectors.content.courier.route.map.common.map)
      .invoke('attr', 'id')
      .then(id => {
        // id has changed => map is remounted
        expect(id).not.equal(mapId);
      });

    cy.get(selectors.content.courier.route.selector.root)
      .should('be.visible')
      .find(selectors.content.courier.route.selector.routeButton)
      .eq(1)
      .should('have.class', selectors.content.courier.route.selector.activeRouteButton.slice(1));
    cy.get(selectors.content.courier.route.selector.root)
      .should('be.visible')
      .find(selectors.content.courier.route.selector.routeButton)
      .eq(0)
      .should(
        'not.have.class',
        selectors.content.courier.route.selector.activeRouteButton.slice(1),
      );
  });

  it('should display dashboard after pressing "Back"', () => {
    const dashboard = sidebarKeyset.ru.monitoring_dashboard;
    cy.go('back');
    cy.url().should('include', 'dashboard');
    cy.get(selectors.sidebar.selectedItem).should('have.text', dashboard);
  });
});
