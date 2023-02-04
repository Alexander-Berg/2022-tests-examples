describe('smoke/geoobject/circleEditor.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoobject/circleEditor.html')
            .waitReady(PO.map.pane.editor());
    });

    it('Изменяется радиус за угловой пин', function () {
        return this.browser
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'before_drag')
            .csDrag([110, 141], [172, 193])
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'after_drag')
            .verifyNoErrors();
    });

    it('Изменяется радиус за боковой пин', function () {
        return this.browser
            .pause(500)
            .csDrag([109, 257], [166, 246])
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'after_drag_pin')
            .verifyNoErrors();
    });

    it('Круг перетаскивается за центральный пин', function () {
        return this.browser
            .pause(500)
            .csDrag([225, 256], [284, 214])
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'after_drag_center')
            .verifyNoErrors();
    });

    it('Круг перетаскивается за рамку', function () {
        return this.browser
            .pause(500)
            .csDrag([269, 286], [290, 185])
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'after_drag_frame')
            .verifyNoErrors();
    });

    it('Завершается редактирование через меню круга', function () {
        return this.browser
            .pause(500)
            .pointerClick(269, 286)
            .waitForVisible(PO.editorMenuItem())
            .moveToObject(PO.editorMenuItem())
            .csVerifyMapScreenshot(PO.mapId(), 'before_disable_editor')
            .pointerClick(274, 290)
            .waitForInvisible(PO.editorMenuItem())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'disable_editor')
            .verifyNoErrors();
    });

    it('Меню появляется при нажатии на центральную, боковую, угловую точки и на рамку, закрывается при повторном клике', function () {
        return this.browser
            //Центральная точка
            .pointerClick(225, 254)
            .waitForVisible(PO.editorMenuItem())
            .pointerClick(42, 345)
            .waitForInvisible(PO.editorMenuItem())

            //Боковая точка
            .pointerClick(109, 256)
            .waitForVisible(PO.editorMenuItem())
            .pointerClick(42, 345)
            .waitForInvisible(PO.editorMenuItem())

            //Угловая точка
            .pointerClick(112, 371)
            .waitForVisible(PO.editorMenuItem())
            .pointerClick(42, 345)
            .waitForInvisible(PO.editorMenuItem())

            //Точка внутри рамки
            .pointerClick(162, 297)
            .waitForVisible(PO.editorMenuItem())
            .pointerClick(42, 345)
            .waitForInvisible(PO.editorMenuItem())
            .verifyNoErrors();
    });
});
