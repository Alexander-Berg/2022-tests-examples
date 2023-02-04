describe('smoke/objectManager/events.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/objectManager/events.html')
            .waitReady();
    });

    it('Метка меняет свой цвет по ховеру и возвращает при сведении', function () {
        return this.browser
            .waitForVisible(PO.map.placemark(), 20000)
            .pause(1000)
            .moveToObject('body', 149, 301)
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'clusterHover')
            .moveToObject('body', 351, 228)
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'anotherClusterHover')
            .verifyNoErrors();
    });
});
