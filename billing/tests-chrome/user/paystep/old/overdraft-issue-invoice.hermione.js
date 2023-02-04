const { hideElements, ignoreElements } = require('./elements');
const { Roles } = require('../../../../helpers/role_perm');

describe('user', () => {
    describe('paystep', () => {
        describe('old', () => {
            it('Выставление овердрафтного счета на физ.лицо, под админом [smoke]', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({
                    baseRole: Roles.Support,
                    include: [],
                    exclude: []
                });
                const [, request_id] = await browser.ybRun('test_request_ph_with_overdraft', {
                    login
                });
                await browser.ybUrl('user', `paychoose.xml?request_id=${request_id}`);

                await browser.click('input[id="is_ur_0"]');
                await browser.click('input[id="paysys_id_1001"]');
                await browser.click('input[id="sub"]');
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'paypreview оведрафтный без договора ФЛ',
                    '.yb-user-content',
                    { hideElements: [hideElements, '.b-payments-person__name__label'] }
                );

                await browser.click('#b-deferred-payment__confirm');
                await browser.click('input[id="oversub"]');
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'success оведрафтный без договора ФЛ',
                    '.yb-user-content',
                    {
                        hideElements: [
                            hideElements,
                            '.b-payments-person__name__label',
                            '.b-page-title',
                            'tr:nth-child(3) td.l-success__r'
                        ]
                    }
                );
            });
            it('Оповещение о просроченной задолженности', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({
                    baseRole: Roles.Support,
                    include: [],
                    exclude: []
                });
                const [client_id, request_id, ,] = await browser.ybRun(
                    'test_overdraft_overdue_invoice_request'
                );
                await browser.ybUrl('user', `paypreview.xml?request_id=${request_id}`);

                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'paypreview оведрафтный просроченная задолженность',
                    '.yb-user-content',
                    { hideElements: [hideElements, '.b-payments-person__name__label'] }
                );

                await browser.click(
                    '.b-alert-message__summ',
                    'overdrafts-popup.xml?client_id=${client_id}'
                );
                await browser.ybUrl('user', `overdrafts-popup.xml?client_id=${client_id}`);
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'Окошко отсроченных платежей, просроченная задолженность ',
                    '.l-page-c',
                    { hideElements: [hideElements, '.b-payments__td b-payments__nowrap'] }
                );
            });

            it('Выставление овердрафтного счета, юр.лицо карта под админом + почти просроченная задолженность', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({
                    baseRole: Roles.Support,
                    include: [],
                    exclude: []
                });
                const [, , request_id_2] = await browser.ybRun(
                    'test_almost_overdue_overdraft_invoice_request'
                );
                await browser.ybUrl('user', `paychoose.xml?request_id=${request_id_2}`);
                await browser.click('input[id="is_ur_1"]');
                await browser.click('input[id="paysys_id_1033"]');
                await browser.click('input[id="sub"]');
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'paypreview оведрафтный почти просроченная задолженность',
                    '.yb-user-content',
                    { hideElements: [hideElements, '.b-payments-person__name__label'] }
                );

                await browser.click('#b-deferred-payment__confirm');
                await browser.click('input[id="oversub"]');
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'success овердрафтный без договора ЮЛ карта',
                    '.yb-user-content',
                    {
                        hideElements: [
                            hideElements,
                            '.b-payments-person__name__label',
                            '.b-page-title',
                            'tr:nth-child(3) td.l-success__r'
                        ]
                    }
                );
            });
            it('Выставление овердрафтного счета, юр.лицо, банк, под админом + есть задолженность', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({
                    baseRole: Roles.Support,
                    include: [],
                    exclude: []
                });
                const [, , request_id_2] = await browser.ybRun('test_overdraft_invoice_request');
                await browser.ybUrl('user', `paypreview.xml?request_id=${request_id_2}`);
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'paypreview оведрафтный есть задолженность',
                    '.yb-user-content',
                    { hideElements: [hideElements, '.b-payments-person__name__label'] }
                );

                await browser.click('#b-deferred-payment__confirm');
                await browser.click('input[id="oversub"]');
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'success овердрафтный без договора ЮЛ банк',
                    '.yb-user-content',
                    {
                        hideElements: [
                            hideElements,
                            '.b-payments-person__name__label',
                            '.b-page-title',
                            'tr:nth-child(3) td.l-success__r'
                        ]
                    }
                );
            });
            it('Выставление овердрафтного счета, превышен лимит овердрафта', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({
                    baseRole: Roles.Support,
                    include: [],
                    exclude: []
                });
                const [, request_id] = await browser.ybRun(
                    'test_request_ph_with_overdraft_under_limit'
                );
                await browser.ybUrl('user', `paychoose.xml?request_id=${request_id}`);

                await browser.click('input[id="is_ur_1"]');
                await browser.click('input[id="sub"]');
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'paypreview оведрафтный превышен лимит',
                    '.yb-user-content',
                    { hideElements: [hideElements, '.b-payments-person__name__label'] }
                );
            });
        });
    });
});
