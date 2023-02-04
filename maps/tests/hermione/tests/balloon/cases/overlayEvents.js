describe('balloon/overlayEvents.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/overlayEvents.html')
            .waitReady(PO.map.balloon.closeButton());
    });

    it('Проверим события: Наведение-Сведение', function () {
        return this.browser
            .moveToObject(PO.map.balloon.closeButton())
            .moveToObject(PO.map.controls.button())
            .csCheckText('body #logger', 'Listening for events on balloon overlay...\n' +
                'mouseenter\n' +
                'mouseleave')
            .verifyNoErrors();
    });

    it('Проверим события: Дабл-клик', function () {
        return this.browser
            .moveToObject(PO.map.balloon.layout())
            .pointerDblClick(PO.map.balloon.layout())
            .moveToObject(PO.map.controls.button())
            .csCheckText('body #logger', 'Listening for events on balloon overlay...\n' +
                'mouseenter\n' +
                'mousedown\n' +
                'mouseup\n' +
                'click\n' +
                'mousedown\n' +
                'mouseup\n' +
                'click\n' +
                'dblclick\n' +
                'mouseleave')
            .verifyNoErrors();
    });
});