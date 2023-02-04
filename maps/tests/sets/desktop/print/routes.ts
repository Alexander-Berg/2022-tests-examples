import cssSelectors from '../../../common/css-selectors';

describe('Страница печати. Маршруты.', () => {
    const url = '/print/?mode=routes&rtext=55.736931,37.596582~55.752924,37.605938&ll=37.600499,55.744869&z=15';
    const SIZES = {
        landscape: {width: 1920, height: 1080},
        portrait: {width: 1024, height: 3000}
    };

    [
        {
            text: 'Автомобиль',
            type: 'auto',
            cssSelector: cssSelectors.printPage.routes.auto,
            fileName: 'auto'
        },
        {
            text: 'Автомобильный маршрут с избеганием платных дорог',
            type: 'auto',
            cssSelector: cssSelectors.printPage.routes.auto,
            fileName: 'auto-with-avoid-tolls',
            url:
                '/print/?ll=37.432204,55.914127&mode=routes&routes[avoid]=tolls' +
                '&rtext=55.891911,37.486379~55.950301,37.396259&rtn=1&ruri=~&z=13'
        },
        {
            text: 'ОТ',
            type: 'mt',
            cssSelector: cssSelectors.printPage.routes.masstransit,
            fileName: 'masstransit'
        },
        {
            text: 'Маршрут ОТ с фильтрами',
            type: 'mt',
            cssSelector: cssSelectors.printPage.routes.masstransit,
            fileName: 'masstransit-filter',
            url:
                '/print/?ll=37.4719500,55.6957398&mode=routes' +
                '&routes[avoidTypes]=underground,bus&rtext=55.884268,37.662329~55.625604,37.611518&rtn=1',
            size: SIZES.portrait
        },
        {
            text: 'Пешеходный',
            type: 'pd',
            cssSelector: cssSelectors.printPage.routes.short,
            fileName: 'pedestrian',
            url: '/print/?mode=routes&rtext=55.736931,37.596580~55.752924,37.605935&ll=37.600499,55.744869&z=15'
        },
        {
            text: 'Велосипед',
            type: 'bc',
            cssSelector: cssSelectors.printPage.routes.short,
            fileName: 'bicycle'
        },
        {
            text: 'Велосипед без лестниц',
            type: 'bc',
            cssSelector: cssSelectors.printPage.map,
            ignoreElements: cssSelectors.qrCodeView,
            fileName: 'bicycle-without-stairs',
            url: '/print/?mode=routes&rtext=55.736931,37.596581~55.752630,37.605755&ll=37.600499,55.744869&z=15',
            size: SIZES.landscape
        },
        {
            text: 'Велосипед по разным участкам дорог',
            type: 'bc',
            cssSelector: cssSelectors.printPage.map,
            ignoreElements: cssSelectors.qrCodeView,
            fileName: 'bicycle-on-different-roads',
            url: '/print/?mode=routes&rtext=55.736931,37.596581~55.752630,37.605755&ll=37.600499,55.744869&z=15',
            size: SIZES.landscape
        },
        {
            text: 'Велосипед по автомобильной дороге',
            type: 'bc',
            cssSelector: cssSelectors.printPage.map,
            ignoreElements: cssSelectors.qrCodeView,
            fileName: 'bicycle-on-auto-road',
            url: '/print/?mode=routes&rtext=55.754964,37.601619~55.753832,37.602184&ll=37.601922,55.754401&z=19',
            size: SIZES.landscape
        }
    ].forEach((el) => {
        it(`Отображается маршрут ${el.text}.`, async function () {
            await this.browser.setViewportSize(el.size || SIZES.landscape);
            await this.browser.openPage(`${el.url || url}&rtt=${el.type}`);
            await this.browser.waitAndVerifyScreenshot(el.cssSelector, el.fileName, {
                ignoreElements: el.ignoreElements
            });
        });
    });
});
