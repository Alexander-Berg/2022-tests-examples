import cssSelectors from '../../../common/css-selectors';

describe('Страница печати. ОТ. ', () => {
    it('Остановка.', async function () {
        await this.browser.openPage('/print/213/moscow/?mode=masstransit&masstransit[stopId]=stop__9644588');
        await this.browser.waitForVisible(cssSelectors.printPage.masstransit.vehiclesList);
    });

    it('Маршрут.', async function () {
        await this.browser.openPage(
            '/print/213/moscow/?ll=37.593442,55.732420&z=16&mode=masstransit&masstransit[stopId]=stop__9644588' +
                '&masstransit[lineId]=213_28_trolleybus_mosgortrans'
        );
        await this.browser.waitForVisible(cssSelectors.printPage.masstransit.routeLegend);
    });

    it('Подробности маршрута ОТ.', async function () {
        await this.browser.setViewportSize({width: 1280, height: 1024});
        await this.browser.openPage(
            '/print/213/moscow/?ll=37.63665500000002,55.682804459070205&mode=routes' +
                '&rtext=55.745802,37.640394~55.704471,37.530272~55.643619,37.704700&rtn=1&rtt=mt&ruri=~~&z=12'
        );
        await this.browser.waitAndVerifyScreenshot(
            cssSelectors.printPage.routes.masstransit,
            'print-masstransit-details'
        );
    });

    it('Червяк маршрута ОТ.', async function () {
        await this.browser.setViewportSize({width: 1000, height: 1400});
        await this.browser.openPage(
            '/print/213/moscow/?ll=37.63665500000002,55.682804459070205&mode=routes' +
                '&rtext=55.745802,37.640394~55.704471,37.530272~55.643619,37.704700&rtn=1&rtt=mt&ruri=~~&z=12'
        );
        await this.browser.waitForVisible(cssSelectors.printPage.routes.masstransit);
        await this.browser.waitAndVerifyMapScreenshot('print-worm', {printPage: true});
    });

    it('Кастомная иконка метро.', async function () {
        await this.browser.setViewportSize({width: 1000, height: 1400});
        await this.browser.openPage(
            '/print/157/minsk/?ll=27.550699,53.904328&' +
                'masstransit%5BstopId%5D=station__9880204&mode=masstransit&z=15.39'
        );
        await this.browser.waitAndVerifyScreenshot(
            cssSelectors.printPage.descriptionList,
            'description-list-color-metro'
        );
    });

    it('Инвертированная иконка для всех видов транспорта кроме метро.', async function () {
        await this.browser.setViewportSize({width: 1000, height: 1400});
        await this.browser.openPage(
            '/print/213/moscow/?ll=37.596449,55.737746&masstransit%5BstopId%5D=stop__9645123&mode=masstransit&z=16'
        );
        await this.browser.waitAndVerifyScreenshot(
            cssSelectors.printPage.descriptionList,
            'description-list-color-transport'
        );
    });

    it('Ночной маршрут дополняется надписью "(ночной)".', async function () {
        await this.browser.openPage(
            '/print/213/moscow/?ll=37.416687,55.957781&masstransit[lineId]=N1_bus_default' +
                '&masstransit[threadId]=2036926069&mode=masstransit&z=16.5'
        );
        await this.browser.waitForVisible(cssSelectors.printPage.masstransit.stopName);
        await this.browser.waitAndCheckValue(cssSelectors.printPage.masstransit.stopName, /\(ночной\)/);
    });
});
