const { baseUrl } = require('../../.hermione.conf');
const TestData = require('../../config/test-data');

describe('Acceptance', () => {
    beforeEach(async function () {
        const bro = this.browser;
        await bro.yaLoginUser(TestData.UserCard0WithoutAva);
        await bro.url(baseUrl);
    });
    afterEach(async function () {
        const bro = this.browser;
        try {
            await bro.yaLogoutFromAllAcc(TestData.UserCard0WithoutAva, TestData.UserCard1WithoutAva);
        } catch (error) {
            console.error(error);
        }
    });
    it('yandexpay-47: Залогин. Нет Карт - есть кнопка', async function () {
        const bro = this.browser;
        await bro.yaShouldSeePayButton();
    });
    it('yandexpay-49: Залогин юзер без карт. Поменять на юзера с картами', async function () {
        const bro = this.browser;
        await bro.yaLogoutUser(TestData.UserCard0WithoutAva);
        await bro.yaLoginUser(TestData.UserCard1WithoutAva);
        await bro.url(baseUrl);
        await bro.yaShouldSeePayButton();
    });
});
