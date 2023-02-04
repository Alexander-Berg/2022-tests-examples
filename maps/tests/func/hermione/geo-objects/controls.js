require('../helper.js')(afterEach);

describe('API / Controls', () => {
    beforeEach(function () {
        return this.browser
            .crInit('MANY_MAPS');
    });

    afterEach(function () {
        return this.browser
            .crSaveMap()
            .crLogout();
    });

    it('Создание метки через поиск', function () {
        return this.browser
            .crWaitForVisible(PO.ymaps.searchBoxInput())
            .crSetValue(PO.ymaps.searchBoxInput(), 'Москва')
            .click(PO.ymaps.searchBoxButton())
            .crWaitForVisible(PO.ymaps.searchSerpFirst())
            .click(PO.ymaps.searchSerpFirst())
            .crWaitForVisible(PO.balloon())
            .crCheckValue(PO.balloon.text(), 'Россия, Москва', 'Текст внутри балуна')
            .crShouldBeVisible(PO.geoObjectList.itemPointNumber())
            .crCheckText(PO.geoObjectList.itemPointNumberTitle(), 'Россия, Москва', 'Описание метки в списке');
    });

    it('Поиск по координатам', function () {
        return this.browser
            .crWaitForVisible(PO.ymaps.searchBoxInput())
            .crSetValue(PO.ymaps.searchBoxInput(), '61.724045, 35.475322')
            .click(PO.ymaps.searchBoxButton())
            .crWaitForVisible(PO.ymaps.searchSerp())
            .click(PO.ymaps.searchSerpFirst())
            .crWaitForVisible(PO.balloon())
            .crCheckValue(PO.balloon.text(), 'Россия, Онежское озеро', 'Текст внутри балуна')
            .crShouldBeVisible(PO.geoObjectList.itemPointNumber())
            .crCheckText(PO.geoObjectList.itemPointNumberTitle(), 'Россия, Онежское озеро', 'Описание метки в списке');
    });

    it('ППО', function () {
        return this.browser
            .crWaitForVisible(PO.ymaps.searchBoxInput())
            .crSetValue(PO.ymaps.searchBoxInput(), 'третьяковская галерея')
            .click(PO.ymaps.searchBoxButton())
            .crWaitForVisible(PO.ymaps.searchSerp())
            .click(PO.ymaps.searchSerpFirst())
            .crWaitForVisible(PO.balloon())
            .crCheckValue(PO.balloon.text(), 'Музей; </br>\nЛаврушинский пер., 10, Москва, Россия', 'Текст внутри балуна')
            .crCheckValue(PO.balloon.captionInput(), 'Государственная Третьяковская галерея')
            .crShouldBeVisible(PO.ymaps.placemark.iconWithCaption.caption())
            .crCheckText(PO.ymaps.placemark.iconWithCaption.caption(), 'Государственная Третьяковская галерея', 'подпись метки');
    });
});
