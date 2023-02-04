describe('newConstructor/basic', () => {
    it('Все объекты', function () {
        return this.browser
            .wdtOpen('sid=Pjq8Cm6tX9mN42pg6KGs3vYXwGX9uG4e&width=500&height=400&lang=ru_RU&sourceType=constructor')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'all-objects');
    });

    it('Объект с маленьким спаном', function () {
        return this.browser
            .wdtOpen('sid=O5HRdfEmYvOQ2RPxvuNVUwzlsd_NJvf6&width=500&height=400&lang=ru_RU&sourceType=constructor')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.placemark())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'small-span');
    });

    it('Объект с большим спаном, балуны поиска', function () {
        return this.browser
            .wdtOpen('sid=X-wRvZgmwxHPkls9ajtjv5zmdWg6PpPu&width=500&height=400&lang=ru_RU&sourceType=constructor')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.placemark())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'big-span');
    });

    it('HTML код в балуне', function () {
        return this.browser
            .wdtOpen('um=constructor%3A1b6b4b34d14246e1db35235a3dc2fc58658357865db2f9c2c0eb16a2d4f1e2fd&width=347&height=439&lang=ru_RU&sourceType=constructor')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.areasPane())
            .wdtPointerClick(150, 150)
            .wdtWaitForVisible(PO.ymaps.panel.balloon())
            .pause(3000)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'balloon-panel');
    });

    it('Спутник', function () {
        return this.browser
            .wdtOpen('sid=ATJHYdF0QNvfpANYyUziO4sHg5ubaWP-&width=500&height=400&lang=ru_RU&sourceType=constructor')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .wdtWaitForVisible(PO.ymaps.copyrightFog(), 'Не появились копирайты для спутника')
            .wdtWaitForVisible(PO.ymaps.layersBtn())
            .click(PO.ymaps.layersBtn())
            .wdtWaitForVisible(PO.ymaps.layersPanel())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'satellite');
    });

    it('Гибрид', function () {
        return this.browser
            .wdtOpen('sid=OieiQ-jsjjXFxYoPaklJGWfZdPgCFFuS&width=500&height=400&lang=ru_RU&sourceType=constructor')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .wdtWaitForVisible(PO.ymaps.copyrightFog(), 'Не появились копирайты для гибрида')
            .wdtWaitForVisible(PO.ymaps.layersBtn())
            .click(PO.ymaps.layersBtn())
            .wdtWaitForVisible(PO.ymaps.layersPanel())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'hybrid');
    });

    it('Разные метки', function () {
        return this.browser
            .wdtOpen('sid=TgzeRL4ShrmDa85Jv3K0YL3ZLRbNE2Y_&width=500&height=400&lang=ru_RU&sourceType=constructor')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.placemark())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'placemarks');
    });

    it('Разные полигоны', function () {
        return this.browser
            .wdtOpen('sid=PdtP7HUFWGqc3N1sGGDgqbU5VVcSp6Zj&width=500&height=400&lang=ru_RU&sourceType=constructor')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.areasPane())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'polygons');
    });

    it('Полигоны с вырезанием', function () {
        return this.browser
            .wdtOpen('um=constructor%3Ad3711fec66009ab97d829bade6b22a8af4102662d60c778532bac1bf1bc5b4c2' +
                '&width=800&height=400&lang=ru_RU')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.areasPane())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'polygons-with-holes');
    });

    it('Линии', function () {
        return this.browser
            .wdtOpen('sid=GhofiMGkcqELImiwo5VBpAVbyea7dmKd&width=500&height=400&lang=ru_RU&sourceType=constructor')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.areasPane())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'lines');
    });

    it('Старая "моя карта"', function () {
        return this.browser
            .wdtOpen('sid=YmkbkmEX3EDxO42K6LFaS3WX4wKAF5GK&width=500&height=400&lang=ru_RU&sourceType=mymaps')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.placemark())
            .setMeta('ignoreErrors', true)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'old-my-map');
    });

    it('Старая карта конструктора', function () {
        return this.browser
            .wdtOpen('sid=S4meeJrMoZqSBW2mL0Xc3cjG4fZf-4rE&width=500&height=400&lang=ru_RU&sourceType=constructor')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.placemark())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'old-constructor');
    });

    it('Старая карта конструктора с авторизацией', function () {
        return this.browser
            .wdtOpen('sid=Onc7bhiX3MLWlKtzzvMs66X_VutJOZPd&width=600&height=450')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.placemark())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'old-constructor-with-auth');
    });

    it('Старая карта конструктора без авторизации', function () {
        return this.browser
            .wdtOpen('sid=bSvs-RK2LezUF1_vLyqILcz_kc5WZp1B&width=600&height=450')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.placemark())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'old-constructor-without-auth');
    });
});
