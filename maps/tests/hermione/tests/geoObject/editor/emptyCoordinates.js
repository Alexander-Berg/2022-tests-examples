describe('geoobject/editor/emptyCoordinates.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('geoobject/editor/emptyCoordinates.html', false)
            .waitReady();
    });
    //TODO: вернуть проверку ошибок после MAPSAPI-14053
    it('На карту добавляется метка, линия, полигон и круг', function () {
        return this.browser
            .waitAndClick('ymaps=placemark')
            .pause(1000)
            .pointerClick(100,100)
            .waitForVisible(PO.map.placemark.placemark())

            .waitAndClick('ymaps=myPolyline')
            .pause(1000)
            .pointerClick(150,150)
            .pause(1000)
            .pointerClick(250,150)

            .waitAndClick('ymaps=myPolygon')
            .pause(1000)
            .pointerClick(200,200)
            .pause(1000)
            .pointerClick(250,200)
            .pause(1000)
            .pointerClick(250,250)

            .waitAndClick('ymaps=circle')
            .pause(1000)
            .pointerClick(300,300)
            .pause(1000)
            .pointerClick(350,350)
            .pause(1500)

            .csVerifyScreenshot(PO.mapId(), 'map')
            //.verifyNoErrors();
    });
});
