describe('smoke/modules/custom.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/modules/custom.html')
            .waitReady();
    });

    it('Добавим метку кнопкой и проверим появление её на карте и в нашем контроле', function () {
        return this.browser
            .csVerifyMapScreenshot(PO.mapId(), 'map')
            .waitAndClick(PO.map.controls.button())
            .waitForVisible(PO.map.placemark.placemark())
            .getText('.placemark_counter').then(function(text){
                text.should.equal('1');
            })
            .verifyNoErrors();
    });
});
