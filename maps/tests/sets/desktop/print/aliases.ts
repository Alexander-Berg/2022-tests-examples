import cssSelectors from '../../../common/css-selectors';
import getSelectorByText from '../../../lib/func/get-selector-by-text';

describe('Алиасы печати.', () => {
    it('Открывается главная', async function () {
        await this.browser.openPage('/print/');
    });

    it('Открывается главная без слеша', async function () {
        await this.browser.openPage('/print');
    });

    it('Открывается с геоалиасом', async function () {
        await this.browser.openPage('/print/213/moscow');
    });

    it('1орг, страница одной организации', async function () {
        await this.browser.setViewportSize({width: 1920, height: 1380});
        await this.browser.openPage('/print/org/tanuki/1264367782/');
        await this.browser.waitForVisible(cssSelectors.printPage.placemark);
        await this.browser.waitAndVerifyScreenshot(cssSelectors.printPage.singleSearch, 'print-page-org');
    });

    it('Адрес', async function () {
        await this.browser.setViewportSize({width: 1920, height: 1380});
        await this.browser.openPage('/print/213/moscow/house/ulitsa_lva_tolstogo_16/Z04Ycw9nSUwEQFtvfXtycnVlbQ==/');
        await this.browser.waitForVisible(
            getSelectorByText('улица Льва Толстого, 16', cssSelectors.printPage.singleSearch)
        );
        await this.browser.waitForVisible(cssSelectors.printPage.placemark);
        await this.browser.waitAndVerifyScreenshot(cssSelectors.printPage.map, 'print-map-toponym', {
            ignoreElements: cssSelectors.qrCodeView
        });
    });
});
