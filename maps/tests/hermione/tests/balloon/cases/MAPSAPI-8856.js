describe('balloon/MAPSAPI-8856.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/MAPSAPI-8856.html', {tileMock: 'blueWithParameters'})
            .waitReady()
            .pause(1000);
    });

    it('Срабатывает автопан balloon', function () {
        return this.browser
            .pointerClick('ymaps=Balloon')
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });

    it('Срабатывает автопан map', function () {
        return this.browser
            .pointerClick('ymaps=Map')
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'balloonMap')
            .verifyNoErrors();
    });

    it('Срабатывает автопан geoObject', function () {
        return this.browser
            .pointerClick('ymaps=geoObject')
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'balloonGeoObject')
            .verifyNoErrors();
    });

    it('Срабатывает автопан placemark', function () {
        return this.browser
            .pointerClick('ymaps=placemark')
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'balloonPlacemark')
            .verifyNoErrors();
    });
});