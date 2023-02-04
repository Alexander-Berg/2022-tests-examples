describe('balloon/overlayMethods.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/overlayMethods.html')
            .waitReady(PO.map.balloonPanel.closeButton());
    });

    it('Проверим возвращаемые значения методов', function () {
        return this.browser
            .pointerClick('ymaps=panel\=\=balloon')
            .pointerClick('ymaps=get')
            .pointerClick('ymaps=panel\=\=balloon')
            .csCheckText('body #logger', 'balloon layout == panel layout: true\n' +
                'balloon layout: undefined\n' +
                'panel layout: undefined\n' +
                'getBalloonElement: [object HTMLUnknownElement]\n' +
                'getData: [object Object]\n' +
                'getElement: [object HTMLUnknownElement]\n' +
                'getGeometry: [object Object]\n' +
                'getMap: [object Object]\n' +
                'getMode: panel\n' +
                'getShape: [object Object]\n' +
                'isEmpty: false\n' +
                'getShadowElement: null\n' +
                'getBalloonLayout: [object Object]\n' +
                'getLayout == getLayoutSync: true\n' +
                'getLayout: [object Object]\n' +
                'getLayout == getLayoutSync: true\n' +
                'balloon layout == panel layout: false\n' +
                'balloon layout: undefined\n' +
                'panel layout: [object Object]')
            .verifyNoErrors();
    });
});