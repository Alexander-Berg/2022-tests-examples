const cr = require('../../credentials.js');

require('../helper.js')(afterEach);

describe('Превью / Интерактивная карта', () => {
    afterEach(function () {
        return this.browser
            .crLogout();
    });

    describe('Script', () => {
        const openPreview = function () {
            return this
                .crSaveMap()
                .crShouldBeVisible(PO.sidebarExport.getCodeBtn())
                .crWaitForVisible(PO.mapEditorInteractiveScript.map());
        };

        it('Стандартные размеры', function () {
            return this.browser
                .crInit('MANY_MAPS')
                .then(openPreview)
                .crShouldBeVisible(PO.sidebarExport.staticSwitcher.interactiveChecked())
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
                .then(openPreview)
                .setValue(PO.sidebarExport.mapWidth(), '1280')
                .setValue(PO.sidebarExport.mapHeight(), '720')
                .crCheckValue(PO.sidebarExport.mapWidth(), '1280', 'максимальная ширина карты')
                .crCheckValue(PO.sidebarExport.mapHeight(), '720', 'максимальная высота карты')
                .crShouldNotBeVisible(PO.popupVisible.error())
                .setValue(PO.sidebarExport.mapWidth(), '1281')
                .crCheckValue(PO.sidebarExport.mapWidth(), '1281', 'больше максимальной ширины')
                .crWaitForVisible(PO.popupVisible.error(), 'Не появился попап с ошибкой')
                .crShouldBeVisible(PO.sidebarExport.mapWidthError())
                .setValue(PO.sidebarExport.mapWidth(), '1280')
                .crWaitForHidden(PO.popupVisible.error(), 700, 'Попап ошибки не пропал')
                .crShouldNotBeVisible(PO.sidebarExport.mapWidthError())
                .setValue(PO.sidebarExport.mapHeight(), '721')
                .crCheckValue(PO.sidebarExport.mapHeight(), '721', 'больше максимальной высоты')
                .crWaitForVisible(PO.popupVisible.error(), 'Не появился попап с ошибкой')
                .crShouldBeVisible(PO.sidebarExport.mapHeightError())
                .setValue(PO.sidebarExport.mapHeight(), '720')
                .crWaitForHidden(PO.popupVisible.error(), 700, 'Попап ошибки не пропал')
                .crShouldNotBeVisible(PO.sidebarExport.mapHeightError());
        });

        it('Минимальные размеры', function () {
            return this.browser
                .crInit('MANY_MAPS')
                .then(openPreview)
                .setValue(PO.sidebarExport.mapWidth(), '320')
                .setValue(PO.sidebarExport.mapHeight(), '240')
                .crCheckValue(PO.sidebarExport.mapWidth(), '320', 'минимальная ширина карты')
                .crCheckValue(PO.sidebarExport.mapHeight(), '240', 'минимальная высота карты')
                .crShouldNotBeVisible(PO.popupVisible.error())
                .setValue(PO.sidebarExport.mapWidth(), '319')
                .crCheckValue(PO.sidebarExport.mapWidth(), '319', 'меньше максимальной ширины')
                .crWaitForVisible(PO.popupVisible.error(), 'Не появился попап с ошибкой')
                .crShouldBeVisible(PO.sidebarExport.mapWidthError())
                .setValue(PO.sidebarExport.mapWidth(), '320')
                .crWaitForHidden(PO.popupVisible.error(), 700, 'Попап ошибки не пропал')
                .crShouldNotBeVisible(PO.sidebarExport.mapWidthError())
                .setValue(PO.sidebarExport.mapHeight(), '239')
                .crCheckValue(PO.sidebarExport.mapHeight(), '239', 'меньше максимальной высоты')
                .crWaitForVisible(PO.popupVisible.error(), 'Не появился попап с ошибкой')
                .crShouldBeVisible(PO.sidebarExport.mapHeightError())
                .setValue(PO.sidebarExport.mapHeight(), '240')
                .crWaitForHidden(PO.popupVisible.error(), 700, 'Попап ошибки не пропал')
                .crShouldNotBeVisible(PO.sidebarExport.mapHeightError());
        });

        it('Растянуть по ширине', function () {
            const resizer = PO.mapEditorInteractiveScript.resizer();
            return this.browser
                .crInit('MANY_MAPS')
                .then(openPreview)
                .getAttribute(PO.sidebarExport.fitWidth.input(), 'checked').then((val) => {
                    assert.isNull(val, 'не чекнут');
                })
                .getCssProperty(PO.mapEditorPreviewHolder(), 'width').then((prop) => {
                    assert.equal(prop.value, '500px', 'ширина превью');
                })
                .getCssProperty(PO.mapEditorPreviewHolder(), 'height').then((prop) => {
                    assert.equal(prop.value, '400px', 'высота превью');
                })
                .crShouldBeVisible(resizer + 'top-left')
                .crShouldBeVisible(resizer + 'left')
                .crShouldBeVisible(resizer + 'bottom-left')
                .crShouldBeVisible(resizer + 'top-right')
                .crShouldBeVisible(resizer + 'right')
                .crShouldBeVisible(resizer + 'bottom-right')
                .click(PO.sidebarExport.fitWidth())
                .getAttribute(PO.sidebarExport.fitWidth.input(), 'checked').then((val) => {
                    assert.strictEqual(val, 'true', 'чекнут');
                })
                .getAttribute(PO.mapEditorPreviewHolder(), 'style').then((val) => {
                    const width = (/width: [0-9a-z%]+;/).exec(val);
                    assert.isArray(width, 'должно быть совпадение');
                    assert.strictEqual(width[0], 'width: 100%;', 'ширина превью');
                })
                .getCssProperty(PO.mapEditorPreviewHolder(), 'height').then((prop) => {
                    assert.equal(prop.value, '400px', 'высота превью');
                })
                .crShouldNotBeVisible(resizer + 'top-left')
                .crShouldNotBeVisible(resizer + 'left')
                .crShouldNotBeVisible(resizer + 'bottom-left')
                .crShouldNotBeVisible(resizer + 'top-right')
                .crShouldNotBeVisible(resizer + 'right')
                .crShouldNotBeVisible(resizer + 'bottom-right');
        });

        it('Получить код карты', function () {
            return this.browser
                .crInit('MANY_MAPS', cr.umForPreview)
                .then(openPreview)
                .crShouldBeVisible(PO.sidebarExport.getCodeBtn())
                .click(PO.sidebarExport.getCodeBtn())
                .crWaitForVisible(PO.popup.getCode(), 'Не открылся попап с кодом карты')
                .crCheckValue(PO.popup.getCode.code(), cr.codeInteractiveScript, 'содержимое кода виджета');
        });

        it('Сохраненные размеры карты', function () {
            return this.browser
                .crInit('MANY_MAPS', cr.umForPreview)
                .then(openPreview)
                .crCheckValue(PO.sidebarExport.mapWidth(), '600', 'сохраненная ширина карты')
                .crCheckValue(PO.sidebarExport.mapHeight(), '550', 'сохраненная высота карты')
                .getCssProperty(PO.mapEditorPreviewHolder(), 'width').then((prop) => {
                    assert.equal(prop.value, '600px', 'ширина превью');
                })
                .getCssProperty(PO.mapEditorPreviewHolder(), 'height').then((prop) => {
                    assert.equal(prop.value, '550px', 'высота превью');
                });
        });

        it('Сохраняется 100% ширина после переключения вкладки', function () {
            const resizer = PO.mapEditorInteractiveScript.resizer();
            return this.browser
                .crInit('MANY_MAPS')
                .crSaveMap()
                .crShouldBeVisible(PO.sidebarExport.getCodeBtn())
                .click(PO.sidebarExport.getCodeBtn())
                .crWaitForVisible(PO.popup.getCode(), 'Не открылся попап с кодом карты')
                .crShouldBeVisible(PO.popup.getCode.switcher.jsChecked())
                .click(PO.popup.getCode.switcher.iframe())
                .crWaitForVisible(PO.popup.getCode.switcher.iframeChecked())
                .getAttribute(PO.sidebarExport.fitWidth.input(), 'checked').then((val) => {
                    assert.isNull(val, 'не чекнут');
                })
                .click(PO.sidebarExport.fitWidth())
                .getAttribute(PO.sidebarExport.fitWidth.input(), 'checked').then((val) => {
                    assert.strictEqual(val, 'true', 'чекнут');
                })
                .click(PO.popup.getCode.switcher.js())
                .crShouldBeVisible(PO.popup.getCode.switcher.jsChecked())
                .getAttribute(PO.sidebarExport.fitWidth.input(), 'checked').then((val) => {
                    assert.strictEqual(val, 'true', 'чекнут "Растянуть по ширине" iframe');
                })
                .crWaitForVisible(PO.mapEditorPreviewHolder())
                .getAttribute(PO.mapEditorPreviewHolder(), 'style').then((val) => {
                    const width = (/width: [0-9a-z%]+;/).exec(val);
                    assert.isArray(width, 'должно быть совпадение');
                    assert.strictEqual(width[0], 'width: 100%;', 'ширина превью');
                })
                .getCssProperty(PO.mapEditorPreviewHolder(), 'height').then((prop) => {
                    assert.equal(prop.value, '400px', 'высота превью');
                })
                .getValue(PO.popup.getCode.code()).then((val) => {
                    const width = (/width=[0-9a-z%]+/).exec(val);
                    const height = (/height=[0-9a-z%]+/).exec(val);
                    assert.match(val, /<script.*?<\/script>/, 'в инпуте скрипт');
                    assert.isArray(width, 'должно быть совпадение');
                    assert.isArray(height, 'должно быть совпадение');
                    assert.strictEqual(width[0], 'width=100%25', 'ширина превью');
                    assert.strictEqual(height[0], 'height=400', 'высота превью');
                })
                .crShouldNotBeVisible(resizer + 'top-left')
                .crShouldNotBeVisible(resizer + 'left')
                .crShouldNotBeVisible(resizer + 'bottom-left')
                .crShouldNotBeVisible(resizer + 'top-right')
                .crShouldNotBeVisible(resizer + 'right')
                .crShouldNotBeVisible(resizer + 'bottom-right');
        });

        it('Нет ошибки при недопустимом масштабе', function () {
            return this.browser
                .crInit('MANY_MAPS')
                .crWaitForVisible(PO.ymaps(), 'Не загрузилось АПИ')
                .crWaitForVisible(PO.ymaps.minus(), 'Нет контрола для уменьшения масштаба')
                .click(PO.ymaps.minus())
                .click(PO.ymaps.minus())
                .click(PO.ymaps.minus())
                .click(PO.ymaps.minus())
                .click(PO.ymaps.minus())
                .click(PO.ymaps.minus())
                .click(PO.ymaps.minus())
                .click(PO.ymaps.minus())
                .click(PO.ymaps.minus())
                .click(PO.ymaps.minus())
                .click(PO.ymaps.minus())
                .click(PO.ymaps.minus())
                .crSaveMap()
                .crWaitForVisible(PO.mapEditorPreviewHolder(), 'Не отображается превью на карте')
                .crShouldBeVisible(PO.sidebarExport.getCodeBtn())
                .click(PO.sidebarExport.getCodeBtn())
                .crWaitForVisible(PO.popup.getCode(), 'Не открылся попап с кодом карты')
                .crShouldNotBeVisible(PO.popup.getCode.iframeError(), 'Не должно быть ошибок во вкладке JS')
                .crShouldNotBeVisible(PO.popup.getCode.iframeErrorZoom(), 'Не должно быть ошибок во вкладке JS');
        });

        it('Нет ошибки при недопустимом слое', function () {
            return this.browser
                .crInit('MANY_MAPS')
                .crWaitForVisible(PO.ymaps.layers())
                .click(PO.ymaps.layers())
                .crWaitForVisible(PO.ymaps.layersList())
                .click(PO.ymapsLayerItem() + '=Гибрид')
                .crSaveMap()
                .crWaitForVisible(PO.mapEditorPreviewHolder(), 'Не отображается превью на карте')
                .crShouldBeVisible(PO.sidebarExport.getCodeBtn())
                .click(PO.sidebarExport.getCodeBtn())
                .crWaitForVisible(PO.popup.getCode(), 'Не открылся попап с кодом карты')
                .crShouldNotBeVisible(PO.popup.getCode.iframeError(), 'Не должно быть ошибок во вкладке JS')
                .crShouldNotBeVisible(PO.popup.getCode.iframeErrorZoom(), 'Не должно быть ошибок во вкладке JS');
        });
    });

    describe('iFrame', () => {
        const openIframe = function () {
            return this
                .crShouldBeVisible(PO.sidebarExport.getCodeBtn())
                .click(PO.sidebarExport.getCodeBtn())
                .crWaitForVisible(PO.popup.getCode(), 'Не открылся попап с кодом карты')
                .crShouldBeVisible(PO.popup.getCode.switcher.jsChecked())
                .click(PO.popup.getCode.switcher.iframe())
                .crWaitForVisible(PO.popup.getCode.switcher.iframeChecked())
                .crWaitForVisible(PO.mapEditorInteractiveIframe.map(), 'Карта-превью для iframe не отображается');
        };

        const switchIframePreview = function () {
            return this
                .crSaveMap()
                .then(openIframe)
                .click(PO.sidebarExport.getCodeBtn())
                .crWaitForHidden(PO.popup.getCode(), 'Не закрылся попап с кодом карты');
        };

        it('Стандартные размеры', function () {
            return this.browser
                .crInit('MANY_MAPS')
                .then(switchIframePreview)
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
                .then(switchIframePreview)
                .crSetValue(PO.sidebarExport.mapWidth(), '1280')
                .crSetValue(PO.sidebarExport.mapHeight(), '720')
                .crCheckValue(PO.sidebarExport.mapWidth(), '1280', 'максимальная ширина карты')
                .crCheckValue(PO.sidebarExport.mapHeight(), '720', 'максимальная высота карты')
                .crShouldNotBeVisible(PO.popupVisible.error())
                .crSetValue(PO.sidebarExport.mapWidth(), '1281')
                .crCheckValue(PO.sidebarExport.mapWidth(), '1281', 'больше максимальной ширины')
                .crWaitForVisible(PO.popupVisible.error(), 'Не появился попап с ошибкой')
                .crShouldBeVisible(PO.sidebarExport.mapWidthError())
                .crSetValue(PO.sidebarExport.mapWidth(), '1280')
                .crWaitForHidden(PO.popupVisible.error(), 700, 'Попап ошибки не пропал')
                .crShouldNotBeVisible(PO.sidebarExport.mapWidthError())
                .crSetValue(PO.sidebarExport.mapHeight(), '721')
                .crCheckValue(PO.sidebarExport.mapHeight(), '721', 'больше максимальной высоты')
                .crWaitForVisible(PO.popupVisible.error(), 'Не появился попап с ошибкой')
                .crShouldBeVisible(PO.sidebarExport.mapHeightError())
                .crSetValue(PO.sidebarExport.mapHeight(), '720')
                .crWaitForHidden(PO.popupVisible.error(), 700, 'Попап ошибки не пропал')
                .crShouldNotBeVisible(PO.sidebarExport.mapHeightError());
        });

        it('Минимальные размеры', function () {
            return this.browser
                .crInit('MANY_MAPS')
                .then(switchIframePreview)
                .crSetValue(PO.sidebarExport.mapWidth(), '320')
                .crSetValue(PO.sidebarExport.mapHeight(), '240')
                .crCheckValue(PO.sidebarExport.mapWidth(), '320', 'минимальная ширина карты')
                .crCheckValue(PO.sidebarExport.mapHeight(), '240', 'минимальная высота карты')
                .crShouldNotBeVisible(PO.popupVisible.error())
                .crSetValue(PO.sidebarExport.mapWidth(), '319')
                .crCheckValue(PO.sidebarExport.mapWidth(), '319', 'меньше максимальной ширины')
                .crWaitForVisible(PO.popupVisible.error(), 'Не появился попап с ошибкой')
                .crShouldBeVisible(PO.sidebarExport.mapWidthError())
                .crSetValue(PO.sidebarExport.mapWidth(), '320')
                .crWaitForHidden(PO.popupVisible.error(), 700, 'Попап ошибки не пропал')
                .crShouldNotBeVisible(PO.sidebarExport.mapWidthError())
                .crSetValue(PO.sidebarExport.mapHeight(), '239')
                .crCheckValue(PO.sidebarExport.mapHeight(), '239', 'меньше максимальной высоты')
                .crWaitForVisible(PO.popupVisible.error(), 'Не появился попап с ошибкой')
                .crShouldBeVisible(PO.sidebarExport.mapHeightError())
                .crSetValue(PO.sidebarExport.mapHeight(), '240')
                .crWaitForHidden(PO.popupVisible.error(), 700, 'Попап ошибки не пропал')
                .crShouldNotBeVisible(PO.sidebarExport.mapHeightError());
        });

        it('Растянуть по ширине', function () {
            const resizer = PO.mapEditorInteractiveIframe.resizer();
            return this.browser
                .crInit('MANY_MAPS')
                .then(switchIframePreview)
                .getAttribute(PO.sidebarExport.fitWidth.input(), 'checked').then((val) => {
                    assert.isNull(val, 'не чекнут');
                })
                .getCssProperty(PO.mapEditorPreviewHolder(), 'width').then((prop) => {
                    assert.equal(prop.value, '500px', 'ширина превью');
                })
                .getCssProperty(PO.mapEditorPreviewHolder(), 'height').then((prop) => {
                    assert.equal(prop.value, '400px', 'высота превью');
                })
                .crShouldBeVisible(resizer + 'top-left')
                .crShouldBeVisible(resizer + 'left')
                .crShouldBeVisible(resizer + 'bottom-left')
                .crShouldBeVisible(resizer + 'top-right')
                .crShouldBeVisible(resizer + 'right')
                .crShouldBeVisible(resizer + 'bottom-right')
                .click(PO.sidebarExport.fitWidth())
                .getAttribute(PO.sidebarExport.fitWidth.input(), 'checked').then((val) => {
                    assert.strictEqual(val, 'true', 'чекнут');
                })
                .getAttribute(PO.mapEditorPreviewHolder(), 'style').then((val) => {
                    const width = (/width: [0-9a-z%]+;/).exec(val);
                    assert.isArray(width, 'должно быть совпадение');
                    assert.strictEqual(width[0], 'width: 100%;', 'ширина превью');
                })
                .getCssProperty(PO.mapEditorPreviewHolder(), 'height').then((prop) => {
                    assert.equal(prop.value, '400px', 'высота превью');
                })
                .crShouldNotBeVisible(resizer + 'top-left')
                .crShouldNotBeVisible(resizer + 'left')
                .crShouldNotBeVisible(resizer + 'bottom-left')
                .crShouldNotBeVisible(resizer + 'top-right')
                .crShouldNotBeVisible(resizer + 'right')
                .crShouldNotBeVisible(resizer + 'bottom-right');
        });

        it('Получить код карты', function () {
            return this.browser
                .crInit('MANY_MAPS', cr.umForPreview)
                .crSaveMap()
                .then(openIframe)
                .crCheckValue(PO.popup.getCode.code(), cr.codeInteractiveIframe, 'содержимое кода iframe');
        });

        it('Сохраненные размеры карты', function () {
            return this.browser
                .crInit('MANY_MAPS', cr.umForPreview)
                .then(switchIframePreview)
                .crCheckValue(PO.sidebarExport.mapWidth(), '600', 'сохраненная ширина карты')
                .crCheckValue(PO.sidebarExport.mapHeight(), '550', 'сохраненная высота карты')
                .getCssProperty(PO.mapEditorPreviewHolder(), 'width').then((prop) => {
                    assert.equal(prop.value, '600px', 'ширина превью');
                })
                .getCssProperty(PO.mapEditorPreviewHolder(), 'height').then((prop) => {
                    assert.equal(prop.value, '550px', 'высота превью');
                });
        });

        it('Недопустимый слой – спутник', function () {
            return this.browser
                .crInit('MANY_MAPS', cr.umSatellite)
                .crSaveMap()
                .then(openIframe)
                .crWaitForVisible(PO.mapEditorPreviewHolder(), 'Не отображается превью на карте')
                .crWaitForVisible(PO.popup.getCode.iframeError(), 'Ошибка о недопустимом слое не появилась')
                .crCheckText(PO.popup.getCode.iframeError(), 'С помощью iframe можно вставить на сайт только ' +
                    'карту со слоем «Схема».', 'Неверный текст ошибки');
        });

        it('Недопустимый слой – гибрид', function () {
            return this.browser
                .crInit('MANY_MAPS', cr.umHybrid)
                .crSaveMap()
                .then(openIframe)
                .crWaitForVisible(PO.mapEditorPreviewHolder(), 'Не отображается превью на карте')
                .crWaitForVisible(PO.popup.getCode.iframeError(), 'Ошибка о недопустимом слое не появилась')
                .crCheckText(PO.popup.getCode.iframeError(), 'С помощью iframe можно вставить на сайт только ' +
                    'карту со слоем «Схема».', 'Неверный текст ошибки');
        });

        it('Недопустимый масштаб', function () {
            return this.browser
                .crInit('MANY_MAPS', cr.umMinZoom)
                .crSaveMap()
                .then(openIframe)
                .crShouldNotBeVisible(PO.mapEditorPreviewHolder())
                .crWaitForVisible(PO.popup.getCode.iframeErrorZoom(), 'Ошибка о недопустимом слое не появилась')
                .crShouldNotBeVisible(PO.popup.getCode.code())
                .crCheckText(PO.popup.getCode.iframeErrorZoom(), 'С помощью iframe нельзя отобразить на карте ' +
                    'некоторые страны. Чтобы получить код, увеличьте масштаб.');
        });

        it('Появление ошибки после редактирования карты', function () {
            return this.browser
                .crInit('MANY_MAPS')
                .crSaveMap()
                .then(openIframe)
                .crShouldNotBeVisible(PO.popup.getCode.iframeErrorZoom())
                .crShouldNotBeVisible(PO.popup.getCode.iframeError())
                .click(PO.sidebarExport.back())
                .crWaitForVisible(PO.stepEditor())
                .click(PO.ymaps.layers())
                .crWaitForVisible(PO.ymaps.layersList())
                .click(PO.ymapsLayerItem() + '=Гибрид')
                .crSaveMap()
                .crWaitForVisible(PO.mapEditorPreviewHolder(), 'Не отображается превью на карте')
                .crShouldBeVisible(PO.sidebarExport.getCodeBtn())
                .click(PO.sidebarExport.getCodeBtn())
                .crWaitForVisible(PO.popup.getCode(), 'Не открылся попап с кодом карты')
                .crShouldBeVisible(PO.popup.getCode.switcher.iframeChecked())
                .crWaitForVisible(PO.popup.getCode.iframeError(), 'Ошибка о недопустимом слое не появилась')
                .crCheckText(PO.popup.getCode.iframeError(), 'С помощью iframe можно вставить на сайт только ' +
                    'карту со слоем «Схема».', 'Неверный текст ошибки');
        });

        it('Сохранение выбранной вкладки при открытии другой карты', function () {
            return this.browser
                .crInit('MANY_MAPS', cr.umForPreview)
                .crSaveMap()
                .then(openIframe)
                .crCheckValue(PO.popup.getCode.code(), cr.codeInteractiveIframe, 'содержимое кода iframe')
                .click(PO.sidebarExport.getCodeBtn())
                .crWaitForHidden(PO.popup.getCode(), 'Не закрылся попап с кодом карты')
                .click(PO.mapListButton())
                .crWaitForVisible(PO.mapSelection(), 'Не открылся список карт')
                .crWaitForVisible(PO.mapSelection.itemFirst())
                .click(PO.mapSelection.itemFirst())
                .crWaitForVisible(PO.stepEditor(), 'Не открылся шаг редактирования карты')
                .crSaveMap()
                .crWaitForVisible(PO.mapEditorPreviewHolder(), 'Не отображается превью на карте')
                .crShouldBeVisible(PO.sidebarExport.getCodeBtn())
                .click(PO.sidebarExport.getCodeBtn())
                .crWaitForVisible(PO.popup.getCode(), 'Не открылся попап с кодом карты')
                .crCheckValue(PO.popup.getCode.code(), cr.codeInteractiveIframe, 'содержимое кода iframe')
                .crWaitForVisible(PO.mapEditorInteractiveIframe.map(), 'Карта-превью для iframe не отображается');
        });

        it('Закрывается попап после открытия новой карты', function () {
            return this.browser
                .crInit('MANY_MAPS')
                .crSaveMap()
                .then(openIframe)
                .click(PO.mapListButton())
                .crWaitForVisible(PO.mapSelection(), 'Не открылся список карт')
                .click(PO.mapSelection.create())
                .crWaitForVisible(PO.stepEditor(), 'Не открылся шаг редактирования карты')
                .crShouldNotBeVisible(PO.popup.getCode());
        });

        it('Сохраняется 100% ширина после переключения вкладки', function () {
            const resizer = PO.mapEditorInteractiveIframe.resizer();
            return this.browser
                .crInit('MANY_MAPS')
                .crSaveMap()
                .getAttribute(PO.sidebarExport.fitWidth.input(), 'checked').then((val) => {
                    assert.isNull(val, 'не чекнут');
                })
                .click(PO.sidebarExport.fitWidth())
                .getAttribute(PO.sidebarExport.fitWidth.input(), 'checked').then((val) => {
                    assert.strictEqual(val, 'true', 'чекнут');
                })
                .then(openIframe)
                .getAttribute(PO.sidebarExport.fitWidth.input(), 'checked').then((val) => {
                    assert.strictEqual(val, 'true', 'чекнут "Растянуть по ширине" iframe');
                })
                .crWaitForVisible(PO.mapEditorPreviewHolder())
                .getAttribute(PO.mapEditorPreviewHolder(), 'style').then((val) => {
                    const width = (/width: [0-9a-z%]+;/).exec(val);
                    assert.isArray(width, 'должно быть совпадение');
                    assert.strictEqual(width[0], 'width: 100%;', 'ширина превью');
                })
                .getCssProperty(PO.mapEditorPreviewHolder(), 'height').then((prop) => {
                    assert.equal(prop.value, '400px', 'высота превью');
                })
                .getValue(PO.popup.getCode.code()).then((val) => {
                    const width = (/width=\"[0-9a-z%]+\"/).exec(val);
                    const height = (/height=\"[0-9a-z%]+\"/).exec(val);
                    assert.isArray(width, 'должно быть совпадение');
                    assert.isArray(height, 'должно быть совпадение');
                    assert.strictEqual(width[0], 'width="100%"', 'ширина превью');
                    assert.strictEqual(height[0], 'height="400"', 'высота превью');
                })
                .crShouldNotBeVisible(resizer + 'top-left')
                .crShouldNotBeVisible(resizer + 'left')
                .crShouldNotBeVisible(resizer + 'bottom-left')
                .crShouldNotBeVisible(resizer + 'top-right')
                .crShouldNotBeVisible(resizer + 'right')
                .crShouldNotBeVisible(resizer + 'bottom-right');
        });
    });
});
