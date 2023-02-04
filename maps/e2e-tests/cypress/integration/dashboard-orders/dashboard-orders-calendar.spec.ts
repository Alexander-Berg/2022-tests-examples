import * as courierRouteKeyset from '../../../../src/translations/courier-route';
import * as commonKeyset from '../../../../src/translations/common';
import addMinutes from 'date-fns/addMinutes';
import addHours from 'date-fns/addHours';
import addDays from 'date-fns/addDays';
import addMonths from 'date-fns/addMonths';
import subDays from 'date-fns/subDays';
import setDate from 'date-fns/setDate';
import parse from 'date-fns/parse';
import differenceInDays from 'date-fns/differenceInDays';
import isBefore from 'date-fns/isBefore';
import isEqual from 'date-fns/isEqual';
import isValid from 'date-fns/isValid';
import getMinutes from 'date-fns/getMinutes';
import getHours from 'date-fns/getHours';
import times from 'lodash/times';

import selectors from '../../../src/constants/selectors';
import dateFnsFormat from '../../../src/utils/date-fns-format';

describe('Dashboard orders calendar', function () {
  before(function () {
    cy.yandexLogin('admin');
  });

  it('Enter the page', function () {
    const today = new Date();

    cy.get(selectors.sidebar.menu.monitoringGroup).click();
    cy.get(selectors.sidebar.menu.orders).click();

    cy.get(selectors.content.orders.view).should('exist');
    cy.get(selectors.content.orders.tableRows).should('exist');
    cy.get(selectors.content.orders.dateFilter.input).should(
      'have.text',
      dateFnsFormat(today, 'dd MMM yyyy').replace('.', ''),
    );
    cy.get(selectors.content.orders.dateFilter.selectedPreset).should(
      'have.text',
      commonKeyset.ru.dateRangeFilter_today,
    );
  });

  it('Click the "Yesterday" button', function () {
    const yesterday = subDays(new Date(), 1);

    cy.get(selectors.content.orders.tableRows).then(({ length: todayRows }) => {
      cy.get(selectors.content.orders.dateFilter.presetYesterday).click();
      cy.get(selectors.content.orders.tableRows).its('length').should('not.equal', todayRows);
      cy.get(selectors.content.orders.dateFilter.input).should(
        'have.text',
        dateFnsFormat(yesterday, 'dd MMM yyyy').replace('.', ''),
      );
      cy.get(selectors.content.orders.dateFilter.selectedPreset).should(
        'have.text',
        commonKeyset.ru.dateRangeFilter_yesterday,
      );
    });

    times(2, i => {
      cy.get(selectors.content.orders.tableRow(i + 1))
        .children()
        .first()
        .click();
      cy.get(selectors.modal.orderPopup.shipmentIntervalRow)
        .invoke('text')
        .then(interval => {
          const items = interval.split(' ');
          expect(items).to.satisfy((items: string[]) => {
            if (items.length === 3) {
              const from = parse(items[0], 'HH:mm', new Date());
              const to = parse(items[2], 'HH:mm', new Date());

              return (
                items[1] === '–' &&
                isValid(from) &&
                isValid(to) &&
                (isBefore(from, to) || isEqual(from, to))
              );
            } else if (items.length === 5) {
              const fromDate = parse(items[0], 'yyyy-MM-dd', new Date());
              const fromTime = parse(items[1], 'HH:mm', new Date());
              const toDate = parse(items[3], 'yyyy-MM-dd', new Date());
              const toTime = parse(items[4], 'HH:mm', new Date());

              const fromDatetime = addMinutes(
                addHours(fromDate, getHours(fromTime)),
                getMinutes(fromTime),
              );
              const toDatetime = addMinutes(addHours(toDate, getHours(toTime)), getMinutes(toTime));

              return (
                items[2] === '–' &&
                isValid(fromDate) &&
                differenceInDays(fromDate, yesterday) >= 0 &&
                isValid(fromTime) &&
                isValid(toDate) &&
                differenceInDays(toDate, yesterday) >= 0 &&
                isValid(toTime) &&
                (isBefore(fromDatetime, toDatetime) || isEqual(fromDatetime, toDatetime))
              );
            }

            return false;
          });
        });
      cy.get(selectors.modal.orderPopup.dateRow)
        .invoke('text')
        .then(dateStr => {
          expect(dateStr).to.satisfy((dateStr: string) => {
            const date = parse(dateStr, 'dd.MM.yyyy', new Date());
            return isValid(date) && differenceInDays(date, yesterday) <= 0;
          });
        });
      cy.get(selectors.modal.orderPopup.closeButton).click();
    });
  });

  it('Click the "Tomorrow" button', function () {
    const tomorrow = addDays(new Date(), 1);

    cy.get(selectors.content.orders.tableRows).then(({ length: yesterdayRows }) => {
      cy.get(selectors.content.orders.dateFilter.presetTomorrow).click();
      cy.get(selectors.content.orders.tableRows).its('length').should('not.equal', yesterdayRows);
      cy.get(selectors.content.orders.dateFilter.input).should(
        'have.text',
        dateFnsFormat(tomorrow, 'dd MMM yyyy').replace('.', ''),
      );
      cy.get(selectors.content.orders.dateFilter.selectedPreset).should(
        'have.text',
        commonKeyset.ru.dateRangeFilter_tomorrow,
      );
    });

    times(2, i => {
      cy.get(selectors.content.orders.tableRow(i + 1))
        .children()
        .first()
        .click();
      cy.get(selectors.modal.orderPopup.shipmentIntervalRow)
        .invoke('text')
        .then(interval => {
          const items = interval.split(' ');
          expect(items).to.satisfy((items: string[]) => {
            if (items.length === 3) {
              const from = parse(items[0], 'HH:mm', new Date());
              const to = parse(items[2], 'HH:mm', new Date());

              return (
                items[1] === '–' &&
                isValid(from) &&
                isValid(to) &&
                (isBefore(from, to) || isEqual(from, to))
              );
            } else if (items.length === 5) {
              const fromDate = parse(items[0], 'yyyy-MM-dd', new Date());
              const fromTime = parse(items[1], 'HH:mm', new Date());
              const toDate = parse(items[3], 'yyyy-MM-dd', new Date());
              const toTime = parse(items[4], 'HH:mm', new Date());

              const fromDatetime = addMinutes(
                addHours(fromDate, getHours(fromTime)),
                getMinutes(fromTime),
              );
              const toDatetime = addMinutes(addHours(toDate, getHours(toTime)), getMinutes(toTime));

              return (
                items[2] === '–' &&
                isValid(fromDate) &&
                differenceInDays(fromDate, tomorrow) >= 0 &&
                isValid(fromTime) &&
                isValid(toDate) &&
                differenceInDays(toDate, tomorrow) >= 0 &&
                isValid(toTime) &&
                (isBefore(fromDatetime, toDatetime) || isEqual(fromDatetime, toDatetime))
              );
            }

            return false;
          });
        });
      cy.get(selectors.modal.orderPopup.closeButton).click();
    });
  });

  it('Click a date with no orders', function () {
    const date = setDate(addMonths(addDays(new Date(), 1), 2), 15);
    cy.get(selectors.datePicker.openButton).click();
    times(2, () => {
      cy.get(selectors.datePicker.daysGrid.nextMonthButton).click();
    });
    cy.get(selectors.datePicker.daysGrid.days[15]).click();
    cy.get(selectors.datePicker.datePickerPopup).should('not.exist');
    cy.get(selectors.content.orders.dateFilter.input).should(
      'have.text',
      dateFnsFormat(date, 'dd MMM yyyy').replace('.', ''),
    );
    cy.get(selectors.content.orders.tableRows).should('not.exist');
    cy.get(selectors.content.orders.message).should('have.text', courierRouteKeyset.ru.emptyOrders);
  });
});
