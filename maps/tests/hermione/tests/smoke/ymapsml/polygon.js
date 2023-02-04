describe('smoke/ymapsml/polygon.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/ymapsml/polygon.html')
            // Дожидаемся видимости карты и полигона.
            .waitReady()
            .waitForVisible(PO.map.pane.areas());
    });

    it('Проверяем многоугольник и линию', function () {
        return this.browser
            // Делаем скриншот.
            .csVerifyMapScreenshot(PO.mapId(), 'geoobjects')

            // Открываем балун и проверяем его содержание.
            .pointerClick(257, 213)
            .waitForVisible(PO.map.balloon.content())
            .getText(PO.map.balloon.content()).then(function(text){
                text.should.equal('Многоугольник\nВнешняя граница многоугольника представляет собой замкнутую ломаную линию');
            })
            .verifyNoErrors();
    });
});
