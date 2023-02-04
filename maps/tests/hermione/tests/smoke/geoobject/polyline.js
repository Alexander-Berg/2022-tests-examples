describe('smoke/geoobject/polyline.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoobject/polyline.html')

            .waitReady();
    });

    it('Проверяем линию на карте', function () {
        return this.browser
            //Проверяем балун первой линии
            .pointerClick(268, 226)
            .waitForVisible(PO.map.balloon.content())
            .getText(PO.map.balloon.content()).then(function(text){
                text.should.equal('Меня можно перетащить')
            })

            //Проверяем балун второй линии
            .pause(200)
            .pointerClick(327, 386)
            .waitForVisible(PO.map.balloon.content())
            .getText(PO.map.balloon.content()).then(function(text){
                text.should.equal('Ломаная линия')
            })

            //Делаем скриншот с открытым балуном и хинтом
            .moveToObject('body', 268, 226)
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'after_load')
            .verifyNoErrors();
    });

    it('Линия драгается', function () {
        return this.browser
            .csDrag([268, 226],[300, 300])
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'after_drag')
            .verifyNoErrors();
    });
});
