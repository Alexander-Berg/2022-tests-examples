const cr = require('../../credentials.js');

require('../helper.js')(afterEach);

describe('Экспорт', () => {
    beforeEach(function () {
        return this.browser
            .crInit('MANY_MAPS', cr.linkMap)
            .crSaveMap()
            .click(PO.exportButton())
            .crWaitForVisible(PO.stepExport(), 'Не открылся шаг экспорта')
            .crWaitForVisible(PO.exportModal());
    });

    afterEach(function () {
        return this.browser
            .crLogout();
    });

    it('Открытие', function () {
        return this.browser
            .crVerifyScreenshot(PO.exportModal(), 'export-modal');
    });

    it('Переключение между форматами', function () {
        return this.browser
            .crShouldBeVisible(PO.exportModal.switcher.typeXLSX() + PO.checked())
            .click(PO.exportModal.switcher.typeKML())
            .crShouldBeVisible(PO.exportModal.switcher.typeKML() + PO.checked())
            .crVerifyScreenshot(PO.exportModal(), 'export-modal-kml')
            .click(PO.exportModal.switcher.typeXLSX())
            .crShouldBeVisible(PO.exportModal.switcher.typeXLSX() + PO.checked())
            .click(PO.exportModal.switcher.typeGPX())
            .crShouldBeVisible(PO.exportModal.switcher.typeGPX() + PO.checked())
            .crVerifyScreenshot(PO.exportModal(), 'export-modal-gpx')
            .click(PO.exportModal.switcher.typeGeoJSON())
            .crShouldBeVisible(PO.exportModal.switcher.typeGeoJSON() + PO.checked())
            .crVerifyScreenshot(PO.exportModal(), 'export-modal-geojson')
            .click(PO.exportModal.switcher.typeCSV())
            .crShouldBeVisible(PO.exportModal.switcher.typeCSV() + PO.checked())
            .crVerifyScreenshot(PO.exportModal(), 'export-modal-csv');
    });

    it('Закрытие кликом на крестик', function () {
        return this.browser
            .click(PO.exportModal.close())
            .crWaitForHidden(PO.exportModal(), 'Окно экспорта не закрылось');
    });

    it('Закрытие кликом вне окна', function () {
        return this.browser
            .pause(700)
            .leftClick(PO.popupVisible.modelCell(), 50, 50)
            .crWaitForHidden(PO.exportModal());
    });
});
