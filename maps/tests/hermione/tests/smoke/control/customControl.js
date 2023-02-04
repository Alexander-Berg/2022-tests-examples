describe('smoke/control/customControl.html', function () {

    beforeEach(function(){
        return this.browser
            .openUrl('smoke/control/customControl.html')
            // Дожидаемся карты и контрола
            .waitReady()
            .waitForVisible('div=Россия, Москва, улица Академика Королёва, 15к1');
    });

    it('Внешний вид кастомного контрола и проверяем наличие ответа', function () {
        return this.browser
            .csVerifyMapScreenshot(PO.mapId(), 'control')
            .verifyNoErrors();
    });

    it('Кастомный контрол должен отобразить адрес центра карты после даблкликзума', function () {
        return this.browser
            .pointerDblClick(398, 271)
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'after_dblClick')
            .verifyNoErrors();
    });

    it('Кастомный контрол должен отобразить адрес центра карты после драга', function () {
        return this.browser
            .csDrag([200, 200],[100, 100])
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'after_drag')
            .verifyNoErrors();
    });
});
