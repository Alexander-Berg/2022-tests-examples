const DRAW_TIMEOUT = 300;
const cr = require('../../credentials.js');

require('../helper.js')(afterEach);

describe('Горячие клавиши', () => {
    afterEach(function () {
        return this.browser.crLogout();
    });

    describe('Инструменты рисования', () => {
        beforeEach(function () {
            return this.browser
                .crInit('MANY_MAPS', '?ll=13.060607%2C83.424304&z=8');
        });

        afterEach(function () {
            return this.browser.crSaveMap();
        });

        it('Метка – Alt+P', function () {
            return this.browser
                .crWaitForVisible(PO.ymaps.addPlacemark())
                .crVerifyScreenshot(PO.ymaps.addPlacemark(), 'placemark-unchecked-1')
                .crPressKey(['Alt', 'p'])
                .crWaitForVisible(PO.ymaps.addPlacemark.checked(), 'Контрол создания метки не включен')
                .crVerifyScreenshot(PO.ymaps.addPlacemark(), 'placemark-checked-1')
                .leftClick(PO.ymaps.map(), 200, 200)
                .crWaitForVisible(PO.balloon(), 'Не открылся балун')
                .crShouldBeVisible(PO.geoObjectList.itemPointNumber())
                .crPressKey(['Alt', 'p'])
                .crWaitForHidden(PO.ymaps.addPlacemark.checked(), 'Контрол создания метки включен')
                .crVerifyScreenshot(PO.ymaps.addPlacemark(), 'placemark-unchecked-2');
        });

        it('Линия – Alt+L', function () {
            return this.browser
                .crWaitForVisible(PO.ymaps.addLine())
                .crVerifyScreenshot(PO.ymaps.addLine(), 'line-unchecked-1')
                .crPressKey(['Alt', 'l'])
                .crWaitForVisible(PO.ymaps.addLine.checked(), 'Контрол создания линии не включен')
                .crVerifyScreenshot(PO.ymaps.addLine(), 'line-checked-1')
                .leftClick(PO.ymaps.map(), 200, 200)
                .pause(DRAW_TIMEOUT)
                .leftClick(PO.ymaps.map(), 250, 250)
                .pause(DRAW_TIMEOUT)
                .crPressKey(['Alt', 'l'])
                .crWaitForHidden(PO.ymaps.addLine.checked(), 'Контрол создания линии включен')
                .crVerifyScreenshot(PO.ymaps.addLine(), 'line-unchecked-2')
                .crShouldBeVisible(PO.geoObjectList.itemLinestring())
                .crWaitForVisible(PO.balloon(), 'Не открылся балун');
        });

        it('Полигон – Alt+M', function () {
            return this.browser
                .crWaitForVisible(PO.ymaps.addPolygon())
                .crVerifyScreenshot(PO.ymaps.addPolygon(), 'polygon-unchecked-1')
                .crPressKey(['Alt', 'm'])
                .crWaitForVisible(PO.ymaps.addPolygon.checked(), 'Контрол создания полигона не включен')
                .crVerifyScreenshot(PO.ymaps.addPolygon(), 'polygon-checked-1')
                .leftClick(PO.ymaps.map(), 200, 200)
                .pause(DRAW_TIMEOUT)
                .leftClick(PO.ymaps.map(), 250, 250)
                .pause(DRAW_TIMEOUT)
                .crPressKey(['Alt', 'm'])
                .crWaitForHidden(PO.ymaps.addPolygon.checked(), 'Контрол создания полигона включен')
                .crVerifyScreenshot(PO.ymaps.addPolygon(), 'polygon-unchecked-2')
                .crShouldBeVisible(PO.geoObjectList.itemPolygon())
                .crWaitForVisible(PO.balloon(), 'Не открылся балун');
        });

        it('Завершить рисование линии – Enter', function () {
            return this.browser
                .crWaitForVisible(PO.ymaps.addLine())
                .crVerifyScreenshot(PO.ymaps.addLine(), 'line-unchecked-3')
                .crPressKey(['Alt', 'l'])
                .crWaitForVisible(PO.ymaps.addLine.checked(), 'Контрол создания линии не включен')
                .crVerifyScreenshot(PO.ymaps.addLine(), 'line-checked-2')
                .leftClick(PO.ymaps.map(), 200, 200)
                .pause(DRAW_TIMEOUT)
                .leftClick(PO.ymaps.map(), 250, 250)
                .pause(DRAW_TIMEOUT)
                .crPressKey('Enter')
                .crWaitForVisible(PO.ymaps.addLine.checked(), 'Контрол создания линии не включен')
                .crVerifyScreenshot(PO.ymaps.addLine(), 'line-checked-3')
                .elements(PO.ymaps.map()).then((el) =>
                    assert.lengthOf(el.value, 1, 'Рисование объекта не завершилось')
                )
                .crShouldBeVisible(PO.geoObjectList.itemLinestring())
                .crWaitForVisible(PO.balloon(), 'Не открылся балун');
        });

        it('Завершить рисование полигона – Enter', function () {
            return this.browser
                .crWaitForVisible(PO.ymaps.addPolygon())
                .crVerifyScreenshot(PO.ymaps.addPolygon(), 'polygon-unchecked-3')
                .crPressKey(['Alt', 'm'])
                .crWaitForVisible(PO.ymaps.addPolygon.checked(), 'Контрол создания полигона не включен')
                .crVerifyScreenshot(PO.ymaps.addPolygon(), 'polygon-checked-2')
                .leftClick(PO.ymaps.map(), 200, 200)
                .pause(DRAW_TIMEOUT)
                .leftClick(PO.ymaps.map(), 250, 250)
                .pause(DRAW_TIMEOUT)
                .crPressKey('Enter')
                .crWaitForVisible(PO.ymaps.addPolygon.checked(), 'Контрол создания полигона не включен')
                .crVerifyScreenshot(PO.ymaps.addPolygon(), 'polygon-checked-3')
                .elements(PO.ymaps.map()).then((el) =>
                    assert.lengthOf(el.value, 1, 'Рисование объекта не завершилось')
                )
                .crShouldBeVisible(PO.geoObjectList.itemPolygon())
                .crWaitForVisible(PO.balloon(), 'Не открылся балун');
        });

        it('Закрыть балун – Enter', function () {
            return this.browser
                .crWaitForVisible(PO.ymaps.addPlacemark())
                .crVerifyScreenshot(PO.ymaps.addPlacemark(), 'placemark-unchecked-3')
                .crPressKey(['Alt', 'p'])
                .crWaitForVisible(PO.ymaps.addPlacemark.checked(), 'Контрол создания метки не включен')
                .crVerifyScreenshot(PO.ymaps.addPlacemark(), 'placemark-checked-2')
                .leftClick(PO.ymaps.map(), 200, 200)
                .crWaitForVisible(PO.balloon(), 'Не открылся балун')
                .crPressKey('Enter')
                .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
                .crWaitForVisible(PO.ymaps.addPlacemark.checked(), 'Контрол создания метки выключился после Enter')
                .crVerifyScreenshot(PO.ymaps.addPlacemark(), 'placemark-checked-3');
        });

        it('Выключить рисование метки – Esc', function () {
            return this.browser
                .crWaitForVisible(PO.ymaps.addPlacemark())
                .crVerifyScreenshot(PO.ymaps.addPlacemark(), 'placemark-unchecked-4')
                .crPressKey(['Alt', 'p'])
                .crWaitForVisible(PO.ymaps.addPlacemark.checked(), 'Контрол создания метки не включен')
                .crVerifyScreenshot(PO.ymaps.addPlacemark(), 'placemark-checked-4')
                .crPressKey('Escape')
                .crWaitForHidden(PO.ymaps.addPlacemark.checked(), 'Контрол создания метки включен после Esc')
                .crVerifyScreenshot(PO.ymaps.addPlacemark(), 'placemark-unchecked-5');
        });

        it('Выключить рисование линии – Esc', function () {
            return this.browser
                .crWaitForVisible(PO.ymaps.addLine())
                .crVerifyScreenshot(PO.ymaps.addLine(), 'line-unchecked-4')
                .crPressKey(['Alt', 'l'])
                .crWaitForVisible(PO.ymaps.addLine.checked(), 'Контрол создания линии не включен')
                .crVerifyScreenshot(PO.ymaps.addLine(), 'line-checked-4')
                .crPressKey('Escape')
                .crWaitForHidden(PO.ymaps.addLine.checked(), 'Контрол создания линии включен после Esc')
                .crVerifyScreenshot(PO.ymaps.addLine(), 'line-unchecked-5')
                .crPressKey(['Alt', 'l'])
                .crWaitForVisible(PO.ymaps.addLine.checked(), 'Контрол создания линии не включен')
                .crVerifyScreenshot(PO.ymaps.addLine(), 'line-checked-5')
                .pause(DRAW_TIMEOUT)
                .leftClick(PO.ymaps.map(), 200, 200)
                .pause(DRAW_TIMEOUT)
                .leftClick(PO.ymaps.map(), 250, 250)
                .pause(DRAW_TIMEOUT)
                .crPressKey('Escape')
                .crWaitForVisible(PO.balloon(), 'Не открылся балун')
                .crWaitForHidden(PO.ymaps.addLine.checked(), 'Контрол создания линии включен после Esc')
                .crVerifyScreenshot(PO.ymaps.addLine(), 'line-unchecked-6');
        });

        it('Выключить рисование полигона – Esc', function () {
            return this.browser
                .crWaitForVisible(PO.ymaps.addPolygon())
                .crVerifyScreenshot(PO.ymaps.addPolygon(), 'polygon-unchecked-4')
                .crPressKey(['Alt', 'm'])
                .crWaitForVisible(PO.ymaps.addPolygon.checked(), 'Контрол создания полигона не включен')
                .crVerifyScreenshot(PO.ymaps.addPolygon(), 'polygon-checked-4')
                .crPressKey('Escape')
                .crWaitForHidden(PO.ymaps.addPolygon.checked(), 'Контрол создания полигона включен после Esc')
                .crVerifyScreenshot(PO.ymaps.addPolygon(), 'polygon-unchecked-5')
                .crPressKey(['Alt', 'm'])
                .crWaitForVisible(PO.ymaps.addPolygon.checked(), 'Контрол создания полигона не включен')
                .crVerifyScreenshot(PO.ymaps.addPolygon(), 'polygon-checked-5')
                .pause(DRAW_TIMEOUT)
                .leftClick(PO.ymaps.map(), 200, 200)
                .pause(DRAW_TIMEOUT)
                .leftClick(PO.ymaps.map(), 250, 250)
                .crPressKey('Escape')
                .crWaitForHidden(PO.ymaps.addPolygon.checked(), 'Контрол создания полигона включен после Esc')
                .crWaitForVisible(PO.balloon(), 'Не открылся балун')
                .crVerifyScreenshot(PO.ymaps.addPolygon(), 'polygon-unchecked-6');
        });

        it('Удалить метку – Delete', function () {
            return this.browser
                .crWaitForVisible(PO.ymaps.addPlacemark())
                .crPressKey(['Alt', 'p'])
                .crWaitForVisible(PO.ymaps.addPlacemark.checked(), 'Контрол создания метки не включен')
                .leftClick(PO.ymaps.map(), 200, 200)
                .crWaitForVisible(PO.balloon(), 'Не открылся балун')
                .elements(PO.geoObjectList.item()).then((el) =>
                    assert.lengthOf(el.value, 1, 'Должен быть 1 элемент в списке')
                )
                .crPressKey('Delete')
                .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
                .elements(PO.geoObjectList.item()).then((el) =>
                    assert.lengthOf(el.value, 0, 'Не должно быть элементов в списке')
                );
        });

        it('Удалить линию – Delete', function () {
            return this.browser
                .crWaitForVisible(PO.ymaps.addLine())
                .crPressKey(['Alt', 'l'])
                .crWaitForVisible(PO.ymaps.addLine.checked(), 'Контрол создания линии не включен')
                .pause(DRAW_TIMEOUT)
                .leftClick(PO.ymaps.map(), 200, 200)
                .pause(DRAW_TIMEOUT)
                .leftClick(PO.ymaps.map(), 250, 250)
                .crPressKey('Enter')
                .crWaitForVisible(PO.balloon(), 'Не открылся балун')
                .elements(PO.geoObjectList.item()).then((el) =>
                    assert.lengthOf(el.value, 1, 'Должен быть 1 элемент в списке')
                )
                .crPressKey('Delete')
                .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
                .elements(PO.geoObjectList.item()).then((el) =>
                    assert.lengthOf(el.value, 0, 'Не должно быть элементов в списке')
                );
        });

        it('Удалить полигон – Delete', function () {
            return this.browser
                .crWaitForVisible(PO.ymaps.addPolygon())
                .crPressKey(['Alt', 'm'])
                .crWaitForVisible(PO.ymaps.addPolygon.checked(), 'Контрол создания линии не включен')
                .pause(DRAW_TIMEOUT)
                .leftClick(PO.ymaps.map(), 200, 200)
                .pause(DRAW_TIMEOUT)
                .leftClick(PO.ymaps.map(), 250, 250)
                .crPressKey('Enter')
                .crWaitForVisible(PO.balloon(), 'Не открылся балун')
                .elements(PO.geoObjectList.item()).then((el) =>
                    assert.lengthOf(el.value, 1, 'Должен быть 1 элемент в списке')
                )
                .crPressKey('Delete')
                .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
                .elements(PO.geoObjectList.item()).then((el) =>
                    assert.lengthOf(el.value, 0, 'Не должно быть элементов в списке')
                );
        });

        it('Сочетание разных хоткеев', function () {
            return this.browser
                // Метка – нарисовать, удалить, отменить, скопировать, вернуть, вставить
                .crWaitForVisible(PO.ymaps.addPlacemark())
                .crPressKey(['Alt', 'p'])
                .crWaitForVisible(PO.ymaps.addPlacemark.checked(), 'Контрол создания линии не включен')
                .leftClick(PO.ymaps.map(), 200, 200)
                .crWaitForVisible(PO.balloon(), 'Не открылся балун')
                .elements(PO.geoObjectList.item()).then((el) =>
                    assert.lengthOf(el.value, 1, 'Должен быть 1 элемент в списке')
                )
                .crPressKey('Delete')
                .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
                .elements(PO.geoObjectList.item()).then((el) =>
                    assert.lengthOf(el.value, 0, 'Не должно быть элементов в списке')
                )
                .crPressKey(['Control', 'z'])
                .crWaitForVisible(PO.geoObjectList.itemPointNumber())
                .elements(PO.geoObjectList.item()).then((el) =>
                    assert.lengthOf(el.value, 1, 'Должен быть 1 элемент в списке после отмены удаления')
                )
                .crWaitForVisible(PO.balloon(), 'Не открылся балун')
                .pause(DRAW_TIMEOUT)
                .crPressKey(['Control', 'c'])
                .crPressKey(['Control', 'y'])
                .crWaitForHidden(PO.geoObjectList.itemPointNumber())
                .elements(PO.geoObjectList.item()).then((el) =>
                    assert.lengthOf(el.value, 0, 'Должно быть 0 элементов в списке после Ctrl+Y')
                )
                .crPressKey(['Control', 'v'])
                .elements(PO.geoObjectList.item()).then((el) =>
                    assert.lengthOf(el.value, 1, 'Должен быть 1 элемент в списке после вставки')
                )
                // Линия – нарисовать, удалить, отменить, скопировать, вернуть, вставить
                .crPressKey(['Alt', 'l'])
                .crWaitForVisible(PO.ymaps.addLine.checked(), 'Контрол создания линии не включен')
                .pause(DRAW_TIMEOUT)
                .leftClick(PO.ymaps.map(), 200, 200)
                .pause(DRAW_TIMEOUT)
                .leftClick(PO.ymaps.map(), 250, 250)
                .crPressKey('Enter')
                .crWaitForVisible(PO.balloon(), 'Не открылся балун')
                .elements(PO.geoObjectList.itemLinestring()).then((el) =>
                    assert.lengthOf(el.value, 1, 'Должна быть одна линия в списке')
                )
                .crPressKey('Delete')
                .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
                .elements(PO.geoObjectList.itemLinestring()).then((el) =>
                    assert.lengthOf(el.value, 0, 'Не должно быть линий в списке')
                )
                .crPressKey(['Control', 'z'])
                .crWaitForVisible(PO.geoObjectList.itemLinestring())
                .elements(PO.geoObjectList.itemLinestring()).then((el) =>
                    assert.lengthOf(el.value, 1, 'Должна быть 1 линия в списке после отмены удаления')
                )
                .crWaitForVisible(PO.balloon(), 'Не открылся балун')
                .pause(DRAW_TIMEOUT)
                .crPressKey(['Control', 'c'])
                .pause(DRAW_TIMEOUT)
                .crPressKey(['Control', 'y'])
                .crWaitForHidden(PO.geoObjectList.itemLinestring())
                .elements(PO.geoObjectList.itemLinestring()).then((el) =>
                    assert.lengthOf(el.value, 0, 'Не должно быть линий в списке после Ctrl+Y')
                )
                .crPressKey(['Control', 'v'])
                .elements(PO.geoObjectList.itemLinestring()).then((el) =>
                    assert.lengthOf(el.value, 1, 'Должна быть 1 линия в списке после вставки')
                )
                // Полигон – нарисовать, удалить, отменить, скопировать, вернуть, вставить
                .crPressKey(['Alt', 'm'])
                .crWaitForVisible(PO.ymaps.addPolygon.checked(), 'Контрол создания полигона не включен')
                .pause(DRAW_TIMEOUT)
                .leftClick(PO.ymaps.map(), 200, 200)
                .pause(DRAW_TIMEOUT)
                .leftClick(PO.ymaps.map(), 250, 250)
                .pause(DRAW_TIMEOUT)
                .leftClick(PO.ymaps.map(), 250, 200)
                .crPressKey('Enter')
                .crWaitForVisible(PO.balloon(), 'Не открылся балун')
                .elements(PO.geoObjectList.itemPolygon()).then((el) =>
                    assert.lengthOf(el.value, 1, 'Должна быть один полигон в списке')
                )
                .crPressKey('Delete')
                .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
                .elements(PO.geoObjectList.itemPolygon()).then((el) =>
                    assert.lengthOf(el.value, 0, 'Не должно быть полигонов в списке')
                )
                .crPressKey(['Control', 'z'])
                .crWaitForVisible(PO.geoObjectList.itemPolygon())
                .elements(PO.geoObjectList.itemPolygon()).then((el) =>
                    assert.lengthOf(el.value, 1, 'Должен быть 1 полигон в списке после отмены удаления')
                )
                .crWaitForVisible(PO.balloon(), 'Не открылся балун')
                .pause(DRAW_TIMEOUT)
                .crPressKey(['Control', 'c'])
                .pause(DRAW_TIMEOUT)
                .crPressKey(['Control', 'y'])
                .crWaitForHidden(PO.geoObjectList.itemPolygon())
                .elements(PO.geoObjectList.itemPolygon()).then((el) =>
                    assert.lengthOf(el.value, 0, 'Не должно быть полигонов в списке после Ctrl+Y')
                )
                .crPressKey(['Control', 'v'])
                .elements(PO.geoObjectList.itemPolygon()).then((el) =>
                    assert.lengthOf(el.value, 1, 'Должен быть 1 полигон в списке после вставки')
                )
                // Скриншот списка
                .crVerifyScreenshot(PO.geoObjectList(), 'all-hotkeys-list');
        });
    });

    describe('Отмена-возврат', () => {
        beforeEach(function () {
            return this.browser
                .crInit('MANY_MAPS', '?ll=13.060607%2C83.424304&z=8');
        });

        afterEach(function () {
            return this.browser.crSaveMap();
        });

        it('Ctrl+Z + Ctrl+Y', function () {
            return this.browser
                .crWaitForVisible(PO.ymaps.addPlacemark())
                .crPressKey(['Alt', 'p'])
                .crWaitForVisible(PO.ymaps.addPlacemark.checked(), 'Контрол создания метки не включен')
                .crVerifyScreenshot(PO.ymaps.addPlacemark(), 'placemark-checked-5')
                .pause(DRAW_TIMEOUT)
                .leftClick(PO.ymaps.map(), 200, 200)
                .crWaitForVisible(PO.balloon(), 'Не открылся балун')
                .crShouldBeVisible(PO.geoObjectList.itemPointNumber())
                .elements(PO.geoObjectList.item()).then((el) =>
                    assert.lengthOf(el.value, 1, 'должен быть 1 элемент в списке')
                )
                .pause(DRAW_TIMEOUT)
                .crPressKey(['Control', 'z'])
                .crWaitForHidden(PO.geoObjectList.itemPointNumber())
                .elements(PO.geoObjectList.item()).then((el) =>
                    assert.lengthOf(el.value, 0, 'не должно быть элементов в списке после Ctrl+Z')
                )
                .pause(DRAW_TIMEOUT)
                .crPressKey(['Control', 'y'])
                .crWaitForVisible(PO.geoObjectList.itemPointNumber())
                .elements(PO.geoObjectList.item()).then((el) =>
                    assert.lengthOf(el.value, 1, 'должен быть 1 элемент в списке после Ctrl+Y')
                )
                .crWaitForVisible(PO.balloon(), 'Не открылся балун');
        });

        it('Ctrl+Z + Ctrl+Shift+Z', function () {
            return this.browser
                .crWaitForVisible(PO.ymaps.addPlacemark())
                .crPressKey(['Alt', 'p'])
                .crVerifyScreenshot(PO.ymaps.addPlacemark(), 'placemark-checked-6')
                .leftClick(PO.ymaps.map(), 200, 200)
                .crWaitForVisible(PO.balloon(), 'Не открылся балун')
                .crShouldBeVisible(PO.geoObjectList.itemPointNumber())
                .elements(PO.geoObjectList.item()).then((el) =>
                    assert.lengthOf(el.value, 1, 'должен быть 1 элемент в списке')
                )
                .crPressKey(['Control', 'z'])
                .crWaitForHidden(PO.geoObjectList.itemPointNumber())
                .elements(PO.geoObjectList.item()).then((el) =>
                    assert.lengthOf(el.value, 0, 'не должно быть элементов в списке после Ctrl+Z')
                )
                .crPressKey(['Control', 'Shift', 'z'])
                .crWaitForVisible(PO.geoObjectList.itemPointNumber())
                .elements(PO.geoObjectList.item()).then((el) =>
                    assert.lengthOf(el.value, 1, 'должен быть 1 элемент в списке после Ctrl+Shift+Z')
                )
                .crWaitForVisible(PO.balloon(), 'Не открылся балун');
        });
    });

    describe('Закрыть окно – Esc', () => {
        it('Список карт', function () {
            return this.browser
                .crInit('MANY_MAPS', '', cr.mapState.list)
                .crShouldBeVisible(PO.mapSelection())
                .crPressKey('Escape')
                .crShouldNotBeVisible(PO.mapSelection())
                .crShouldBeVisible(PO.stepEditor());
        });

        it('Экспорт', function () {
            return this.browser
                .crInit('MANY_MAPS', cr.linkMap)
                .crSaveMap()
                .click(PO.exportButton())
                .crWaitForVisible(PO.stepExport(), 'Окно экспорта не открылось')
                .crWaitForVisible(PO.exportModal())
                .crPressKey('Escape')
                .crWaitForHidden(PO.exportModal(), 'Окно экспорта не закрылось');
        });

        it('Помощь', function () {
            return this.browser
                .crInit()
                .crShouldBeVisible(PO.help())
                .click(PO.help())
                .crWaitForVisible(PO.helpPopup(), 'Помощь не открылась')
                .crPressKey('Escape')
                .crWaitForHidden(PO.helpPopup(), 'Помощь не закрылось');
        });

        it('Меню пользователя', function () {
            return this.browser
                .crInit('MANY_MAPS')
                .click(PO.userData.username())
                .crWaitForVisible(PO.userMenu(), 'Меню пользователя не открылось')
                .crPressKey('Escape')
                .crWaitForHidden(PO.userMenu(), 'Меню пользователя не закрылось');
        });
    });
});
