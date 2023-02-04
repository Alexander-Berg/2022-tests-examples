describe('smoke/control/button.html', function () {

    beforeEach(function(){
        return this.browser
            .openUrl('smoke/control/button.html')
            // Дожидаемся карты
            .waitReady();
    });
    it('Проверяем внешний вид кнопок', function () {
        return this.browser
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'button')
            .getText('body #logger').then(function (text) {
                text.should.equal('new1\nnew1\nnew1\nnew1\nSave\nEdit\nSubscribe\nCopy\nfalse\ntrue\nbutton1: optionschange\nbutton2: optionschange\nbutton3: optionschange\nbutton4: optionschange\nbutton1: parentchange\nbutton2: parentchange\nbutton3: parentchange\nbutton4: parentchange\nbutton1: mapchange\nbutton2: mapchange\nbutton3: mapchange\nbutton4: mapchange\noldMap is null\nnewMap is null\nbutton1: disable\nbutton2: disable\nbutton3: disable\nbutton4: disable\nbutton1: enable\nbutton2: enable\nbutton3: enable\nbutton4: enable\nbutton1: mapchange\nbutton2: mapchange\nbutton3: mapchange\nbutton4: mapchange\noldMap is null\nnewMap is null\nbutton1: parentchange\nbutton2: parentchange\nbutton3: parentchange\nbutton4: parentchange\nbutton1: optionschange\nbutton2: optionschange\nbutton3: optionschange\nbutton4: optionschange\nbutton1: optionschange\nbutton2: optionschange\nbutton3: optionschange\nbutton4: optionschange\nbutton1: optionschange\nbutton2: optionschange\nbutton3: optionschange\nbutton4: optionschange\nbutton1: parentchange\nbutton2: parentchange\nbutton3: parentchange\nbutton4: parentchange\nbutton1: mapchange\nbutton2: mapchange\nbutton3: mapchange\nbutton4: mapchange\noldMap is null\nnewMap is null\nbutton1: optionschange\nbutton2: optionschange\nbutton3: optionschange\nbutton4: optionschange\nbutton1: optionschange\nbutton2: optionschange\nbutton3: optionschange\nbutton4: optionschange\nbutton1: optionschange\nbutton2: optionschange\nbutton3: optionschange\nbutton4: optionschange\nbutton1: select\nbutton2: select\nbutton3: select\nbutton4: select\nbutton1: deselect\nbutton2: deselect\nbutton3: deselect\nbutton4: deselect');

            })
            .verifyNoErrors();
    });
});
