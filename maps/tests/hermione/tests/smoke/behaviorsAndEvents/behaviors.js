describe('smoke/behaviorsAndEvents/behaviors.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/behaviorsAndEvents/behaviors.html')
            .waitReady();
    });

    it('При клике срабатывает линейка и драг не срабатывает', function () {
        return this.browser
            .pointerClick(142, 127)
            .csDrag([42, 127], [362, 347])
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'ruler')
            .verifyNoErrors();
    });
});
