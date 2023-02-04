const { baseUrl } = require('../../.hermione.conf');
const TestData = require('../../config/test-data');

describe('Acceptance. Скриншотные тесты', () => {
    beforeEach(async function () {
        const bro = this.browser;
        await bro.yaLoginUser(TestData.UserCard0WithAva);
        await bro.yaOpenDemo(baseUrl);
    });
    afterEach(async function () {
        const bro = this.browser;
        await bro.yaLogoutUser(TestData.UserCard0WithAva);
    });
    it('yandexpay-47: Залогин. Нет карт - видим кнопку', async function () {
        const bro = this.browser;
        await bro.yaShouldSeePayButton();
        await bro.pause(1000); // ожидаем отрисовку элементов на кнопке, заменить на обнаружение элементов в iframe https://st.yandex-team.ru/YANDEXPAY-1992
        await bro.yaAssertView('user-no-card-screen-47', 'body');
    });
});
