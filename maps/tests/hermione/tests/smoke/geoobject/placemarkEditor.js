describe('smoke/geoobject/placemarkEditor.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoobject/placemarkEditor.html')

            //Появилась карта.
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Проверяем редактор метки', function () {
        return this.browser
            //Проверяем то что метка переместилась.
            .pointerClick(245, 271)
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'map_with_placemark')

            //Кликаем по самой метке и проверяем что она переместилась
            .pointerClick(198, 242)
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'map_with_placemark_after_click')

            //Проверяем возможность драга метки
            .csDrag([198, 242], [350, 350])
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'map_with_placemark_after_drag')
            .verifyNoErrors()
    });
});
