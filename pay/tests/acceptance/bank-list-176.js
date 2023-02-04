const ndaUrl = 'https://test.pay.yandex.ru/demo-nda';

describe('Acceptance. Скриншотные тесты', () => {
    beforeEach(async function () {
        const bro = this.browser;
        await bro.yaOpenNdaPage(ndaUrl);
    });
    it('yandexpay-176: Лого банков на списке карт', async function () {
        const bro = this.browser;
        await bro.yaAssertView('bank-list-screen', 'body');
    });
});
