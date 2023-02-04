describe('balloon/panel/shape.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/panel/shape.html')
            .waitReady(PO.map.placemark.placemark());
    });

    it('Проверим вывод методов', function () {
        return this.browser
            .pointerClick(PO.map.placemark.placemark())
            .pause(1000)
            .csCheckText('body #logger', 'balloon.getOverlay():\n[object Object]\nOK\ngetShape():\n[object Object]\nOK\ngetShape().getType():\nRectangle\nOK\nmap.balloon.getOverlay():\n[object Object]\nOK\ngetShape():\n[object Object]\nOK\ngetShape().getType():\nRectangle\nOK\nplacemark.balloon.getOverlay():\n[object Object]\nOK\ngetShape():\n[object Object]\nOK\ngetShape().getType():\nRectangle\nOK')
            .verifyNoErrors();
    });

});