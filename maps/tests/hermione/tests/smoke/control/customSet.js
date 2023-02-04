describe('smoke/control/customSet.html', function () {

    beforeEach(function(){
        return this.browser
            .openUrl('smoke/control/customSet.html')
            // Дожидаемся карты
            .waitReady();
    });

    it('Проверяем внешний вид кастомного набора контролов', function () {
        return this.browser
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'control')
            .verifyNoErrors();
    });

});
