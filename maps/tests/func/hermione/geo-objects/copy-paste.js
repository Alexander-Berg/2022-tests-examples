require('../helper.js')(afterEach);
const OPTIONS = {
    tolerance: 20,
    ignoreElements: [PO.ymaps.searchBoxInput()]
};

describe('Копирование объектов', () => {
    beforeEach(function () {
        return this.browser
            .crInit('MANY_MAPS');
    });

    afterEach(function () {
        return this.browser
            .crSaveMap()
            .crLogout();
    });

    it('Копирование метки', function () {
        return this.browser
            .crWaitForVisible(PO.ymaps.addPlacemark(), 'Не появилась кнопка добавления метки на карту')
            .click(PO.ymaps.addPlacemark())
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .pause(500)
            .click(PO.balloon.type.number())
            .pause(500)
            .crShouldBeVisible(PO.balloon.numberInput())
            .setValue(PO.balloon.numberInput(), '555')
            .setValue(PO.balloon.captionInput(), 'Подпись')
            .setValue(PO.balloon.text(), 'Описание')
            .crCheckText(PO.geoObjectList.itemPointCompactNumberTitle(), '(Подпись) Описание',
                'описание метки')
            .crCheckText(PO.geoObjectList.itemPointCompactNumberIcon(), '555', 'Должно быть значение 555')
            .click(PO.balloon.save())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 1, 'Должен быть 1 элемент в списке')
            )
            .click(PO.geoObjectList.itemPointCompactNumber())
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crPressKey(['Control', 'c'])
            .crPressKey(['Control', 'v'])
            .crPressKey(['Control', 'v'])
            .crPressKey(['Control', 'v'])
            .crPressKey(['Control', 'v'])
            .crPressKey(['Control', 'v'])
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 6, 'Должно быть 6 элементов в списке')
            )
            .crVerifyScreenshot(PO.geoObjectList(), 'placemarks-list');
    });

    it('Копирование линии', function () {
        return this.browser
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/copy-paste/linestring.kml')
            .crWaitForVisible(PO.stepEditor(), 'Не открылся шаг редактирования после импорта')
            .crWaitForVisible(PO.geoObjectList.itemLinestring(), ' Не появился полигон в списке объектов')
            .click(PO.geoObjectList.itemLinestring())
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crPressKey(['Control', 'c'])
            .crPressKey(['Control', 'v'])
            .crPressKey(['Control', 'v'])
            .crPressKey(['Control', 'v'])
            .crPressKey(['Control', 'v'])
            .crPressKey(['Control', 'v'])
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 6, 'Должно быть 6 элементов в списке')
            )
            .crVerifyScreenshot(PO.geoObjectList(), 'linestring-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'linestring-map', OPTIONS);
    });

    it('Копирование полигона', function () {
        return this.browser
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/copy-paste/polygon.kml')
            .crWaitForVisible(PO.stepEditor(), 'Не открылся шаг редактирования после импорта')
            .crWaitForVisible(PO.geoObjectList.itemPolygon(), ' Не появился полигон в списке объектов')
            .click(PO.geoObjectList.itemPolygon())
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crPressKey(['Control', 'c'])
            .crPressKey(['Control', 'v'])
            .crPressKey(['Control', 'v'])
            .crPressKey(['Control', 'v'])
            .crPressKey(['Control', 'v'])
            .crPressKey(['Control', 'v'])
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 6, 'Должно быть 6 элементов в списке')
            )
            .crVerifyScreenshot(PO.geoObjectList(), 'polygons-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'polygons-map', OPTIONS);
    });
});
