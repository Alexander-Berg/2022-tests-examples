describe('smoke/geoobject/placemarkShape.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoobject/placemarkShape.html')

            //Появилась карта.
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Проверяем внешний вид меток с кастомными изображениями', function () {
        return this.browser
            //Проверяем внешний вид меток
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'map_with_placemarks')
            .verifyNoErrors();
    });

    it('Проверяем хотспоты кастомных меток', function () {
        return this.browser
            //Проверяем что при наведении на углы меток открывается хинт, а при сведении пропадает
            .moveToObject('body', 227, 293)
            .waitForVisible(PO.map.hint.text())
            .getText(PO.map.hint.text()).then(function(text){
                text.should.equal('HTML метка сложной формы')
            })
            .moveToObject('body', 244, 295)
            .waitForInvisible(PO.map.hint.text())

            .moveToObject('body', 254, 283)
            .waitForVisible(PO.map.hint.text())
            .moveToObject('body', 262, 259)
            .waitForInvisible(PO.map.hint.text())

            .moveToObject('body', 254, 229)
            .waitForVisible(PO.map.hint.text())
            .moveToObject('body', 226, 226)
            .waitForInvisible(PO.map.hint.text())

            .moveToObject('body', 201, 231)
            .waitForVisible(PO.map.hint.text())
            .moveToObject('body', 196, 261)
            .waitForInvisible(PO.map.hint.text())

            .moveToObject('body', 201, 282)
            .waitForVisible(PO.map.hint.text())
            .moveToObject('body', 215, 290)
            .waitForInvisible(PO.map.hint.text())

            //Проверяем что при наведении на углы меток открывается хинт, а при сведении пропадает
            .moveToObject('body', 341, 248)
            .waitForVisible(PO.map.hint.text())
            .getText(PO.map.hint.text()).then(function(text){
                text.should.equal('Метка с прямоугольным HTML макетом')
            })
            .moveToObject('body', 347, 226)
            .waitForInvisible(PO.map.hint.text())

            .moveToObject('body', 341, 204)
            .waitForVisible(PO.map.hint.text())
            .moveToObject('body', 317, 195)
            .waitForInvisible(PO.map.hint.text())

            .moveToObject('body', 299, 204)
            .waitForVisible(PO.map.hint.text())
            .moveToObject('body', 289, 225)
            .waitForInvisible(PO.map.hint.text())

            .moveToObject('body', 300, 246)
            .waitForVisible(PO.map.hint.text())

            //Проверяем что при наведении на углы меток открывается хинт, а при сведении пропадает
            .moveToObject('body', 264, 173)
            .waitForVisible(PO.map.hint.text())
            .getText(PO.map.hint.text()).then(function(text){
                text.should.equal('Метка с круглым HTML макетом')
            })
            .moveToObject('body', 284, 171)
            .waitForInvisible(PO.map.hint.text())

            .moveToObject('body', 286, 153)
            .waitForVisible(PO.map.hint.text())
            .moveToObject('body', 283, 134)
            .waitForInvisible(PO.map.hint.text())

            .moveToObject('body', 262, 125)
            .waitForVisible(PO.map.hint.text())
            .moveToObject('body', 245, 126)
            .waitForInvisible(PO.map.hint.text())

            .moveToObject('body', 240, 147)
            .waitForVisible(PO.map.hint.text())
            .moveToObject('body', 246, 173)
            .waitForInvisible(PO.map.hint.text())

            .verifyNoErrors();
    });

    it('Проверяем что вместо драга метки драгается карта', function () {
        return this.browser
            //Проверяем возможность драга метки(метка не драгается)
            .csDrag([226, 255],[324, 361])
            .pointerMoveTo(100,100)
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'map_with_placemarks_after_drag')
            .verifyNoErrors();
    });
});
