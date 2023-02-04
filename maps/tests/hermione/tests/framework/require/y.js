describe('framework/require/y.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('framework/require/y.html', false)
            .waitReady(PO.map.controls.zoom.minus());
    });
    //TODO: версия тестинга и версия БЯК может не совпадать
    it.skip('Проверим загрузку тайлов', function () {
        return this.browser
            .pause(4000)
            .csVerifyScreenshot('body #YMapsID', 'map');
    });
});
