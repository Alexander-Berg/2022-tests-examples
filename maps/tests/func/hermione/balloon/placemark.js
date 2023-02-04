const DRAW_TIMEOUT = 400;
const EXCLUDES = {
    ignoreElements: [PO.ymaps.searchBoxInput()]
};

require('../helper.js')(afterEach);

describe('Балун / Метки', () => {
    beforeEach(function () {
        return this.browser
            .crInit('MANY_MAPS', '?ll=13.060607%2C83.424304&z=8')
            .crWaitForVisible(PO.ymaps.addPlacemark(), 'Не появилась кнопка добавления метки на карту')
            .click(PO.ymaps.addPlacemark());
    });

    afterEach(function () {
        return this.browser
            .crSaveMap()
            .crLogout();
    });

    it('Создание метки с номером', function () {
        return this.browser
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crShouldNotBeVisible(PO.balloon.numberInput())
            .click(PO.balloon.type.number())
            .crShouldBeVisible(PO.balloon.numberInput())
            .getAttribute(PO.balloon.numberInput(), 'placeholder').then((val) => {
                assert.strictEqual(val, '0-999', 'Placeholder инпута ввода номера');
            })
            .setValue(PO.balloon.numberInput(), '11')
            .crCheckValue(PO.balloon.numberInput(), '11', 'должно быть значение 11')
            .crShouldBeVisible(PO.geoObjectList.itemPointNumberIcon())
            .getAttribute(PO.geoObjectList.itemPointNumberIcon(), 'style').then((val) => {
                assert.strictEqual(val, 'border-color: rgb(30, 152, 255);', 'синяя метка без наполнителя');
            })
            .crCheckText(PO.geoObjectList.itemPointNumberIcon(), '11', 'должно быть значение 11')
            .crCheckText(PO.ymaps.placemark.iconEmpty.content(), '11', 'должно быть значение 11')
            .setValue(PO.balloon.numberInput(), '999')
            .crCheckValue(PO.balloon.numberInput(), '999', 'должно быть значение 999')
            .crShouldBeVisible(PO.geoObjectList.itemPointCompactNumberIcon())
            .getAttribute(PO.geoObjectList.itemPointCompactNumberIcon(), 'style').then((val) => {
                assert.strictEqual(val, 'border-color: rgb(30, 152, 255);', 'синяя метка без наполнителя');
            })
            .crCheckText(PO.geoObjectList.itemPointCompactNumberIcon(), '999', 'должно быть значение 999')
            .crCheckText(PO.ymaps.placemark.iconEmpty.content(), '999', 'должно быть значение 999')
            .setValue(PO.balloon.numberInput(), '10000')
            .crCheckValue(PO.balloon.numberInput(), '100', 'должно быть значение 100')
            .crShouldBeVisible(PO.geoObjectList.itemPointCompactNumberIcon())
            .getAttribute(PO.geoObjectList.itemPointCompactNumberIcon(), 'style').then((val) => {
                assert.strictEqual(val, 'border-color: rgb(30, 152, 255);', 'синяя метка без наполнителя');
            })
            .crCheckText(PO.geoObjectList.itemPointCompactNumberIcon(), '100', 'должно быть значение 100')
            .crCheckText(PO.ymaps.placemark.iconEmpty.content(), '100', 'должно быть значение 100')
            .setValue(PO.balloon.numberInput(), 'bla')
            .pause(DRAW_TIMEOUT)
            .crCheckValue(PO.balloon.numberInput(), '', 'инпут при вводе нецифрового значени должен оставаться пустым')
            .crShouldBeVisible(PO.geoObjectList.itemPointNumberIcon())
            .getAttribute(PO.geoObjectList.itemPointNumberIcon(), 'style').then((val) => {
                assert.strictEqual(val, 'border-color: rgb(30, 152, 255);', 'синяя метка без наполнителя');
            })
            .crCheckText(PO.geoObjectList.itemPointNumberIcon(), '', 'иконка должна быть пустой при вводе нецифрового' +
                ' значения')
            .setValue(PO.balloon.numberInput(), '1')
            .crCheckText(PO.geoObjectList.itemPointNumberTitle(), 'Без описания')
            .click(PO.balloon.save())
            .crVerifyScreenshot(PO.ymaps.map(), 'placemark-number-map', EXCLUDES)
            .crVerifyScreenshot(PO.geoObjectList(), 'placemark-number-list');
    });

    it('Создание метки с подписью', function () {
        return this.browser
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crShouldBeVisible(PO.balloon.captionInput())
            .crShouldBeVisible(PO.ymaps.placemark.iconWithCaption())
            .crShouldBeVisible(PO.geoObjectList.itemPointNumberIcon())
            .getAttribute(PO.balloon.captionInput(), 'placeholder').then((val) => {
                assert.strictEqual(val, 'Подпись метки', 'Placeholder инпута ввода подписи');
            })
            .getAttribute(PO.geoObjectList.itemPointNumberIcon(), 'style').then((val) => {
                assert.strictEqual(val, 'border-color: rgb(30, 152, 255);', 'синяя метка без наполнителя');
            })
            .crSetValue(PO.balloon.captionInput(), 'Подпись метки из 27 символов')
            .pause(DRAW_TIMEOUT)
            .crCheckText(PO.geoObjectList.itemPointNumberTitle(), '(Подпись метки из 27 символов) Без описания',
                'описание метки')
            .crShouldBeVisible(PO.ymaps.placemark.iconWithCaption.caption())
            .crCheckText(PO.ymaps.placemark.iconWithCaption.caption(), 'Подпись метки из 27 символов', 'подпись метки')
            .getAttribute(PO.ymaps.placemark.iconWithCaption.caption(), 'style').then((val) => {
                assert.strictEqual(val, 'max-width: 188px;', 'ширина контента подписи метки ограничена');
            })
            .crSetValue(PO.balloon.captionInput(), 'Подпись метки из 30 символов йй')
            .pause(DRAW_TIMEOUT)
            .crCheckText(PO.geoObjectList.itemPointNumberTitle(), '(Подпись метки из 30 символов йй) Без описания',
                'описание метки')
            .crShouldBeVisible(PO.ymaps.placemark.iconWithCaption.caption())
            .crCheckText(PO.ymaps.placemark.iconWithCaption.caption(), 'Подпись метки из 30 символов йй',
                'подпись метки')
            .click(PO.balloon.save())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
            .crVerifyScreenshot(PO.ymaps.map(), 'placemark-caption-map', EXCLUDES)
            .crVerifyScreenshot(PO.geoObjectList(), 'placemark-caption-list');
    });

    it('Редактирование номера метки', function () {
        return this.browser
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crWaitForVisible(PO.balloon.type.number(), 'Не появилась кнопка добавления номера')
            .click(PO.balloon.type.number())
            .crShouldBeVisible(PO.balloon.numberInput())
            .crSetValue(PO.balloon.numberInput(), '666')
            .crCheckValue(PO.balloon.numberInput(), '666', 'должно быть значение 666')
            .crShouldBeVisible(PO.geoObjectList.itemPointCompactNumberIcon())
            .crCheckText(PO.geoObjectList.itemPointCompactNumberIcon(), '666', 'должно быть значение 666')
            .crCheckText(PO.ymaps.placemark.iconEmpty.content(), '666', 'должно быть значение 666')
            .click(PO.balloon.save())
            .crSetValue(PO.sidebar.mapName(), 'Редактирование номера метки')
            .crSaveMap()
            .crOpenMap('Редактирование номера метки')
            .crWaitForVisible(PO.geoObjectList.itemPointCompactNumberIcon(), 'Не появилась метка в списке объектов')
            .crCheckText(PO.geoObjectList.itemPointCompactNumberIcon(), '666', 'должно быть значение 666')
            .crCheckText(PO.ymaps.placemark.iconEmpty.content(), '666', 'должно быть значение 666')
            .click(PO.geoObjectList.itemPointCompactNumberIcon())
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crShouldBeVisible(PO.balloon.numberInput())
            .crCheckValue(PO.balloon.numberInput(), '666', 'должно быть значение 666')
            .crSetValue(PO.balloon.numberInput(), '777')
            .crCheckValue(PO.balloon.numberInput(), '777', 'должно быть значение 777')
            .crCheckText(PO.geoObjectList.itemPointCompactNumberIcon(), '777', 'должно быть значение 777')
            .crCheckText(PO.ymaps.placemark.iconEmpty.content(), '777', 'должно быть значение 777')
            .click(PO.balloon.save());
    });

    it('Редактирование подписи метки', function () {
        return this.browser
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crShouldBeVisible(PO.balloon.captionInput())
            .crShouldBeVisible(PO.ymaps.placemark.iconWithCaption())
            .crShouldBeVisible(PO.geoObjectList.itemPointNumberIcon())
            .crSetValue(PO.balloon.captionInput(), 'Подпись1')
            .pause(DRAW_TIMEOUT)
            .crCheckText(PO.geoObjectList.itemPointNumberTitle(), '(Подпись1) Без описания', 'описание метки')
            .crShouldBeVisible(PO.ymaps.placemark.iconWithCaption.caption())
            .crCheckText(PO.ymaps.placemark.iconWithCaption.caption(), 'Подпись1')
            .click(PO.balloon.save())
            .crSetValue(PO.sidebar.mapName(), 'Редактирование подписи метки')
            .crSaveMap()
            .crOpenMap('Редактирование подписи метки')
            .crWaitForVisible(PO.geoObjectList.itemPointNumberIcon(), 'Не появилась метка в списке объектов')
            .click(PO.geoObjectList.itemPointNumberIcon())
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crShouldBeVisible(PO.balloon.captionInput())
            .crCheckValue(PO.balloon.captionInput(), 'Подпись1')
            .crCheckText(PO.geoObjectList.itemPointNumberTitle(), '(Подпись1) Без описания', 'описание метки')
            .crShouldBeVisible(PO.ymaps.placemark.iconWithCaption.caption())
            .crCheckText(PO.ymaps.placemark.iconWithCaption.caption(), 'Подпись1')
            .crSetValue(PO.balloon.captionInput(), 'Подпись2')
            .pause(DRAW_TIMEOUT)
            .crCheckText(PO.geoObjectList.itemPointNumberTitle(), '(Подпись2) Без описания', 'описание метки')
            .crCheckText(PO.ymaps.placemark.iconWithCaption.caption(), 'Подпись2', 'подпись метки')
            .click(PO.balloon.save())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
            .crVerifyScreenshot(PO.ymaps.map(), 'placemark-caption-edit-map', EXCLUDES)
            .crVerifyScreenshot(PO.geoObjectList(), 'placemark-caption-edit-list');
    });

    it('Редактирование пресета метки', function () {
        return this.browser
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crWaitForVisible(PO.balloon.selectColor(), 'Не появилась кнопка выбора цвета')
            .click(PO.balloon.selectColor())
            .crWaitForVisible(PO.balloonColorMenu(), 500, 'Не открылось меню выбора цвета')
            .click(PO.balloonColorMenu.item() + 9)
            .crWaitForHidden(PO.balloonColorMenu(), 200, 'Меню выбора цвета не закрылось')
            .crShouldBeVisible(PO.geoObjectList.itemPointNumberIcon())
            .getAttribute(PO.geoObjectList.itemPointNumberIcon(), 'style').then((val) => {
                assert.strictEqual(val, 'border-color: rgb(27, 173, 3);', 'зеленая метка');
            })
            .crCheckText(PO.balloon.selectColor.text(), '9', 'должно быть значение 9')
            .getAttribute(PO.balloon.iconType.withLeg(), 'aria-pressed').then((val) => {
                assert.strictEqual(val, 'true', 'кнопка зажата');
            })
            .getAttribute(PO.balloon.iconType.withoutLeg(), 'aria-pressed').then((val) => {
                assert.strictEqual(val, 'false', 'кнопка не зажата');
            })
            .click(PO.balloon.iconType.withoutLeg())
            .getAttribute(PO.balloon.iconType.withLeg(), 'aria-pressed').then((val) => {
                assert.strictEqual(val, 'false', 'кнопка не зажата');
            })
            .getAttribute(PO.balloon.iconType.withoutLeg(), 'aria-pressed').then((val) => {
                assert.strictEqual(val, 'true', 'кнопка зажата');
            })
            .crShouldNotBeVisible(PO.balloon.selectIcon())
            .click(PO.balloon.type.icon())
            .crShouldBeVisible(PO.balloon.selectIcon())
            .click(PO.balloon.selectIcon())
            .crWaitForVisible(PO.balloonIconsMenu(), 500, 'Не открылось меню выбора иконки')
            .crShouldBeVisible(PO.geoObjectList.itemPointDotIcon())
            .click(PO.balloonIconsMenu.item() + 32)
            .crShouldBeVisible(PO.balloon.selectIcon.iconBicycle())
            .crShouldBeVisible(PO.ymaps.placemark.bicycle())
            .click(PO.balloon.save())
            .crVerifyScreenshot(PO.ymaps.map(), 'placemark-edit-before-map', EXCLUDES)
            .crVerifyScreenshot(PO.geoObjectList(), 'placemark-edit-before-list-1')
            .crSetValue(PO.sidebar.mapName(), 'Редактирование пресета метки')
            .crSaveMap()
            .crOpenMap('Редактирование пресета метки')
            .crWaitForVisible(PO.geoObjectList.itemPointPicIcon(), 'Не появилась метка в списке геообъектов')
            .crVerifyScreenshot(PO.geoObjectList(), 'placemark-edit-before-list-2')
            .click(PO.geoObjectList.itemPointPicIcon())
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crCheckText(PO.balloon.selectColor.text(), '9', 'должно быть значение 9')
            .getAttribute(PO.balloon.iconType.withoutLeg(), 'aria-pressed').then((val) => {
                assert.strictEqual(val, 'true', 'кнопка не зажата');
            })
            .crShouldBeVisible(PO.balloon.selectIcon())
            .crShouldBeVisible(PO.balloon.selectIcon.iconBicycle())
            .click(PO.balloon.selectColor())
            .crWaitForVisible(PO.balloonColorMenu(), 500, 'Не открылось меню выбора цвета')
            .click(PO.balloonColorMenu.item() + 13)
            .crWaitForHidden(PO.balloonColorMenu(), 200, 'Меню выбора цвета не закрылось')
            .getAttribute(PO.geoObjectList.itemPointPicIcon(), 'style').then((val) => {
                assert.strictEqual(val, 'border-color: rgb(243, 113, 209);', 'розовая метка');
            })
            .crCheckText(PO.balloon.selectColor.text(), '13', 'должно быть значение 13')
            .click(PO.balloon.iconType.withLeg())
            .getAttribute(PO.balloon.iconType.withLeg(), 'aria-pressed').then((val) => {
                assert.strictEqual(val, 'true', 'кнопка зажата');
            })
            .click(PO.balloon.selectIcon())
            .crWaitForVisible(PO.balloonIconsMenu(), 500, 'Не открылось меню выбора иконки')
            .crShouldNotBeVisible(PO.geoObjectList.itemPointDotIcon())
            .click(PO.balloonIconsMenu.item() + 46)
            .crShouldBeVisible(PO.balloon.selectIcon.iconDog())
            .crShouldBeVisible(PO.ymaps.placemark.dog())
            .click(PO.balloon.save())
            .crVerifyScreenshot(PO.ymaps.map(), 'placemark-edit-after-map', EXCLUDES)
            .crVerifyScreenshot(PO.geoObjectList(), 'placemark-edit-after-list');
    });

    it('Удаление метки', function () {
        return this.browser
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 0, 'не должно быть объектов в списке')
            )
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент в списке')
            )
            .pause(DRAW_TIMEOUT)
            .click(PO.balloon.remove())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 0, 'не должно быть объектов в списке после удаления')
            )
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент в списке')
            )
            .pause(DRAW_TIMEOUT)
            .click(PO.balloon.save())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
            .crWaitForVisible(PO.geoObjectList.itemPointNumber())
            .click(PO.geoObjectList.itemPointNumber())
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .pause(DRAW_TIMEOUT)
            .click(PO.balloon.remove())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 0, 'не должно быть объектов в списке после удаления')
            )
            .crVerifyScreenshot(PO.ymaps.map(), 'placemark-remove-map', EXCLUDES)
            .crVerifyScreenshot(PO.sidebar(), 'placemark-remove-list');
    });
});
