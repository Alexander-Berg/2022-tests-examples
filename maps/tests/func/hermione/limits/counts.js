const DRAW_TIMEOUT = 300;
const LIMITS = require('../../limits.js');
const genText = require('../../utils/genText.js');
const maxName = genText(LIMITS.size.mapName);
const maxDescription = genText(LIMITS.size.mapDescription);

require('../helper.js')(afterEach);

describe('Лимиты', () => {
    beforeEach(function () {
        return this.browser
            .crInit('MANY_MAPS')
            .crWaitForVisible(PO.sidebar.mapDesc(), 'Не появился инпут описания карты');
    });
    afterEach(function () {
        return this.browser.crLogout();
    });

    it('Сохранение карты с максимально длинным названием и описанием', function () {
        return this.browser
            .crSetValue(PO.sidebar.mapName(), maxName)
            .crSetValue(PO.sidebar.mapDesc(), maxDescription)
            .crSaveMap();
    });

    it('Создание копии карты с максимально длинным названием и описанием', function () {
        return this.browser
            .crSetValue(PO.sidebar.mapName(), maxName)
            .crSetValue(PO.sidebar.mapDesc(), maxDescription)
            .crSaveMap()
            .click(PO.mapListButton())
            .crWaitForVisible(PO.mapSelection(), 'Не открылся список карт')
            .getText(PO.mapSelection.itemFirstName()).then((text) =>
                assert.equal(0, maxName.indexOf(text), 'Нет в списке сохраненной карты')
            )
            .click(PO.mapSelection.itemFirstMenuBtn())
            .crShouldBeVisible(PO.mapSelectionMenu())
            .click(PO.mapSelectionMenu.menuCopy())
            .crShouldNotBeVisible(PO.mapSelectionMenu())
            .crWaitForVisible(PO.mapSelection.itemFirst(), 'Не появилась первая карта в списке')
            .getText(PO.mapSelection.itemFirstName()).then((text) =>
                assert.equal(0, ('(Копия) ' + maxName).indexOf(text), 'Название не соответствует копии карты')
            )
            .click(PO.mapSelection.itemFirst())
            .crWaitForVisible(PO.stepEditor(), 'Не открылся шаг редактирования карты')
            .crWaitForVisible(PO.sidebar.mapName(), 'Не появился инпут названия карты')
            .getValue(PO.sidebar.mapName()).then((name) =>
                assert.lengthOf(name, LIMITS.size.mapName, 'Длина названия не соотвестствует лимиту')
            );
    });

    it('Создание метки с максимально длиной подписью', function () {
        return this.browser
            .crWaitForVisible(PO.ymaps.addPlacemark(), 'Не появилась кнопка добавления метки на карту')
            .click(PO.ymaps.addPlacemark())
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crShouldBeVisible(PO.balloon.captionInput())
            .crSetValue(PO.balloon.captionInput(), maxName)
            .crShouldBeVisible(PO.ymaps.placemark.iconWithCaption())
            .click(PO.balloon.save())
            .crSaveMap();
    });

    it('Создание линии с максимально длинным описанием', function () {
        return this.browser
            .crWaitForVisible(PO.ymaps.addLine(), 'Не появилась кнопка добавления метки на карту')
            .click(PO.ymaps.addLine())
            .leftClick(PO.ymaps.map(), 200, 200)
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 250, 250)
            .keys('Enter')
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crCheckText(PO.geoObjectList.itemLinestringTitle(), 'Без описания')
            .crSetValue(PO.balloon.text(), maxDescription)
            .getText(PO.geoObjectList.itemLinestringTitle())
            .then((text) => {
                assert.equal(0, maxDescription.indexOf(text), 'Неверное описание объекта в списке');
            })
            .crSaveMap();
    });

    it('Сохранение линии из 1 вершины', function () {
        return this.browser
            .crSetValue(PO.sidebar.mapName(), 'Линии из 1 вершины')
            .crWaitForVisible(PO.ymaps.addLine(), 'Не появилась кнопка добавления линии на карту')
            .click(PO.ymaps.addLine())
            .leftClick(PO.ymaps.map(), 200, 200)
            .pause(DRAW_TIMEOUT)
            .keys('Enter')
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .pause(DRAW_TIMEOUT)
            .crWaitForVisible(PO.balloon.save(), 'Не появилась кнопка "сохранить"')
            .click(PO.balloon.save())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
            .leftClick(PO.ymaps.map(), 250, 250)
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 2, 'в списке объектов должно быть 2 элемента')
            )
            .crSaveMap()
            .crOpenMap('Линии из 1 вершины')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 2, 'в списке объектов должно быть 2 элемента')
            );
    });

    it('Сохранение полигонов из 1-3 вершин', function () {
        return this.browser
            .crSetValue(PO.sidebar.mapName(), 'Полигон из 1-3 вершин')
            .crWaitForVisible(PO.ymaps.addPolygon(), 'Не появилась кнопка добавления полигона на карту')
            .click(PO.ymaps.addPolygon())
            .leftClick(PO.ymaps.map(), 200, 200)
            .pause(DRAW_TIMEOUT)
            .keys('Enter')
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .pause(DRAW_TIMEOUT)
            .click(PO.balloon.save())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
            .leftClick(PO.ymaps.map(), 250, 250)
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 255, 270)
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 260, 280)
            .pause(DRAW_TIMEOUT)
            .keys('Enter')
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .pause(DRAW_TIMEOUT)
            .click(PO.balloon.save())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
            .leftClick(PO.ymaps.map(), 255, 255)
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 255, 255)
            .crWaitForVisible(PO.ymaps.vertexMenu(), 'Не открылось меню при клике на вершину полигона')
            .click(PO.ymaps.vertexMenu.item() + '[item-index="2"]')
            .crWaitForHidden(PO.ymaps.vertexMenu(), 'Не закрылось меню полигона')
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 275, 275)
            .keys('Enter')
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .pause(DRAW_TIMEOUT)
            .click(PO.balloon.save())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
            .crSaveMap()
            .crOpenMap('Полигон из 1-3 вершин')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 3, 'в списке объектов должно быть 3 элемента')
            );
    });
});
