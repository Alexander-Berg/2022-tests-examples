describe('smoke/objectManager/balloon.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/objectManager/balloon.html')
            .waitReady();
    });
    // TODO: Убрать после https://st.yandex-team.ru/MAPSAPI-13727
    it.skip('Открывается балун с выбранной меткой', function () {
        return this.browser
            .waitForVisible(PO.map.balloon.content())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon').catch(() => this.browser.csVerifyMapScreenshot(PO.mapId(), 'alt.balloon'))
            .verifyNoErrors();
    });

    it('Балун пропадает при зуме', function () {
        return this.browser
            .waitForVisible(PO.map.balloon.content())
            .pause(500)
            .click(PO.map.controls.zoom.minus())
            .moveToObject(PO.map.controls.zoom.minus())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'map')
            .verifyNoErrors();
    });

    it('Опция disableClickZoom по умолчанию выключена', function () {
        return this.browser
            .waitForVisible(PO.map.balloon.content())
            .pause(500)
            .click(PO.map.controls.zoom.minus())
            .pause(500)
            .click(PO.map.controls.zoom.minus())
            .pause(1000)
            .pointerClick(196, 309)
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'geoobjects')
            .verifyNoErrors();
    });
});
