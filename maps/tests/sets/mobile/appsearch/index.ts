const headerSelectors = {
    mainLayout: '.header-view__main-layout'
};

describe('appsearch', () => {
    it('Не должно быть хедера', async function () {
        await this.browser.openPage('/?debug=appsearch_session');
        await this.browser.waitForNotAppear(headerSelectors.mainLayout, 1000);
    });
});
