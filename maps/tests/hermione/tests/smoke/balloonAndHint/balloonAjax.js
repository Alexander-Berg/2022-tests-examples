describe('smoke/balloonAndHint/balloonAjax.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/balloonAndHint/balloonAjax.html')
            .waitReady();
    });

    it('При наведении у метки появляется хинт', function () {
        return this.browser
            .waitForVisible(PO.map.placemark.placemark())
            .moveToObject(PO.map.placemark.placemark())
            .waitForVisible(PO.map.hint.text())
            .getText(PO.map.hint.text()).then(function(text){
                text.should.equal('Перетащите метку и кликните, чтобы узнать адрес')
            })
            .verifyNoErrors();
    });

    it('По клику на метку происходит геокодирование и показывается адрес', function () {
        return this.browser
            .waitAndPointerClick(PO.map.placemark.placemark())
            .waitUntilTextEql(PO.map.balloon.content(), 'Идет загрузка данных...')
            .waitUntilTextEql(PO.map.balloon.content(), 'улица Хромова, 24')
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });

    it('По клику на метку происходит геокодирование после драга метки', function () {
        return this.browser
            .waitForVisible(PO.map.placemark.placemark())
            .pause(1000)
            .csDrag([238, 334], [249, 226])
            .pause(1000)
            .waitAndPointerClick(PO.map.placemark.placemark())
            .waitUntilTextEql(PO.map.balloon.content(), 'Идет загрузка данных...')
            .waitUntilTextEql(PO.map.balloon.content(), 'Рыбинское водохранилище')
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'anotherBalloon')
            .verifyNoErrors();
    });
});
