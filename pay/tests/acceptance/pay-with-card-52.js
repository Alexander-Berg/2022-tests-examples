const { baseUrl } = require('../../.hermione.conf');
const TestData = require('../../config/test-data');

describe('Acceptance', () => {
    beforeEach(async function () {
        const bro = this.browser;
        await bro.yaLoginUser(TestData.UserCard1WithAva);
        await bro.yaOpenDemo(baseUrl);
    });
    afterEach(async function () {
        const bro = this.browser;
        try {
            await bro.yaLogoutUser(TestData.UserCard1WithAva);
        } catch (error) {
            console.error(error);
        }
    });
    it('yandexpay-52: Залогин. Оплатить привязанной активной картой', async function () {
        const bro = this.browser;
        await bro.yaClickOnPayButton();
        await bro.yaSwitchOnPayForm();
        await bro.yaClickOnFormPayButton();
        await bro.pause(5000); //для отработки скрытия формы
        await bro.yaSwitchOnDemo();
        await bro.yaShouldSeePayButton();
        await bro.yaShouldSeeTokenLog();
    });
});
