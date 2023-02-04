describe('smoke/geoQuery/closestObject.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoQuery/closestObject.html')

            .waitReady();
    });

    it('Проверяем что открылся балун, нашелся близжайший результат к станции метро', function () {
        return this.browser
            //Проверяем что открылся балун, нашелся близжайший результат к станции метро
            .waitForVisible(PO.map.balloon.content())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });

    it('Проверяем что открылся балун, нашелся близжайший результат к месту клика', function () {
        return this.browser
            //Проверяем что открылся балун, нашелся близжайший результат к месту клика
            .waitForVisible(PO.map.balloon.content())
            .pause(1000)
            .pointerClick(13, 469)
            .waitForVisible(PO.map.balloon.content())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'another_balloon')
            .verifyNoErrors();
    });

    it('Проверяем что открылся балун, нашелся близжайший результат к месту клика и он не отличается от первоначально открытого', function () {
        return this.browser
            //Проверяем что открылся балун, нашелся близжайший результат к месту клика и он не отличается от первоначально открытого
            .waitForVisible(PO.map.balloon.content())
            .pause(1000)
            .pointerClick(34, 371)
            .pause(100)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });
});
