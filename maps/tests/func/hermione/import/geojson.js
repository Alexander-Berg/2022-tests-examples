const FORMATS = 'https://yandex.ru/support/maps-builder/concept/markers_5.html';
const LIMITS = 'https://yandex.ru/support/maps-builder/concept/markers_4.html#markers_4__limit';
const OPTIONS = {
    tolerance: 20,
    ignoreElements: [PO.ymaps.searchBoxInput()]
};

require('../helper.js')(afterEach);

describe('Импорт / GeoJSON', () => {
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

    it('Все виды объектов', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/geojson/all_objects.geojson')
            .then(checkAfterImport)
            .crCheckValue(PO.sidebar.mapName(), 'Все объекты', 'Название карты')
            .crCheckValue(PO.sidebar.mapDesc(), '4 метки, 2 линии и полигон', 'Описание карты')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 7, 'должно быть 7 элементов в списке')
            )
            .crVerifyScreenshot(PO.geoObjectList(), 'all-objects-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'all-objects-map', OPTIONS);
    });

    it('Пример из доки', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/geojson/Sample_GEOJSON.geojson')
            .then(checkAfterImport)
            .crCheckValue(PO.sidebar.mapName(), 'Импорт из «Sample_GEOJSON.geojson»', 'Название карты')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 11, 'должно быть 11 элементов в списке')
            )
            .crVerifyScreenshot(PO.geoObjectList(), 'sample-list');
    });

    it('Файл без объектов', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/geojson/empty_features.geojson')
            .then(checkAfterImport)
            .crCheckValue(PO.sidebar.mapName(), 'Импорт из «empty_features.geojson»', 'Название карты');
    });

    it('Пустой файл', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/geojson/empty_file.geojson')
            .crWaitForVisible(PO.popup.fileTypeError(), 'Не появилась ошибка чтения формата')
            .crCheckLink(PO.popup.fileTypeError.link()).then((url) => this.browser
                .crCheckURL(url, FORMATS, 'Сломана ссылка на импорт объектов карты')
            )
            .click(PO.popupVisible.close())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .click(PO.popupVisible.close())
            .crWaitForVisible(PO.stepEditor(), 'Шаг редактирования карты не открылся');
    });

    it('Без метаданных', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/geojson/without_meta.geojson')
            .then(checkAfterImport)
            .crCheckValue(PO.sidebar.mapName(), 'Импорт из «without_meta.geojson»', 'Название карты')
            .crCheckValue(PO.sidebar.mapDesc(), '', 'Нет описание карты')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 3, 'должно быть 3 элемента в списке')
            )
            .crVerifyScreenshot(PO.ymaps.map(), 'without-meta-map', OPTIONS);
    });

    it('Невалидный файл', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/geojson/invalid.geojson')
            .crWaitForVisible(PO.popup.fileTypeError(), 'Не появилась ошибка чтения формата')
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
            .chooseFile(PO.import.attach(), 'import-files/geojson/simplification.geojson')
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
            .chooseFile(PO.import.attach(), 'import-files/geojson/limit_objects.geojson')
            .crWaitForVisible(PO.import.status(), 'Не появился статус о превышении лимитов')
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
});
