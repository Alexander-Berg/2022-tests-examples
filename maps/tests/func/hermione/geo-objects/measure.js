const DRAW_TIMEOUT = 400;

require('../helper.js')(afterEach);

describe('Измерения', () => {
    const checkAfterImport = function () {
        return this
            .crWaitForVisible(PO.popupVisible.modelCell(), 2000, 'Не появилось затемнение')
            .catch(() => {
                return true;
            })
            .crWaitForHidden(PO.popupVisible.modelCell(), 'Не исчезло затемнение')
            .crWaitForVisible(PO.stepEditor(), 'Не открылся шаг редактирования после импорта');
    };

    beforeEach(function () {
        return this.browser
            .crInit('MANY_MAPS');
    });

    afterEach(function () {
        return this.browser
            .crSaveMap()
            .crLogout();
    });

    describe('Площадь полигонов', () => {
        it('Обычные полигоны', function () {
            return this.browser
                .click(PO.sidebar.importBtn())
                .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
                .chooseFile(PO.import.attach(), 'import-files/measure/polygon1.kml')
                .then(checkAfterImport)
                .crWaitForVisible(PO.geoObjectList.itemPolygon())
                .elements(PO.geoObjectList.itemPolygon()).then((el) =>
                    assert.lengthOf(el.value, 5, 'Должно быть 5 полигонов в списке')
                )
                .getText(PO.geoObjectList.itemPolygonSquare()).then((text) => {
                    const values = ['65 км2', '25 км2', '1.2 км2', '2250 см2', '5900 м2'];
                    text.forEach((elem, i) => {
                        assert.strictEqual(elem, values[i], 'Площадь полигона ' + i);
                    });
                    return this;
                });
        });

        it('Полигоны с вершинами в одну линию', function () {
            return this.browser
                .click(PO.sidebar.importBtn())
                .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
                .chooseFile(PO.import.attach(), 'import-files/measure/polygon2.kml')
                .then(checkAfterImport)
                .crWaitForVisible(PO.geoObjectList.itemPolygon())
                .elements(PO.geoObjectList.itemPolygon()).then((el) =>
                    assert.lengthOf(el.value, 1, 'Должен быть 1 полигон в списке')
                )
                .getText(PO.geoObjectList.itemPolygonSquare()).then((text) => {
                    assert.isNotEmpty(text, 'Есть площадь полигона');
                    assert.strictEqual(text, '31827 м2', 'Площадь полигона равна');
                    return this;
                });
        });

        it('Полигоны с самопересечениями', function () {
            return this.browser
                .click(PO.sidebar.importBtn())
                .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
                .chooseFile(PO.import.attach(), 'import-files/measure/polygon3.kml')
                .then(checkAfterImport)
                .crWaitForVisible(PO.geoObjectList.itemPolygon())
                .elements(PO.geoObjectList.itemPolygon()).then((el) =>
                    assert.lengthOf(el.value, 5, 'Должно быть 5 полигонов в списке')
                )
                .getText(PO.geoObjectList.itemPolygonSquare()).then((text) => {
                    text.forEach((elem, i) => {
                        assert.isEmpty(elem, 'Площадь полигона ' + i);
                    });
                    return this;
                });
        });

        it('Совпадают вершины', function () {
            return this.browser
                .click(PO.sidebar.importBtn())
                .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
                .chooseFile(PO.import.attach(), 'import-files/measure/polygon4.kml')
                .then(checkAfterImport)
                .crWaitForVisible(PO.geoObjectList.itemPolygon())
                .elements(PO.geoObjectList.itemPolygon()).then((el) =>
                    assert.lengthOf(el.value, 1, 'Должен быть 1 полигон в списке')
                )
                .getText(PO.geoObjectList.itemPolygonSquare()).then((text) => {
                    assert.isNotEmpty(text, 'Есть площадь полигона');
                    assert.strictEqual(text, '6.0 км', 'Площадь полигона равна');
                    return this;
                });
        });

        it('Полигоны из 1-2 вершин', function () {
            return this.browser
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
                .keys('Enter')
                .crWaitForVisible(PO.balloon(), 'Не открылся балун')
                .pause(DRAW_TIMEOUT)
                .click(PO.balloon.save())
                .crWaitForHidden(PO.balloon(), 'Не закрылся балун')
                .crWaitForVisible(PO.geoObjectList.itemPolygon())
                .elements(PO.geoObjectList.itemPolygon()).then((el) =>
                    assert.lengthOf(el.value, 2, 'Должно быть 2 полигона в списке')
                )
                .getText(PO.geoObjectList.itemPolygonSquare()).then((text) => {
                    text.forEach((elem, i) => {
                        assert.isEmpty(elem, 'Площадь полигона ' + i);
                    });
                    return this;
                });
        });
    });

    describe('Длина линий', () => {
        it('Обычные линии', function () {
            return this.browser
                .click(PO.sidebar.importBtn())
                .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
                .chooseFile(PO.import.attach(), 'import-files/measure/linestring1.kml')
                .then(checkAfterImport)
                .crWaitForVisible(PO.geoObjectList.itemLinestring())
                .elements(PO.geoObjectList.itemLinestring()).then((el) =>
                    assert.lengthOf(el.value, 6, 'Должно быть 6 линий в списке')
                )
                .getText(PO.geoObjectList.itemLinestringDistance()).then((text) => {
                    const values = ['250 км', '250 м', '25 м', '3.0 м', '2.5 м', '25 см'];
                    text.forEach((elem, i) => {
                        assert.strictEqual(elem, values[i], 'Длина линии ' + i);
                    });
                    return this;
                });
        });

        it('Линия из 1 вершины', function () {
            return this.browser
                .crWaitForVisible(PO.ymaps.addLine(), 'Не появилась кнопка добавления линии на карту')
                .click(PO.ymaps.addLine())
                .leftClick(PO.ymaps.map(), 200, 200)
                .pause(DRAW_TIMEOUT)
                .elements(PO.geoObjectList.itemLinestring()).then((el) =>
                    assert.lengthOf(el.value, 1, 'Должна быть 1 линия в списке')
                )
                .getText(PO.geoObjectList.itemLinestringDistance()).then((text) =>
                    assert.isEmpty(text, 'Нет длины линии')
                );
        });
    });
});
