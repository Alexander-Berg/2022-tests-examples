const { baseUrl } = require('../../.hermione.conf');
const TestData = require('../../config/test-data');

describe('Acceptance. Скриншотные тесты', () => {
    beforeEach(async function () {
        const bro = this.browser;
        await bro.yaLoginUser(TestData.UserCard1WithAva);
        await bro.yaOpenDemo(baseUrl);
    });
    afterEach(async function () {
        const bro = this.browser;
        await bro.yaLogoutUser(TestData.UserCard1WithAva);
    });
    it('yandexpay-130: Залогин. Есть одна привязанная карта', async function () {
        const bro = this.browser;
        await bro.yaClickOnPayButton();
        await bro.yaSwitchOnPayForm();
        await bro.yaShouldSeePayOrderForm();
        await bro.yaAssertView('user-with-card-screen-130', 'body');
    });
});
