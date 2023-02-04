describe('balloon/methods.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/methods.html')
            .waitReady();
    });

    it('Проверим события и setData', function () {
        return this.browser
            .pointerClick(PO.map.controls.button())
            .pointerClick(300, 300)
            .waitForVisible(PO.map.balloon.closeButton())
            .csCheckText(PO.map.balloon.content(), 'Balloon')
            .pointerClick(100, 100)
            .waitForVisible(PO.map.balloon.closeButton())
            .csCheckText(PO.map.balloon.content(), 'New text in balloon')
            .pointerClick(PO.map.balloon.closeButton())
            .csCheckText('body #logger', 'Waiting for events...\n' +
                'balloon: open\n' +
                'balloon: userclose\n' +
                'balloon: close')
            .verifyNoErrors();
    });
});