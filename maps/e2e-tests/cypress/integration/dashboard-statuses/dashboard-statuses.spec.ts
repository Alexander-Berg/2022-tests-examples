import * as importKeyset from '../../../../src/translations/import';
import * as ordersKeyset from '../../../../src/translations/orders';
import * as courierMapKeyset from '../../../../src/translations/courier-map';
import * as commonKeyset from '../../../../src/translations/common';
import selectors from '../../../src/constants/selectors';
import parseISO from 'date-fns/parseISO';
import dateFnsFormat from '../../../src/utils/date-fns-format';
import time from '../../../src/utils/time';

type FilterDataT = {
  text: string;
  urlParam: string;
};

type FiltersT = {
  Cancelled: FilterDataT;
  'Finished late': FilterDataT;
  Finished: FilterDataT;
  Sequenced: FilterDataT;
  Delays: FilterDataT;
  Unsequenced: FilterDataT;
};

const filters: FiltersT = {
  Cancelled: { text: commonKeyset.ru.tagsInput_tag_cancelled, urlParam: 'filter=is%3Acancelled' },
  'Finished late': {
    text: commonKeyset.ru['tagsInput_tag_untimely-finished'],
    urlParam: 'filter=is%3Auntimely-finished',
  },
  Finished: { text: commonKeyset.ru.tagsInput_tag_finished, urlParam: 'filter=is%3Afinished' },
  Sequenced: { text: commonKeyset.ru.tagsInput_tag_sequenced, urlParam: 'filter=is%3Asequenced' },
  Unsequenced: {
    text: commonKeyset.ru.tagsInput_tag_unsequenced,
    urlParam: 'filter=is%3Aunsequenced',
  },
  Delays: { text: commonKeyset.ru.tagsInput_tag_late, urlParam: 'filter=is%3Alate' },
};

const assertValidationDateInput = (): Mocha.Test => {
  return it('Validation of the date in the input', () => {
    const date = parseISO(time.TIME_TODAY);
    const dateStr = dateFnsFormat(date, 'dd MMM yyyy').replace('.', '');
    cy.get(selectors.content.orders.view);
    cy.get(selectors.content.orders.dateFilter.input).invoke('text').should('eq', dateStr);
  });
};

const assertOrdersMenuHighlighted = (): Mocha.Test => {
  return it('The "Orders" menu is highlighted in the sidebar', () => {
    cy.get(selectors.sidebar.selectedItem)
      .invoke('text')
      .should('eq', courierMapKeyset.ru.tableColumn_ordersCount);
  });
};

const assertCountOrdersInList = (number: number): Mocha.Test => {
  return it('Validation of the number of orders in the list', () => {
    cy.get(selectors.content.orders.view);
    cy.get(selectors.content.orders.tableRows).should('have.length', number);
  });
};

const assertFilterPlate = (filterName: string): Mocha.Test => {
  return it(`There is a filter plate "${filterName}"`, () => {
    cy.get(selectors.content.orders.filter.tag)
      .invoke('text')
      .should('eq', filters[filterName as keyof FiltersT].text);
  });
};

const assertFilterTagUrl = (filterName: string): Mocha.Test => {
  return it(`There is a filter tag in the url "${filterName}"`, () => {
    cy.url().should('include', filters[filterName as keyof FiltersT].urlParam);
  });
};

const assertClearInputFilter = (): Mocha.Test => {
  return it('After clean input filter display all orders', () => {
    cy.get(selectors.content.orders.filter.closeButton).click();
    cy.wait(1500);
    cy.get(selectors.content.orders.view);
    cy.get(selectors.content.orders.tableRows).should('have.length', 16);
  });
};

// @see https://testpalm.yandex-team.ru/courier/testsuite/5fda1df8c79113008cf8a0db?testcase=299
const roles: Array<keyof AccountsT> = ['manager', 'admin'];

