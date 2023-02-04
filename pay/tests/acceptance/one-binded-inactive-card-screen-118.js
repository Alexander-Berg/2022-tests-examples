const { baseUrl } = require('../../.hermione.conf');
const TestData = require('../../config/test-data');

describe('Acceptance. Скриншотные тесты', () => {
    beforeEach(async function () {
        const bro = this.browser;
        await bro.yaLoginUser(TestData.UserInactiveCard);
        await bro.yaOpenDemo(baseUrl);
    });
    afterEach(async function () {
        const bro = this.browser;
        await bro.yaLogoutUser(TestData.UserInactiveCard);
    });
    it('yandexpay-118: Залогин. У юзера одна карта и она не доступна - видим кнопку', async function () {
        const bro = this.browser;
        await bro.pause(1000);
        await bro.yaAssertView('user-no-card-screen-118', 'body');
    });
});
