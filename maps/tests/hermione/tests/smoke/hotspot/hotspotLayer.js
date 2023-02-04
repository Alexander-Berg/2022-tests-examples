describe.skip('smoke/hotspot/hotspotLayer.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/hotspot/hotspotLayer.html')
            .waitReady();
    });

    it('При наведении появляется хинт и при клике балун', function () {
        return this.browser
            .pointerClick(309, 240)
            .waitForVisible(PO.map.balloon.closeButton())
            .moveToObject('body', 300, 250)
            .waitForVisible(PO.map.hint.text())
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'balloonWithHint');
    });

    it('Проверяем границы хотспота с помощью хинта', function () {
        return this.browser
            .moveToObject(PO.mapId(), 310, 210)
            .waitForVisible(PO.map.hint.text())
            .moveToObject(PO.mapId(), 286, 199)
            .waitForInvisible(PO.map.hint.text())
            .moveToObject(PO.mapId(), 285, 213)
            .waitForVisible(PO.map.hint.text())
            .moveToObject(PO.mapId(), 263, 223)
            .waitForInvisible(PO.map.hint.text());
    });
});
