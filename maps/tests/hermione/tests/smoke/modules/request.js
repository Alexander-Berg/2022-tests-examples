describe('smoke/modules/request.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/modules/request.html')
            .waitReady()
            .waitForVisible(PO.map.controls.button());
    });

    it('Загрузка модуля происходит перед первым добавлением метки', function () {
        return this.browser
            .pointerClick(PO.map.controls.button())
            .waitForVisible(PO.map.placemark.placemark())
            .verifyNoErrors();
    });
});
