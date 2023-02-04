const { baseUrl } = require('../../.hermione.conf');
const TestData = require('../../config/test-data');

describe('Acceptance', () => {
    beforeEach(async function () {
        const bro = this.browser;
        await bro.yaLoginUser(TestData.UserInactiveCard);
        await bro.url(baseUrl);
    });
    afterEach(async function () {
        const bro = this.browser;
        await bro.yaLogoutUser(TestData.UserInactiveCard);
    });
    it('yandexpay-118: Залогин. У юзера одна карта и она не доступна - видим кнопку', async function () {
        const bro = this.browser;
        await bro.yaShouldSeePayButton();
    });
});
