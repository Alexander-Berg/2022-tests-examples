const { baseUrl } = require('../../.hermione.conf');
const TestData = require('../../config/test-data');
const demo = require('../../page-objects/demo');

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
    it('yandexpay-122: Перс. кнопка. У пользователя есть карта и аватарка', async function () {
        const bro = this.browser;
        await bro.pause(1000); //для отработки анимации отрисовки перс кнопки поверх обычной
        await bro.yaAssertView('user-with-card-with-ava-122', demo.pay_button());
    });
});
