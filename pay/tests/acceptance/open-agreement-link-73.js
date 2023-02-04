const { baseUrl } = require('../../.hermione.conf');
const TestData = require('../../config/test-data');
const agreement = require('../../page-objects/agreement');

describe('Acceptance', () => {
    beforeEach(async function () {
        const bro = this.browser;
        await bro.yaLoginUser(TestData.UserCard1WithAva);
        await bro.yaOpenDemo(baseUrl);
    });
    afterEach(async function () {
        const bro = this.browser;
        await bro.yaLogoutUser(TestData.UserCard1WithAva);
    });
    it('yandexpay-73: Открыть текст лицензионного соглашения в подвале', async function () {
        const bro = this.browser;
        await bro.yaClickOnPayButton();
        await bro.yaSwitchOnPayForm();
        await bro.yaClickOnAgreementLink();
        await bro.yaSwitchOnAgreement();
        await bro.yaAssertView('should_see_first-title-agreement-73', agreement.agreement_first_title()); // пришлось скриншотить, тк
        //видит все пункты лиц. соглашения, даже те, что за скроллом (проявляется проблема только на стр. лиц соглашения)
    });
});
