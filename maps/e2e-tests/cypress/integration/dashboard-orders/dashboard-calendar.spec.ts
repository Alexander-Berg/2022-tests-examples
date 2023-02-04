import * as vehiclesReferenceBookKeyset from '../../../../src/translations/vehicles-reference-book';
import * as commonKeyset from '../../../../src/translations/common';
import * as dashboardOrdersKeyset from '../../../../src/translations/dashboard-orders';
import moment from 'moment';
import 'moment/locale/ru';
import map from 'lodash/map';
import times from 'lodash/times';

import selectors from '../../../src/constants/selectors';

moment.locale('ru');

context('Dashboard calendar', function () {
  describe('Calendar date changes', function () {
    before(function () {
      cy.yandexLogin('admin');
      cy.get(selectors.content.dashboard.view);
    });

    it('Today click', function () {
      const date = moment();

      cy.get(selectors.content.dashboard.dayTitle).click();
      cy.get(selectors.datePicker.daysGrid.currentMonthTitle).should(
        'have.text',
        date.format('MMMM YYYY'),
      );
      cy.get(selectors.datePicker.daysGrid.days.today).should('have.text', date.format('D'));
    });

    it('Yesterday click', function () {
      cy.get(selectors.content.dashboard.dayOrderNumber).invoke('text').as('todayOrders');
      cy.get(selectors.content.dashboard.couriers.table.row)
        .then(({ length }) => length)
        .as('todayCouriers');
      cy.get(selectors.content.dashboard.orders.chart.all)
        .then($chartColumns => map($chartColumns, col => col.clientWidth))
        .as('todayColumnWidths');
      cy.get(selectors.datePicker.daysGrid.days.today).then($el => {
        expect($el.length).to.equal(1);

        $el.each(function () {
          if (this.previousElementSibling) {
            cy.wrap(this.previousElementSibling).click();
          } else if (this.parentNode?.previousSibling) {
            cy.wrap(this.parentNode.previousSibling.lastChild).click();
          } else {
            cy.get(selectors.datePicker.daysGrid.previousMonthButton).click();
            cy.get(selectors.datePicker.daysGrid.days.superLast).click();
          }
        });
      });

      cy.get(selectors.datePicker.datePickerPopup).should('not.exist');
      cy.get(selectors.content.dashboard.dayTitle).should(
        'have.text',
        commonKeyset.ru.dateRangeFilter_yesterday,
      );
      cy.get('@todayOrders').then(todayOrders => {
        cy.get(selectors.content.dashboard.dayOrderNumber).should('not.have.text', todayOrders);
      });
      cy.get('@todayCouriers').then(todayCouriers => {
        cy.get(selectors.content.dashboard.couriers.table.row)
          .its('length')
          .should('not.equal', todayCouriers);
      });
      cy.get('@todayColumnWidths').then(todayColumnWidths => {
        cy.get(selectors.content.dashboard.orders.chart.all).then($el => {
          const yesterdayColumnWidths = map($el, col => col.clientWidth);
          cy.wrap(yesterdayColumnWidths).should('not.deep.equal', todayColumnWidths);
        });
      });
    });

    it('Tomorrow click', function () {
      cy.get(selectors.content.dashboard.dayOrderNumber).invoke('text').as('yesterdayOrders');
      cy.get(selectors.content.dashboard.couriers.table.row)
        .then(({ length }) => length)
        .as('yesterdayCouriers');
      cy.get(selectors.content.dashboard.orders.chart.all)
        .then($chartColumns => map($chartColumns, col => col.clientWidth))
        .as('yesterdayColumnWidths');

      cy.get(selectors.content.dashboard.dayTitle).click();
      cy.get(selectors.datePicker.daysGrid.days.today).then($el => {
        expect($el.length).to.equal(1);

        $el.each(function () {
          if (this.nextElementSibling) {
            cy.wrap(this.nextElementSibling).click();
          } else if (this.parentNode?.nextSibling) {
            cy.wrap(this.parentNode.nextSibling.firstChild).click();
          } else {
            cy.get(selectors.datePicker.daysGrid.nextMonthButton).click();
            cy.get(selectors.datePicker.daysGrid.days.superFirst).click();
          }
        });
      });

      cy.get(selectors.datePicker.datePickerPopup).should('not.exist');
      cy.get(selectors.content.dashboard.dayTitle).should(
        'have.text',
        commonKeyset.ru.dateRangeFilter_tomorrow,
      );
      cy.get('@yesterdayOrders').then(yesterdayOrders => {
        cy.get(selectors.content.dashboard.dayOrderNumber).should('not.have.text', yesterdayOrders);
      });
      cy.get('@yesterdayCouriers').then(yesterdayCouriers => {
        cy.get(selectors.content.dashboard.couriers.table.row)
          .its('length')
          .should('not.equal', yesterdayCouriers);
      });
      cy.get('@yesterdayColumnWidths').then(yesterdayColumnWidths => {
        cy.get(selectors.content.dashboard.orders.chart.all).then($el => {
          const tomorrowColumnWidths = map($el, col => col.clientWidth);
          cy.wrap(tomorrowColumnWidths).should('not.deep.equal', yesterdayColumnWidths);
        });
      });
    });

    it('Previous month clicks', function () {
      cy.get(selectors.content.dashboard.dayTitle).click();

      times(2, () => {
        cy.get(selectors.datePicker.daysGrid.currentMonthTitle)
          .invoke('text')
          .then(prevValue => {
            const expectedDate = moment(prevValue, 'MMMM YYYY')
              .subtract(1, 'months')
              .format('MMMM YYYY');

            cy.get(selectors.datePicker.daysGrid.previousMonthButton).click();
            cy.get(selectors.datePicker.daysGrid.currentMonthTitle).should(
              'have.text',
              expectedDate,
            );
          });
      });
    });

    it('Click on the previous month date', function () {
      cy.get(selectors.content.dashboard.dayOrderNumber).invoke('text').as('previousOrders');
      cy.get(selectors.content.dashboard.couriers.table.row)
        .then(({ length }) => length)
        .as('previousCouriers');
      cy.get(selectors.content.dashboard.orders.chart.all)
        .then($chartColumns => map($chartColumns, col => col.clientWidth))
        .as('previousColumnWidths');

      cy.get(selectors.datePicker.daysGrid.currentMonthTitle)
        .invoke('text')
        .then(monthTitle => {
          if (moment(monthTitle, 'MMMM YYYY').get('month') !== moment().get('month') - 1) {
            cy.get(selectors.datePicker.daysGrid.nextMonthButton).click();
          }
        });

      cy.get(selectors.datePicker.daysGrid.currentMonthTitle)
        .invoke('text')
        .then(lastMonthTitle => moment(lastMonthTitle, 'MMMM YYYY').date(15).format('D MMMM'))
        .as('expectedDate');

      cy.get(selectors.datePicker.daysGrid.days[15]).click();
      cy.get(selectors.datePicker.datePickerPopup).should('not.exist');
      cy.get('@expectedDate').then(expectedDate => {
        cy.get(selectors.content.dashboard.dayTitle).should('have.text', expectedDate);
      });
      cy.get('@previousOrders').then(previousOrders => {
        cy.get(selectors.content.dashboard.dayOrderNumber).should('not.have.text', previousOrders);
      });
      cy.get('@previousCouriers').then(previousCouriers => {
        cy.get(selectors.content.dashboard.couriers.table.row)
          .its('length')
          .should('not.equal', previousCouriers);
      });

      cy.get('@previousColumnWidths').then(previousColumnWidths => {
        cy.get(selectors.content.dashboard.orders.chart.all).then($el => {
          const currentColumnWidths = map($el, col => col.clientWidth);
          cy.wrap(currentColumnWidths).should('not.deep.equal', previousColumnWidths);
        });
      });
    });

    it('Next month clicks', function () {
      cy.get(selectors.content.dashboard.dayTitle).click();

      times(4, () => {
        cy.get(selectors.datePicker.daysGrid.currentMonthTitle)
          .invoke('text')
          .then(prevValue => {
            const expectedDate = moment(prevValue, 'MMMM YYYY')
              .add(1, 'months')
              .format('MMMM YYYY');

            cy.get(selectors.datePicker.daysGrid.nextMonthButton).click();
            cy.get(selectors.datePicker.daysGrid.currentMonthTitle).should(
              'have.text',
              expectedDate,
            );
          });
      });
    });

    it('Click on a date with no orders', function () {
      cy.get(selectors.datePicker.daysGrid.currentMonthTitle)
        .invoke('text')
        .then(futureMonthTitle => {
          const expectedDate = moment(futureMonthTitle, 'MMMM YYYY').date(15).format('D MMMM');
          cy.get(selectors.datePicker.daysGrid.days[15]).click();
          cy.get(selectors.datePicker.datePickerPopup).should('not.exist');
          cy.get(selectors.content.dashboard.dayTitle).should('have.text', expectedDate);
          cy.get(selectors.content.dashboard.dayOrderNumber).should(
            'have.text',
            `0 ${dashboardOrdersKeyset.ru.orders.many}`,
          );
          cy.get(selectors.content.dashboard.couriers.table.row).should('not.exist');

          cy.get(selectors.content.dashboard.orders.chart.all).then($el => {
            const currentColumnsWidth = $el
              .toArray()
              .reduce((prev, cur) => prev + cur.clientWidth, 0);
            cy.wrap(currentColumnsWidth).should('equal', 0);
          });

          cy.get(selectors.content.dashboard.orders.legendValue).then($el => {
            $el.each(function () {
              cy.wrap(this.textContent).should(
                'equal',
                vehiclesReferenceBookKeyset.ru.extendedForm_shift_perStopLackPenalty_placeholder,
              );
            });
          });
        });
    });
  });
});
