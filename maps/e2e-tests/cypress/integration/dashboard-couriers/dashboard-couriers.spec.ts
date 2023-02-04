import * as courierQualityReportKeyset from '../../../../src/translations/courier-quality-report';
import * as dashboardKeyset from '../../../../src/translations/dashboard';
import * as commonKeyset from '../../../../src/translations/common';
import * as importRoutesKeyset from '../../../../src/translations/import-routes';
import moment from 'moment';
import size from 'lodash/size';
import uniq from 'lodash/uniq';
import values from 'lodash/values';
import time from '../../../src/utils/time';
import selectors from '../../../src/constants/selectors';

import urls from '../../../src/utils/urls';
import { courierNameRecord, courierNumberRecord } from '../../../src/constants/couriers';
import 'moment/locale/ru';
import parseISO from 'date-fns/parseISO';
import dateFnsFormat from '../../../src/utils/date-fns-format';

moment.locale('ru');

const roles: Array<keyof AccountsT> = ['admin', 'manager'];

context('Dashboard - Couriers', function () {
  const couriersAmount = size(uniq(values(courierNumberRecord)));
  beforeEach(() => {
    cy.preserveCookies();
  });

  roles.forEach(role => {
    context(`As ${role} role`, () => {
      context('Common data', () => {
        before(() => {
          cy.yandexLogin(role);
          cy.get(selectors.content.dashboard.view);
        });

        // @see https://testpalm.yandex-team.ru/testcase/courier-32
        it('Number of started routes', () => {
          cy.get(selectors.content.dashboard.dayStartedRoutesNumber)
            .invoke('text')
            .should('eq', '1');
        });
      });

      describe('Total number of routes', () => {
        before(() => {
          cy.yandexLogin(role);
          cy.get(selectors.content.dashboard.view);
        });

        it('Valid number in the dashboard', () => {
          cy.get(selectors.content.dashboard.dayTotalRoutesNumber).invoke('text').should('eq', '4');
        });

        // @see https://testpalm.yandex-team.ru/testcase/courier-45
        describe('Go to the courier`s page', () => {
          before(() => {
            cy.fixture('company-data').then(({ common }) => {
              const link = urls.couriersList.createLink(common.companyId, {
                date: common.date,
              });

              cy.yandexLogin(role, { link });
            });
          });

          it('Validation of the number of couriers in the list', () => {
            cy.get(selectors.content.couriers.list);
            cy.get(selectors.content.couriers.tableRows).should('have.length', couriersAmount);
          });

          it('The couriers menu is highlighted in the sidebar', () => {
            cy.get(selectors.sidebar.menu.monitoringGroup).click();
            cy.get(selectors.sidebar.selectedItem)
              .invoke('text')
              .should('eq', importRoutesKeyset.ru.tabTitles_vehicles);
          });
        });
      });

      // @see https://testpalm.yandex-team.ru/courier/testcases/308
      // @see https://testpalm.yandex-team.ru/testcase/courier-34
      describe('Go to the courier`s page (today)', () => {
        before(() => {
          cy.yandexLogin(role);
          cy.get(selectors.content.dashboard.couriers.table.todayRow).click();
          cy.get(selectors.content.couriers.loaded);
        });

        it('The couriers menu is highlighted in the sidebar', () => {
          cy.fixture('testData').then(({ courierNameRecord }) => {
            cy.get(selectors.sidebar.menu.monitoringGroup).click();
            cy.get(selectors.sidebar.menu.courierNameMenuItem)
              .invoke('text')
              .should('eq', courierNameRecord.gumba);
          });
        });

        it('Validation of the date in the input', () => {
          cy.fixture('company-data').then(({ common }) => {
            cy.get(selectors.content.couriers.view);
            const dateStr = dateFnsFormat(common.date, 'dd MMM yyyy').replace('.', '');

            cy.get(selectors.content.couriers.dateFilter.input)
              .invoke('text')
              .should('eq', dateStr);
          });
        });

        it('Validation of the selected date (preset)', () => {
          cy.get(selectors.content.couriers.view);
          cy.get(selectors.content.couriers.dateFilter.selectedPreset)
            .invoke('text')
            .should('eq', commonKeyset.ru.dateRangeFilter_today);
        });

        it('Validation of the courier`s name', () => {
          cy.get(selectors.content.couriers.view);
          cy.get(selectors.content.couriers.singleCourier.courierName)
            .invoke('text')
            .should('eq', courierNameRecord.gumba);
        });

        it('Validation of the courier`s login', () => {
          cy.get(selectors.content.couriers.view);
          cy.get(selectors.content.couriers.singleCourier.courierDetails.loginValue)
            .invoke('text')
            .should('eq', '1');
        });

        it('Validation of the courier`s phone', () => {
          cy.get(selectors.content.couriers.view);
          cy.get(selectors.content.couriers.singleCourier.courierDetails.phoneValue)
            .invoke('text')
            .should('eq', '+7(000)0000001');
        });

        it('Validation of count orders', () => {
          cy.get(selectors.content.couriers.view);
          cy.get(selectors.content.couriers.singleCourier.orderRow).should('have.length', 5);
        });
      });

      // @see https://testpalm.yandex-team.ru/testcase/courier-119
      describe('Go to the courier`s page (the second route).', () => {
        before(() => {
          cy.yandexLogin(role);
          cy.get(selectors.content.dashboard.couriers.table.twoAdditionalOrderRow).click();
          cy.get(selectors.content.couriers.loaded);
        });

        it('The courier name is highlighted in the sidebar menu item', () => {
          cy.fixture('testData').then(({ courierNameRecord }) => {
            cy.get(selectors.sidebar.menu.courierNameMenuItem)
              .invoke('text')
              .should('eq', courierNameRecord.gumba);
          });
        });

        it('Validation of the date in the input', () => {
          cy.fixture('company-data').then(({ common }) => {
            moment.locale('ru');
            const dateStr = dateFnsFormat(common.date, 'dd MMM yyyy').replace('.', '');

            cy.get(selectors.content.couriers.view);
            cy.get(selectors.content.couriers.dateFilter.input)
              .invoke('text')
              .should('eq', dateStr);
          });
        });

        it('Validation of the selected date (preset)', () => {
          cy.get(selectors.content.couriers.view);
          cy.get(selectors.content.couriers.dateFilter.selectedPreset)
            .invoke('text')
            .should('eq', commonKeyset.ru.dateRangeFilter_today);
        });

        it('Validation of count orders in route', () => {
          cy.get(selectors.content.couriers.singleCourier.orderRow).should('have.length', 2);
        });

        it('Validation checked second route', () => {
          cy.get(selectors.content.couriers.singleCourier.routesList.second).should(
            'have.class',
            'route-selector__route-button_active',
          );
        });
        it('Validation of the courier`s name', () => {
          cy.get(selectors.content.couriers.view);
          cy.get(selectors.content.couriers.singleCourier.courierName)
            .invoke('text')
            .should('eq', courierNameRecord.gumba);
        });

        it('Validation of the courier`s login', () => {
          cy.get(selectors.content.couriers.view);
          cy.get(selectors.content.couriers.singleCourier.courierDetails.loginValue)
            .invoke('text')
            .should('eq', '1');
        });

        it('Validation of the courier`s phone', () => {
          cy.get(selectors.content.couriers.view);
          cy.get(selectors.content.couriers.singleCourier.courierDetails.phoneValue)
            .invoke('text')
            .should('eq', '+7(000)0000001');
        });
      });

      // @see https://testpalm.yandex-team.ru/testcase/courier-48
      describe('Go to the courier`s page (tomorrow)', () => {
        const date = moment(time.TIME_TODAY).add(1, 'days').format(urls.dashboard.dateFormat);

        before(() => {
          cy.fixture('company-data').then(({ common }) => {
            const link = urls.dashboard.createLink(common.companyId, { date });

            cy.yandexLogin(role, { link });
            cy.get(selectors.content.dashboard.couriers.table.courierLink).click();
            cy.get(selectors.content.couriers.loaded);
          });
        });

        it('The couriers menu is highlighted in the sidebar', () => {
          cy.fixture('testData').then(({ courierNameRecord }) => {
            cy.get(selectors.sidebar.selectedItem)
              .invoke('text')
              .should('eq', courierNameRecord.gumba);
          });
        });

        it('Validation of the date in the input', () => {
          const date = parseISO(time.TIME_TODAY);
          date.setDate(date.getDate() + 1);
          const dateStr = dateFnsFormat(date, 'dd MMM yyyy').replace('.', '');
          cy.get(selectors.content.couriers.view);
          cy.get(selectors.content.couriers.dateFilter.input).invoke('text').should('eq', dateStr);
        });

        it('Validation of the selected date (preset)', () => {
          cy.get(selectors.content.couriers.view);
          cy.get(selectors.content.couriers.dateFilter.selectedPreset)
            .invoke('text')
            .should('eq', commonKeyset.ru.dateRangeFilter_tomorrow);
        });

        it('Validation of the courier`s name', () => {
          cy.get(selectors.content.couriers.view);
          cy.get(selectors.content.couriers.singleCourier.courierName)
            .invoke('text')
            .should('eq', courierNameRecord.gumba);
        });

        // @see https://testpalm.yandex-team.ru/testcase/courier-188
        it('Return to the dashboard via the sidebar', () => {
          cy.get(selectors.sidebar.menu.monitoringGroup).click();
          cy.get(selectors.sidebar.menu.dashboard).click();
          cy.get(selectors.content.dashboard.view);
          cy.get(selectors.content.dashboard.dayTitle)
            .invoke('text')
            .should('eq', commonKeyset.ru.dateRangeFilter_tomorrow);
        });
      });
    });

    // @see https://testpalm.yandex-team.ru/courier/testcases/307
    describe(`Sorting as ${role} role`, () => {
      before(() => {
        cy.yandexLogin(role);
        cy.get(selectors.content.dashboard.view);
      });

      // @see https://testpalm.yandex-team.ru/testcase/courier-33
      describe('By name', () => {
        describe('Enabled by default', () => {
          it('The text is highlighted', () => {
            cy.get(selectors.content.dashboard.couriers.sort.activeSnippet)
              .invoke('text')
              .should('eq', 'По имени А-Я');
          });

          it('Couriers are sorted', () => {
            cy.get(selectors.content.dashboard.couriers.table.courierNames)
              .invoke('text')
              .should('eq', [courierNameRecord.gumba, courierNameRecord.kypa].join(''));
          });
        });

        describe('Enabled when transmitting by link (ascending)', () => {
          before(() => {
            cy.fixture('company-data').then(({ common }) => {
              const link = urls.dashboard.createLink(common.companyId, {
                date: common.date,
                sort: urls.dashboard.sorts.nameAsc,
              });

              cy.yandexLogin(role, { link });
              cy.get(selectors.content.dashboard.view);
            });
          });

          it('The text is highlighted', () => {
            cy.get(selectors.content.dashboard.couriers.sort.activeSnippet)
              .invoke('text')
              .should('eq', 'По имени А-Я');
          });

          it('Couriers are sorted', () => {
            cy.get(selectors.content.dashboard.couriers.table.courierNames)
              .invoke('text')
              .should('eq', [courierNameRecord.gumba, courierNameRecord.kypa].join(''));
          });

          describe('Enabled when transmitting by link (decreasing)', () => {
            before(() => {
              cy.fixture('company-data').then(({ common }) => {
                const link = urls.dashboard.createLink(common.companyId, {
                  date: common.date,
                  sort: urls.dashboard.sorts.nameDesc,
                });

                cy.yandexLogin(role, { link });
                cy.get(selectors.content.dashboard.view);
              });
            });

            it('The text is highlighted', () => {
              cy.get(selectors.content.dashboard.couriers.sort.activeSnippet)
                .invoke('text')
                .should('eq', 'По имени Я-А');
            });

            it('Couriers are sorted', () => {
              cy.get(selectors.content.dashboard.couriers.table.courierNames)
                .invoke('text')
                .should('eq', [courierNameRecord.kypa, courierNameRecord.gumba].join(''));
            });
          });
        });

        describe('When switching from a different sort', () => {
          before(() => {
            cy.fixture('company-data').then(({ common }) => {
              const link = urls.dashboard.createLink(common.companyId, {
                date: common.date,
                sort: urls.dashboard.sorts.lateAsc,
              });

              cy.yandexLogin(role, { link });
              cy.get(selectors.content.dashboard.view);
              cy.get(selectors.content.dashboard.couriers.sort.byName).click();
              cy.wait(1000);
            });
          });

          it('The text is highlighted', () => {
            cy.get(selectors.content.dashboard.couriers.sort.activeSnippet)
              .invoke('text')
              .should('eq', 'По имени А-Я');
          });

          it('Couriers are sorted', () => {
            cy.get(selectors.content.dashboard.couriers.table.courierNames)
              .invoke('text')
              .should('eq', [courierNameRecord.gumba, courierNameRecord.kypa].join(''));
          });
        });

        describe('Reverse sorting', () => {
          before(() => {
            cy.get(selectors.content.dashboard.couriers.sort.byName).click();
            cy.wait(1000);
          });

          it('The text is highlighted', () => {
            cy.get(selectors.content.dashboard.couriers.sort.activeSnippet)
              .invoke('text')
              .should('eq', 'По имени Я-А');
          });

          it('Couriers are sorted', () => {
            cy.get(selectors.content.dashboard.couriers.table.courierNames)
              .invoke('text')
              .should('eq', [courierNameRecord.kypa, courierNameRecord.gumba].join(''));
          });
        });
      });

      // @see https://testpalm.yandex-team.ru/testcase/courier-46
      describe('By being late', () => {
        describe('Enabled when transmitting by link (ascending)', () => {
          before(() => {
            cy.fixture('company-data').then(({ common }) => {
              const link = urls.dashboard.createLink(common.companyId, {
                date: common.date,
                sort: urls.dashboard.sorts.lateAsc,
              });

              cy.yandexLogin(role, { link });
              cy.get(selectors.content.dashboard.view);
            });
          });

          it('The text is highlighted', () => {
            cy.get(selectors.content.dashboard.couriers.sort.activeSnippet)
              .invoke('text')
              .should('eq', 'По опозданиям ⋀');
          });

          it('Couriers are sorted', () => {
            cy.get(selectors.content.dashboard.couriers.table.courierNames)
              .invoke('text')
              .should('eq', [courierNameRecord.gumba, courierNameRecord.kypa].join(''));
          });
        });

        describe('Enabled when transmitting by link (descending)', () => {
          before(() => {
            cy.fixture('company-data').then(({ common }) => {
              const link = urls.dashboard.createLink(common.companyId, {
                date: common.date,
                sort: urls.dashboard.sorts.lateDesc,
              });

              cy.yandexLogin(role, { link });
              cy.get(selectors.content.dashboard.view);
            });
          });

          it('The text is highlighted', () => {
            cy.get(selectors.content.dashboard.couriers.sort.activeSnippet)
              .invoke('text')
              .should('eq', 'По опозданиям ⋁');
          });

          it('Couriers are sorted', () => {
            cy.get(selectors.content.dashboard.couriers.table.courierNames)
              .invoke('text')
              .should('eq', [courierNameRecord.kypa, courierNameRecord.gumba].join(''));
          });
        });

        describe('When switching from a different sort', () => {
          before(() => {
            cy.get(selectors.content.dashboard.couriers.sort.byLate).click();
            cy.wait(1000);
          });

          it('The text is highlighted', () => {
            cy.get(selectors.content.dashboard.couriers.sort.activeSnippet)
              .invoke('text')
              .should('eq', 'По опозданиям ⋀');
          });

          it('Couriers are sorted', () => {
            cy.get(selectors.content.dashboard.couriers.table.courierNames)
              .invoke('text')
              .should('eq', [courierNameRecord.gumba, courierNameRecord.kypa].join(''));
          });
        });

        describe('Reverse sorting', () => {
          before(() => {
            cy.get(selectors.content.dashboard.couriers.sort.byLate).click();
          });

          it('The text is highlighted', () => {
            cy.get(selectors.content.dashboard.couriers.sort.activeSnippet)
              .invoke('text')
              .should('eq', 'По опозданиям ⋁');
          });

          it('Couriers are sorted', () => {
            cy.get(selectors.content.dashboard.couriers.table.courierNames)
              .invoke('text')
              .should('eq', [courierNameRecord.kypa, courierNameRecord.gumba].join(''));
          });
        });
      });

      // @see https://testpalm.yandex-team.ru/testcase/courier-47
      describe('For completed orders', () => {
        describe('Enabled when transmitting by link (ascending)', () => {
          before(() => {
            cy.fixture('company-data').then(({ common }) => {
              const link = urls.dashboard.createLink(common.companyId, {
                date: common.date,
                sort: urls.dashboard.sorts.finishedAsc,
              });

              cy.yandexLogin(role, { link });
              cy.get(selectors.content.dashboard.view);
            });
          });

          it('The text is highlighted', () => {
            cy.get(selectors.content.dashboard.couriers.sort.activeSnippet)
              .invoke('text')
              .should('eq', 'По проценту завершения ⋀');
          });

          it('Couriers are sorted', () => {
            cy.get(selectors.content.dashboard.couriers.table.courierNames)
              .invoke('text')
              .should('eq', [courierNameRecord.gumba, courierNameRecord.kypa].join(''));
          });
        });

        describe('Enabled when transmitting by link (descending)', () => {
          before(() => {
            cy.fixture('company-data').then(({ common }) => {
              const link = urls.dashboard.createLink(common.companyId, {
                date: common.date,
                sort: urls.dashboard.sorts.finishedDesc,
              });

              cy.yandexLogin(role, { link });
              cy.get(selectors.content.dashboard.view);
            });
          });

          it('The text is highlighted', () => {
            cy.get(selectors.content.dashboard.couriers.sort.activeSnippet)
              .invoke('text')
              .should('eq', 'По проценту завершения ⋁');
          });

          it('Couriers are sorted', () => {
            cy.get(selectors.content.dashboard.couriers.table.courierNames)
              .invoke('text')
              .should('eq', [courierNameRecord.kypa, courierNameRecord.gumba].join(''));
          });
        });

        describe('When switching from a different sort', () => {
          before(() => {
            cy.get(selectors.content.dashboard.couriers.sort.byFinished).click();
          });

          it('The text is highlighted', () => {
            cy.get(selectors.content.dashboard.couriers.sort.activeSnippet)
              .invoke('text')
              .should('eq', 'По проценту завершения ⋀');
          });

          it('Couriers are sorted', () => {
            cy.get(selectors.content.dashboard.couriers.table.courierNames)
              .invoke('text')
              .should('eq', [courierNameRecord.gumba, courierNameRecord.kypa].join(''));
          });
        });

        describe('Reverse sorting', () => {
          before(() => {
            cy.get(selectors.content.dashboard.couriers.sort.byFinished).click();
          });

          it('The text is highlighted', () => {
            cy.get(selectors.content.dashboard.couriers.sort.activeSnippet)
              .invoke('text')
              .should('eq', 'По проценту завершения ⋁');
          });

          it('Couriers are sorted', () => {
            cy.get(selectors.content.dashboard.couriers.table.courierNames)
              .invoke('text')
              .should('eq', [courierNameRecord.kypa, courierNameRecord.gumba].join(''));
          });
        });
      });

      describe('By status', () => {
        describe('Enabled when transmitting by link (ascending)', () => {
          before(() => {
            cy.fixture('company-data').then(({ common }) => {
              const link = urls.dashboard.createLink(common.companyId, {
                date: common.date,
                sort: urls.dashboard.sorts.courierPositionStateAsc,
              });

              cy.yandexLogin(role, { link });
              cy.get(selectors.content.dashboard.view);
            });
          });

          it('The text is highlighted', () => {
            cy.get(selectors.content.dashboard.couriers.sort.activeSnippet)
              .invoke('text')
              .should('eq', 'По статусу ⋀');
          });
        });

        describe('Enabled when transmitting by link (descending)', () => {
          before(() => {
            cy.fixture('company-data').then(({ common }) => {
              const link = urls.dashboard.createLink(common.companyId, {
                date: common.date,
                sort: urls.dashboard.sorts.courierPositionStateDesc,
              });

              cy.yandexLogin(role, { link });
              cy.get(selectors.content.dashboard.view);
            });
          });

          it('The text is highlighted', () => {
            cy.get(selectors.content.dashboard.couriers.sort.activeSnippet)
              .invoke('text')
              .should('eq', 'По статусу ⋁');
          });
        });

        describe('When switching from a different sort', () => {
          before(() => {
            cy.get(selectors.content.dashboard.couriers.sort.byCourierPositionState).click();
            cy.wait(1000);
          });

          it('The text is highlighted', () => {
            cy.get(selectors.content.dashboard.couriers.sort.activeSnippet)
              .invoke('text')
              .should('eq', 'По статусу ⋀');
          });
        });

        describe('Reverse sorting', () => {
          before(() => {
            cy.get(selectors.content.dashboard.couriers.sort.byCourierPositionState).click();
          });

          it('The text is highlighted', () => {
            cy.get(selectors.content.dashboard.couriers.sort.activeSnippet)
              .invoke('text')
              .should('eq', 'По статусу ⋁');
          });
        });
      });
    });
  });
});
