const cr = require('../../credentials.js');

require('../helper.js')(afterEach);

describe('Превью / Статическая карта', () => {
    afterEach(function () {
        return this.browser
            .crLogout();
    });

    it('Стандартные размеры', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .crSaveMap()
            .click(PO.sidebarExport.staticSwitcher.static())
            .crShouldBeVisible(PO.sidebarExport.staticSwitcher.staticChecked())
            .crCheckValue(PO.sidebarExport.mapWidth(), '500', 'дефолтная ширина карты')
            .crCheckValue(PO.sidebarExport.mapHeight(), '400', 'дефолтная высота карты')
            .getAttribute(PO.mapEditorPreviewHolder(), 'style').then((val) => {
                const width = (/width: [0-9a-z%]+;/).exec(val);
                const height = (/height: [0-9a-z%]+;/).exec(val);
                assert.isArray(width, 'должно быть совпадение');
                assert.strictEqual(width[0], 'width: 500px;');
                assert.isArray(height, 'должно быть совпадение');
                assert.strictEqual(height[0], 'height: 400px;');
            })
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
            .click(PO.sidebarExport.staticSwitcher.static())
            .crShouldBeVisible(PO.sidebarExport.staticSwitcher.staticChecked())
            .crSetValue(PO.sidebarExport.mapWidth(), '600')
            .crSetValue(PO.sidebarExport.mapHeight(), '450')
            .crCheckValue(PO.sidebarExport.mapWidth(), '600', 'максимальная ширина карты')
            .crCheckValue(PO.sidebarExport.mapHeight(), '450', 'максимальная высота карты')
            .crShouldNotBeVisible(PO.popupVisible.error())
            .crSetValue(PO.sidebarExport.mapWidth(), '601')
            .crCheckValue(PO.sidebarExport.mapWidth(), '601', 'больше максимальной ширины')
            .crWaitForVisible(PO.popupVisible.error(), 'Не появился попап с ошибкой')
            .crShouldBeVisible(PO.sidebarExport.mapWidthError())
            .crSetValue(PO.sidebarExport.mapWidth(), '600')
            .crWaitForHidden(PO.popupVisible.error(), 600, 'Не закрылся попап ошибки')
            .crShouldNotBeVisible(PO.sidebarExport.mapWidthError())
            .crSetValue(PO.sidebarExport.mapHeight(), '451')
            .crCheckValue(PO.sidebarExport.mapHeight(), '451', 'больше максимальной высоты')
            .crWaitForVisible(PO.popupVisible.error(), 'Не появился попап с ошибкой')
            .crShouldBeVisible(PO.sidebarExport.mapHeightError())
            .crSetValue(PO.sidebarExport.mapHeight(), '450')
            .crWaitForHidden(PO.popupVisible.error(), 600, 'Не закрылся попап ошибки')
            .crShouldNotBeVisible(PO.sidebarExport.mapHeightError());
    });

    it('Минимальные размеры', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .crSaveMap()
            .click(PO.sidebarExport.staticSwitcher.static())
            .crShouldBeVisible(PO.sidebarExport.staticSwitcher.staticChecked())
            .crSetValue(PO.sidebarExport.mapWidth(), '150')
            .crSetValue(PO.sidebarExport.mapHeight(), '150')
            .crCheckValue(PO.sidebarExport.mapWidth(), '150', 'минимальная ширина карты')
            .crCheckValue(PO.sidebarExport.mapHeight(), '150', 'минимальная высота карты')
            .crShouldNotBeVisible(PO.popupVisible.error())
            .crSetValue(PO.sidebarExport.mapWidth(), '149')
            .crCheckValue(PO.sidebarExport.mapWidth(), '149', 'меньше максимальной ширины')
            .crWaitForVisible(PO.popupVisible.error(), 'Не появился попап с ошибкой')
            .crShouldBeVisible(PO.sidebarExport.mapWidthError())
            .crSetValue(PO.sidebarExport.mapWidth(), '150')
            .crWaitForHidden(PO.popupVisible.error(), 600, 'Не закрылся попап ошибки')
            .crShouldNotBeVisible(PO.sidebarExport.mapWidthError())
            .crSetValue(PO.sidebarExport.mapHeight(), '149')
            .crCheckValue(PO.sidebarExport.mapHeight(), '149', 'меньше минимальной высота')
            .crWaitForVisible(PO.popupVisible.error(), 'Не появился попап с ошибкой')
            .crShouldBeVisible(PO.sidebarExport.mapHeightError())
            .crSetValue(PO.sidebarExport.mapHeight(), '150')
            .crWaitForHidden(PO.popupVisible.error(), 600, 'Не закрылся попап ошибки')
            .crShouldNotBeVisible(PO.sidebarExport.mapHeightError());
    });

    it('Получить код карты', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.umForPreview)
            .crSaveMap()
            .click(PO.sidebarExport.staticSwitcher.static())
            .crShouldBeVisible(PO.sidebarExport.staticSwitcher.staticChecked())
            .crShouldBeVisible(PO.sidebarExport.getCodeBtn())
            .click(PO.sidebarExport.getCodeBtn())
            .crWaitForVisible(PO.popup.getCode(), 'Не открылся попап с кодом карты')
            .crCheckValue(PO.popup.getCode.code(), cr.codeStatic, 'содержимое кода статика')
            .click(PO.sidebarExport.getCodeBtn())
            .crWaitForHidden(PO.popup.getCode(), 600, 'Не закрылся попап с кодом карты')
            .crShouldNotBeVisible(PO.popup.getCode());
    });

    it('Сохраненные размеры карты', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.umForPreview)
            .crSaveMap()
            .click(PO.sidebarExport.staticSwitcher.static())
            .crShouldBeVisible(PO.sidebarExport.staticSwitcher.staticChecked())
            .crCheckValue(PO.sidebarExport.mapWidth(), '600', 'сохраненная ширина карты')
            .crCheckValue(PO.sidebarExport.mapHeight(), '450', 'сохраненная высота карты')
            .crWaitForVisible(PO.mapEditorPreviewHolder(), 'Не появилось превью на карте')
            .getCssProperty(PO.mapEditorPreviewHolder(), 'width').then((prop) => {
                assert.equal(prop.value, '600px', 'ширина превью');
            })
            .getCssProperty(PO.mapEditorPreviewHolder(), 'height').then((prop) => {
                assert.equal(prop.value, '450px', 'высота превью');
            })
            .crWaitForVisible(PO.mapEditorPreviewHolder(), 'Не отображается превью на карте');
    });
});
