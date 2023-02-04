const DRAW_TIMEOUT = 300;
const OPTIONS = {
    ignoreElements: [PO.ymaps.searchBoxInput()]
};

require('../helper.js')(afterEach);

describe('Отмена-возврат', () => {
    beforeEach(function () {
        return this.browser
            .crInit('MANY_MAPS', '?ll=13.060607%2C83.424304&z=8');
    });

    afterEach(function () {
        return this.browser
            .isVisible(PO.saveAndContinue()).then((val) => {
                if (val) {
                    return this.browser.crSaveMap();
                }
                return true;
            })
            .crLogout();
    });

    it('Отмена удаления метки', function () {
        return this.browser
            .crWaitForVisible(PO.ymaps.addPlacemark(), 'Не появилась кнопка добавления метки на карту')
            .click(PO.ymaps.addPlacemark())
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crWaitForVisible(PO.geoObjectList.itemPointNumberIcon(), 'Не появилась метка в списке')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент в списке')
            )
            .crSetValue(PO.sidebar.mapName(), 'Отмена удаления метки')
            .crSaveMap()
            .crOpenMap('Отмена удаления метки')
            .crWaitForVisible(PO.geoObjectList.itemPointNumberIcon(), 'Не появилась метка в списке')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент в списке')
            )
            .click(PO.geoObjectList.itemPointNumber())
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .click(PO.balloon.remove())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 0, 'не должно быть объектов в списке после удаления')
            )
            .click(PO.mapHistoryUndo())
            .crWaitForVisible(PO.geoObjectList.itemPointNumberIcon(), 'Не появилась метка в списке')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент в списке')
            )
            .crSaveMap();
    });

    it('Не дублируются контролы после возврата в редактирование', function () {
        return this.browser
            .elements(PO.mapHistoryUndo()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент undo')
            )
            .elements(PO.mapHistoryRedo()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент redo')
            )
            .elements(PO.mapHistoryShadow()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент shadow')
            )
            .crSaveMap()
            .click(PO.sidebarExport.back())
            .crWaitForVisible(PO.stepEditor(), 'Не открылся шаг редактирования карты')
            .elements(PO.mapHistoryUndo()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент undo')
            )
            .elements(PO.mapHistoryRedo()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент redo')
            )
            .elements(PO.mapHistoryShadow()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент shadow')
            );
    });

    it('Исчезают контролы на шаге превью', function () {
        return this.browser
            .crSaveMap()
            .crShouldNotBeVisible(PO.mapHistoryUndo())
            .crShouldNotBeVisible(PO.mapHistoryRedo())
            .crShouldNotBeVisible(PO.mapHistoryShadow());
    });

    it('Отмена стиля метки', function () {
        return this.browser
            .crWaitForVisible(PO.ymaps.addPlacemark(), 'Не появилась кнопка добавления метки на карту')
            .click(PO.ymaps.addPlacemark())
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crWaitForVisible(PO.balloon.selectColor(), 'Не появилась кнопка выбора цвета')
            .pause(DRAW_TIMEOUT)
            .click(PO.balloon.selectColor())
            .crWaitForVisible(PO.balloonColorMenu(), 500, 'Не открылось меню выбора цвета')
            .click(PO.balloonColorMenu.item() + 9)
            .click(PO.balloon.save())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
            .crVerifyScreenshot(PO.geoObjectList(), 'undo-style-placemark-before1-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-style-placemark-before1-map', OPTIONS)
            .click(PO.mapHistoryUndo())
            .pause(DRAW_TIMEOUT)
            .crVerifyScreenshot(PO.geoObjectList(), 'undo-style-placemark-after1-list-1')
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-style-placemark-after1-map', OPTIONS)
            .crSetValue(PO.sidebar.mapName(), 'Отмена стиля метки')
            .crSaveMap()
            .crOpenMap('Отмена стиля метки')
            .crWaitForVisible(PO.geoObjectList.itemPointNumber(), 'Не появилась метка в списке геообъектов')
            .click(PO.geoObjectList.itemPointNumber())
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crVerifyScreenshot(PO.geoObjectList(), 'undo-style-placemark-after1-list-2')
            .crWaitForVisible(PO.balloon.selectColor(), 'Не появилась кнопка выбора цвета')
            .pause(DRAW_TIMEOUT)
            .click(PO.balloon.selectColor())
            .crWaitForVisible(PO.balloonColorMenu(), 500, 'Не открылось меню выбора цвета')
            .click(PO.balloonColorMenu.item() + 9)
            .click(PO.balloon.save())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
            .crVerifyScreenshot(PO.geoObjectList(), 'undo-style-placemark-before2-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-style-placemark-before2-map', OPTIONS)
            .click(PO.mapHistoryUndo())
            .pause(DRAW_TIMEOUT)
            .crVerifyScreenshot(PO.geoObjectList(), 'undo-style-placemark-after2-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-style-placemark-after2-map', OPTIONS)
            .crSaveMap();
    });

    it('Отмена стиля линии', function () {
        return this.browser
            .crWaitForVisible(PO.ymaps.addLine(), 'Не появилась кнопка добавления метки на карту')
            .click(PO.ymaps.addLine())
            .leftClick(PO.ymaps.map(), 200, 200)
            .pause(DRAW_TIMEOUT)
            .leftClick(PO.ymaps.map(), 250, 250)
            .pause(DRAW_TIMEOUT)
            .keys('Enter')
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crWaitForVisible(PO.balloon.selectColor(), 'Не появилась кнопка выбора цвета')
            .pause(DRAW_TIMEOUT)
            .click(PO.balloon.selectColor())
            .crWaitForVisible(PO.balloonColorMenu(), 500, 'Не появилось меню выбора цвета')
            .click(PO.balloonColorMenu.item() + 14)
            .crSetValue(PO.balloon.width(), 10)
            .crSetValue(PO.balloon.transparency(), 20)
            .click(PO.balloon.save())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
            .crVerifyScreenshot(PO.geoObjectList(), 'undo-style-line-before1-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-style-line-before1-map', OPTIONS)
            .click(PO.mapHistoryUndo())
            .pause(DRAW_TIMEOUT)
            .crVerifyScreenshot(PO.geoObjectList(), 'undo-style-line-after1-list-1')
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-style-line-after1-map', OPTIONS)
            .crSetValue(PO.sidebar.mapName(), 'Отмена стиля линии')
            .crSaveMap()
            .crOpenMap('Отмена стиля линии')
            .crWaitForVisible(PO.geoObjectList.itemLinestring(), 'Не появилась линия в списке геообъектов')
            .click(PO.geoObjectList.itemLinestring())
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crVerifyScreenshot(PO.geoObjectList(), 'undo-style-line-after1-list-2')
            .crWaitForVisible(PO.balloon.selectColor(), 'Не появилась кнопка выбора цвета')
            .pause(DRAW_TIMEOUT)
            .click(PO.balloon.selectColor())
            .crWaitForVisible(PO.balloonColorMenu(), 500, 'Не появилось меню выбора цвета')
            .click(PO.balloonColorMenu.item() + 14)
            .crSetValue(PO.balloon.width(), '10')
            .crSetValue(PO.balloon.transparency(), '20')
            .click(PO.balloon.save())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
            .crVerifyScreenshot(PO.geoObjectList(), 'undo-style-line-before2-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-style-line-before2-map', OPTIONS)
            .click(PO.mapHistoryUndo())
            .pause(DRAW_TIMEOUT)
            .crVerifyScreenshot(PO.geoObjectList(), 'undo-style-line-after2-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-style-line-after2-map', OPTIONS)
            .crSaveMap();
    });

    it('Отмена стиля полигона', function () {
        return this.browser
            .crWaitForVisible(PO.ymaps.addPolygon(), 'Не появилась кнопка добавления полигона на карту')
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
            .keys('Enter')
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crWaitForVisible(PO.balloon.poly.strokeColor(), 'Не появилась кнопка выбора цвета')
            .pause(DRAW_TIMEOUT)
            .click(PO.balloon.poly.strokeColor())
            .crWaitForVisible(PO.balloonColorMenu(), 500, 'Не появилось меню выбора цвета')
            .click(PO.balloonColorMenu.item() + 14)
            .crWaitForHidden(PO.balloonColorMenu(), 200, 'Меню выбора цвета не закрылось')
            .click(PO.balloon.poly.fillColor())
            .crWaitForVisible(PO.balloonColorMenu(), 500, 'Не появилось меню выбора цвета')
            .click(PO.balloonColorMenu.item() + 12)
            .crWaitForHidden(PO.balloonColorMenu(), 200, 'Меню выбора цвета не закрылось')
            .crSetValue(PO.balloon.width(), '10')
            .crSetValue(PO.balloon.poly.strokeOpacity(), '20')
            .crSetValue(PO.balloon.poly.fillOpacity(), '60')
            .click(PO.balloon.save())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
            .crVerifyScreenshot(PO.geoObjectList(), 'undo-style-polygon-before1-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-style-polygon-before1-map', OPTIONS)
            .click(PO.mapHistoryUndo())
            .pause(DRAW_TIMEOUT)
            .crVerifyScreenshot(PO.geoObjectList(), 'undo-style-polygon-after1-list-1')
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-style-polygon-after1-map', OPTIONS)
            .crSetValue(PO.sidebar.mapName(), 'Отмена стиля полигона')
            .crSaveMap()
            .crOpenMap('Отмена стиля полигона')
            .crWaitForVisible(PO.geoObjectList.itemPolygon(), 'Не появился полигон в списке геообъектов')
            .click(PO.geoObjectList.itemPolygon())
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crVerifyScreenshot(PO.geoObjectList(), 'undo-style-polygon-after1-list-2')
            .crWaitForVisible(PO.balloon.poly.strokeColor(), 'Не появилась кнопка выбора цвета')
            .pause(DRAW_TIMEOUT)
            .click(PO.balloon.poly.strokeColor())
            .crWaitForVisible(PO.balloonColorMenu(), 500, 'Не появилось меню выбора цвета')
            .click(PO.balloonColorMenu.item() + 14)
            .crWaitForHidden(PO.balloonColorMenu(), 200, 'Меню выбора цвета не закрылось')
            .click(PO.balloon.poly.fillColor())
            .crWaitForVisible(PO.balloonColorMenu(), 500, 'Не появилось меню выбора цвета')
            .click(PO.balloonColorMenu.item() + 12)
            .crWaitForHidden(PO.balloonColorMenu(), 200, 'Меню выбора цвета не закрылось')
            .crSetValue(PO.balloon.width(), '10')
            .crSetValue(PO.balloon.poly.strokeOpacity(), '20')
            .crSetValue(PO.balloon.poly.fillOpacity(), '60')
            .click(PO.balloon.save())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
            .crVerifyScreenshot(PO.geoObjectList(), 'undo-style-polygon-before2-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-style-polygon-before2-map', OPTIONS)
            .click(PO.mapHistoryUndo())
            .pause(DRAW_TIMEOUT)
            .crVerifyScreenshot(PO.geoObjectList(), 'undo-style-polygon-after2-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-style-polygon-after2-map', OPTIONS)
            .crSaveMap();
    });

    it('Возврат в карту и отмена удаления метки', function () {
        return this.browser
            .crWaitForVisible(PO.ymaps.addPlacemark(), 'Не появилась кнопка добавления метки на карту')
            .crSetValue(PO.sidebar.mapName(), 'Возврат в карту и отмена удаления')
            .click(PO.ymaps.addPlacemark())
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crWaitForVisible(PO.geoObjectList.itemPointNumberIcon(), 'Не появилась метка в списке')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент в списке')
            )
            .click(PO.balloon.remove())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 0, 'не должно быть объектов в списке после удаления')
            )
            .crSaveMap()
            .click(PO.sidebarExport.back())
            .crWaitForVisible(PO.stepEditor())
            .click(PO.mapHistoryUndo())
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент в списке после undo')
            )
            .crSaveMap()
            .crOpenMap('Возврат в карту и отмена удаления')
            .crWaitForVisible(PO.geoObjectList.itemPointNumberIcon(), 'Не появилась метка в списке')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент в списке после переоткрытия карты')
            );
    });

    it('Отмена и возврат удаления метки', function () {
        return this.browser
            .crWaitForVisible(PO.ymaps.addPlacemark(), 'Не появилась кнопка добавления метки на карту')
            .crSetValue(PO.sidebar.mapName(), 'Отмена и возврат удаления метки')
            .click(PO.ymaps.addPlacemark())
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crWaitForVisible(PO.geoObjectList.itemPointNumberIcon(), 'Не появилась метка в списке')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент в списке')
            )
            .click(PO.balloon.remove())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 0, 'не должно быть объектов в списке после удаления')
            )
            .click(PO.mapHistoryUndo())
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент в списке после undo')
            )
            .crSaveMap()
            .click(PO.sidebarExport.back())
            .crWaitForVisible(PO.stepEditor())
            .click(PO.mapHistoryRedo())
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 0, 'не должно быть объектов в списке после удаления')
            )
            .click(PO.mapHistoryUndo())
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент в списке после undo')
            )
            .crSaveMap()
            .crOpenMap('Отмена и возврат удаления метки')
            .crWaitForVisible(PO.geoObjectList.itemPointNumberIcon(), 'Не появилась метка в списке')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент в списке после переоткрытия карты')
            );
    });

    it('Отмена и возврат создания метки', function () {
        return this.browser
            .crWaitForVisible(PO.ymaps.addPlacemark(), 'Не появилась кнопка добавления метки на карту')
            .crSetValue(PO.sidebar.mapName(), 'Отмена и возврат создания метки')
            .click(PO.ymaps.addPlacemark())
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crWaitForVisible(PO.geoObjectList.itemPointNumberIcon(), 'Не появилась метка в списке')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент в списке')
            )
            .click(PO.mapHistoryUndo())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 0, 'не должно быть объектов в списке после отмены создания')
            )
            .crSaveMap()
            .click(PO.sidebarExport.back())
            .crWaitForVisible(PO.stepEditor())
            .click(PO.mapHistoryRedo())
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент в списке')
            )
            .click(PO.mapHistoryUndo())
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 0, 'не должно быть объектов в списке после отмены создания')
            )
            .crSaveMap()
            .crOpenMap('Отмена и возврат создания метки')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 0, 'не должно быть объектов в списке после переоткрытия карты')
            );
    });

    it('Отмена и возврат изменения названия и описания карты', function () {
        return this.browser
            .setValue(PO.sidebar.mapName(), 'Отмена и возврат изменения названия и описания карты')
            .crCheckValue(PO.sidebar.mapName(), 'Отмена и возврат изменения названия и описания карты',
                'Не изменилось имя карты')
            .click(PO.mapHistoryUndo())
            .crCheckValue(PO.sidebar.mapName(), '', 'Не отменилось имя карты')
            .click(PO.mapHistoryRedo())
            .crCheckValue(PO.sidebar.mapName(), 'Отмена и возврат изменения названия и описания карты',
                'Не вернулось имя карты')
            .setValue(PO.sidebar.mapDesc(), 'Описание карты')
            .crCheckValue(PO.sidebar.mapDesc(), 'Описание карты', 'Не изменилось описание карты')
            .click(PO.mapHistoryUndo())
            .crCheckValue(PO.sidebar.mapDesc(), '', 'Не отменилось описание карты')
            .click(PO.mapHistoryRedo())
            .crCheckValue(PO.sidebar.mapDesc(), 'Описание карты', 'Не вернулось описание карты')
            .crSaveMap()
            .click(PO.sidebarExport.back())
            .crWaitForVisible(PO.stepEditor())
            .click(PO.mapHistoryUndo())
            .crCheckValue(PO.sidebar.mapDesc(), '', 'Не отменилось описание карты после сохранения')
            .crCheckValue(PO.sidebar.mapName(), 'Отмена и возврат изменения названия и описания карты',
                'Название карты')
            .click(PO.mapHistoryUndo())
            .crCheckValue(PO.sidebar.mapDesc(), '', 'Описание карты после сохранения')
            .crCheckValue(PO.sidebar.mapName(), '', 'Не отменилось название карты после сохранения')
            .click(PO.mapHistoryRedo())
            .crCheckValue(PO.sidebar.mapDesc(), '', 'Описание карты после сохранения и отмены')
            .crCheckValue(PO.sidebar.mapName(), 'Отмена и возврат изменения названия и описания карты',
                'Не вернулось описание карты')
            .click(PO.mapHistoryRedo())
            .crCheckValue(PO.sidebar.mapDesc(), 'Описание карты',
                'Не вернулось описание карты после сохранения и отмены')
            .crCheckValue(PO.sidebar.mapName(), 'Отмена и возврат изменения названия и описания карты',
                'Название карты после сохранения и отмены')
            .crOpenMap('Отмена и возврат изменения названия и описания карты')
            .crCheckValue(PO.sidebar.mapDesc(), 'Описание карты',
                'Не сохранилось описание карты после отмены и возврата')
            .crCheckValue(PO.sidebar.mapName(), 'Отмена и возврат изменения названия и описания карты',
                'Не сохранилось название карты после отмены и возврата');
    });

    it('Отмена и возврат изменения геометрии полигона', function () {
        const TOLERANCE = {
            tolerance: 50,
            ignoreElements: [PO.ymaps.searchBoxInput()]
        };

        return this.browser
            .crSetValue(PO.sidebar.mapName(), 'Отмена и возврат изменения геометрии полигона')
            .crWaitForVisible(PO.ymaps.addPolygon(), 'Не появилась кнопка добавления полигона на карту')
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
            .keys('Enter')
            .crWaitForVisible(PO.balloon(), 'Не открылся балун 1')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент в списке')
            )
            .click(PO.balloon.remove())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун 1')
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-geometry-polygon-map-del-1')
            .click(PO.mapHistoryUndo()) // отмена удаления
            .crWaitForVisible(PO.balloon(), 'Не открылся балун 2')
            .pause(DRAW_TIMEOUT)
            .click(PO.balloon.save())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун 2')
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-geometry-polygon-map-4p-1')
            .click(PO.mapHistoryUndo()) // отмена 4 точки
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-geometry-polygon-map-3p-1')
            .click(PO.mapHistoryUndo()) // отмена 3 точки
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-geometry-polygon-map-2p-1')
            .click(PO.mapHistoryUndo()) // отмена 2 точки
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-geometry-polygon-map-1p-1')
            .click(PO.mapHistoryUndo()) // отмена рисования 1 точки и создания
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-geometry-polygon-map-beforecreate-1')
            .click(PO.mapHistoryRedo()) // возвращение 1 точки и полигона
            .crWaitForVisible(PO.balloon(), 'Не открылся балун 3')
            .pause(DRAW_TIMEOUT)
            .click(PO.balloon.save())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун 3')
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-geometry-polygon-map-1p-2')
            .click(PO.mapHistoryRedo()) // возвращение 2 точки
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-geometry-polygon-map-2p-2')
            .click(PO.mapHistoryRedo()) // возвращение 3 точки
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-geometry-polygon-map-3p-2')
            .click(PO.mapHistoryRedo()) // возвращение 4 точки
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-geometry-polygon-map-4p-2')
            .click(PO.mapHistoryRedo()) // возвращение удаления
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-geometry-polygon-map-del-2')
            .click(PO.mapHistoryUndo()) // отмена удаления
            .crSaveMap()
            .click(PO.sidebarExport.back())
            .crWaitForVisible(PO.stepEditor())
            .click(PO.mapHistoryUndo()) // отмена 4 точки
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-geometry-polygon-map-3p-3')
            .click(PO.mapHistoryUndo()) // отмена 3 точки
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-geometry-polygon-map-2p-3')
            .click(PO.mapHistoryUndo()) // отмена 2 точки
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-geometry-polygon-map-1p-3')
            .click(PO.mapHistoryUndo()) // отмена рисования 1 точки и создания
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-geometry-polygon-map-beforecreate-2')
            .click(PO.mapHistoryRedo()) // возвращение 1 точки и полигона
            .crWaitForVisible(PO.balloon(), 'Не открылся балун 4')
            .pause(DRAW_TIMEOUT)
            .click(PO.balloon.save())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун 4')
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-geometry-polygon-map-1p-4')
            .click(PO.mapHistoryRedo()) // возвращение 2 точки
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-geometry-polygon-map-2p-4')
            .click(PO.mapHistoryRedo()) // возвращение 3 точки
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-geometry-polygon-map-3p-4')
            .click(PO.mapHistoryRedo()) // возвращение 4 точки
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-geometry-polygon-map-4p-3')
            .crSaveMap()
            .crOpenMap('Отмена и возврат изменения геометрии полигона')
            .crVerifyScreenshot(PO.ymaps.map(), 'undo-geometry-polygon-map-result');
    });

    it('Не выключается рисование после отмены создания метки', function () {
        return this.browser
            .crWaitForVisible(PO.ymaps.addPlacemark(), 'Не появилась кнопка добавления метки на карту')
            .crVerifyScreenshot(PO.ymaps.addPlacemark(), 'placemark-unchecked')
            .click(PO.ymaps.addPlacemark())
            .crWaitForVisible(PO.ymaps.addPlacemark.checked(), 'Контрол создания метки не включен')
            .crVerifyScreenshot(PO.ymaps.addPlacemark(), 'placemark-checked-1')
            .crVerifyScreenshot(PO.mapHistoryShadow(), 'undo-redo-controls-1')
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crWaitForVisible(PO.geoObjectList.itemPointNumberIcon(), 'Не появилась метка в списке')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 1, 'Должен быть 1 элемент в списке')
            )
            .crVerifyScreenshot(PO.mapHistoryShadow(), 'undo-redo-controls-2-1')
            .click(PO.mapHistoryUndo())
            .crVerifyScreenshot(PO.ymaps.addPlacemark(), 'placemark-checked-2')
            .crVerifyScreenshot(PO.mapHistoryShadow(), 'undo-redo-controls-3')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 0, 'Не должно быть объектов в списке после удаления')
            )
            .leftClick(PO.ymaps.map(), 200, 200)
            .crWaitForVisible(PO.balloon(), 'Не открылся балун')
            .crWaitForVisible(PO.geoObjectList.itemPointNumberIcon(), 'Не появилась метка в списке')
            .crVerifyScreenshot(PO.mapHistoryShadow(), 'undo-redo-controls-2-2')
            .click(PO.balloon.remove())
            .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 0, 'Не должно быть объектов в списке после удаления')
            )
            .click(PO.mapHistoryUndo())
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 1, 'Должен быть 1 элемент в списке')
            )
            .crVerifyScreenshot(PO.mapHistoryShadow(), 'undo-redo-controls-4')
            .crVerifyScreenshot(PO.ymaps.addPlacemark(), 'placemark-checked');
    });
});
