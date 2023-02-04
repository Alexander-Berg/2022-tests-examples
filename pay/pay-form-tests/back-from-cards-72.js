const { expect } = require('chai');

const { baseUrl } = require('../../../.hermione.conf');
const TestData = require('../../../config/test-data');
const list = require('../../../page-objects/cards-list');
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
    it('yandexpay-72: Вернуться со страницы выбора карты не меняя карту: галка не меняется', async function () {
        const bro = this.browser;
        await bro.yaClickOnPayButton();
        await bro.yaSwitchOnPayForm();
        await bro.yaOpenCardList();

        const selectedCardNum = await bro.getText(list.selected_card_number());

        await bro.yaClickOnBack();

        const currentNum = await bro.getText(form.pay_order_form.card_info_number());
        expect(currentNum).to.equal(selectedCardNum);
    });
});
