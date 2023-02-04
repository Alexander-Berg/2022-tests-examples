describe('smoke/ymapsml/placemark.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/ymapsml/placemark.html')
            // Дожидаемся видимости карты и полигона.
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Обычная метка', function () {
        return this.browser
            // Проверяем контент балуна.
            .pointerClick(250, 231)
            .waitForVisible(PO.map.balloon.content())
            .getText(PO.map.balloon.content()).then(function(text){
                text.should.equal('Патриаршие пруды\nОднажды весною, в час небывало жаркого заката, в Москве, на Патриарших прудах, появились два гражданина.');
            })
            .verifyNoErrors();
    });
});
