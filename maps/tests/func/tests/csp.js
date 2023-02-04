const DRAW_TIMEOUT = 300;
const AUTOPAN_TIMEOUT = 1000;
const PANORAMA_TIMEOUT = 1000;

describe('newConstructor/csp', () => {
    hermione.skip.in('ie11', 'С csp=true виджет не работает в IE');
    it('Открыть балун с картинкой', function () {
        return this.browser
            .wdtOpen('um=constructor%3A1b6b4b34d14246e1db35235a3dc2fc58658357865db2f9c2c0eb16a2d4f1e2fd&amp;' +
                'width=500&amp;height=400&amp;lang=ru_RU&amp;scroll=true&amp;' +
                ' apikey=b027f76e-cc66-f012-4f64-696c7961c395&amp;csp=true')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.areasPane())
            .wdtPointerClick(150, 150)
            .wdtWaitForVisible(PO.ymaps.balloon())
            .pause(AUTOPAN_TIMEOUT)
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'balloon');
    });

    hermione.skip.in(['ie11', 'edge'], 'С csp=true виджет не работает в IE');
    it('Построить маршрут', function () {
        return this.browser
            .wdtOpen('um=constructor%3A0f88baa3904e20365f5a0cd1dc2a7628106dae63a5c73edbd3c3ac2317d88266&amp;' +
                'width=500&amp;height=400&amp;lang=ru_RU&amp;scroll=true&amp;' +
                'apikey=b027f76e-cc66-f012-4f64-696c7961c395&amp;csp=true')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.areasPane())
            .wdtWaitForVisible(PO.ymaps.routeBtn())
            .click(PO.ymaps.routeBtn())
            .wdtWaitForVisible(PO.ymaps.routePanel())
            .wdtPointerClick(300, 100)
            .pause(DRAW_TIMEOUT)
            .wdtPointerClick(300, 200)
            .click(PO.ymaps.routeBtn())
            .wdtWaitForHidden(PO.ymaps.routePanel())
            .wdtWaitForVisible(PO.ymaps.routerPointsPin.A())
            .pause(DRAW_TIMEOUT)
            .click(PO.ymaps.routerPointsPin.A())
            .wdtWaitForVisible(PO.ymaps.balloonRoute())
            .wdtShouldNotBeVisible(PO.ymaps.routeContentTaxiLink());
    });

    it('Включить пробки', function () {
        return this.browser
            .wdtOpen('um=constructor%3A0f88baa3904e20365f5a0cd1dc2a7628106dae63a5c73edbd3c3ac2317d88266&amp;' +
                'width=500&amp;height=400&amp;lang=ru_RU&amp;scroll=true&amp;csp=true')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.areasPane())
            .wdtWaitForVisible(PO.ymaps.trafficBtn())
            .click(PO.ymaps.trafficBtn())
            .wdtWaitForVisible(PO.ymaps.trafficBtn.checked())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'traffic');
    });

    it('Переключение слоев', function () {
        return this.browser
            .wdtOpen('um=constructor%3A0f88baa3904e20365f5a0cd1dc2a7628106dae63a5c73edbd3c3ac2317d88266&amp;' +
                'width=500&amp;height=400&amp;lang=ru_RU&amp;scroll=true&amp;' +
                'apikey=b027f76e-cc66-f012-4f64-696c7961c395&amp;csp=true')
            .wdtWaitForVisible(PO.ymaps.eventPane())
            .waitForExist(PO.ymaps.areasPane())
            .wdtWaitForVisible(PO.ymaps.layersBtn())
            .click(PO.ymaps.layersBtn())
            .wdtWaitForVisible(PO.ymaps.layersPanel())
            .click(PO.yamapsLayerItem() + '=Спутник')
            .pause(DRAW_TIMEOUT)
            .click(PO.ymaps.layersBtn())
            .wdtWaitForVisible(PO.ymaps.layersPanel())
            .click(PO.yamapsLayerItem() + '=Гибрид')
            .pause(DRAW_TIMEOUT)
            .click(PO.ymaps.layersBtn())
            .wdtWaitForVisible(PO.ymaps.layersPanel())
            .click(PO.yamapsLayerItem() + '=Панорамы')
            .wdtWaitForVisible(PO.ymaps.panoramaPane())
            .pause(DRAW_TIMEOUT)
            .wdtPointerClick(250, 250)
            .pause(PANORAMA_TIMEOUT)
            .wdtWaitForVisible(PO.ymaps.panorama())
            .wdtVerifyScreenshot(PO.ymaps.eventPane(), 'layers');
    });
});
