describe('balloon/input.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/input.html')
            .waitReady();
    });

    it('Текст в инпуте сохраняется при переходе в полноэкранный режим', function () {
        return this.browser
            .waitAndClick('input')
            .keys('Москва-psjyzn rjkjrjkf')
            .pointerClick(PO.map.controls.button())
            .pause(500)
            .csVerifyMapScreenshot(PO.map.balloon.content(), 'content')
            .verifyNoErrors();
    });
});