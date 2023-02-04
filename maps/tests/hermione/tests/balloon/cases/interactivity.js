describe('balloon/interactivity.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/interactivity.html')
            .waitReady();
    });
    // Одно ложное срабатывание ФФ из-за отсутствия даблклика
    it('Дефолтная интерактивность', function () {
        return this.browser
            .waitForVisible(PO.map.balloon.layout())
            .pointerDblClick(PO.map.balloon.layout())
            .moveToObject(PO.mapId(), 100, 100)
            .getText('body #logger').then(function (text) {
                text.should.equal(
                    'balloon: mouseenter\n' +
                    'balloon: mousedown\n' +
                    'balloon: mouseup\n' +
                    'balloon: click\n' +
                    'balloon: mousedown\n' +
                    'balloon: mouseup\n' +
                    'balloon: click\n' +
                    'balloon: dblclick\n' +
                    'balloon: mouseleave\n' +
                    'map: mouseenter');
            })
            .verifyNoErrors();
    });

    it('silent интерактивность', function () {
        return this.browser
            .waitForVisible(PO.map.balloon.layout())
            .pointerClick('ymaps=silent')
            .pointerDblClick(PO.map.balloon.layout())
            .moveToObject(PO.mapId(), 100, 100)
            .getText('body #logger').then(function (text) {
                text.should.equal('map: mouseenter\n' +
                    'map: mousedown\n' +
                    'map: mouseup\n' +
                    'map: click\n' +
                    'map: mousedown\n' +
                    'map: mouseup\n' +
                    'map: click\n' +
                    'map: dblclick\n' +
                    'map: mouseleave\n' +
                    'map: mouseenter');
            })
            .verifyNoErrors();
    });

    it('transparent интерактивность', function () {
        return this.browser
            .waitForVisible(PO.map.balloon.layout())
            .pointerClick('ymaps=transparent')
            .pointerDblClick(PO.map.balloon.layout())
            .moveToObject(PO.mapId(), 100, 100)
            .getText('body #logger').then(function (text) {
                text.should.equal('balloon: mouseenter\nmap: mouseenter\n' +
                    'balloon: mousedown\nmap: mousedown\n' +
                    'balloon: mouseup\nmap: mouseup\n' +
                    'balloon: click\nmap: click\n' +
                    'balloon: mousedown\nmap: mousedown\n' +
                    'balloon: mouseup\nmap: mouseup\n' +
                    'balloon: click\nballoon: dblclick\n' +
                    'map: click\nmap: dblclick\n' +
                    'balloon: mouseleave\nmap: mouseleave\n' +
                    'map: mouseenter');
            })
            .verifyNoErrors();
    });

    it('layer интерактивность', function () {
        return this.browser
            .waitForVisible(PO.map.balloon.layout())
            .pointerClick('ymaps=layer')
            .pointerDblClick(PO.map.balloon.layout())
            .moveToObject(PO.mapId(), 100, 100)
            .getText('body #logger').then(function (text) {
                text.should.equal('balloon: mouseenter\n' +
                    'balloon: mousedown\n' +
                    'map: mousedown\n' +
                    'balloon: mouseup\n' +
                    'balloon: click\n' +
                    'balloon: mousedown\n' +
                    'map: mousedown\n' +
                    'balloon: mouseup\n' +
                    'balloon: click\n' +
                    'balloon: dblclick\n' +
                    'balloon: mouseleave\n' +
                    'map: mouseenter');
            })
            .verifyNoErrors();
    });

    it('geoObject интерактивность', function () {
        return this.browser
            .waitForVisible(PO.map.balloon.layout())
            .pointerClick('ymaps=geoObject')
            .pointerDblClick(PO.map.balloon.layout())
            .moveToObject(PO.mapId(), 100, 100)
            .getText('body #logger').then(function (text) {
                text.should.equal('balloon: mouseenter\n' +
                    'balloon: mousedown\n' +
                    'map: mousedown\n' +
                    'balloon: mouseup\n' +
                    'balloon: click\n' +
                    'balloon: mousedown\n' +
                    'map: mousedown\n' +
                    'balloon: mouseup\n' +
                    'balloon: click\n' +
                    'balloon: dblclick\n' +
                    'map: dblclick\n' +
                    'balloon: mouseleave\n' +
                    'map: mouseenter')
            })
            .verifyNoErrors();
    });

    it('opaque интерактивность', function () {
        return this.browser
            .waitForVisible(PO.map.balloon.layout())
            .pointerClick('ymaps=opaque')
            .pointerDblClick(PO.map.balloon.layout())
            .moveToObject(PO.mapId(), 100, 100)
            .getText('body #logger').then(function (text) {
                text.should.equal('balloon: mouseenter\n' +
                    'balloon: mousedown\n' +
                    'balloon: mouseup\n' +
                    'balloon: click\n' +
                    'balloon: mousedown\n' +
                    'balloon: mouseup\n' +
                    'balloon: click\n' +
                    'balloon: dblclick\n' +
                    'balloon: mouseleave\n' +
                    'map: mouseenter');
            })
            .verifyNoErrors();
    });
});