context('Dashboard - statuses.', () => {
  roles.forEach(role => {
    describe(`For ${role} role`, () => {
      beforeEach(() => {
        cy.preserveCookies();
      });
      before(() => {
        cy.yandexLogin(role);
        cy.get(selectors.content.dashboard.view);
        cy.get(selectors.sidebar.menu.monitoringGroup).click();
      });

      // @see https://testpalm.yandex-team.ru/testcase/courier-61
      describe('Cancelled.', () => {
        before(() => {
          cy.get(selectors.content.dashboard.orders.legend.cancelled).click();
          cy.wait(200);
          cy.get(selectors.modal.paranja).should('not.exist');
        });

        assertOrdersMenuHighlighted();
        assertCountOrdersInList(2);
        assertFilterPlate(commonKeyset.en['tagsInput_tag_cancelled']);
        assertFilterTagUrl(commonKeyset.en['tagsInput_tag_cancelled']);
        assertValidationDateInput();

        it('Order statuses "Cancelled/ could not get through"', () => {
          const statusesRegExp = new RegExp(
            `${ordersKeyset.ru.status_cancelled}|${ordersKeyset.ru.status_postponed}`,
          );
          cy.get(selectors.content.orders.view);
          cy.get(selectors.content.orders.statusCell)
            .invoke('text')
            .should('match', statusesRegExp);
        });
        assertClearInputFilter();
        after(() => {
          cy.get(selectors.sidebar.menu.dashboard).click();
          cy.wait(300);
        });
      });

      // @see https://testpalm.yandex-team.ru/testcase/courier-62
      describe('Finished late', () => {
        before(() => {
          cy.get(selectors.content.dashboard.orders.legend.finishedLate).click();
          cy.wait(200);
          cy.get(selectors.modal.paranja).should('not.exist');
        });

        assertOrdersMenuHighlighted();
        assertCountOrdersInList(1);
        assertFilterPlate('Finished late');
        assertFilterTagUrl('Finished late');
        assertValidationDateInput();
        assertClearInputFilter();
        after(() => {
          cy.get(selectors.sidebar.menu.dashboard).click();
          cy.wait(300);
        });
      });

      // @see https://testpalm.yandex-team.ru/testcase/courier-63
      describe('Finished', () => {
        before(() => {
          cy.get(selectors.content.dashboard.orders.legend.finished).click();
          cy.wait(200);
          cy.get(selectors.modal.paranja).should('not.exist');
        });

        assertOrdersMenuHighlighted();
        assertCountOrdersInList(1);
        assertFilterPlate('Finished');
        assertFilterTagUrl('Finished');
        assertValidationDateInput();
        assertClearInputFilter();
        after(() => {
          cy.get(selectors.sidebar.menu.dashboard).click();
          cy.wait(300);
        });
      });

      // @see https://testpalm.yandex-team.ru/testcase/courier-64
      describe('Sequenced', () => {
        before(() => {
          cy.get(selectors.content.dashboard.orders.legend.sequenced).click();
          cy.wait(200);
          cy.get(selectors.modal.paranja).should('not.exist');
        });

        assertOrdersMenuHighlighted();
        assertCountOrdersInList(11);
        assertFilterPlate('Sequenced');
        assertFilterTagUrl('Sequenced');
        assertValidationDateInput();

        it('Order statuses "Confirmed / New"', () => {
          const statusesRegExp = new RegExp(
            `${importKeyset.ru.params_initialStatus_confirmed}|${importKeyset.ru.params_initialStatus_new}`,
          );
          cy.get(selectors.content.orders.view);
          cy.get(selectors.content.orders.statusCell)
            .invoke('text')
            .should('match', statusesRegExp);
        });
        assertClearInputFilter();
        after(() => {
          cy.get(selectors.sidebar.menu.dashboard).click();
          cy.wait(300);
        });
      });

      describe('Unsequenced', () => {
        before(() => {
          cy.get(selectors.content.dashboard.orders.legend.unsequenced).click();
          cy.wait(200);
          cy.get(selectors.modal.paranja).should('not.exist');
        });

        assertOrdersMenuHighlighted();
        assertCountOrdersInList(0);
        assertFilterPlate('Unsequenced');
        assertFilterTagUrl('Unsequenced');
        assertValidationDateInput();
        assertClearInputFilter();
        after(() => {
          cy.get(selectors.sidebar.menu.dashboard).click();
          cy.wait(300);
        });
      });

      // @see https://testpalm.yandex-team.ru/testcase/courier-65
      describe('Late', () => {
        before(() => {
          cy.get(selectors.content.dashboard.orders.legend.late).click();
          cy.wait(200);
          cy.get(selectors.modal.paranja).should('not.exist');
        });

        assertOrdersMenuHighlighted();
        assertCountOrdersInList(1);
        assertFilterPlate(commonKeyset.en.tagsInput_tag_late);
        assertFilterTagUrl(commonKeyset.en.tagsInput_tag_late);
        assertValidationDateInput();

        it('The "Time late" field is filled', () => {
          cy.get(selectors.content.orders.view);
          cy.get(selectors.content.orders.lateCell)
            .invoke('text')
            .should('match', /[0-9][0-9]* ч ([1-5][0-9])|([0-9]) мин$/);
        });
        assertClearInputFilter();
        after(() => {
          cy.get(selectors.sidebar.menu.dashboard).click();
          cy.wait(300);
        });
      });

      // @see https://testpalm.yandex-team.ru/testcase/courier-37
      describe('Validation of the number of "squares" of orders', () => {
        it('Cancelled', () => {
          cy.get(selectors.content.dashboard.couriers.table.orders.cancelled).should(
            'have.length',
            2,
          );
        });

        it('Finished late', () => {
          cy.get(selectors.content.dashboard.couriers.table.orders.finishedLate).should(
            'have.length',
            1,
          );
        });

        it('Finished', () => {
          cy.get(selectors.content.dashboard.couriers.table.orders.finished).should(
            'have.length',
            1,
          );
        });

        it('Sequenced', () => {
          cy.get(selectors.content.dashboard.couriers.table.orders.sequenced).should(
            'have.length',
            11,
          );
        });

        it('Late', () => {
          cy.get(selectors.content.dashboard.couriers.table.orders.late).should('have.length', 1);
        });

        after(() => {
          cy.get(selectors.sidebar.menu.dashboard).click();
          cy.wait(300);
        });
      });
    });
  });
});
