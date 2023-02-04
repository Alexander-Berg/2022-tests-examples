import moment from 'moment';
import 'moment/locale/ru';
import selectors from '../../../src/constants/selectors';
import time from '../../../src/utils/time';
import * as mvrpExportKeyset from '../../../../src/translations/mvrp-export';
import * as mvrpKeyset from '../../../../src/translations/mvrp';
import i18n from '@yandex-int/i18n';

moment.locale('ru');

const TEMPLATE_1 = 'template1.xlsx';
// this time is enough to upload an excel file or waiting solver
const UPLOAD_TIMEOUT = 60000;

const mvrpExportI18n = i18n(mvrpExportKeyset);
const mvrpI18n = i18n(mvrpKeyset);
const dublicateOrdersText = mvrpExportI18n('monitoring_orderWarningtitle_hasOrders', {
  existOrdersCount: 9,
});
const formattedDate = moment(time.TIME_TODAY).format(mvrpI18n('monitoringExport_dateFormat'));
const duplicateRoutesText = mvrpI18n('checkExistRoutes_title', {
  formattedDate: formattedDate,
  link: mvrpI18n('monitoringExport_existRoutes'),
});
const errorText = mvrpI18n('errors_EXPORT_TO_MONITORING_FAILURE');

context('Export routes', () => {
  before(() => {
    cy.yandexLogin('exportRoutesAdmin');
    cy.clearLocalforage();
    cy.openAndCloseVideo();

    cy.get(selectors.content.mvrp.fileInput).then(subject => {
      if (!subject) {
        cy.get(selectors.content.mvrp.startNewPlanning).click();
      }

      return;
    });
  });

  describe('First time export solution', () => {
    it('exporting solution should be successful', () => {
      cy.get(selectors.content.mvrp.fileInput).attachFile(TEMPLATE_1);
      cy.get(selectors.content.mvrp.startPlanningButton, {
        timeout: UPLOAD_TIMEOUT,
      }).click();

      cy.get(selectors.content.mvrp.solverViewLoaded);

      cy.get(selectors.content.mvrp.export.exportButtonOpenModal)
        .click()
        .get(selectors.content.mvrp.export.exportButtonMonitoring)
        .click();
      cy.get(selectors.content.mvrp.export.exportMonitoringPrepare.view);
      cy.get(
        selectors.content.mvrp.export.exportMonitoringPrepare.actionUploadMonitoringButton,
      ).should('not.be.disabled');

      // need to export task monitoring
      cy.wait(3000);

      cy.get(
        selectors.content.mvrp.export.exportMonitoringPrepare.actionUploadMonitoringButton,
      ).click();
      cy.get(selectors.content.mvrp.export.exportMonitoringPrepare.exportSuccess);
    });

    it('check that routes added to monitoring', () => {
      cy.get(selectors.content.mvrp.export.exportMonitoringPrepare.toMonitoringButton).click();
      cy.get(selectors.content.dashboard.dayTotalRoutesNumber).invoke('text').should('eq', '3');
      cy.get(selectors.content.dashboard.couriers.table.routeName)
        .invoke('text')
        .should('eq', `M1-1-${time.TIME_TODAY}M1-2-${time.TIME_TODAY}M2-1-${time.TIME_TODAY}`);
    });

    after(() => {
      cy.get(selectors.sidebar.menu.mvrp).click();
    });
  });

  describe('Second time export solution ', () => {
    it('export solution should have export warnings dublicate orders', () => {
      cy.get(selectors.content.mvrp.solverViewLoaded);
      cy.get(selectors.content.mvrp.export.exportButtonOpenModal).click();
      cy.get(selectors.content.mvrp.export.exportButtonMonitoring).click();
      cy.get(selectors.content.mvrp.export.exportMonitoringPrepare.view);
      cy.get(selectors.content.mvrp.export.exportMonitoringPrepare.routesExistWarnings);
      cy.get(selectors.content.mvrp.export.exportMonitoringPrepare.warningTitle)
        .invoke('text')
        .should('eq', dublicateOrdersText);
      cy.get(selectors.content.mvrp.export.exportMonitoringPrepare.routesExistWarningsButtons)
        .first()
        .click();
    });

    it('export solution should have export warnings dublicate routes, we can add new', () => {
      cy.get(selectors.content.mvrp.export.exportMonitoringPrepare.warningTitle)
        .invoke('text')
        .should('eq', duplicateRoutesText);
      cy.get(selectors.content.mvrp.export.exportMonitoringPrepare.routesExistWarningsButtons)
        .first()
        .click();
      cy.wait(3000);
      cy.get(selectors.content.mvrp.export.exportMonitoringPrepare.exportSuccess);
    });

    it('check that new routes added to monitoring', () => {
      cy.get(selectors.content.mvrp.export.exportMonitoringPrepare.toMonitoringButton).click();
      cy.get(selectors.content.dashboard.dayTotalRoutesNumber).invoke('text').should('eq', '6');
      cy.get(selectors.content.dashboard.couriers.table.routeName)
        .invoke('text')
        .should(
          'eq',
          `M1-1-${time.TIME_TODAY}M1-2-${time.TIME_TODAY}M2-1-${time.TIME_TODAY}M1-1-${time.TIME_TODAY}-1M1-2-${time.TIME_TODAY}-1M2-1-${time.TIME_TODAY}-1`,
        );
    });

    after(() => {
      cy.get(selectors.sidebar.menu.mvrp).click();
    });
  });

  describe('Third time export solution ', () => {
    it('export solution should have export warnings dublicate orders', () => {
      cy.get(selectors.content.mvrp.solverViewLoaded);
      cy.get(selectors.content.mvrp.export.exportButtonOpenModal).click();
      cy.get(selectors.content.mvrp.export.exportButtonMonitoring).click();
      cy.get(selectors.content.mvrp.export.exportMonitoringPrepare.view);
      cy.get(selectors.content.mvrp.export.exportMonitoringPrepare.routesExistWarnings);
      cy.get(selectors.content.mvrp.export.exportMonitoringPrepare.warningTitle)
        .invoke('text')
        .should('eq', dublicateOrdersText);

      cy.get(selectors.content.mvrp.export.exportMonitoringPrepare.routesExistWarningsButtons)
        .first()
        .click();
    });

    it('export solution should have export warnings dublicate routes, show error when we try add new', () => {
      cy.get(selectors.content.mvrp.export.exportMonitoringPrepare.warningTitle)
        .invoke('text')
        .should('eq', duplicateRoutesText);
      cy.get(selectors.content.mvrp.export.exportMonitoringPrepare.routesExistWarningsButtons)
        .first()
        .click();

      cy.get(selectors.modal.errorPopup.view);
      cy.get(selectors.modal.errorPopup.messageTitle).invoke('text').should('eq', errorText);
      cy.get(selectors.modal.errorPopup.closeButton).click();
      cy.get(selectors.content.mvrp.export.exportModalCloseButton).click();
    });
  });

  after(() => {
    cy.get(selectors.sidebar.menu.monitoringGroup).click();
    cy.get(selectors.sidebar.menu.dashboard).click();
    cy.get(selectors.content.dashboard.moreOptionsDropdown.button).click();
    cy.get(selectors.content.dashboard.moreOptionsDropdown.options.deleteRoutes).click();
    cy.get(selectors.modal.dialog.submit).click();
  });
});
