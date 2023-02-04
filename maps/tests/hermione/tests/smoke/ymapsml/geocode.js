describe('smoke/ymapsml/geocode.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/ymapsml/geocode.html')
            // Дожидаемся видимости карты и метки.
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Проверяем геокодирование', function () {
        return this.browser
            .pointerClick(PO.map.placemark.placemark())
            .waitForVisible(PO.map.balloon.content())
            .getText(PO.map.balloon.content()).then(function(text){
                text.should.equal('Москва\nРоссия');
            })
            .verifyNoErrors();
    });
});
