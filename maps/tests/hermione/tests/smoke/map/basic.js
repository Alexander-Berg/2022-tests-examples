describe('smoke/map/basic.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/map/basic.html')
            .waitReady();
    });

    it('Карта должна пропасть при нажатии кнопки удалить', function () {
        return this.browser
            .csVerifyMapScreenshot(PO.mapId(), 'before_destroy')
            .waitAndClick('body #destroyButton')
            .csVerifyMapScreenshot(PO.mapId(), 'after_destroy')
            .verifyNoErrors();
    });
});
