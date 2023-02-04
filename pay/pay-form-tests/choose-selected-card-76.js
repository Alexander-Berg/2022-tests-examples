const { expect } = require('chai');

const { baseUrl } = require('../../../.hermione.conf');
const TestData = require('../../../config/test-data');
const form = require('../../../page-objects/pay-form');

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
    it('yandexpay-76: Повторно выбрать выбранный способ оплаты', async function () {
        const bro = this.browser;
        await bro.yaClickOnPayButton();
        await bro.yaSwitchOnPayForm();
        await bro.yaShouldSeePayOrderForm();
        const cardNum = await bro.getText(form.pay_order_form.card_info_number());

        await bro.yaOpenCardList();
        await bro.yaClickOnSelectedCard();
        await bro.yaWaitForVisible(form.pay_order_form.pay_button_red());

        const currentCardNum = await bro.getText(form.pay_order_form.card_info_number());

        expect(cardNum).to.equal(currentCardNum);
        await bro.yaClickOnFormPayButton();
        await bro.pause(5000); //для отработки скрытия формы
        await bro.yaSwitchOnDemo();
        await bro.yaShouldSeePayButton();
    });
});
