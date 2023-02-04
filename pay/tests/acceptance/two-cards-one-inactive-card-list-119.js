const { baseUrl } = require('../../.hermione.conf');
const TestData = require('../../config/test-data');

describe('Acceptance. Скриншотные тесты', () => {
    beforeEach(async function () {
        const bro = this.browser;
        await bro.yaLoginUser(TestData.UserActiveCardAndInactiveCard);
        await bro.yaOpenDemo(baseUrl);
    });
    afterEach(async function () {
        const bro = this.browser;
        await bro.yaLogoutUser(TestData.UserActiveCardAndInactiveCard);
    });
    it('yandexpay-119: Залогин. У юзера две карты и одна из них недоступна - список карт', async function () {
        const bro = this.browser;
        await bro.yaClickOnPayButton();
        await bro.yaSwitchWindow('Yandex Pay');
        await bro.yaOpenCardList();
        await bro.yaAssertView('user-with-card-screen-119-2', 'body');
    });
});
