describe('smoke/control/listboxLayout.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/control/listboxLayout.html', {tileMock: 'withParameters'})
            .waitReady()
            .waitForVisible('body #my-listbox-header');

    });
    it('Проверяем что мы переместились в Омск и в Москву при нажатии на элемент списка', function () {
        return this.browser
            .pointerClick('body #my-listbox-header', 10, 10)
            .pointerClick('a=Москва', 10, 10)
            .pointerMoveTo('a=Москва', 5, 5)
            .pause(100)
            .csVerifyMapScreenshot(PO.mapId(), 'listbox_hover')
            .verifyNoErrors();
    });
    it('Проверяем внешний вид открытого и закрытого кастомного листбокса', function () {
        return this.browser
            .pointerClick('body #my-listbox-header', 10, 10)
            .pointerClick('a=Омск', 10, 10)
            .pointerMoveTo('a=Омск', 5, 5)
            .pause(100)
            .csVerifyMapScreenshot(PO.mapId(), 'listbox_hover_another')
            .verifyNoErrors();
    });
});
