const DRAW_TIMEOUT = 400;

require('../helper.js')(afterEach);

describe('API / Editor', () => {
    beforeEach(function () {
        return this.browser
            .crInit('MANY_MAPS', '/?ll=30.297152%2C59.956357&z=13');
    });

    afterEach(function () {
        return this.browser
            .crSaveMap()
            .crLogout();
    });

    it('Удаление вершин линии через меню', function () {
        return this.browser
            .crWaitForVisible(PO.ymaps.addLine(), 'Не появилась кнопка добавления линии на карту')
            .click(PO.ymaps.addLine())
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.geoObjectList.itemLinestring(), 'Должна появиться линия в списке')
            .crShouldNotBeVisible(PO.geoObjectList.itemLinestringDistance())
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 250, 250)
            .pause(DRAW_TIMEOUT)
            .crShouldBeVisible(PO.geoObjectList.itemLinestringDistance())
            .crCheckText(PO.geoObjectList.itemLinestringDistance(), '678 м', 'Длина линии')
            .leftClick(PO.ymaps.map(), 250, 250)
            .crWaitForVisible(PO.ymaps.vertexMenu(), 'Не открылось меню при клике на вершину линии')
            .click(PO.ymaps.vertexMenu.item() + '[item-index="0"]')
            .crWaitForHidden(PO.ymaps.vertexMenu(), 'Не закрылось меню линии')
            .pause(DRAW_TIMEOUT)
            .crShouldNotBeVisible(PO.geoObjectList.itemLinestringDistance())
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.ymaps.vertexMenu(), 'Не открылось меню при клике на вершину линии')
            .click(PO.ymaps.vertexMenu.item() + '[item-index="0"]')
            .crWaitForHidden(PO.ymaps.vertexMenu(), 'Не закрылось меню линии')
            .crWaitForHidden(PO.geoObjectList.itemLinestring(), 'Должна исчезнуть линия в списке');
    });

    it('Удаление вершин линии даблкликом', function () {
        return this.browser
            .crWaitForVisible(PO.ymaps.addLine(), 'Не появилась кнопка добавления линии на карту')
            .click(PO.ymaps.addLine())
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.geoObjectList.itemLinestring(), 'Должна появиться линия в списке')
            .crShouldNotBeVisible(PO.geoObjectList.itemLinestringDistance())
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 250, 250)
            .pause(DRAW_TIMEOUT)
            .crShouldBeVisible(PO.geoObjectList.itemLinestringDistance())
            .crCheckText(PO.geoObjectList.itemLinestringDistance(), '678 м', 'Длина линии')
            .leftClick(PO.ymaps.map(), 250, 250)
            .leftClick(PO.ymaps.map(), 250, 250)
            .pause(DRAW_TIMEOUT)
            .crShouldNotBeVisible(PO.geoObjectList.itemLinestringDistance())
            .leftClick(PO.ymaps.map(), 200, 200)
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForHidden(PO.geoObjectList.itemLinestring(), 'Должна исчезнуть линия в списке');
    });

    it('Удаление вершин полигона через меню', function () {
        return this.browser
            .crWaitForVisible(PO.ymaps.addPolygon(), 'Не появилась кнопка добавления полигона на карту')
            .click(PO.ymaps.addPolygon())
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.geoObjectList.itemPolygon(), 'Должен появиться полигон в списке')
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 250, 250)
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 250, 250)
            .crWaitForVisible(PO.ymaps.vertexMenu(), 'Не открылось меню при клике на вершину полигона')
            .click(PO.ymaps.vertexMenu.item() + '[item-index="0"]')
            .crWaitForHidden(PO.ymaps.vertexMenu(), 'Не закрылось меню полигона')
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.ymaps.vertexMenu(), 'Не открылось меню при клике на вершину полигона')
            .click(PO.ymaps.vertexMenu.item() + '[item-index="0"]')
            .crWaitForHidden(PO.ymaps.vertexMenu(), 'Не закрылось меню полигона')
            .crWaitForHidden(PO.geoObjectList.itemPolygon(), 'Должен исчезнуть полигон в списке');
    });

    it('Удаление вершин полигона даблкликом', function () {
        return this.browser
            .crWaitForVisible(PO.ymaps.addPolygon(), 'Не появилась кнопка добавления полигона на карту')
            .click(PO.ymaps.addPolygon())
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.geoObjectList.itemPolygon(), 'Должен появиться полигон в списке')
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 250, 250)
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 250, 250)
            .leftClick(PO.ymaps.map(), 250, 250)
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 200, 200)
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForHidden(PO.geoObjectList.itemPolygon(), 'Должен исчезнуть полигон в списке');
    });
});
