const FORMATS = 'https://yandex.ru/support/maps-builder/concept/markers_5.html';
const XLSX = 'https://l7test.yandex.ru/map-constructor/res/import_templates/template_XLSX_ru.xlsx';
const CSV = 'https://l7test.yandex.ru/map-constructor/res/import_templates/template_CSV_ru.csv';

require('../helper.js')(afterEach);

describe('Импорт', () => {
    afterEach(function () {
        return this.browser
            .crLogout();
    });

    it('Через кнопку в списке карт', function () {
        return this.browser
            .crInit('MANY_MAPS', '', 'openmap')
            .crShouldBeVisible(PO.mapSelection.import())
            .click(PO.mapSelection.import())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .crVerifyScreenshot(PO.import(), 'import-modal')
            .crCheckLink(PO.import.desc()).then((url) => this.browser
                .crCheckURL(url, FORMATS, 'Сломана ссылка на форматы импорта')
            )
            .crCheckLink(PO.import.templates.xlsx()).then((url) => this.browser
                .crCheckURL(url, XLSX, 'Сломана ссылка шаблон XLSX')
            )
            .crCheckLink(PO.import.templates.csv()).then((url) => this.browser
                .crCheckURL(url, CSV, 'Сломана ссылка шаблон CSV')
            )
            .getAttribute(PO.import.templates.xlsx(), 'download').then((val) => {
                assert.strictEqual(val, 'template_XLSX_ru.xlsx', 'название файла xlsx');
            })
            .getAttribute(PO.import.templates.xlsx(), 'data-filetype').then((val) => {
                assert.strictEqual(val, 'XLSX', 'формат xlsx');
            })
            .getAttribute(PO.import.templates.csv(), 'download').then((val) => {
                assert.strictEqual(val, 'template_CSV_ru.csv', 'название файла csv');
            })
            .getAttribute(PO.import.templates.csv(), 'data-filetype').then((val) => {
                assert.strictEqual(val, 'CSV', 'формат csv');
            });
    });

    it('Через кнопку в новой карте', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .crVerifyScreenshot(PO.import(), 'import-modal')
            .crCheckLink(PO.import.desc()).then((url) => this.browser
                .crCheckURL(url, FORMATS, 'Сломана ссылка на форматы импорта')
            )
            .crCheckLink(PO.import.templates.xlsx()).then((url) => this.browser
                .crCheckURL(url, XLSX, 'Сломана ссылка шаблон XLSX')
            )
            .crCheckLink(PO.import.templates.csv()).then((url) => this.browser
                .crCheckURL(url, CSV, 'Сломана ссылка шаблон CSV')
            )
            .getAttribute(PO.import.templates.xlsx(), 'download').then((val) => {
                assert.strictEqual(val, 'template_XLSX_ru.xlsx', 'название файла xlsx');
            })
            .getAttribute(PO.import.templates.xlsx(), 'data-filetype').then((val) => {
                assert.strictEqual(val, 'XLSX', 'формат xlsx');
            })
            .getAttribute(PO.import.templates.csv(), 'download').then((val) => {
                assert.strictEqual(val, 'template_CSV_ru.csv', 'название файла csv');
            })
            .getAttribute(PO.import.templates.csv(), 'data-filetype').then((val) => {
                assert.strictEqual(val, 'CSV', 'формат csv');
            });
    });

    it('Закрыть окно импорта через список карт', function () {
        return this.browser
            .crInit('MANY_MAPS', '', 'openmap')
            .crShouldBeVisible(PO.mapSelection.import())
            .click(PO.mapSelection.import())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось 1')
            .crShouldNotBeVisible(PO.mapSelection())
            .click(PO.popupVisible.close())
            .crWaitForHidden(PO.import(), 'Окно импорта не закрылось 1')
            .crShouldBeVisible(PO.mapSelection())
            .click(PO.mapSelection.import())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось 2')
            .crShouldNotBeVisible(PO.mapSelection())
            .pause(700)
            .leftClick(PO.popupVisible.modelCell(), 50, 50)
            .crWaitForHidden(PO.import(), 'Окно импорта не закрылось 2')
            .crShouldBeVisible(PO.mapSelection());
    });

    it('Закрыть окно импорта через новую карту', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось 1')
            .crShouldNotBeVisible(PO.mapSelection())
            .click(PO.popupVisible.close())
            .crWaitForHidden(PO.import(), 'Окно импорта не закрылось 1')
            .crShouldNotBeVisible(PO.mapSelection())
            .crShouldBeVisible(PO.stepEditor())
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось 2')
            .crShouldNotBeVisible(PO.mapSelection())
            .pause(700)
            .leftClick(PO.popupVisible.modelCell(), 50, 50)
            .crWaitForHidden(PO.import(), 'Окно импорта не закрылось 2')
            .crShouldNotBeVisible(PO.mapSelection())
            .crShouldBeVisible(PO.stepEditor());
    });

    it('Закрыть ошибку чтения файла', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/picture.png')
            .crWaitForVisible(PO.popup.fileTypeError(), 'Не появилась ошибка чтения формата')
            .crCheckLink(PO.popup.fileTypeError.link()).then((url) => this.browser
                .crCheckURL(url, FORMATS, 'Сломана ссылка на импорт объектов карты')
            )
            .click(PO.popupVisible.close())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось');
    });

    it('Закрыть статус импорта через список карт', function () {
        return this.browser
            .crInit('MANY_MAPS', '?config={"limits":{"count":{"geoObjects":10}}}', 'openmap')
            .crShouldBeVisible(PO.mapSelection.import())
            .click(PO.mapSelection.import())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/csv/limits_and_coords.csv')
            .crWaitForVisible(PO.import.status(), 'Не появился статус о превышении лимитов')
            .crShouldBeVisible(PO.import.status.abort())
            .click(PO.import.status.abort())
            .crWaitForHidden(PO.import.status(), 'Статус импорта не закрылся')
            .crShouldNotBeVisible(PO.import())
            .crShouldBeVisible(PO.mapSelection());
    });

    it('Закрыть статус импорта через новую карту', function () {
        return this.browser
            .crInit('MANY_MAPS', '?config={"limits":{"count":{"geoObjects":10}}}')
            .click(PO.sidebar.importBtn())
            .crWaitForVisible(PO.import(), 'Окно импорта не открылось')
            .chooseFile(PO.import.attach(), 'import-files/csv/limits_and_coords.csv')
            .crWaitForVisible(PO.import.status(), 'Не появился статус о превышении лимитов')
            .crShouldBeVisible(PO.import.status.abort())
            .click(PO.import.status.abort())
            .crWaitForHidden(PO.import.status(), 'Статус импорта не закрылся')
            .crShouldNotBeVisible(PO.import())
            .crShouldBeVisible(PO.stepEditor());
    });
});
