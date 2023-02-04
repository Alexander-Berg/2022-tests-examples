import cssSelectors from '../../../common/css-selectors';

describe('Тач.', () => {
    it('Повторный поиск', async function () {
        await this.browser.openPage('');
        await this.browser.submitSearch('кафе');
        await this.browser.waitForVisible(cssSelectors.search.placemark.view);
        await this.browser.clearElement(cssSelectors.search.input);
        await this.browser.submitSearch('гостиница');
        await this.browser.waitForHidden(cssSelectors.search.loadingIndicator);
        await this.browser.waitForVisible(cssSelectors.search.placemark.view);
        await this.browser.waitForVisible(cssSelectors.search.list.collapsed);
        await this.browser.swipeShutter('up');
        await this.browser.waitForHidden(cssSelectors.sidebar.minicard);
        await this.browser.waitForVisible(cssSelectors.sidebar.panel);
        await this.browser.waitForVisible(cssSelectors.search.list.view);
        await this.browser.verifyRequestId({
            lastId: '1652895046817340-4117830818-sas1-6138-sas-addrs-nmeta-new-8031',
            amount: 2
        });
    });

    it('Строка очищается после возврата с поиска', async function () {
        await this.browser.openPage('/');
        await this.browser.submitSearch('Кафе');
        await this.browser.waitForVisible(cssSelectors.search.panel);
        await this.browser.back();
        await this.browser.waitAndCheckValue(cssSelectors.search.input, '');
    });
});
