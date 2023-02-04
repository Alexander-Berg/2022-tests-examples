import cssSelectors from '../../../common/css-selectors';

describe('Страница печати.', () => {
    it('Отображается список поиска.', async function () {
        await this.browser.openPage('/print/213/moscow/?l=stv,sta&ll=37.537561,55.741463&mode=search&text=Кафе');
        await this.browser.setViewportSize({width: 1024, height: 2000});
        await this.browser.waitAndVerifyScreenshot(cssSelectors.printPage.searchList, 'search-list');
    });

    it('Отображается список discovery.', async function () {
        await this.browser.openPage('/print/discovery/glavnye-moskovskye-rynki');
        await this.browser.setViewportSize({width: 1024, height: 2000});
        await this.browser.waitAndVerifyScreenshot(cssSelectors.printPage.descriptionList, 'discovery-list');
    });

    it('Отображается единичный результат поиска', async function () {
        await this.browser.openPage('/print/213/moscow/?mode=search&oid=1203061165&ol=biz');
        await this.browser.setViewportSize({width: 1024, height: 2000});
        await this.browser.waitAndVerifyScreenshot(cssSelectors.printPage.singleSearch, 'single-search');
    });

    it('Отображается публичная пользовательская карта', async function () {
        // login: 'geo.auto.test'
        const umLink = 'constructor:6d05c3b921fec896d033698456a980ef4ad0d31d01cd7e20944a52c604a81844';
        await this.browser.openPage(`/print/213/moscow/?um=${umLink}`);
        await this.browser.setViewportSize({width: 1024, height: 3000});
        await this.browser.waitAndVerifyScreenshot(cssSelectors.printPage.body, 'user-maps', {
            ignoreElements: cssSelectors.qrCodeView
        });
    });
});
