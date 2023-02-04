const TEMPLATE = 'https://yandex.ru/support/maps-builder/concept/markers_5.html#markers_5__CSV_import';
const LIMITS = 'https://yandex.ru/support/maps-builder/concept/markers_4.html#markers_4__limit';
const FORMATS = 'https://yandex.ru/support/maps-builder/concept/markers_5.html';
const OPTIONS = {
    tolerance: 50,
    ignoreElements: [PO.ymaps.searchBoxInput()]
};

require('../helper.js')(afterEach);

describe('Импорт / CSV', () => {
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

    it('Точка с запятой', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/csv/semicolon.csv')
            .then(checkAfterImport)
            .crCheckValue(PO.sidebar.mapName(), 'Импорт из «semicolon.csv»', 'Название карты')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 7, 'должно быть 7 элементов в списке')
            )
            .crVerifyScreenshot(PO.geoObjectList(), 'seven-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'seven-map');
    });

    it('Запятая', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/csv/comma.csv')
            .then(checkAfterImport)
            .crCheckValue(PO.sidebar.mapName(), 'Импорт из «comma.csv»', 'Название карты')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 7, 'должно быть 7 элементов в списке')
            )
            .crVerifyScreenshot(PO.geoObjectList(), 'seven-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'seven-map');
    });

    it('Табуляция', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/csv/tab.csv')
            .then(checkAfterImport)
            .crCheckValue(PO.sidebar.mapName(), 'Импорт из «tab.csv»', 'Название карты')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 7, 'должно быть 7 элементов в списке')
            )
            .crVerifyScreenshot(PO.geoObjectList(), 'seven-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'seven-map');
    });

    it('Пустой файл', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/csv/empty.csv')
            .then(checkAfterImport)
            .crCheckValue(PO.sidebar.mapName(), 'Импорт из «empty.csv»', 'Название карты')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 0, 'должно быть 0 элементов в списке')
            );
    });

    it('Есть невалидные метки', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/csv/invalid_objects.csv')
            .crWaitForVisible(PO.import.status(), 'Не появился статус об ошибках')
            .crVerifyScreenshot(PO.import.status(), 'invalid-objects-status')
            .crCheckLink(PO.import.warningErrors()).then((url) => this.browser
                .crCheckURL(url, TEMPLATE, 'Сломана ссылка на шаблон CSV')
            )
            .click(PO.import.status.approve())
            .crCheckValue(PO.sidebar.mapName(), 'Импорт из «invalid_objects.csv»', 'Название карты')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 3, 'должно быть 3 элемента в списке')
            )
            .crVerifyScreenshot(PO.geoObjectList(), 'invalid-objects-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'invalid-objects-map');
    });

    it('Нет валидных меток', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/csv/invalid_all.csv')
            .crWaitForVisible(PO.popup.fileTypeError(), 'Не появилась ошибка чтения формата')
            .crCheckLink(PO.popup.fileTypeError.link()).then((url) => this.browser
                .crCheckURL(url, FORMATS, 'Сломана ссылка на импорт объектов карты')
            )
            .click(PO.popupVisible.close())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .click(PO.popupVisible.close())
            .crWaitForVisible(PO.stepEditor(), 'Шаг редактирования карты не открылся');
    });

    it('Лимит объектов', function () {
        return this.browser
            .crInit('MANY_MAPS', '?config={"limits":{"count":{"geoObjects":10}}}')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/csv/limits_and_coords.csv')
            .crWaitForVisible(PO.import.status(), 'Не появился статус о превышении лимитов')
            .crVerifyScreenshot(PO.import.status(), 'limit-object-status')
            .crCheckLink(PO.import.warningLink()).then((url) => this.browser
                .crCheckURL(url, LIMITS, 'Сломана ссылка на ограничения')
            )
            .click(PO.import.status.approve())
            .then(checkAfterImport)
            .crCheckValue(PO.sidebar.mapName(), 'Импорт из «limits_and_coords.csv»', 'Название карты')
            .crShouldBeVisible(PO.sidebar.limit())
            .crCheckText(PO.sidebar.limit(), '10 / 10', 'Количество объектов / лимит')
            .crVerifyScreenshot(PO.geoObjectList(), 'limit-object-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'limit-object-map', OPTIONS);
    });

    it('Шаблон', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/csv/template_CSV_ru.csv')
            .then(checkAfterImport)
            .crCheckValue(PO.sidebar.mapName(), 'Импорт из «template_CSV_ru.csv»', 'Название карты')
            .crVerifyScreenshot(PO.geoObjectList(), 'template-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'template-map', OPTIONS);
    });
});
