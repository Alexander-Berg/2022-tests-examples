import cssSelectors from '../../common/css-selectors';

describe('Базовые тесты. Десктоп.', () => {
    it('Карты открываются.', async function () {
        await this.browser.openPage('/213/moscow', {mockGeolocation: 'unavailable'});
        // Клик в панель делается чтобы снять фокус с поискового инпута в ie
        await this.browser.waitAndClick(cssSelectors.sidebar.panel);
        await this.browser.waitForVisible(cssSelectors.home.weather);
        await this.browser.waitAndVerifyScreenshot(cssSelectors.mapBody, 'general');
    });

    it('Панель маршрутов.', async function () {
        await this.browser.openPage(
            '/213/moscow/?mode=routes&rtext=55.733705%2C37.589482~55.733994%2C37.664441&rtt=auto',
            {mockToday: '2020-02-01T13:50'}
        );
        // Клик в панель делается чтобы снять фокус с первого поля маршрутной формы
        await this.browser.waitAndClick(cssSelectors.sidebar.panel);
        await this.browser.waitAndVerifyScreenshot(cssSelectors.sidebar.panel, 'routes');
    });

    it('Orgpage.', async function () {
        if (!this.browser.isPhone) {
            await this.browser.setViewportSize({width: 980, height: 1440});
        }
        await this.browser.openPage('/org/upitanny_yenot/1117269301/?ll=32.051801,54.797963&z=15');
        await this.browser.waitAndVerifyScreenshot(cssSelectors.sidebar.panel, 'orgpage');
    });

    it('Поиск.', async function () {
        await this.browser.openPage('/213/moscow/?display-text=кафе&mode=search&text=кафе');
        await this.browser.waitAndVerifyScreenshot(cssSelectors.sidebar.panel, 'search');
    });

    it('Каталог.', async function () {
        await this.browser.openPage('/213/moscow/catalog/');
        await this.browser.waitAndVerifyScreenshot(cssSelectors.sidebar.panel, 'catalog');
    });
});
