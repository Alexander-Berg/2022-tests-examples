import * as commonKeyset from '../../../../../src/translations/common';
import selectors from '../../../../src/constants/selectors';
import urls from '../../../../src/utils/urls';
import moment from 'moment';

// @see https://testpalm.yandex-team.ru/testcase/courier-322#
describe('Courier - Search', function () {
  const courierNumber = '79665554433';
  const courierName = 'courier_login';

  before(() => {
    cy.fixture('company-data').then(({ common }) => {
      const date = moment(new Date()).add(-2, 'days').format(urls.dashboard.dateFormat);
      const link = urls.dashboard.createLink(common.companyId, { date });
      cy.yandexLogin('manager', { link });
    });
    cy.get(selectors.sidebar.menu.monitoringGroup).click();
    cy.get(selectors.sidebar.menu.couriers).click();
  });

  it('should show list of couriers which number or name contains 7', () => {
    cy.get(selectors.content.couriers.search.input).type('7');
    cy.get(selectors.content.couriers.search.line).each($el => {
      cy.wrap($el).invoke('text').should('contain', '7');
    });
  });

  it('should show found courier number when search by using numbers', () => {
    cy.get(selectors.content.couriers.search.input).type(courierNumber);
    cy.get(selectors.content.couriers.search.lineNumber).invoke('text').should('eq', courierNumber);
  });

  it('should show found courier name when search by using letters', () => {
    cy.get(selectors.content.couriers.search.input).type(courierName);
    cy.get(selectors.content.couriers.search.lineName).invoke('text').should('eq', courierName);
  });

  it('should show empty list of couriers when no couriers were found', () => {
    cy.get(selectors.content.couriers.search.input).type('No couriers with this name or number');
    cy.get(selectors.content.couriers.search.message)
      .invoke('text')
      .should('eq', commonKeyset.ru.search_notFound);
    cy.get(selectors.content.couriers.search.lineNumber).should('have.length', 0);
  });

  it('should show empty list of couriers when spaces were entered into search input', () => {
    cy.get(selectors.content.couriers.search.input).type('  ');
    cy.get(selectors.content.couriers.search.message)
      .invoke('text')
      .should('eq', commonKeyset.ru.search_notFound);
    cy.get(selectors.content.couriers.search.lineNumber).should('have.length', 0);
  });

  afterEach(() => {
    cy.get(selectors.content.couriers.search.cancel).click();
  });
});

// testpalm.yandex-team.ru/courier/testcases/452

describe('Create courier', () => {
  const courierPhone = '79990002222';
  const courierNumber = '7776690';

  it('Add button disabled', () => {
    cy.get(selectors.content.couriers.newCourier.submitButton).should('be.disabled');
  });

  it('Inputs are cleared after clicking on the cross ', () => {
    cy.get(selectors.content.couriers.newCourier.phone).type(courierPhone);
    cy.get(selectors.content.couriers.newCourier.number).type(courierNumber);

    cy.get(selectors.content.couriers.newCourier.submitButton).should('not.be.disabled');

    cy.get(selectors.content.couriers.newCourier.numberCross).click();
    cy.get(selectors.content.couriers.newCourier.phoneCross).click();

    cy.get(selectors.content.couriers.newCourier.submitButton).should('be.disabled');
    cy.get(selectors.content.couriers.newCourier.phone).invoke('val').should('be.empty');
    cy.get(selectors.content.couriers.newCourier.number).invoke('val').should('be.empty');
  });

  it('Add new courier', () => {
    cy.get(selectors.content.couriers.newCourier.phone).type(courierPhone);
    cy.get(selectors.content.couriers.newCourier.number).type(courierNumber);
    cy.get(selectors.content.couriers.newCourier.submitButton).click();

    const selectorToNewCourierLine = `${selectors.content.couriers.search.line}[data-test-anchor="${courierNumber}"]`;

    cy.get(`${selectorToNewCourierLine} ${selectors.content.couriers.search.lineName}`)
      .invoke('text')
      .should('eq', courierNumber);

    cy.get(`${selectorToNewCourierLine} ${selectors.content.couriers.search.lineNumber}`)
      .invoke('text')
      .should('eq', courierNumber);

    cy.get(`${selectorToNewCourierLine} ${selectors.content.couriers.search.lineSms}`)
      .invoke('val')
      .should('eq', 'true');
  });

  it('Add button disables when you enter an existing login', () => {
    cy.get(selectors.content.couriers.newCourier.phone).type(courierPhone);
    cy.get(selectors.content.couriers.newCourier.number).type(courierNumber);

    cy.get(selectors.content.couriers.newCourier.submitButton).should('be.disabled');
  });

  after(() => {
    const selectorToNewCourierLine = `${selectors.content.couriers.search.line}[data-test-anchor="${courierNumber}"]`;

    cy.get(`${selectorToNewCourierLine} ${selectors.content.couriers.search.lineRemove}`).click();
    cy.waitForElement(selectors.content.couriers.removeCourier.dialogContainer);
    cy.get(selectors.content.couriers.removeCourier.submitButton).click();
  });
});
