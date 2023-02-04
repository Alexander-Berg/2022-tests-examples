const cr = require('../../credentials.js');
const DRAW_TIMEOUT = 300;
const TOLERANCE = 50;

require('../helper.js')(afterEach);

describe('GET', () => {
    afterEach(function () {
        return this.browser
            .crLogout();
    });

    it('Открытие приватной Моей карты владельцем карты', function () {
        return this.browser
            .crInit('WITH_MY_MAPS', cr.umPrivateMap)
            .crWaitForVisible(PO.ymaps.map(), 'API не загрузилось')
            .pause(DRAW_TIMEOUT)
            .crVerifyScreenshot(PO.ymaps.map(), 'private-map', {tolerance: TOLERANCE})
            .crVerifyScreenshot(PO.geoObjectList(), 'private-map-list');
    });

    it('Открытие чужой приватной Моей карты', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.umPrivateMap, cr.mapState.list)
            .crWaitForVisible(PO.ymaps.map(), 'API не загрузилось', {tolerance: TOLERANCE})
            .crShouldNotBeVisible(PO.geoObjectList(), 'Должен быть пустой список объектов');
    });

    it('Центр карты, зум, тип карты', function () {
        return this.browser
            .crInit('MANY_MAPS', '?ll=40.927570%2C57.768962&z=15&l=sat')
            .crWaitForVisible(PO.ymaps.map(), 'API не загрузилось')
            .pause(DRAW_TIMEOUT)
            .crVerifyScreenshot(PO.ymaps.map(), 'll-z_15-l_sat', {tolerance: TOLERANCE});
    });

    it('Центр карты и зум', function () {
        return this.browser
            .crInit('MANY_MAPS', '?ll=40.927570%2C57.768962&z=7')
            .crWaitForVisible(PO.ymaps.map(), 'API не загрузилось')
            .pause(DRAW_TIMEOUT)
            .crVerifyScreenshot(PO.ymaps.map(), 'll-z_7', {tolerance: TOLERANCE});
    });

    it('Центр карты', function () {
        return this.browser
            .crInit('MANY_MAPS', '?ll=40.927570%2C57.768962')
            .crWaitForVisible(PO.ymaps.map(), 'API не загрузилось')
            .pause(DRAW_TIMEOUT)
            .crVerifyScreenshot(PO.ymaps.map(), 'll', {tolerance: TOLERANCE});
    });

    it('Зум', function () {
        return this.browser
            .crInit('MANY_MAPS', '?z=1')
            .crWaitForVisible(PO.ymaps.map(), 'API не загрузилось');
    });

    it('Зум больше допустимого', function () {
        return this.browser
            .crInit('MANY_MAPS', '?ll=40.927570%2C57.768962&z=900')
            .crWaitForVisible(PO.ymaps.map(), 'API не загрузилось')
            .pause(DRAW_TIMEOUT)
            .crVerifyScreenshot(PO.ymaps.map(), 'z-too-much', {tolerance: TOLERANCE});
    });

    it('Зум отрицательный', function () {
        return this.browser
            .crInit('MANY_MAPS', '?ll=40.927570%2C57.768962&z=-900')
            .crWaitForVisible(PO.ymaps.map(), 'API не загрузилось')
            .pause(DRAW_TIMEOUT)
            .crVerifyScreenshot(PO.ymaps.map(), 'z-too-little', {tolerance: TOLERANCE});
    });

    it('Недопустимый центр', function () {
        return this.browser
            .crInit('MANY_MAPS', '?ll=4000%2C5700')
            .crWaitForVisible(PO.ymaps.map(), 'API не загрузилось')
            .pause(DRAW_TIMEOUT)
            .crVerifyScreenshot(PO.ymaps.map(), 'll-too-much', {tolerance: TOLERANCE});
    });

    it('Некорректный центр карты', function () {
        return this.browser
            .crInit('MANY_MAPS', '?ll=9.927570%2C57.abc')
            .crWaitForVisible(PO.ymaps.map(), 'API не загрузилось');
    });

    it('Некорректный зум', function () {
        return this.browser
            .crInit('MANY_MAPS', '?z=abc')
            .crWaitForVisible(PO.ymaps.map(), 'API не загрузилось');
    });

    it('Тип экспорта – interactive', function () {
        return this.browser
            .crInit('MANY_MAPS', '?exportType=interactive')
            .crSaveMap()
            .crShouldBeVisible(PO.sidebarExport.staticSwitcher.interactiveChecked())
            .crWaitForVisible(PO.mapEditorPreviewHolder(), 'Не отображается превью на карте');
    });

    it('Тип экспорта – static', function () {
        return this.browser
            .crInit('MANY_MAPS', '?exportType=static')
            .crSaveMap()
            .crShouldBeVisible(PO.sidebarExport.staticSwitcher.staticChecked())
            .crWaitForVisible(PO.mapEditorPreviewHolder(), 'Не отображается превью на карте');
    });

    it('Тип экспорта – print', function () {
        return this.browser
            .crInit('MANY_MAPS', '?exportType=print')
            .crSaveMap()
            .crShouldBeVisible(PO.sidebarExport.mainSwitcher.printChecked())
            .crWaitForVisible(PO.mapEditorPreviewHolder(), 'Не отображается превью на карте');
    });

    it('Корректный um', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.umForCopy)
            .crWaitForVisible(PO.ymaps.map(), 'API не загрузилось')
            .pause(DRAW_TIMEOUT)
            .crVerifyScreenshot(PO.ymaps.map(), 'um-map', {tolerance: TOLERANCE})
            .crVerifyScreenshot(PO.geoObjectList(), 'um-list');
    });

    it('Некорректный um', function () {
        return this.browser
            .crInit('MANY_MAPS', '?um=12345')
            .crWaitForVisible(PO.ymaps.map(), 'API не загрузилось');
    });

    it('Несуществующий параметр', function () {
        return this.browser
            .crInit('MANY_MAPS', '?pac=pacpac')
            .crWaitForVisible(PO.ymaps.map(), 'API не загрузилось');
    });
});
