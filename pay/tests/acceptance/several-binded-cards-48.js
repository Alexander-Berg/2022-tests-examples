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
    it('yandexpay-48: Залогин. Есть несколько привязанных карт', async function () {
        const bro = this.browser;
        await bro.yaClickOnPayButton();
        await bro.yaSwitchOnPayForm();
        await bro.yaAssertView('user-with-cards-screen-48', 'body');
    });
});
