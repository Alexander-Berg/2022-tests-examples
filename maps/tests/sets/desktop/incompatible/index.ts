describe('Страница устаревших браузеров', () => {
    it('Диалог при открытии страницы', async function () {
        await this.browser.setViewportSize({width: 1920, height: 1380});
        await this.browser.openPage('?debug=incompatible', {
            readySelector: 'body',
            ignoreMapReady: true
        });
        await this.browser.addStyles('ymaps[class*=ground-pane] {display: none;}');
        await this.browser.waitAndVerifyScreenshot('.dialog', 'incompatible-page', {ignoreFonts: true});
    });
});
