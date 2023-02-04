const { elements, hideElements } = require('./elements');
const waitUntilTimeout = 50000;

describe('user', () => {
    describe('paystep', () => {
        describe('sms', () => {
            it('выставление овердрафтного счета c подтверждением по смс, физик', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({
                    login: 'yndx-test-balance-assessor-3',
                    testEnv: true
                });

                const [, request_id] = await browser.ybRun('test_request_ph_with_overdraft', {
                    login
                });
                await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                await browser.ybWaitForLoad();
                await browser.ybWaitForInvisible(elements.paystepPreload);
                await browser.ybWaitForInvisible(elements.preload);

                await browser.click(elements.mainButtons.tumbler);

                await browser.click(elements.mainButtons.submit);
                await browser.waitForVisible(elements.popup);
                await browser.ybWaitForLoad();

                await browser.ybAssertView(
                    'paystep выставление овердрафтного счета c подтверждением по смс, физик, модальное окно подтверждение',
                    elements.page,
                    {
                        hideElements: [
                            ...hideElements,
                            '.yb-cart-row__eid',
                            elements.mainButtons.submit
                        ]
                    }
                );

                await browser.click(elements.phonePopup.phonePicker);
                await browser.click(elements.phonePopup.phone);
                await browser.click(elements.phonePopup.send);

                const [code] = await browser.ybRun('test_sms_code', { login });

                await browser.setValue(elements.phonePopup.codeInput, code);
                await browser.click(elements.phonePopup.confirm);
                await browser.waitForVisible(elements.phonePopup.success);

                await browser.ybAssertView(
                    'paystep выставление овердрафтного счета c подтверждением по смс, физик, модальное окно с success',
                    elements.page,
                    {
                        hideElements: [
                            ...hideElements,
                            '.yb-success-header__id',
                            '.yb-paystep-success__payment-date',
                            '.yb-cart-row__eid'
                        ]
                    }
                );
            });
            it('выставление кредитного счета c подтверждением по смс, физик', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({
                    login: 'yndx-test-balance-assessor-4',
                    testEnv: true
                });

                const [, , , request_id] = await browser.ybRun('test_request_fictive_pa', {
                    login
                });
                await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                await browser.ybWaitForLoad();
                await browser.ybWaitForInvisible(elements.paystepPreload);
                await browser.ybWaitForInvisible(elements.preload);

                await browser.click(elements.mainButtons.tumbler);

                await browser.click(elements.mainButtons.submit);
                await browser.waitForVisible(elements.popup);
                await browser.ybWaitForLoad();

                await browser.ybAssertView(
                    'paystep выставление кредитного счета c подтверждением по смс, физик, модальное окно подтверждение',
                    elements.page,
                    {
                        hideElements: [
                            ...hideElements,
                            '.yb-cart-row__eid',
                            elements.mainButtons.submit
                        ]
                    }
                );

                const [code] = await browser.ybRun('test_sms_code', { login });

                await browser.setValue(elements.phonePopup.codeInput, code);
                await browser.click(elements.phonePopup.confirm);
                await browser.ybWaitForInvisible(elements.mainButtons.submit);

                await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);

                await browser.ybAssertView(
                    'выставление кредитного счета через смс, физик, страница счета',
                    elements.page,
                    { hideElements: ['.date', '.show-order', '.oper-details', 'h1'] }
                );
            });
        });
    });
});
