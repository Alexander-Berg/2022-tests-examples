describe('smoke/geoobject/iconRollover.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoobject/iconRollover.html')

            //Появилась карта и метки на ней.
            .waitReady();
    });

    it('Проверяем разные метки', function () {
        return this.browser
            .waitForVisible(PO.map.placemark.placemark())

            //Включаем изменение пресета при наведении.
            .pointerClick(437, 50)
            //Наводимся на метку и снимаем скриншот
            .moveToObject(PO.map.placemark.placemark())
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'placemarks')

            //Проверяем что все метки превратились в blueSportIcon
            .pointerClick(460, 410)
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'placemarks_blue_sport_icon')

            //Проверяем что все метки превратились в glyphIcon
            .pointerClick(460, 440)
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'placemarks_glyph_icon')

            //Проверяем что все метки остались без пресета
            .pointerClick(460, 470)
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'placemarks_without_preset')
            .verifyNoErrors();
    });
});
