describe('balloon/autoPan.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/autoPan.html', {tileMock: 'withParameters'})
            .waitReady();
    });

    it('Срабатывает автопан по координатам, проверка промиса', function () {
        return this.browser
            .waitAndClick("ymaps=[60, 30]")
            .pause(1000)
            .getText('body #logger').then(function (text) {
                text.should.equal('open\nautopanbegin\nautopanend\nautopanbegin\nballoon.setPosition() fulfilled: true\nballoon.setPosition() rejected: false\nautopanend');
            })
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });

    it('Не срабатывает автопан балуна при выключении опции автопана', function () {
        return this.browser
            .waitAndClick("ymaps=!autoPan")
            .waitAndClick("ymaps=[60, 30]")
            .pause(1000)
            .getText('body #logger').then(function (text) {
                text.should.equal('open\nautopanbegin\nautopanend\nballoon.setPosition() fulfilled: true\nballoon.setPosition() rejected: false');
            })
            .csVerifyMapScreenshot(PO.mapId(), 'balloonWithoutAutoPan')
            .verifyNoErrors();
    });

    it('Срабатывает автопан с опцией autoPanMargin', function () {
        return this.browser
            .waitAndClick("ymaps=autoPan")
            .pause(5000)
            .csVerifyMapScreenshot(PO.mapId(), 'balloonAfterAutoPanInCentre')
            .csDrag([100, 100], [450, 450])
            .pause(1000)
            .waitAndClick("ymaps=autoPan")
            .pause(2000)
            .csVerifyMapScreenshot(PO.mapId(), 'balloonAfterAutoPanNearAnEdge')
            .verifyNoErrors();
    });

    it('Срабатывает автопан с опцией autoPanDuration', function () {
        return this.browser
            .waitAndClick("ymaps=autoPan")
            .pause(1000)
            .getText('body #logger').then(function (text) {
                text.should.equal('open\nautopanbegin\nautopanend\nautopanbegin');
            })
            .pause(4000)
            .getText('body #logger').then(function (text) {
                text.should.equal('open\nautopanbegin\nautopanend\nautopanbegin\nautopanend');
            })
            .verifyNoErrors();
    });
});