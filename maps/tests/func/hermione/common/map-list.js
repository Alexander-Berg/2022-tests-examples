const cr = require('../../credentials.js');
const HOST = require('../../.hermione.conf.js').baseUrl;
const WAIT_MAPLIST_TIMEOUT = 900;

require('../helper.js')(afterEach);

describe('Список карт', () => {
    afterEach(function () {
        return this.browser.crLogout();
    });

    it('Открытие через кнопку в шапке', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .crShouldBeEnabled(PO.mapListButton())
            .click(PO.mapListButton())
            .crWaitForVisible(PO.mapSelection(), 'Не открылся список карт')
            .crShouldBeVisible(PO.stepMapselection())
            .crShouldBeVisible(PO.mapSelectionActive())
            .click(PO.mapSelection.close());
    });

    it('Подгрузка', function () {
        return this.browser
            .crInit('MANY_MAPS', '', cr.mapState.list)
            .crShouldBeVisible(PO.mapSelectionActive())
            .elements(PO.mapSelection.item()).then((el) =>
                assert.lengthOf(el.value, 25, 'в списке должно быть 25 карт')
            )
            .crScroll(PO.mapSelection.item25())
            .crWaitForVisible(PO.mapSelection.item50(), 'Не загрузилась 50 карта в списке')
            .elements(PO.mapSelection.item()).then((el) =>
                assert.lengthOf(el.value, 50, 'в списке должно быть 50 карт')
            )
            .crScroll(PO.mapSelection.item50())
            .crWaitForVisible(PO.mapSelection.item75(), 'Не загрузилась 75 карта в списке')
            .elements(PO.mapSelection.item()).then((el) =>
                assert.lengthOf(el.value, 75, 'в списке должно быть 75 карт')
            )
            .click(PO.mapSelection.close());
    });

    it('Создание копии карты', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.umForCopy)
            .crCheckValue(PO.sidebar.mapName(), 'Карта для копии', 'название карты "Карта для копии"')
            .crCheckValue(PO.sidebar.mapDesc(), 'Описание карты', 'описание карты "Описание карты"')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 3, 'в списке объектов должно быть 3 элемента')
            )
            .crSaveMap()
            .pause(WAIT_MAPLIST_TIMEOUT)
            .click(PO.mapListButton())
            .crWaitForVisible(PO.mapSelection(), 'Не открылся список карт')
            .crWaitForVisible(PO.mapSelection.itemFirstName())
            .crCheckText(PO.mapSelection.itemFirstName(), 'Карта для копии', 'название карты в списке ' +
                '"Карта для копии" 1')
            .crCheckText(PO.mapSelection.itemFirstDesc(), 'Описание карты', 'описание карты ' +
                '"Описание карты"')
            .crShouldNotBeVisible(PO.mapSelectionMenu())
            .click(PO.mapSelection.itemFirstMenuBtn())
            .crShouldBeVisible(PO.mapSelectionMenu())
            .click(PO.mapSelectionMenu.menuCopy())
            .crShouldNotBeVisible(PO.mapSelectionMenu())
            .crWaitForVisible(PO.mapSelection.itemFirst(), 'Не появилась первая карта в списке')
            .crCheckText(PO.mapSelection.itemFirstName(), '(Копия) Карта для копии', 'название карты в списке ' +
                '"(Копия) Карта для копии"')
            .crCheckText(PO.mapSelection.itemFirstDesc(), 'Описание карты', 'описание карты "Описание карты"')
            .click(PO.mapSelection.itemFirst())
            .crWaitForVisible(PO.stepEditor(), 'Не открылся шаг редактирования карты')
            .crWaitForVisible(PO.sidebar.mapName(), 'Не появился инпут названия карты')
            .crCheckValue(PO.sidebar.mapName(), '(Копия) Карта для копии', 'название карты "(Копия) Карта для копии"')
            .crCheckValue(PO.sidebar.mapDesc(), 'Описание карты', 'описание карты "Описание карты"')
            .elements(PO.geoObjectList.item()).then((el) =>
                assert.lengthOf(el.value, 3, 'в списке объектов должно быть 3 элемента')
            )
            .crShouldBeVisible(PO.geoObjectList.itemPointNumber())
            .crCheckText(PO.geoObjectList.itemPointNumberTitle(), 'Метка', 'описание метки')
            .crShouldBeVisible(PO.geoObjectList.itemLinestring())
            .crCheckText(PO.geoObjectList.itemLinestringTitle(), 'Линия', 'описание линии')
            .crShouldBeVisible(PO.geoObjectList.itemPolygon())
            .crCheckText(PO.geoObjectList.itemPolygonTitle(), 'Полигон', 'описание полигона')
            .getCssProperty(PO.geoObjectList.itemLinestringIcon(), 'border-color').then((prop) => {
                if (prop.value !== 'rgba(27,173,3,0.9)') {
                    assert.equal(prop.value, 'rgba(27,173,3,0.901961)', 'темно-зеленая линия');
                }
            })
            .getCssProperty(PO.geoObjectList.itemPolygonIcon(), 'border-color').then((prop) => {
                assert.equal(prop.value, 'rgba(255,210,30,0.6)', 'фиолетовый контур полигона');
            })
            .getCssProperty(PO.geoObjectList.itemPolygonIcon(), 'outline-color').then((prop) => {
                if (prop.value !== 'rgba(181,30,255,0.9)') {
                    assert.equal(prop.value, 'rgba(181,30,255,0.901961)', 'желтая заливка полигона');
                }
            })
            .click(PO.mapListButton())
            .crWaitForVisible(PO.mapSelection(), 'Не открылся список карт')
            .crCheckText(PO.mapSelection.itemFirstName(), '(Копия) Карта для копии', 'название карты в списке ' +
                '"(Копия) Карта для копии"')
            .crDeleteFirstMap()
            .crCheckText(PO.mapSelection.itemFirstName(), 'Карта для копии', 'название карты в списке ' +
                '"Карта для копии" 2')
            .click(PO.mapSelection.close());
    });

    it('Удаление карты', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .crSetValue(PO.sidebar.mapName(), 'Карта для удаления')
            .crSetValue(PO.sidebar.mapDesc(), 'Описание карты для удаления')
            .crSaveMap()
            .click(PO.mapListButton())
            .crWaitForVisible(PO.mapSelection(), 'Не открылся список карт')
            .crCheckText(PO.mapSelection.itemFirstName(), 'Карта для удаления', 'название карты в списке ' +
                '"Карта для удаления"')
            .crCheckText(PO.mapSelection.itemFirstDesc(), 'Описание карты для удаления', 'описание карты ' +
                '"Описание карты для удаления"')
            .crDeleteFirstMap()
            .crShouldNotBeVisible(PO.mapSelectionMenu())
            .getText(PO.mapSelection.itemFirstName()).then((text) =>
                assert.notEqual(text, 'Карта для удаления', 'название карты в списке НЕ должно быть ' +
                    '"Карта для удаления"')
            )
            .getText(PO.mapSelection.itemFirstDesc()).then((text) =>
                assert.notEqual(text, 'Описание карты для удаления', 'описание карты НЕ должно быть ' +
                    '"Описание карты для удаления"')
            )
            .refresh()
            .crWaitForVisible(PO.mapSelection.itemFirst(), 'Не появилась первая карта в списке')
            .getText(PO.mapSelection.itemFirstName()).then((text) =>
                assert.notEqual(text, 'Карта для удаления', 'название карты в списке НЕ должно быть ' +
                    '"Карта для удаления"')

            )
            .getText(PO.mapSelection.itemFirstDesc()).then((text) =>
                assert.notEqual(text, 'Описание карты для удаления', 'описание карты НЕ должно быть ' +
                    '"Описание карты для удаления"')

            )
            .click(PO.mapSelection.close());
    });

    it('Поделиться картой', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.umForShare)
            .crWaitForVisible(PO.sidebar.mapName(), 'Не появился инпут названия карты')
            .crCheckValue(PO.sidebar.mapName(), 'Карта для копирования ссылки', 'название карты  ' +
                '"Карта для копирования ссылки"')
            .crSaveMap()
            .pause(WAIT_MAPLIST_TIMEOUT)
            .click(PO.mapListButton())
            .crWaitForVisible(PO.mapSelection(), 'Не открылся список карт')
            .crWaitForVisible(PO.mapSelection.itemFirstName())
            .crCheckText(PO.mapSelection.itemFirstName(), 'Карта для копирования ссылки', 'название карты ' +
                '"Карта для копирования ссылки"')
            .getAttribute(PO.mapSelection.itemFirst(), 'data-index').then((val) =>
                assert.strictEqual(val, cr.sidForShare, 'должен быть sid карты в атрибутах')
            )
            .click(PO.mapSelection.itemFirstMenuBtn())
            .crShouldBeVisible(PO.mapSelectionMenu())
            .crShouldNotBeVisible(PO.mapSelectionMenu.share())
            .click(PO.mapSelectionMenu.menuShare())
            .crShouldBeVisible(PO.mapSelectionMenu.share())
            .getValue(PO.mapSelectionMenu.shareInput()).then((url) => this.browser
                .crCheckURL(url, HOST + '/maps' + cr.umForShare + '&source=constructorLink', 'ссылка на бяк')
            )
            .getAttribute(PO.mapSelectionMenu.shareInput(), 'readonly').then((val) =>
                assert.strictEqual(val, 'true', 'должен атрибут readonly')
            )
            .crShouldBeVisible(PO.mapSelectionMenu.shareBtn.copyCaption())
            .crShouldNotBeVisible(PO.mapSelectionMenu.shareBtn.copiedCaption())
            .click(PO.mapSelectionMenu.shareBtn())
            .crShouldNotBeVisible(PO.mapSelectionMenu.shareBtn.copyCaption())
            .crShouldBeVisible(PO.mapSelectionMenu.shareBtn.copiedCaption())
            .click(PO.mapSelection.itemFirstMenuBtn())
            .crShouldNotBeVisible(PO.mapSelectionMenu())
            .crShouldNotBeVisible(PO.mapSelectionMenu.menuShare())
            .click(PO.mapSelection.close());
    });

    it('Отображение карты в списке "Из моих карт"', function () {
        return this.browser
            .crInit('WITH_MY_MAPS', '', cr.mapState.list)
            .crVerifyScreenshot(PO.mapSelection(), 'map-list-with-my-maps')
            .click(PO.mapSelection.close());
    });

    it('Скролл с открытым меню карты', function () {
        return this.browser
            .crInit('MANY_MAPS', '', cr.mapState.list)
            .click(PO.mapSelection.item3MenuBtn())
            .crShouldBeVisible(PO.mapSelectionMenu())
            .crScroll(PO.mapSelection.item3())
            .crWaitForHidden(PO.mapSelectionMenu(), 150, 'Меню карты не закрылось')
            .click(PO.mapSelection.close());
    });

    it('Создание карты', function () {
        const currentDateTime = {};
        return this.browser
            .crInit('MANY_MAPS')
            .crWaitForVisible(PO.stepEditor(), 'Не открылся шаг редактирования карты')
            .crSetValue(PO.sidebar.mapName(), 'Название карты')
            .crSetValue(PO.sidebar.mapDesc(), 'Описание карты')
            .crSaveMap()
            .then(() => {
                const date = new Date();
                currentDateTime.date = ('0' + date.getDate()).slice(-2) + '.' + ('0' + (date.getMonth() + 1))
                    .slice(-2) + '.' + (date.getFullYear()).toString().slice(-2);
                currentDateTime.time = ('0' + date.getHours()).slice(-2) + ':' + ('0' + date.getMinutes()).slice(-2);

                return currentDateTime;
            })
            .click(PO.mapListButton())
            .crWaitForVisible(PO.mapSelection(), 'Не открылся список карт')
            .crCheckText(PO.mapSelection.itemFirstName(), 'Название карты', 'название карты в списке "Название карты"')
            .crCheckText(PO.mapSelection.itemFirstDesc(), 'Описание карты', 'описание карты "Описание карты"')
            .getText(PO.mapSelection.itemFirstUpdated()).then((text) =>
                assert.strictEqual(text, currentDateTime.date + ' ' + currentDateTime.time, 'время и дата сохранения ' +
                    'должны быть текущими')

            )
            .crDeleteFirstMap()
            .click(PO.mapSelection.close());
    });

    it('Закрытие по клику на крестик', function () {
        return this.browser
            .crInit('MANY_MAPS', '', cr.mapState.list)
            .crShouldBeVisible(PO.mapSelection())
            .click(PO.mapSelection.close())
            .crShouldNotBeVisible(PO.mapSelection())
            .crShouldBeVisible(PO.stepEditor());
    });

    it('Закрытие по клику вне окна', function () {
        return this.browser
            .crInit('MANY_MAPS', '', cr.mapState.list)
            .crShouldBeVisible(PO.mapSelection())
            .click(PO.modalCell())
            .crShouldNotBeVisible(PO.mapSelection())
            .crShouldBeVisible(PO.stepEditor());
    });

    it('Удаление единственной карты', function () {
        return this.browser
            .crInit('FOR_DELETE_SINGLE_MAP')
            .catch(() => {
                this.browser
                    .isVisible(PO.mapSelection.itemFirst())
                    .crDeleteFirstMap(true);
            })
            .crShouldNotBeEnabled(PO.mapListButton())
            .crShouldNotBeVisible(PO.mapSelection())
            .click(PO.mapListButton())
            .crShouldNotBeVisible(PO.mapSelection())
            .crSaveMap()
            .crShouldBeEnabled(PO.mapListButton())
            .click(PO.mapListButton())
            .crWaitForVisible(PO.mapSelection(), 'Не открылся список карт')
            .elements(PO.mapSelection.item()).then((el) =>
                assert.lengthOf(el.value, 1, 'в списке должна быть 1 карта')
            )
            .crDeleteFirstMap(true)
            .crWaitForVisible(PO.stepEditor(), 'Не открылся шаг редактирования карты')
            .crShouldNotBeEnabled(PO.mapListButton())
            .crShouldNotBeVisible(PO.mapSelection());
    });

    it('Сохранение карты с пустым заголовком и описанием', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .crCheckValue(PO.sidebar.mapName(), '', 'название карты должно быть пустым')
            .crCheckValue(PO.sidebar.mapDesc(), '', 'описание карты должно быть пустым')
            .crSaveMap()
            .click(PO.mapListButton())
            .crWaitForVisible(PO.mapSelection(), 'Не открылся список карт')
            .crCheckText(PO.mapSelection.itemFirstName(), 'Без названия', 'название карты в списке "Без названия"')
            .crCheckText(PO.mapSelection.itemFirstDesc(), '', 'не должно быть описания карты')
            .crDeleteFirstMap()
            .click(PO.mapSelection.close());
    });

    it('Редактирование заголовка и описания карты', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .crSetValue(PO.sidebar.mapName(), 'ДО')
            .crSetValue(PO.sidebar.mapDesc(), 'ДО')
            .crSaveMap()
            .click(PO.mapListButton())
            .crWaitForVisible(PO.mapSelection(), 'Не открылся список карт')
            .crCheckText(PO.mapSelection.itemFirstName(), 'ДО', 'название карты в списке "ДО"')
            .crCheckText(PO.mapSelection.itemFirstDesc(), 'ДО', 'описание карты в списке "ДО"')
            .click(PO.mapSelection.itemFirst())
            .crWaitForVisible(PO.stepEditor(), 'Не открылся шаг редактирования карты')
            .crWaitForVisible(PO.sidebar.mapName(), 'Не появился инпут названия карты')
            .crCheckValue(PO.sidebar.mapName(), 'ДО', 'название карты "ДО"')
            .crCheckValue(PO.sidebar.mapDesc(), 'ДО', 'описание карты "ДО"')
            .crSetValue(PO.sidebar.mapName(), 'ПОСЛЕ')
            .crSetValue(PO.sidebar.mapDesc(), 'ПОСЛЕ')
            .crSaveMap()
            .click(PO.mapListButton())
            .crWaitForVisible(PO.mapSelection(), 'Не открылся список карт')
            .crCheckText(PO.mapSelection.itemFirstName(), 'ПОСЛЕ', 'название карты в списке "ПОСЛЕ"')
            .crCheckText(PO.mapSelection.itemFirstDesc(), 'ПОСЛЕ', 'описание карты в списке "ПОСЛЕ"')
            .crDeleteFirstMap()
            .click(PO.mapSelection.close());
    });

    it('Вернуться в редактирование карты', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .crSetValue(PO.sidebar.mapName(), 'Вернуться в редактирование карты')
            .crSaveMap()
            .click(PO.sidebarExport.back())
            .crWaitForVisible(PO.stepEditor(), 'Не открылся шаг редактирования карты')
            .crCheckValue(PO.sidebar.mapName(), 'Вернуться в редактирование карты', 'название карты ' +
                '"Вернуться в редактирование карты"')
            .click(PO.mapListButton())
            .crWaitForVisible(PO.mapSelection(), 'Не открылся список карт')
            .crCheckText(PO.mapSelection.itemFirstName(), 'Вернуться в редактирование карты', 'название карты ' +
                'в списке "Вернуться в редактирование карты"')
            .crDeleteFirstMap()
            .click(PO.mapSelection.close());
    });
});
