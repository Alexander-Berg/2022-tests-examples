describe('balloon/panel/panelLayout.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/panel/panelLayout.html')
            .waitReady(PO.map.placemark.placemark());
    });

    it('Кастомный лэйаут контента балуна-панели', function () {
        return this.browser
            .pointerClick(PO.map.placemark.placemark())
            .pointerClick('ymaps=panelMode')
            .pause(500)
            .pointerClick('ymaps=panelMode')
            .waitAndClick(PO.counterButton())
            .waitAndClick(PO.counterButton())
            .waitAndClick(PO.counterButton())
            .waitAndClick(PO.counterButton())
            .waitAndClick(PO.counterButton())
            .csCheckText('body #logger', 'custom\n' +
                'custom\n' +
                'custom\n' +
                'custom\n' +
                'Done! All over again.\n' +
                'custom')
            .verifyNoErrors();
    });
});