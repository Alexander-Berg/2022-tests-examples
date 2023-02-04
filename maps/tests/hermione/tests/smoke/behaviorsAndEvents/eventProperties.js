describe('smoke/behaviorsAndEvents/eventProperties.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/behaviorsAndEvents/eventProperties.html')
            .waitReady();
    });

    it('По левому клику открывается балун', function () {
        return this.browser
            .pointerClick(256, 256)
            .waitForVisible(PO.map.balloon.content())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });

    it('Повторный клик закрывает балун', function () {
        return this.browser
            .pointerClick(256, 256)
            .waitForVisible(PO.map.balloon.content())
            .pointerClick(400, 400)
            .waitForInvisible(PO.map.balloon.content())
            .verifyNoErrors();
    });

    it('По правому клику открывается хинт', function () {
        return this.browser
            .pointerRightClick(256, 256)
            .pause(1500)
            .csVerifyMapScreenshot(PO.mapId(), 'hint')
            .verifyNoErrors();
    });

    hermione.skip.in('yandex');
    it('Повторный клик закрывает хинт', function () {
        return this.browser
            .pointerRightClick(256, 256)
            .waitForVisible(PO.map.hint.text())
            .pointerClick(250, 250)
            .waitForInvisible(PO.map.hint.text())
            .verifyNoErrors();
    });
});
