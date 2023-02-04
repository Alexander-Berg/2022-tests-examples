describe('smoke/control/basic.html', function () {
    beforeEach(function () {
        return this.browser
            .openUrl('smoke/control/basic.html')
            .waitReady();
    });

    it('Строится простой маршрут', function () {
        return this.browser
            .pointerClick(PO.map.controls.routeEditor())
            .pause(500)
            .pointerClick(422, 180)
            .pause(500)
            .pointerClick(445, 438)
            .waitForVisible('ymaps=1')
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'route')
            .verifyNoErrors();
    });

    it('Cтроится маршрут через границу', function () {
        return this.browser
            .pointerClick(PO.map.controls.zoom.minus())
            .pause(100)
            .pointerClick(PO.map.controls.zoom.minus())
            .pause(100)
            .pointerClick(PO.map.controls.zoom.minus())
            .pause(500)
            .pointerClick(PO.map.controls.routeEditor())
            .pause(500)
            .pointerClick(285, 269)
            .pause(500)
            .pointerClick(94, 65)
            .waitForVisible('ymaps=1')
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'route2')
            .verifyNoErrors();
    });

    it('Работает простой поиск', function () {
        return this.browser
            .waitAndClick(PO.mapControlsButtonText() + '=Search')
            .waitAndClick(PO.searchPanel.input())
            .keys('Москва')
            .waitAndClick(PO.searchPanel.button())
            .waitForVisible(PO.map.balloon.closeButton())
            .csVerifyMapScreenshot(PO.mapId(), 'search')
            .verifyNoErrors();
    });

    it('Работает фулскрин', function () {
        return this.browser
            .waitAndClick(PO.map.controls.fullscreen())
            .pause(100)
            .csVerifyMapScreenshot(PO.map.map(), 'afterfullscreen')
            .waitAndClick(PO.map.controls.fullscreenCollapse())
            .pause(100)
            .csVerifyMapScreenshot(PO.map.map(), 'beforefullscreen')
            .verifyNoErrors();
    });

    it('Работает линейка', function () {
        return this.browser
            .pointerClick(PO.map.controls.ruler())
            .pointerClick(453, 233)
            .pointerClick(417, 392)
            .pointerClick(317, 200)
            .pointerClick(484, 325)
            .pause(20000)
            .verifyNoErrors()
            .csVerifyMapScreenshot(PO.mapId(), 'ruler');
    });

    it('Работает ПС', function () {
        return this.browser
            .pointerClick(PO.map.copyrights.link())
            .getAttribute(PO.map.copyrights.link(), 'href').then(function(res){
                res.should.equal('https://yandex.com/legal/maps_termsofuse/?lang=en')
            })
            .getTabIds().then(function(e){
                this.switchTab(e[e.length - 1]);
            })
            .waitForVisible('h1=Terms of Use for Yandex.Maps Service');
    });

    it('Клик по надписи Яндекс перемещает на БЯК', function () {
        return this.browser
            .waitForVisible(PO.map.copyrights.logo())
            .pointerClick(PO.map.copyrights.logo(), 10, 10)
            .verifyNoErrors()
            .getTabIds().then(function(e){
                this.switchTab(e[e.length - 1]);
            })
            .waitForVisible('.input_air-search-large__control');
    });
});
