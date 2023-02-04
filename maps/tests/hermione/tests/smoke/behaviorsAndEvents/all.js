describe('smoke/behaviorsAndEvents/all.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/behaviorsAndEvents/all.html')
            .waitReady();
    });

    it('Даблкликзум работает при клике по карте', function () {
        return this.browser
            .chooseItemOnListbox('dblClickZoom')
            .waitForInvisible('dblClickZoom')
            .pointerDblClick(339, 335)
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'afterDblClick')
            .verifyNoErrors();
    });

    it('Даблкликзум работает при клике по неточечным объектам', function () {
        return this.browser
            .chooseItemOnListbox('dblClickZoom')
            .waitForInvisible('dblClickZoom')
            .pointerDblClick(321, 255)
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'afterDblClickGeoObject')
            .verifyNoErrors();
    });

    it('Даблкликзум работает при клике по точечным объектам', function () {
        return this.browser
            .chooseItemOnListbox('dblClickZoom')
            .waitForInvisible('dblClickZoom')
            .pointerDblClick(113, 245)
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'afterDblClickPlacemark')
            .verifyNoErrors();
    });

    it('Драг карты работает', function () {
        return this.browser
            .chooseItemOnListbox('drag')
            .csDrag([42, 127], [362, 347])
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'drag')
            .verifyNoErrors();
    });

    it('Драг через точечные и неточечные объекты работает', function () {
        return this.browser
            .chooseItemOnListbox('drag')
            .csDrag([113, 115], [157, 229])
            .pause(500)
            .csDrag([366, 248], [364, 146])
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'dragGeoObjects')
            .verifyNoErrors();
    });

    it('Рисуем маршрут на карте', function () {
        return this.browser
            .chooseItemOnListbox('routeEditor')
            .pointerClick(193, 179)
            .pointerClick(263, 337)
            .waitForVisible('ymaps=1')
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'routeEditor')
            .verifyNoErrors();
    });

    it('Рисуем маршрут на неточечном и точечном объекте', function () {
        return this.browser
            .chooseItemOnListbox('routeEditor')
            .pointerClick(320, 131)
            .pause(500)
            .pointerClick(110, 117)
            .waitForVisible('ymaps=2')
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'routeEditorGeoObjects')
            .verifyNoErrors();
    });

    it('Рисуем линейку на карте', function () {
        return this.browser
            .chooseItemOnListbox('ruler')
            .pointerClick(193, 179)
            .pointerClick(263, 337)
            .pointerClick(400, 117)
            .pointerClick(230, 129)
            .pointerClick(159, 167)
            .pointerClick(114, 119)
            .pointerClick(112, 244)
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'ruler')
            .verifyNoErrors();
    });

    it('Лупа левой кнопкой', function () {
        return this.browser
            .chooseItemOnListbox('leftMouseButtonMagnifier')
            .csDrag([142, 127], [362, 347])
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'leftMouseButtonMagnifier')
            .verifyNoErrors();
    });

    it('Лупа левой кнопкой на неточечном и точечном объекте', function () {
        return this.browser
            .chooseItemOnListbox('leftMouseButtonMagnifier')
            .csDrag([112, 117], [223, 255])
            .pause(1000)
            .csDrag([142, 143], [383, 383])
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'leftMouseButtonMagnifierGeoObjects')
            .verifyNoErrors();
    });

    it('Лупа правой кнопкой', function () {
        return this.browser
            .chooseItemOnListbox('rightMouseButtonMagnifier')
            .csDrag([142, 127], [362, 347], 300, 'right')
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'rightMouseButtonMagnifier')
            .verifyNoErrors();
    });

    it('Лупа правой кнопкой на неточечном и точечном объекте', function () {
        return this.browser
            .chooseItemOnListbox('rightMouseButtonMagnifier')
            .csDrag([112, 117], [223, 255], 300, 'right')
            .pause(1000)
            .csDrag([142, 143], [383, 383], 300, 'right')
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'rightMouseButtonMagnifierGeoObjects')
            .verifyNoErrors();
    });
});
