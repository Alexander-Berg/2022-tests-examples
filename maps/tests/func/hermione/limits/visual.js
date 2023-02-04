const LIMITS = require('../../limits.js');
const genText = require('../../utils/genText.js');
const maxName = genText(LIMITS.size.mapName + 1);
const maxDescription = genText(LIMITS.size.mapDescription + 1);
const DRAW_TIMEOUT = 300;
const LINK_LIMITS = 'https://yandex.ru/support/maps-builder/concept/markers_4.html#markers_4__limit';
const cr = require('../../credentials.js');

require('../helper.js')(afterEach);

describe('Лимиты / Визуальные', () => {
    afterEach(function () {
        return this.browser
            .crSaveMap()
            .crLogout();
    });

    it('Длина текста больше лимита в названии карты', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .crWaitForVisible(PO.sidebar.mapDesc(), 'Не появился инпут описания карты')
            .crSetValue(PO.sidebar.mapName(), maxName)
            .crWaitForVisible(PO.sidebar.mapNameError(), 1000, 'Не появилась ошибка в инпуте')
            .crWaitForVisible(PO.popupVisible.error(), 1000, 'Не появился попап с ошибкой')
            .crVerifyScreenshot(PO.popupVisible.error(), 'map-name-error-popup');
    });

    it('Длина текста больше лимита в описании карты', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .crWaitForVisible(PO.sidebar.mapDesc(), 'Не появился инпут описания карты')
            .crSetValue(PO.sidebar.mapDesc(), maxDescription)
            .crWaitForVisible(PO.sidebar.mapDescError(), 1000, 'Не появилась ошибка в инпуте')
            .crWaitForVisible(PO.popupVisible.error(), 1000, 'Не появился попап с ошибкой')
            .getCssProperty(PO.sidebar.mapDescError(), 'box-shadow').then((prop) => {
                assert.equal(prop.value, 'rgb(255,88,88)0px0px0px2pxinset', 'Не появилась красная рамка');
            })
            .crVerifyScreenshot(PO.popupVisible.error(), 'map-desc-error-popup');
    });

    it('Обрезается текст названия и описания сверх лимита', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .crWaitForVisible(PO.sidebar.mapDesc(), 'Не появился инпут описания карты')
            .crSetValue(PO.sidebar.mapName(), maxName)
            .crSetValue(PO.sidebar.mapDesc(), maxDescription)
            .crWaitForVisible(PO.sidebar.mapNameError(), 1000, 'Не появилась ошибка в инпуте')
            .crWaitForVisible(PO.popupVisible.error(), 1000, 'Не появился попап с ошибкой')
            .crWaitForVisible(PO.sidebar.mapDescError(), 1000, 'Не появилась ошибка в инпуте')
            .crWaitForVisible(PO.popupVisible.error(), 1000, 'Не появился попап с ошибкой')
            .getValue(PO.sidebar.mapName()).then((text) => {
                assert.equal(text.length, LIMITS.size.mapName + 1, 'Длина текста должна быть == лимиту + 1');
            })
            .getValue(PO.sidebar.mapDesc()).then((text) => {
                assert.equal(text.length, LIMITS.size.mapDescription + 1, 'Длина текста должна быть == лимиту + 1');
            })
            .crSaveMap()
            .click(PO.sidebarExport.back())
            .crWaitForVisible(PO.stepEditor())
            .crShouldNotBeVisible(PO.sidebar.mapNameError())
            .crShouldNotBeVisible(PO.sidebar.mapDescError())
            .crShouldNotBeVisible(PO.popupVisible.error())
            .getValue(PO.sidebar.mapName()).then((text) => {
                assert.equal(text.length, LIMITS.size.mapName, 'Длина текста должна быть == лимиту');
            })
            .getValue(PO.sidebar.mapDesc()).then((text) => {
                assert.equal(text.length, LIMITS.size.mapDescription, 'Длина текста должна быть == лимиту');
            });
    });

    it('Длина текста больше лимита в балуне объекта', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .crWaitForVisible(PO.ymaps.addPlacemark(), 'Не появилась кнопка добавления метки на карту')
            .click(PO.ymaps.addPlacemark())
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crSetValue(PO.balloon.text(), maxDescription)
            .getText(PO.geoObjectList.itemPointNumberTitle())
            .then((text) => {
                assert.equal(0, maxDescription.indexOf(text), 'Неверное описание объекта в списке');
            })
            .crWaitForVisible(PO.balloon.textError(), 1000, 'Не появилась ошибка в инпуте')
            .crWaitForVisible(PO.popupVisible.error(), 1000, 'Не появился попап с ошибкой')
            .getCssProperty(PO.balloon.text(), 'box-shadow').then((prop) => {
                assert.equal(prop.value, 'rgb(255,88,88)0px0px0px2pxinset', 'Не появилась красная рамка');
            })
            .crVerifyScreenshot(PO.popupVisible.error(), 'balloon-error-popup');
    });

    it('Длина текста больше лимита в подписи метки', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .crWaitForVisible(PO.ymaps.addPlacemark(), 'Не появилась кнопка добавления метки на карту')
            .click(PO.ymaps.addPlacemark())
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crShouldBeVisible(PO.balloon.captionInput())
            .crSetValue(PO.balloon.captionInput(), maxName)
            .getText(PO.geoObjectList.itemPointNumberTitle()).then((text) => {
                assert.equal(0, ('(' + maxName).indexOf(text), 'Неверная подпись объекта в списке');
            })
            .crWaitForVisible(PO.balloon.captionError(), 1000, 'Не появилась ошибка в инпуте')
            .crWaitForVisible(PO.popupVisible.error(), 1000, 'Не появился попап с ошибкой')
            .crVerifyScreenshot(PO.popupVisible.error(), 'caption-error-popup');
    });

    it('Обрезается текст в балуне сверх лимита', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .crWaitForVisible(PO.ymaps.addPlacemark(), 'Не появилась кнопка добавления метки на карту')
            .click(PO.ymaps.addPlacemark())
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crSetValue(PO.balloon.text(), maxDescription)
            .crWaitForVisible(PO.balloon.textError(), 1000, 'Не появилась ошибка в инпуте')
            .crWaitForVisible(PO.popupVisible.error(), 1000, 'Не появился попап с ошибкой')
            .getValue(PO.balloon.text()).then((text) => {
                assert.equal(text.length, LIMITS.size.mapDescription + 1, 'Длина текста должна быть == лимиту + 1');
            })
            .click(PO.balloon.save())
            .click(PO.geoObjectList.itemPointNumber())
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crShouldNotBeVisible(PO.balloon.textError(), 'Не должно быть ошибки после повторного открытия балуна')
            .crShouldNotBeVisible(PO.popupVisible.error(), 'Не должно быть ошибки после повторного открытия балуна')
            .getValue(PO.balloon.text()).then((text) => {
                assert.equal(text.length, LIMITS.size.mapDescription, 'Длина текста должна быть == лимиту');
            });
    });

    it('Обрезается текст в подписи сверх лимита', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .crWaitForVisible(PO.ymaps.addPlacemark(), 'Не появилась кнопка добавления метки на карту')
            .click(PO.ymaps.addPlacemark())
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crShouldBeVisible(PO.balloon.captionInput())
            .crSetValue(PO.balloon.captionInput(), maxName)
            .crWaitForVisible(PO.balloon.captionError(), 1000, 'Не появилась ошибка в инпуте')
            .crWaitForVisible(PO.popupVisible.error(), 1000, 'Не появился попап с ошибкой')
            .getValue(PO.balloon.captionInput()).then((text) => {
                assert.equal(text.length, LIMITS.size.mapName + 1, 'Длина текста должна быть == лимиту + 1');
            })
            .click(PO.balloon.save())
            .click(PO.geoObjectList.itemPointNumberIcon())
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crShouldNotBeVisible(PO.balloon.captionError(), 'Не должно быть ошибки после повторного открытия балуна')
            .crShouldNotBeVisible(PO.popupVisible.error(), 'Не должно быть ошибки после повторного открытия балуна')
            .getValue(PO.balloon.captionInput()).then((text) => {
                assert.equal(text.length, LIMITS.size.mapName, 'Длина текста должна быть == лимиту');
            });
    });

    it('Количество вершин больше лимита в полигоне во время рисования', function () {
        return this.browser
            .crInit('MANY_MAPS', '?config={"limits":{"count":{"vertexes":5}}}')
            .crWaitForVisible(PO.ymaps.addPlacemark(), 'Не появилась кнопка добавления полигона на карту')
            .click(PO.ymaps.addPolygon())
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 200, 200)
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 500, 200)
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 500, 400)
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 200, 400)
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 150, 300)
            .pause(DRAW_TIMEOUT)
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crShouldBeVisible(PO.balloon.limits())
            .crCheckText(PO.balloon.limits.text(), 'В объекте 5 точек из 5 возможных.', 'Неверный текст о лимитах')
            .crCheckLink(PO.balloon.limits.helpLink())
            .then((url) => this.browser
                .crCheckURL(url, LINK_LIMITS, 'Сломана ссылка на лимиты')
            );
    });

    it('Количество вершин больше лимита в линии во время рисования', function () {
        return this.browser
            .crInit('MANY_MAPS', '?config={"limits":{"count":{"vertexes":5}}}')
            .crWaitForVisible(PO.ymaps.addLine(), 'Не появилась кнопка добавления линии на карту')
            .click(PO.ymaps.addLine())
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 200, 200)
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 500, 200)
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 500, 400)
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 200, 400)
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 150, 300)
            .pause(DRAW_TIMEOUT)
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crShouldBeVisible(PO.balloon.limits())
            .crCheckText(PO.balloon.limits.text(), 'В объекте 5 точек из 5 возможных.', 'Неверный текст о лимитах')
            .crCheckLink(PO.balloon.limits.helpLink())
            .then((url) => this.browser
                .crCheckURL(url, LINK_LIMITS, 'Сломана ссылка на лимиты')
            );
    });

    it('Количество вершин больше лимита в полигоне в открытой карте', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.umVertexLimit + '&config={"limits":{"count":{"vertexes":5}}}')
            .crWaitForVisible(PO.geoObjectList.itemPolygon())
            .click(PO.geoObjectList.itemPolygon())
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crShouldBeVisible(PO.balloon.limits())
            .crCheckText(PO.balloon.limits.text(), 'В объекте 31 точка из 5 возможных.', 'Неверный текст о лимитах')
            .crCheckLink(PO.balloon.limits.helpLink());
    });

    it('Количество объектов больше лимита', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.umObjectsLimit + '&config={"limits":{"count":{"geoObjects":10}}}')
            .crShouldBeVisible(PO.sidebar.limit())
            .crCheckText(PO.sidebar.limit(), '20 / 10', 'Количество объектов / лимит')
            .crCheckLink(PO.sidebar.limit()).then((url) => this.browser
                .crCheckURL(url, LINK_LIMITS, 'Сломана ссылка на лимиты')
            )
            .crVerifyScreenshot(PO.ymaps.map(), 'limit-more-object-map');
    });

    it('Количество объектов равно лимиту', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.umObjectsLimit + '&config={"limits":{"count":{"geoObjects":20}}}')
            .crShouldBeVisible(PO.sidebar.limit())
            .crCheckText(PO.sidebar.limit(), '20 / 20', 'Количество объектов / лимит')
            .crCheckLink(PO.sidebar.limit()).then((url) => this.browser
                .crCheckURL(url, LINK_LIMITS, 'Сломана ссылка на лимиты')
            )
            .crVerifyScreenshot(PO.ymaps.map(), 'limit-more-object-map');
    });

    it('Количество объектов больше половины лимита', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.umObjectsLimit + '&config={"limits":{"count":{"geoObjects":39}}}')
            .crShouldBeVisible(PO.sidebar.limit())
            .crCheckText(PO.sidebar.limit(), '20 / 39', 'Количество объектов / лимит')
            .crCheckLink(PO.sidebar.limit()).then((url) => this.browser
                .crCheckURL(url, LINK_LIMITS, 'Сломана ссылка на лимиты')
            )
            .crVerifyScreenshot(PO.ymaps.map(), 'limit-half-object-map');
    });
});
