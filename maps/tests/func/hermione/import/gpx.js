const FORMATS = 'https://yandex.ru/support/maps-builder/concept/markers_5.html';
const LIMITS = 'https://yandex.ru/support/maps-builder/concept/markers_4.html#markers_4__limit';
const OPTIONS = {
    tolerance: 20,
    ignoreElements: [PO.ymaps.searchBoxInput()]
};

require('../helper.js')(afterEach);

describe('Импорт / GPX', () => {
    const checkAfterImport = function () {
        return this
            .crWaitForVisible(PO.popupVisible.modelCell(), 2000, 'Не появилось затемнение')
            .catch(() => {
                return true;
            })
            .crWaitForHidden(PO.popupVisible.modelCell(), 'Не исчезло затемнение')
            .crWaitForVisible(PO.stepEditor(), 'Не открылся шаг редактирования после импорта');
    };

    afterEach(function () {
        return this.browser
            .crCloseAdverseStatus()
            .crSaveMap()
            .crLogout();
    });

    it('Из экспорта', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/gpx/from_export.gpx')
            .then(checkAfterImport)
            .crCheckValue(PO.sidebar.mapName(), 'Импорт gpx', 'Название карты')
            .crCheckValue(PO.sidebar.mapDesc(), '2 линии, 2 метки', 'Описание карты')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 4, 'должно быть 4 элемента в списке')
            )
            .crVerifyScreenshot(PO.geoObjectList(), 'from-export-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'from-export-map', OPTIONS);
    });

    it('Пример из доки', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/gpx/Sample_GPX.gpx')
            .then(checkAfterImport)
            .crCheckValue(PO.sidebar.mapName(), 'Первый метрополитен в мире', 'Название карты')
            .crCheckValue(PO.sidebar.mapDesc(), 'Первая линия метрополитена была запущена в в Лондоне в январе 1863' +
                ' года. Её протяженность составляла 6 километров.', 'Описание карты')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 8, 'должно быть 8 элементов в списке')
            )
            .crVerifyScreenshot(PO.geoObjectList(), 'sample-list');
    });

    it('Файл без объектов', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/gpx/empty_features.gpx')
            .then(checkAfterImport)
            .crCheckValue(PO.sidebar.mapName(), 'Нет объектов', 'Название карты')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 0, 'должно быть 0 элементов в списке')
            );
    });

    it('Пустой файл', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/gpx/empty.gpx')
            .crWaitForVisible(PO.popup.fileTypeError(), 'Не появилась ошибка чтения формата')
            .crCheckLink(PO.popup.fileTypeError.link()).then((url) => this.browser
                .crCheckURL(url, FORMATS, 'Сломана ссылка на импорт объектов карты')
            )
            .click(PO.popupVisible.close())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .click(PO.popupVisible.close())
            .crWaitForVisible(PO.stepEditor(), 'Шаг редактирования карты не открылся');
    });

    it('Без: metadata, version, creator, xmlns, <?xml>', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/gpx/without_meta.gpx')
            .then(checkAfterImport)
            .crCheckValue(PO.sidebar.mapName(), 'Импорт из «without_meta.gpx»', 'Название карты')
            .crCheckValue(PO.sidebar.mapDesc(), '', 'Нет описание карты')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 4, 'должно быть 4 элемента в списке')
            )
            .crVerifyScreenshot(PO.ymaps.map(), 'without-meta-map', OPTIONS);
    });

    it('Невалидный файл', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/gpx/invalid.gpx')
            .crWaitForVisible(PO.popup.fileTypeError(), 'Не появилась ошибка чтения формата')
            .crVerifyScreenshot(PO.popup.fileTypeError(), 'file-type-error')
            .crCheckLink(PO.popup.fileTypeError.link()).then((url) => this.browser
                .crCheckURL(url, FORMATS, 'Сломана ссылка на импорт объектов карты')
            )
            .click(PO.popupVisible.close())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .click(PO.popupVisible.close())
            .crWaitForVisible(PO.stepEditor(), 'Шаг редактирования карты не открылся');
    });

    it('Симплификация', function () {
        return this.browser
            .crInit('MANY_MAPS', '?config={"limits":{"count":{"vertexes":10}}}')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/gpx/simplification.gpx')
            .crWaitForVisible(PO.import.status(), 'Не появился статус симплификации')
            .crVerifyScreenshot(PO.import.status(), 'simplification-status')
            .crCheckLink(PO.import.warningLink()).then((url) => this.browser
                .crCheckURL(url, LIMITS, 'Сломана ссылка на ограничения')
            )
            .click(PO.import.status.approve())
            .then(checkAfterImport)
            .crVerifyScreenshot(PO.geoObjectList(), 'simplification-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'simplification-map', OPTIONS);
    });

    it('Лимит объектов', function () {
        return this.browser
            .crInit('MANY_MAPS', '?config={"limits":{"count":{"geoObjects":10}}}')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/gpx/limit_objects.gpx')
            .crWaitForVisible(PO.import.status(), 'Не появился статус симплификации')
            .crVerifyScreenshot(PO.import.status(), 'limit-object-status')
            .crCheckLink(PO.import.warningLink()).then((url) => this.browser
                .crCheckURL(url, LIMITS, 'Сломана ссылка на ограничения')
            )
            .click(PO.import.status.approve())
            .then(checkAfterImport)
            .crShouldBeVisible(PO.sidebar.limit())
            .crCheckText(PO.sidebar.limit(), '10 / 10', 'Количество объектов / лимит')
            .crVerifyScreenshot(PO.geoObjectList(), 'limit-object-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'limit-object-map', OPTIONS);
    });

    it('Линия trk', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/gpx/trk.gpx')
            .then(checkAfterImport)
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 1, 'должен быть 1 элемент в списке')
            )
            .crVerifyScreenshot(PO.geoObjectList(), 'trk-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'trk-map', OPTIONS);
    });
});
