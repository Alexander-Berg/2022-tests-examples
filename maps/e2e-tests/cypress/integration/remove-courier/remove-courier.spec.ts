import moment from 'moment';
import urls from '../../../src/utils/urls';
import selectors from '../../../src/constants/selectors';

describe('Remove courier', () => {
  const courierPhone = '12345678910';
  const courierNumber = '10987654321';

  beforeEach(() => {
    cy.fixture('company-data').then(({ common }) => {
      const date = moment(new Date()).add(-2, 'days').format(urls.dashboard.dateFormat);

      const link = urls.dashboard.createLink(common.companyId, { date });

      cy.fixture('testData').then(() => {
        cy.yandexLogin('manager', { link });
      });
    });
  });

  it('Remove courier', () => {
    cy.fixture('testData').then(() => {
      cy.get(selectors.sidebar.menu.monitoringGroup).click();
      cy.get(selectors.sidebar.menu.couriers).click();
      const selectorToNewCourierLine = `${selectors.content.couriers.search.line}[data-test-anchor="${courierNumber}"]`;

      cy.get(`${selectorToNewCourierLine} ${selectors.content.couriers.search.lineRemove}`).click();
      cy.get(selectors.content.couriers.removeCourier.dialogContainer);
      cy.get(selectors.content.couriers.removeCourier.submitButton).click();
      cy.get(selectorToNewCourierLine).should('not.exist');
    });
  });

  it('Check removed courierName in dashboard', () => {
    cy.get(selectors.content.dashboard.couriers.firstRoute)
      .invoke('text')
      .should('not.eq', courierNumber);
  });

  it('Check removed courierName in route order', () => {
    cy.get(selectors.content.dashboard.couriers.firstRouteFirstOrder).click();
    cy.get(selectors.content.dashboard.orders.modal.courierNumber)
      .invoke('text')
      .should('not.eq', `${courierNumber} (${courierNumber})`);
    cy.get(selectors.content.dashboard.orders.modal.chatButton).should('not.exist');
  });

  it('Check removed courierName in courier page', () => {
    cy.get(selectors.content.dashboard.couriers.firstRoute).click();
    cy.get(selectors.content.courier.name).invoke('text').should('not.eq', courierNumber);
    cy.get(selectors.sidebar.menu.courierNameMenuItem).should('exist');
    cy.get(selectors.sidebar.menu.courierNameMenuItem)
      .invoke('text')
      .should('not.eq', courierNumber);
    cy.get(selectors.content.courier.chatButton).should('not.exist');
    cy.get(selectors.content.courier.ordersTable);
    cy.get(selectors.content.courier.details.root).should('not.exist');
  });

  it('Add courier with removed number', () => {
    cy.fixture('testData').then(() => {
      cy.get(selectors.sidebar.menu.monitoringGroup).click();
      cy.get(selectors.sidebar.menu.couriers).click();
      cy.get(selectors.content.couriers.newCourier.phone).type(courierPhone);
      cy.get(selectors.content.couriers.newCourier.number).type(courierNumber);
      cy.get(selectors.content.couriers.newCourier.submitButton).click();
      cy.wait(500);

      const selectorToNewCourierLine = `${selectors.content.couriers.search.line}[data-test-anchor="${courierNumber}"]`;
      cy.get(`${selectorToNewCourierLine} ${selectors.content.couriers.search.lineName}`)
        .invoke('text')
        .should('eq', courierNumber);
      cy.get(`${selectorToNewCourierLine} ${selectors.content.couriers.search.lineNumber}`)
        .invoke('text')
        .should('eq', courierNumber);
    });
  });
});
