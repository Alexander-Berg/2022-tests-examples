describe('smoke/control/buttonLayout.html', function () {

    beforeEach(function(){
        return this.browser
            .openUrl('smoke/control/buttonLayout.html')
            // Дожидаемся карты
            .waitReady();
    });

    it('Проверяем внешний вид кастомной кнопки', function () {
        return this.browser
            .csVerifyMapScreenshot(PO.mapId(), 'button')
            .verifyNoErrors();
    });

    it('Проверяем внешний вид нажатой кастомной кнопки', function () {
        return this.browser
            .waitAndClick('.my-button')
            .csVerifyMapScreenshot(PO.mapId(), 'pressed_button')
            .verifyNoErrors();
    });
});
