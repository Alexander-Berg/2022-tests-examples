describe('smoke/control/smallMap.html', function () {

    beforeEach(function(){
        return this.browser
            .openUrl('smoke/control/smallMap.html')
            // Ждём карту.
            .waitReady();
    });

    it('Проверяем внешний вид "smallMapDefaultSet"', function () {
        return this.browser
            .waitAndClick(PO.map.controls.fullscreen())
            .csVerifyMapScreenshot(PO.map.pane.events(), 'smallMapDefaultSet')
            .verifyNoErrors();
    });
});
