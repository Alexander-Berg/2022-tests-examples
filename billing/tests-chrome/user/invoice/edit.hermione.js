const { assertViewOpts } = require('./helpers');
const { waitUntilTimeout } = require('../../../helpers');

describe('user', () => {
    describe('invoice', () => {
        describe('edit', () => {
            it('нельзя изменить счет с немгновенным способом оплаты под клиентом', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn();
                const [, id] = await browser.ybRun('test_prepayment_unpaid_invoice', {
                    login
                });
                await browser.ybUrl('user', 'invoice.xml?invoice_id=' + id);
                await browser.ybWaitForLoad();
                await browser.ybWaitForInvisible('a[href^="paychoose.xml"]');
            });

            it('изменение cпособа оплаты в старом КИ под клиентом', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn();
                const [client_id, id, person_id] = await browser.ybRun(
                    'test_prepayment_unpaid_invoice_with_card_paysys',
                    {
                        login
                    }
                );
                await browser.ybUrl('user', 'invoice.xml?invoice_id=' + id);
                await browser.ybWaitForLoad();
                await browser.ybRun('test_create_ph_replace_person', {
                    client_id
                });
                await browser.click('a[href^="paychoose.xml"]');
                await browser.click('#paysys_id_1000');
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'paychoose.xml, нельзя изменить плательщика',
                    '.l-payments-choose'
                );
                await browser.click('#sub');
                await browser.click('.blc_change_payment_method_or_person_button');
                await browser.ybAssertUrl(
                    '/paychoose.xml?invoice_id=' +
                        id +
                        '&paysys_id=1000' +
                        '&person_id=' +
                        person_id +
                        '&contract_id=&mode=ci'
                );

                await browser.click('#sub');
                await browser.ybWaitForLoad();

                await browser.ybAssertView(
                    'измененный способ оплаты на paypreview.xml',
                    '.blc_header'
                );

                await browser.click('.blc_submit');
                await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);

                await browser.ybAssertView(
                    'страница, изменен способ оплаты',
                    '.yb-user-content',
                    assertViewOpts
                );

                await browser.ybUrl('user', 'success.xml?invoice_id=' + id);
                await browser.click('a[href^="paychoose.xml"]');
                await browser.ybAssertUrl(
                    '/paychoose.xml?invoice_id=' +
                        id +
                        '&person_id=' +
                        person_id +
                        '&paysys_id=1000' +
                        '&contract_id=&mode=ci'
                );
            });

            it('изменение cпособа оплаты на paystep под клиентом', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn();
                const [client_id, id] = await browser.ybRun(
                    'test_prepayment_unpaid_invoice_with_card_paysys',
                    {
                        login
                    }
                );
                await browser.ybRun('test_create_ph_replace_person', {
                    client_id
                });
                await browser.ybUrl('user', 'paystep.xml?invoice_id=' + id);
                await browser.ybWaitForLoad();
                await browser.ybWaitForInvisible('.yb-paystep-preload');
                await browser.ybWaitForInvisible('.yb-preload_isLoading');
                await browser.ybSetSteps(`Открываем список плательщиков`);
                await browser.click('.yb-paystep-main__person button');

                await browser.ybWaitForLoad();
                await browser.ybAssertView('в списке только один плательщик', '.yb-user-content', {
                    hideElements: ['.yb-paystep-preview', '.yb-paystep__right']
                });
                await browser.click('.yb-user-popup__btn-close');
                await browser.click('.yb-paystep-main__pay-method');
                await browser.click('#yamoney_wallet');
                await browser.click('[type^=submit]');
                await browser.waitForVisible('img[alt="Waiting for data"]');
                await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);
                await browser.ybAssertView(
                    'страница, изменен способ оплаты через paystep',
                    '.yb-user-content',
                    assertViewOpts
                );
            });
            it('изменение cпособа оплаты на paystep под админом', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                const [client_id, id] = await browser.ybRun(
                    'test_prepayment_unpaid_invoice_with_card_paysys',
                    {
                        login
                    }
                );
                const person_id = await browser.ybRun('test_create_ph_replace_person', {
                    client_id
                });
                await browser.ybUrl('user', 'paystep.xml?invoice_id=' + id);
                await browser.ybWaitForLoad();
                await browser.ybWaitForInvisible('.yb-paystep-preload');
                await browser.ybWaitForInvisible('.yb-preload_isLoading');
                await browser.ybSetSteps(`Открываем список плательщиков`);
                await browser.click('.yb-paystep-main__person button');

                await browser.ybWaitForLoad();
                await browser.ybAssertView('в списке несколько плательщиков', '.yb-user-content', {
                    hideElements: ['.yb-paystep-preview', '.yb-paystep__right']
                });
                await browser.click('.yb-paystep-main-persons-list-person_id_' + person_id);
                await browser.click('.yb-paystep-main__pay-method');
                await browser.click('#yamoney_wallet');
                await browser.click('[type^=submit]');
                await browser.waitForVisible('img[alt="Waiting for data"]');
                await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);
                await browser.ybAssertView(
                    'страница, изменен способ оплаты и плательщик через paystep',
                    '.yb-user-content',
                    assertViewOpts
                );
            });
        });
    });
});
