describe('smoke/geolink/basic.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geolink/basic.html')
            .waitForVisible(PO.geolink(), 40000);
    });

    it('Проверяем общий вид геоссылок в тексте', function () {
        return this.browser
            .pause(500)
            .csVerifyScreenshot('body #box', 'geolink')
            .verifyNoErrors();
    });

    it('Вид геоссылки при наведении', function () {
        return this.browser
            .moveToObject(PO.geolink())
            .pause(500)
            .csVerifyScreenshot('body #box', 'geolinkHover')
            .moveToObject('h3')
            .pause(500)
            .csVerifyScreenshot('body #box', 'geolink')
            .verifyNoErrors();
    });

    it('При клике геоссылка появляется и при повторном пропадает', function () {
        return this.browser
            .click(PO.geolink())
            .waitForVisible(PO.geolink.popup())
            .pause(1500)
            .csVerifyScreenshot('body #box', 'geolinkPopup', {
                ignoreElements: [PO.geolink.popup.map()]
            })
            .click(PO.geolink())
            .waitForInvisible(PO.geolink.popup())
            .moveToObject('h3')
            .pause(500)
            .csVerifyScreenshot('body #box', 'geolink')
            .verifyNoErrors();
    });

    it('Панель геоссылки закрывается на крестик', function () {
        return this.browser
            .click(PO.geolink())
            .waitForVisible(PO.geolink.popup())
            .click(PO.geolink.popup.close())
            .waitForInvisible(PO.geolink.popup())
            .verifyNoErrors();
    });

    it('Панель геоссылки закрывается кликом вне геоссылки', function () {
        return this.browser
            .click(PO.geolink())
            .waitForVisible(PO.geolink.popup())
            .pointerClick('h3')
            .waitForInvisible(PO.geolink.popup())
            .verifyNoErrors();
    });
    hermione.only.in('chrome');
    describe('Работа с БЯК', function () {

        afterEach(function () {
            return this.browser
                .close();
        });

        it('Клик по карте ведёт на БЯК', function () {
            return this.browser
                .click(PO.geolink())
                .waitForVisible(PO.geolink.popup())
                .waitAndClick(PO.geolink.popup.map())
                .getTabIds().then(function (e) {
                    this.switchTab(e[e.length - 1])
                })
                //.waitReady()
                .waitForVisible('.card-title-view__title=улица Льва Толстого, 16');
        });

        it('Клик "Подробнее" ведёт на БЯК', function () {
            return this.browser
                .click(PO.geolink())
                .waitForVisible(PO.geolink.popup())
                .waitAndClick(PO.geolink.popup.footer.more())
                .getTabIds().then(function (e) {
                    this.switchTab(e[e.length - 1])
                })
                //.waitReady()
                .waitForVisible('.card-title-view__title=улица Льва Толстого, 16');
        });

        it('Клик на кафе ведёт на кафе на БЯК', function () {
            return this.browser
                .pointerClick('body #whiteText span')
                .waitForVisible(PO.geolink.popup())
                .waitForVisible(PO.geolink.popup.map())
                .csVerifyScreenshot('body #box', 'geolinkCafe', {
                    ignoreElements: [PO.geolink.popup.map()]
                })
                .waitAndClick(PO.geolink.popup.footer.more())
                .getTabIds().then(function (e) {
                    this.switchTab(e[e.length - 1])
                })
                //.waitReady()
                .waitForVisible('.business-card-title-view__category');
        });
        it('Клик "Как добраться" строит маршрут на БЯК', function () {
            return this.browser
                .click(PO.geolink())
                .waitForVisible(PO.geolink.popup())
                .waitAndClick(PO.geolink.popup.footer.howToGet())
                .getTabIds().then(function (e) {
                    this.switchTab(e[e.length - 1])
                })
                .waitForVisible(PO.maps.routeInput())
                .getValue(PO.maps.routeInput()).then(function (text) {
                    text[1].should.equal('улица Льва Толстого, 16')
                });
        });
    });
});
