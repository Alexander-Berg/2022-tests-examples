const { baseUrl } = require('../../.hermione.conf');
const TestData = require('../../config/test-data');

describe('Acceptance. Скриншотные тесты', () => {
    beforeEach(async function () {
        const bro = this.browser;
        await bro.yaLoginUser(TestData.UserCard3WithAva);
        await bro.yaOpenDemo(baseUrl);
    });
    afterEach(async function () {
        const bro = this.browser;
        await bro.yaLogoutUser(TestData.UserCard3WithAva);
    });
    it('yandexpay-113: Залогин. Есть <5 карт. Проверить окно выбора карт', async function () {
        const bro = this.browser;
        await bro.yaClickOnPayButton();
        await bro.yaSwitchOnPayForm();
        await bro.yaOpenCardList();
        await bro.yaAssertView('choose-cards-screen-113', 'body');
    });
});
