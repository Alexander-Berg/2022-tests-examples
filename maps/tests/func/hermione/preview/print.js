const cr = require('../../credentials.js');
const DISK = 'https://disk.yandex.ru/client/disk/Конструктор%20карт%20Яндекса';
const DOMEN = require('../../.hermione.conf.js').baseUrl;

require('../helper.js')(afterEach);

describe('Превью / Печатная карта', () => {
    const checkDisabled = function () {
        return this
            .getAttribute(PO.sidebarExport.yadiskBtn(), 'disabled').then((val) => {
                assert.equal(val, 'true', 'Кнопка сохранения не задизейблена');
            })
            .getAttribute(PO.sidebarExport.downloadBtn(), 'disabled').then((val) => {
                assert.equal(val, 'true', 'Кнопка скачивания не задизейблена');
            })
            .getAttribute(PO.sidebarExport.mapWidth(), 'disabled').then((val) => {
                assert.equal(val, 'true', 'Инпут ширины не задизейблен');
            })
            .getAttribute(PO.sidebarExport.mapHeight(), 'disabled').then((val) => {
                assert.equal(val, 'true', 'Инпут высоты не задизейблен');
            })
            .getAttribute(PO.sidebarExport.imageExtension(), 'disabled').then((val) => {
                assert.equal(val, 'true', 'Селектор формата не задизейблен');
            })
            .getAttribute(PO.sidebarExport.imageDpi(), 'disabled').then((val) => {
                assert.equal(val, 'true', 'Селектор качества не задизейблен');
            })
            .crShouldBeVisible(PO.sidebarExport.error())
            .click(PO.sidebarExport.yadiskBtn())
            .crShouldNotBeVisible(PO.modalCell())
            .click(PO.sidebarExport.downloadBtn())
            .crShouldNotBeVisible(PO.modalCell());
    };

    const checkSaveMap = function () {
        return this
            .crWaitForVisible(PO.modalCell(), 'Не появилось затемнение')
            .crWaitForVisible(PO.spin(), 'Не появился спиннер')
            .crWaitForVisible(PO.modalCell.mapSaved(), 70000, 'Карта не сохранилась на Диск');
    };

    afterEach(function () {
        return this.browser.crLogout();
    });

    it('Стандартные размеры', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .crSaveMap()
            .click(PO.sidebarExport.mainSwitcher.print())
            .crShouldBeVisible(PO.sidebarExport.mainSwitcher.printChecked())
            .crCheckValue(PO.sidebarExport.mapWidth(), '500', 'дефолтная ширина карты')
            .crCheckValue(PO.sidebarExport.mapHeight(), '400', 'дефолтная высота карты')
            .crWaitForVisible(PO.mapEditorPreviewHolder(), 'Не появилось превью на карте')
            .getCssProperty(PO.mapEditorPreviewHolder(), 'width').then((prop) => {
                assert.equal(prop.value, '500px', 'ширина превью');
            })
            .getCssProperty(PO.mapEditorPreviewHolder(), 'height').then((prop) => {
                assert.equal(prop.value, '400px', 'высота превью');
            });
    });

    it('Максимальные размеры', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .crSaveMap()
            .click(PO.sidebarExport.mainSwitcher.print())
            .crShouldBeVisible(PO.sidebarExport.mainSwitcher.printChecked())
            .setValue(PO.sidebarExport.mapWidth(), '1500')
            .setValue(PO.sidebarExport.mapHeight(), '1500')
            .crCheckValue(PO.sidebarExport.mapWidth(), '1500', 'максимальная ширина карты')
            .crCheckValue(PO.sidebarExport.mapHeight(), '1500', 'максимальная высота карты')
            .crShouldNotBeVisible(PO.popupVisible.error())
            .setValue(PO.sidebarExport.mapWidth(), '1501')
            .crCheckValue(PO.sidebarExport.mapWidth(), '1501', 'больше максимальной ширины')
            .crWaitForVisible(PO.popupVisible.error(), 'Не появился попап с ошибкой')
            .crShouldBeVisible(PO.sidebarExport.mapWidthError())
            .setValue(PO.sidebarExport.mapWidth(), '1500')
            .crWaitForHidden(PO.popupVisible.error(), 600, 'Не закрылся попап ошибки')
            .crShouldNotBeVisible(PO.sidebarExport.mapWidthError())
            .setValue(PO.sidebarExport.mapHeight(), '1501')
            .crCheckValue(PO.sidebarExport.mapHeight(), '1501', 'больше максимальной высоты')
            .crWaitForVisible(PO.popupVisible.error(), 'Не появился попап с ошибкой')
            .crShouldBeVisible(PO.sidebarExport.mapHeightError())
            .setValue(PO.sidebarExport.mapHeight(), '1500')
            .crWaitForHidden(PO.popupVisible.error(), 600, 'Не закрылся попап ошибки')
            .crShouldNotBeVisible(PO.sidebarExport.mapHeightError());
    });

    it('Минимальные размеры', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .crSaveMap()
            .click(PO.sidebarExport.mainSwitcher.print())
            .crShouldBeVisible(PO.sidebarExport.mainSwitcher.printChecked())
            .setValue(PO.sidebarExport.mapWidth(), '200')
            .setValue(PO.sidebarExport.mapHeight(), '200')
            .crCheckValue(PO.sidebarExport.mapWidth(), '200', 'минимальная ширина карты')
            .crCheckValue(PO.sidebarExport.mapHeight(), '200', 'минимальная высота карты')
            .crShouldNotBeVisible(PO.popupVisible.error())
            .setValue(PO.sidebarExport.mapWidth(), '199')
            .crCheckValue(PO.sidebarExport.mapWidth(), '199', 'меньше максимальной ширины')
            .crWaitForVisible(PO.popupVisible.error(), 'Не появился попап с ошибкой')
            .crShouldBeVisible(PO.sidebarExport.mapWidthError())
            .setValue(PO.sidebarExport.mapWidth(), '200')
            .crWaitForHidden(PO.popupVisible.error(), 600, 'Не закрылся попап ошибки')
            .crShouldNotBeVisible(PO.sidebarExport.mapWidthError())
            .setValue(PO.sidebarExport.mapHeight(), '199')
            .crCheckValue(PO.sidebarExport.mapHeight(), '199', 'меньше максимальной высоты')
            .crWaitForVisible(PO.popupVisible.error(), 'Не появился попап с ошибкой')
            .crShouldBeVisible(PO.sidebarExport.mapHeightError())
            .setValue(PO.sidebarExport.mapHeight(), '200')
            .crWaitForHidden(PO.popupVisible.error(), 600, 'Не закрылся попап ошибки')
            .crShouldNotBeVisible(PO.sidebarExport.mapHeightError());
    });

    it('Сохраненные размеры карты', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.umForPreview)
            .crSaveMap()
            .click(PO.sidebarExport.mainSwitcher.print())
            .crShouldBeVisible(PO.sidebarExport.mainSwitcher.printChecked())
            .crCheckValue(PO.sidebarExport.mapWidth(), '600', 'сохраненная ширина карты')
            .crCheckValue(PO.sidebarExport.mapHeight(), '550', 'сохраненная высота карты')
            .crWaitForVisible(PO.mapEditorPreviewHolder(), 'Не появилось превью на карте')
            .getCssProperty(PO.mapEditorPreviewHolder(), 'width').then((prop) => {
                assert.equal(prop.value, '600px', 'ширина превью');
            })
            .getCssProperty(PO.mapEditorPreviewHolder(), 'height').then((prop) => {
                assert.equal(prop.value, '550px', 'высота превью');
            });
    });

    it('Сохранение на Диск карты со всеми видами объектов', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.umForPreview)
            .crSaveMap()
            .click(PO.sidebarExport.mainSwitcher.print())
            .crShouldBeVisible(PO.sidebarExport.mainSwitcher.printChecked())
            .click(PO.sidebarExport.yadiskBtn())
            .then(checkSaveMap)
            .crCheckWindowOpen(PO.modalCell.mapSaved.openDisk())
            .then((url) => this.browser
                .crCheckURL(url, DISK, 'Ссылка на диск после сохранения картинки')
            );
    });

    it('Сохранение на Диск карты с включенными пробками', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.umTraffic)
            .crSaveMap()
            .click(PO.sidebarExport.mainSwitcher.print())
            .crShouldBeVisible(PO.sidebarExport.mainSwitcher.printChecked())
            .click(PO.sidebarExport.yadiskBtn())
            .then(checkSaveMap)
            .crCheckWindowOpen(PO.modalCell.mapSaved.openDisk())
            .then((url) => this.browser
                .crCheckURL(url, DISK, 'Ссылка на диск после сохранения картинки')
            );
    });

    it('Сохранение на Диск карты в формате JPG', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.umForPreview)
            .crSaveMap()
            .click(PO.sidebarExport.mainSwitcher.print())
            .crShouldBeVisible(PO.sidebarExport.mainSwitcher.printChecked())
            .crCheckText(PO.sidebarExport.imageExtension.text(), 'PNG', 'Должен быть выбран формат PNG')
            .click(PO.sidebarExport.imageExtension())
            .crWaitForVisible(PO.popupVisible(), 'Не появился попап')
            .click(PO.popupVisible.menuItem() + '_index_1')
            .crShouldNotBeVisible(PO.popupVisible())
            .crCheckText(PO.sidebarExport.imageExtension.text(), 'JPG', 'Должен быть выбран формат JPG')
            .click(PO.sidebarExport.yadiskBtn())
            .then(checkSaveMap)
            .crCheckWindowOpen(PO.modalCell.mapSaved.openDisk())
            .then((url) => this.browser
                .crCheckURL(url, DISK, 'Ссылка на диск после сохранения картинки')
            );
    });

    it('Сохранение на Диск карты в качестве 300 DPI', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.umForPreview)
            .crSaveMap()
            .click(PO.sidebarExport.mainSwitcher.print())
            .crShouldBeVisible(PO.sidebarExport.mainSwitcher.printChecked())
            .crCheckText(PO.sidebarExport.imageDpi.text(), '96', 'Должно быть выбрано качество 96')
            .click(PO.sidebarExport.imageDpi())
            .crWaitForVisible(PO.popupVisible(), 'Не появился попап')
            .click(PO.popupVisible.menuItem() + '_index_1')
            .crShouldNotBeVisible(PO.popupVisible())
            .crCheckText(PO.sidebarExport.imageDpi.text(), '300', 'Должно быть выбрано качество 300')
            .click(PO.sidebarExport.yadiskBtn())
            .then(checkSaveMap)
            .crCheckWindowOpen(PO.modalCell.mapSaved.openDisk())
            .then((url) => this.browser
                .crCheckURL(url, DISK, 'Ссылка на диск после сохранения картинки')
            );
    });

    it('Можно сохранить карту со слоем Гибрид', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.umHybrid)
            .crSaveMap()
            .click(PO.sidebarExport.mainSwitcher.print())
            .crShouldBeVisible(PO.sidebarExport.mainSwitcher.printChecked())
            .crShouldBeVisible(PO.sidebarExport.error())
            .crShouldBeVisible(PO.sidebarExport.errorMap())
            .crWaitForVisible(PO.mapEditorPreviewHolder(), 'Не отображается превью на карте')
            .getAttribute(PO.sidebarExport.yadiskBtn(), 'disabled').then((val) => {
                assert.equal(val, null, 'Кнопка сохранения не задизейблена');
            })
            .getAttribute(PO.sidebarExport.downloadBtn(), 'disabled').then((val) => {
                assert.equal(val, null, 'Кнопка скачивания не задизейблена');
            });
    });

    it('Можно сохранить карту со слоем Спутник', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.umSatellite)
            .crSaveMap()
            .click(PO.sidebarExport.mainSwitcher.print())
            .crShouldBeVisible(PO.sidebarExport.mainSwitcher.printChecked())
            .crShouldBeVisible(PO.sidebarExport.error())
            .crShouldBeVisible(PO.sidebarExport.errorMap())
            .crWaitForVisible(PO.mapEditorPreviewHolder(), 'Не отображается превью на карте')
            .getAttribute(PO.sidebarExport.yadiskBtn(), 'disabled').then((val) => {
                assert.equal(val, null, 'Кнопка сохранения не задизейблена');
            })
            .getAttribute(PO.sidebarExport.downloadBtn(), 'disabled').then((val) => {
                assert.equal(val, null, 'Кнопка скачивания не задизейблена');
            });
    });

    it('Невозможно сохранить карту с минимальным зумом', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.umMinZoom)
            .crSaveMap()
            .click(PO.sidebarExport.mainSwitcher.print())
            .then(checkDisabled)
            .crShouldBeVisible(PO.sidebarExport.errorZoom());
    });

    it('Стандартная печать', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.umForPreview)
            .crSaveMap()
            .click(PO.sidebarExport.mainSwitcher.print())
            .crShouldBeVisible(PO.sidebarExport.mainSwitcher.printChecked())
            .crShouldBeVisible(PO.sidebarExport.classicPrint())
            .click(PO.sidebarExport.classicPrint())
            .crSelectLastTab()
            .crWaitForVisible('.print-view', 'Стандартная печать не открылась')
            .elements('.user-maps-features-view > div').then((res) => {
                assert.lengthOf(res.value, 4, 'должно быть 4 объекта в стандартной печати');
            });
    });

    it('Стандартная печать при недопустимых параметрах карты', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.umMinZoom)
            .crSaveMap()
            .click(PO.sidebarExport.mainSwitcher.print())
            .crShouldBeVisible(PO.sidebarExport.mainSwitcher.printChecked())
            .crShouldBeVisible(PO.sidebarExport.error.classicPrint())
            .click(PO.sidebarExport.error.classicPrint())
            .crSelectLastTab()
            .crWaitForVisible('.print-view', 'Стандартная печать не открылась')
            .elements('.user-maps-features-view > div').then((res) => {
                assert.lengthOf(res.value, 1, 'должно быть 1 объект в стандартной печати');
            });
    });

    it('Закрытие статуса сохранения карты по клику на крестик и по затемнению', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.umForPreview)
            .crSaveMap()
            .click(PO.sidebarExport.mainSwitcher.print())
            .crShouldBeVisible(PO.sidebarExport.mainSwitcher.printChecked())
            .click(PO.sidebarExport.yadiskBtn())
            .then(checkSaveMap)
            .click(PO.modalCell.mapSaved.close())
            .crWaitForHidden(PO.modalCell.mapSaved(), 'Статус сохранения не закрылся 1')
            .click(PO.sidebarExport.yadiskBtn())
            .then(checkSaveMap)
            .pause(1000)
            .leftClick(PO.popupVisible.modelCell(), 10, 10)
            .crWaitForHidden(PO.modalCell.mapSaved(), 'Статус сохранения не закрылся 2');
    });
});
