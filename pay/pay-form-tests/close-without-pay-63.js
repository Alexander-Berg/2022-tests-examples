const { baseUrl } = require('../../../.hermione.conf');
const TestData = require('../../../config/test-data');
const demo = require('../../../page-objects/demo');

describe('BL. Окно оплаты', () => {
    beforeEach(async function () {
        const bro = this.browser;
        await bro.yaLoginUser(TestData.UserCard3WithAva);
        await bro.yaOpenDemo(baseUrl);
    });
    afterEach(async function () {
        const bro = this.browser;
        await bro.yaLogoutUser(TestData.UserCard3WithAva);
    });
    it('yandexpay-63: Закрыть окно пея без оплаты', async function () {
        const bro = this.browser;
        await bro.yaClickOnPayButton();
        await bro.yaSwitchOnPayForm();
        await bro.yaClosePay();
        await bro.yaShouldSeePayButton();
        await bro.pause(1000); // ожидаем отрисовку элементов на кнопке, заменить на обнаружение элементов в iframe https://st.yandex-team.ru/YANDEXPAY-1992
        await bro.yaAssertView('demo-after-close-pay-63', demo.pay_button());

        await bro.yaClickOnPayButton();
        await bro.yaSwitchOnPayForm();
        await bro.yaClickOnFormPayButton();
        await bro.pause(5000); //для отработки скрытия формы
        await bro.yaSwitchOnDemo();
        await bro.yaShouldSeePayButton();
        await bro.yaShouldSeeTokenLog(); //добавить после мерджа https://github.yandex-team.ru/trust/yandex-pay/pull/128
    });
});
