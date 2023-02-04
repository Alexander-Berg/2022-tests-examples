const TEMPLATE = 'https://yandex.ru/support/maps-builder/concept/markers_5.html#markers_5__XLSX_import';
const LIMITS = 'https://yandex.ru/support/maps-builder/concept/markers_4.html#markers_4__limit';
const FORMATS = 'https://yandex.ru/support/maps-builder/concept/markers_5.html';
const OPTIONS = {
    tolerance: 50,
    ignoreElements: [PO.ymaps.searchBoxInput()]
};

require('../helper.js')(afterEach);

describe('Импорт / XLSX', () => {
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

    it('Метки с разным содержимым', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/xlsx/six.xlsx')
            .then(checkAfterImport)
            .crCheckValue(PO.sidebar.mapName(), 'Импорт из «six.xlsx»', 'Название карты')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 6, 'должно быть 6 элементов в списке')
            )
            .crVerifyScreenshot(PO.geoObjectList(), 'six-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'six-map', OPTIONS);
    });

    it('Пустой файл', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/xlsx/empty.xls')
            .then(checkAfterImport)
            .crCheckValue(PO.sidebar.mapName(), 'Импорт из «empty.xls»', 'Название карты')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 0, 'должно быть 0 элементов в списке')
            );
    });

    it('Есть невалидные метки', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/xlsx/invalid_objects.xls')
            .crWaitForVisible(PO.import.status(), 'Не появился статус об ошибках')
            .crVerifyScreenshot(PO.import.status(), 'invalid-objects-status')
            .crCheckLink(PO.import.warningErrors()).then((url) => this.browser
                .crCheckURL(url, TEMPLATE, 'Сломана ссылка на шаблон xlsx')
            )
            .click(PO.import.status.approve())
            .crCheckValue(PO.sidebar.mapName(), 'Импорт из «invalid_objects.xls»', 'Название карты')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 3, 'должно быть 3 элемента в списке')
            )
            .crVerifyScreenshot(PO.geoObjectList(), 'invalid-objects-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'invalid-objects-map', OPTIONS);
    });

    it('Нет валидных меток', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/xlsx/invalid_all.xls')
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
            .chooseFile(PO.import.attach(), 'import-files/xlsx/limit.xlsx')
            .crWaitForVisible(PO.import.status(), 'Не появился статус о превышении лимитов')
            .crVerifyScreenshot(PO.import.status(), 'limit-object-status')
            .crCheckLink(PO.import.warningLink()).then((url) => this.browser
                .crCheckURL(url, LIMITS, 'Сломана ссылка на ограничения')
            )
            .click(PO.import.status.approve())
            .then(checkAfterImport)
            .crCheckValue(PO.sidebar.mapName(), 'Импорт из «limit.xlsx»', 'Название карты')
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
            .chooseFile(PO.import.attach(), 'import-files/xlsx/template_XLSX_ru.xlsx')
            .then(checkAfterImport)
            .crCheckValue(PO.sidebar.mapName(), 'Импорт из «template_XLSX_ru.xlsx»', 'Название карты')
            .crVerifyScreenshot(PO.geoObjectList(), 'template-list')
            .crVerifyScreenshot(PO.ymaps.map(), 'template-map', OPTIONS);
    });
});
