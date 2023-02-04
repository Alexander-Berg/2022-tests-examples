const DRAW_TIMEOUT = 500;
const AUTOPAN_TIMEOUT = 300;
const EXCLUDES = {
    ignoreElements: [PO.ymaps.searchBoxInput()]
};

require('../helper.js')(afterEach);

describe('Балун / Неточечные объекты', () => {
    const drawPolygon = function () {
        return this
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 200, 200)
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 500, 200)
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 500, 400)
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 200, 400)
            .pause(DRAW_TIMEOUT);
    };

    const drawLine = function () {
        return this
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 200, 200)
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 250, 250)
            .pause(DRAW_TIMEOUT);
    };

    beforeEach(function () {
        return this.browser
            .crInit('MANY_MAPS', '?ll=13.060607%2C83.424304&z=8')
            .crWaitForVisible(PO.ymaps.addLine(), 'Не появилась кнопка добавления метки на карту');
    });

    afterEach(function () {
        return this.browser
            .crSaveMap()
            .crLogout();
    });

    it('Создание линии с дефолтными настройками', function () {
        return this.browser
            .click(PO.ymaps.addLine())
            .then(drawLine)
            .crShouldBeVisible(PO.geoObjectList.itemLinestring())
            .keys('Enter')
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crWaitForVisible(PO.balloon.selectColor(), 'Не появилась кнопка открытия выбора цвета')
            .crCheckText(PO.balloon.selectColor.text(), '7', 'дефолтный цвет в селекторе')
            .crCheckValue(PO.balloon.transparency(), '10', 'дефолтное значение прозрачности')
            .getAttribute(PO.balloon.transparency(), 'maxlength').then((val) => {
                assert.strictEqual(val, '3', 'максимальная длина инпута');
            })
            .crCheckValue(PO.balloon.width(), '5', 'дефолтное значение толщины')
            .getAttribute(PO.balloon.width(), 'maxlength').then((val) => {
                assert.strictEqual(val, '2', 'максимальная длина инпута');
            })
            .crCheckText(PO.geoObjectList.itemLinestringTitle(), 'Без описания')
            .click(PO.balloon.save())
            .crVerifyScreenshot(PO.ymaps.map(), 'line-default-map', EXCLUDES)
            .crVerifyScreenshot(PO.geoObjectList(), 'line-default-list');
    });

    it('Создание линии максимальной толщины и прозрачности', function () {
        return this.browser
            .click(PO.ymaps.addLine())
            .then(drawLine)
            .crShouldBeVisible(PO.geoObjectList.itemLinestring())
            .keys('Enter')
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crSetValue(PO.balloon.width(), '99')
            .crCheckValue(PO.balloon.width(), '99', 'значение толщины')
            .crSetValue(PO.balloon.transparency(), '999')
            .crCheckValue(PO.balloon.transparency(), '100', 'значение прозрачности')
            .click(PO.balloon.save())
            .crVerifyScreenshot(PO.ymaps.map(), 'line-max-map', EXCLUDES)
            .crVerifyScreenshot(PO.geoObjectList(), 'line-max-list');
    });

    it('Создание линии минимальной толщины и прозрачности', function () {
        return this.browser
            .click(PO.ymaps.addLine())
            .then(drawLine)
            .crShouldBeVisible(PO.geoObjectList.itemLinestring())
            .keys('Enter')
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crSetValue(PO.balloon.width(), '0')
            .crCheckValue(PO.balloon.width(), '0', 'значение толщины')
            .crSetValue(PO.balloon.transparency(), '0')
            .crCheckValue(PO.balloon.transparency(), '0', 'значение прозрачности')
            .click(PO.balloon.save())
            .crVerifyScreenshot(PO.ymaps.map(), 'line-min-map', EXCLUDES)
            .crVerifyScreenshot(PO.geoObjectList(), 'line-min-list');
    });

    it('Редактирование свойств линии', function () {
        return this.browser
            .click(PO.ymaps.addLine())
            .then(drawLine)
            .crShouldBeVisible(PO.geoObjectList.itemLinestring())
            .keys('Enter')
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crWaitForVisible(PO.balloon.selectColor(), 'Не появилась кнопка открытия выбора цвета')
            .click(PO.balloon.selectColor())
            .crWaitForVisible(PO.balloonColorMenu(), 500, 'Не появилось меню выбора цвета')
            .click(PO.balloonColorMenu.item() + 14)
            .crWaitForHidden(PO.balloonColorMenu(), 200, 'Меню выбора цвета не закрылось')
            .crCheckText(PO.balloon.selectColor.text(), '14', '14 цвет')
            .crSetValue(PO.balloon.width(), '10')
            .crSetValue(PO.balloon.transparency(), '20')
            .pause(200)
            .click(PO.balloon.save())
            .crVerifyScreenshot(PO.ymaps.map(), 'line-edit-before-map', EXCLUDES)
            .crVerifyScreenshot(PO.geoObjectList(), 'line-edit-before-list-1')
            .crSetValue(PO.sidebar.mapName(), 'Редактирование свойств линии')
            .crSaveMap()
            .crOpenMap('Редактирование свойств линии')
            .crWaitForVisible(PO.geoObjectList.itemLinestring(), 'Не появилась линия в списке объектов')
            .click(PO.geoObjectList.itemLinestring())
            .crVerifyScreenshot(PO.geoObjectList(), 'line-edit-before-list-2')
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crCheckText(PO.balloon.selectColor.text(), '14', '14 цвет')
            .crCheckValue(PO.balloon.width(), '10', 'значение толщины')
            .crCheckValue(PO.balloon.transparency(), '20', 'значение прозрачности')
            .crSetValue(PO.balloon.width(), '15')
            .crSetValue(PO.balloon.transparency(), '25')
            .click(PO.balloon.selectColor())
            .crWaitForVisible(PO.balloonColorMenu(), 500, 'Не появилось меню выбора цвета')
            .click(PO.balloonColorMenu.item() + 12)
            .crWaitForHidden(PO.balloonColorMenu(), 200, 'Меню выбора цвета не закрылось')
            .crCheckText(PO.balloon.selectColor.text(), '12', '12 цвет')
            .pause(200)
            .click(PO.balloon.save())
            .crVerifyScreenshot(PO.ymaps.map(), 'line-edit-after-map', EXCLUDES)
            .crVerifyScreenshot(PO.geoObjectList(), 'line-edit-after-list');
    });

    it('Удаление линии', function () {
        return this.browser
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 0, 'не должно быть объектов в списке')
            )
            .click(PO.ymaps.addLine())
            .then(drawLine)
            .keys('Enter')
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент в списке')
            )
            .pause(AUTOPAN_TIMEOUT)
            .click(PO.balloon.remove())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун 1')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 0, 'не должно быть объектов в списке после удаления')
            )
            .then(drawLine)
            .keys('Enter')
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент в списке')
            )
            .pause(AUTOPAN_TIMEOUT)
            .click(PO.balloon.save())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун 2')
            .click(PO.geoObjectList.itemLinestring())
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .pause(AUTOPAN_TIMEOUT)
            .click(PO.balloon.remove())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун 3')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 0, 'не должно быть объектов в списке после удаления')
            )
            .crVerifyScreenshot(PO.ymaps.map(), 'line-remove-map', EXCLUDES)
            .crVerifyScreenshot(PO.sidebar(), 'line-remove-list');
    });

    it('Создание полигона с дефолтными настройками', function () {
        return this.browser
            .click(PO.ymaps.addPolygon())
            .then(drawPolygon)
            .crShouldBeVisible(PO.geoObjectList.itemPolygon())
            .keys('Enter')
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crWaitForVisible(PO.balloon.selectColor(), 'Не появилась кнопка открытия выбора цвета')
            .crCheckText(PO.balloon.poly.strokeColor.text(), '7', '7 цвет')
            .crCheckText(PO.balloon.poly.fillColor.text(), '7', '7 цвет')
            .crCheckValue(PO.balloon.poly.strokeOpacity(), '10', 'дефолтное значение прозрачности')
            .getAttribute(PO.balloon.poly.strokeOpacity(), 'maxlength').then((val) => {
                assert.strictEqual(val, '3', 'максимальная длина инпута');
            })
            .crCheckValue(PO.balloon.width(), '5', 'дефолтное значение толщины')
            .getAttribute(PO.balloon.width(), 'maxlength').then((val) => {
                assert.strictEqual(val, '2', 'максимальная длина инпута');
            })
            .crCheckValue(PO.balloon.poly.fillOpacity(), '40', 'дефолтное значение прозрачности заливки')
            .getAttribute(PO.balloon.poly.fillOpacity(), 'maxlength').then((val) => {
                assert.strictEqual(val, '3', 'максимальная длина инпута');
            })
            .crCheckText(PO.geoObjectList.itemPolygonTitle(), 'Без описания')
            .click(PO.balloon.save())
            .crVerifyScreenshot(PO.ymaps.map(), 'polygon-default-map', EXCLUDES)
            .crVerifyScreenshot(PO.geoObjectList(), 'polygon-default-list');
    });

    it('Создание полигона с контуром и заливкой максимальной толщины и прозрачности', function () {
        return this.browser
            .click(PO.ymaps.addPolygon())
            .then(drawPolygon)
            .keys('Enter')
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crWaitForVisible(PO.balloon.selectColor(), 'Не появилась кнопка открытия выбора цвета')
            .crSetValue(PO.balloon.width(), '99')
            .crCheckValue(PO.balloon.width(), '99', 'значение толщины')
            .crSetValue(PO.balloon.poly.strokeOpacity(), '999')
            .crCheckValue(PO.balloon.poly.strokeOpacity(), '100', 'значение прозрачности контура')
            .crSetValue(PO.balloon.poly.fillOpacity(), '999')
            .crCheckValue(PO.balloon.poly.fillOpacity(), '100', 'значение прозрачности заливки')
            .click(PO.balloon.save())
            .crVerifyScreenshot(PO.ymaps.map(), 'polygon-max-map', EXCLUDES)
            .crVerifyScreenshot(PO.geoObjectList(), 'polygon-max-list');
    });

    it('Создание полигона с контуром и заливкой минимальной толщины и прозрачности', function () {
        return this.browser
            .click(PO.ymaps.addPolygon())
            .then(drawPolygon)
            .keys('Enter')
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crWaitForVisible(PO.balloon.poly.strokeColor(), 'Не появилась кнопка открытия выбора цвета')
            .crSetValue(PO.balloon.width(), '0')
            .crCheckValue(PO.balloon.width(), '0', 'значение толщины')
            .crSetValue(PO.balloon.poly.strokeOpacity(), '0')
            .crCheckValue(PO.balloon.poly.strokeOpacity(), '0', 'значение прозрачности контура')
            .crSetValue(PO.balloon.poly.fillOpacity(), '0')
            .crCheckValue(PO.balloon.poly.fillOpacity(), '0', 'значение прозрачности заливки')
            .click(PO.balloon.save())
            .crVerifyScreenshot(PO.ymaps.map(), 'polygon-min-map', EXCLUDES)
            .crVerifyScreenshot(PO.geoObjectList(), 'polygon-min-list');
    });

    it('Редактирование свойств полигона', function () {
        return this.browser
            .click(PO.ymaps.addPolygon())
            .then(drawPolygon)
            .keys('Enter')
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crWaitForVisible(PO.balloon.poly.strokeColor(), 'Не появилась кнопка выбора цвета контура')
            .click(PO.balloon.poly.strokeColor())
            .crWaitForVisible(PO.balloonColorMenu(), 500, 'Не появилось меню выбора цвета')
            .click(PO.balloonColorMenu.item() + 14)
            .crWaitForHidden(PO.balloonColorMenu(), 200, 'Меню выбора цвета не закрылось')
            .crCheckText(PO.balloon.poly.strokeColor.text(), '14', '14 цвет')
            .click(PO.balloon.poly.fillColor())
            .crWaitForVisible(PO.balloonColorMenu(), 500, 'Не появилось меню выбора цвета')
            .click(PO.balloonColorMenu.item() + 12)
            .crWaitForHidden(PO.balloonColorMenu(), 200, 'Меню выбора цвета не закрылось')
            .crCheckText(PO.balloon.poly.fillColor.text(), '12', '12 цвет')
            .crSetValue(PO.balloon.width(), '10')
            .crSetValue(PO.balloon.poly.strokeOpacity(), '20')
            .crSetValue(PO.balloon.poly.fillOpacity(), '60')
            .crCheckValue(PO.balloon.poly.fillOpacity(), '60', 'значение прозрачности заливки')
            .click(PO.balloon.save())
            .crVerifyScreenshot(PO.ymaps.map(), 'polygon-edit-before-map', EXCLUDES)
            .crVerifyScreenshot(PO.geoObjectList(), 'polygon-edit-before-list-1')
            .crSetValue(PO.sidebar.mapName(), 'Редактирование свойств полигона')
            .crSaveMap()
            .crOpenMap('Редактирование свойств полигона')
            .crWaitForVisible(PO.geoObjectList.itemPolygon(), ' Не появился полигон в списке объектов')
            .click(PO.geoObjectList.itemPolygon())
            .crVerifyScreenshot(PO.geoObjectList(), 'polygon-edit-before-list-2')
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crCheckText(PO.balloon.poly.strokeColor.text(), '14', '14 цвет')
            .crCheckText(PO.balloon.poly.fillColor.text(), '12', '12 цвет')
            .crCheckValue(PO.balloon.width(), '10', 'значение толщины')
            .crCheckValue(PO.balloon.poly.strokeOpacity(), '20', 'значение прозрачности контура')
            .crCheckValue(PO.balloon.poly.fillOpacity(), '60', 'значение прозрачности заливки')
            .crSetValue(PO.balloon.width(), '15')
            .crSetValue(PO.balloon.poly.strokeOpacity(), '25')
            .crSetValue(PO.balloon.poly.fillOpacity(), '65')
            .click(PO.balloon.poly.strokeColor())
            .crWaitForVisible(PO.balloonColorMenu(), 500, 'Не появилось меню выбора цвета')
            .click(PO.balloonColorMenu.item() + 4)
            .crWaitForHidden(PO.balloonColorMenu(), 200, 'Меню выбора цвета не закрылось')
            .crCheckText(PO.balloon.poly.strokeColor.text(), '4', '4 цвет')
            .click(PO.balloon.poly.fillColor())
            .crWaitForVisible(PO.balloonColorMenu(), 500, 'Не появилось меню выбора цвета')
            .click(PO.balloonColorMenu.item() + 3)
            .crWaitForHidden(PO.balloonColorMenu(), 200, 'Меню выбора цвета не закрылось')
            .crCheckText(PO.balloon.poly.fillColor.text(), '3', '3 цвет')
            .click(PO.balloon.save())
            .crVerifyScreenshot(PO.ymaps.map(), 'polygon-edit-after-map', EXCLUDES)
            .crVerifyScreenshot(PO.geoObjectList(), 'polygon-edit-after-list');
    });

    it('Удаление полигона', function () {
        return this.browser
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 0, 'не должно быть объектов в списке')
            )
            .click(PO.ymaps.addPolygon())
            .then(drawPolygon)
            .keys('Enter')
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент в списке')
            )
            .pause(AUTOPAN_TIMEOUT)
            .click(PO.balloon.remove())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун 1')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 0, 'не должно быть объектов в списке после удаления')
            )
            .then(drawPolygon)
            .keys('Enter')
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент в списке')
            )
            .pause(AUTOPAN_TIMEOUT)
            .click(PO.balloon.save())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун 2')
            .click(PO.geoObjectList.itemPolygon())
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .pause(AUTOPAN_TIMEOUT)
            .click(PO.balloon.remove())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун 3')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 0, 'не должно быть объектов в списке после удаления')
            )
            .crVerifyScreenshot(PO.ymaps.map(), 'polygon-remove-map', EXCLUDES)
            .crVerifyScreenshot(PO.sidebar(), 'polygon-remove-list');
    });
});
