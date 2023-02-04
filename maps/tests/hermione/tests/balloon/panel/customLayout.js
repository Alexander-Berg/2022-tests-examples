describe('balloon/panel/customLayout.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/panel/customLayout.html', {tileMock: 'blueWithParameters'})
            .waitReady()
            .waitForVisible(PO.map.balloon.closeButton());
    });

    it('Проверяем внешний вид панели с кастомным лэйаутом', function () {
        return this.browser
            .pointerClick('ymaps=panel/balloon')
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'customPanel')
            .verifyNoErrors();
    });

    it('Кастомная панель не ломается при драге и зуме, панель переходит в балун и обратно и закрывается', function () {
        return this.browser
            .pointerClick('ymaps=panel/balloon')
            .pause(1000)
            .csDrag([100,100], [200,200])
            .pause(1000)
            .pointerDblClick(100,100)
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'anotherCustomPanel')
            .verifyNoErrors();
    });

    it('Панель переходит в балун и обратно и закрывается', function () {
        return this.browser
            .pointerClick('ymaps=panel/balloon')
            .pause(500)
            .pointerClick('ymaps=panel/balloon')
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .pointerClick('ymaps=panel/balloon')
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'anotherCustomPanel2')
            .pointerClick('body #close')
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'withoutCustomPanel')
            .verifyNoErrors();
    });
});