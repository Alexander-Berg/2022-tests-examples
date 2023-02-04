const { waitUntilTimeout } = require('../../../../helpers');
const { hideElements, ignoreElements } = require('./elements');
const { Roles } = require('../../../../helpers/role_perm');

describe('user', () => {
    describe('paystep', () => {
        describe('old', () => {
            it('постоплатный счет, новый ЛС', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({});
                const [
                    ,
                    person_id,
                    contract_id,
                    request_id
                ] = await browser.ybRun('test_request_fictive_pa', { login });
                await browser.ybUrl('user', `paychoose.xml?request_id=${request_id}`);

                await browser.ybWaitForLoad();

                await browser.click('input[id="is_ur_0"]');
                await browser.click('input[id="paysys_id_1002"]');
                await browser.click(`input[id="person_id_p${person_id}_${contract_id}c"]`);
                await browser.ybAssertView(
                    'paychoose постоплатный счет, новый ЛС',
                    '.yb-user-content',
                    { hideElements }
                );

                await browser.click('input[id="sub"]');
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'paypreview постоплатный счет, новый ЛС',
                    '.yb-user-content',
                    { hideElements }
                );

                await browser.click('input[value="Пополнить сейчас, оплатить позже"]');
                await browser.waitForVisible('#invoice_consumes-list-container .show-order');
                await browser.waitForVisible('#invoice_acts_data div');
                await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);
                await browser.ybAssertView(
                    'success -> invoice постоплатный счет, новый ЛС',
                    '.yb-user-content',
                    {
                        hideElements: [
                            ...hideElements,
                            '.b-page-title',
                            '.show-order',
                            '.oper-details',
                            '.report .date',
                            '#invoice_consumes-list-container .date',
                            '.yb-user-content div:nth-child(12) .date',
                            'h1'
                        ]
                    }
                );
            });
            it('счет на погашение, новый ЛС', async function () {
                const { browser } = this;

                await browser.ybSignIn({
                    baseRole: Roles.Support,
                    include: [],
                    exclude: []
                });
                const [, , y_invoice_id] = await browser.ybRun('test_fictive_pa_y_invoice');
                await browser.ybUrl('user', `invoice.xml?invoice_id=${y_invoice_id}`);

                await browser.ybWaitForLoad();
                await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);

                await browser.ybAssertView('счет на погашение, новый ЛС', '.yb-user-content', {
                    hideElements: [
                        ...hideElements,
                        '.b-page-title',
                        '.t-act',
                        '.show-order',
                        '.oper-details',
                        '.report .date',
                        '#invoice_consumes-list-container .date',
                        '.yb-user-content div:nth-child(12) .date',
                        'h1',
                        '#old-content > table > tbody > tr > td > div:nth-child(17) > form > div:nth-child(9)'
                    ]
                });
            });

            it('постоплатный счет, новый ЛС, есть задолженность [smoke]', async function () {
                const { browser } = this;

                await browser.ybSignIn({
                    baseRole: Roles.Support,
                    include: [],
                    exclude: []
                });
                const [, , , , request_id_2] = await browser.ybRun('test_request_fictive_pa_debt');
                await browser.ybUrl('user', `paypreview.xml?request_id=${request_id_2}`);

                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'paypreview постоплатный счет, новый ЛС, есть задолженность',
                    '.yb-user-content',
                    { hideElements }
                );

                await browser.click('input[value="Пополнить сейчас, оплатить позже"]');
                await browser.waitForVisible('#invoice_consumes-list-container .show-order');
                await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);
                await browser.ybAssertView(
                    'success -> выствление предоплатного счета, когда есть задолженность',
                    '.yb-user-content',
                    {
                        hideElements: [
                            '.b-page-title',
                            '.show-order',
                            '.oper-details',
                            '.report .date',
                            '#invoice_consumes-list-container .date',
                            '.yb-user-content div:nth-child(12) .date',
                            'h1',
                            'td.t-act'
                        ]
                    }
                );
            });
            it('постоплатный счет, новый ЛС, есть почти просроченная задолженность', async function () {
                const { browser } = this;

                await browser.ybSignIn({
                    baseRole: Roles.Support,
                    include: [],
                    exclude: []
                });
                const [, , request_id_2] = await browser.ybRun(
                    'test_request_fictive_pa_almost_overdue_debt'
                );
                await browser.ybUrl('user', `paypreview.xml?request_id=${request_id_2}`);

                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'paypreview постоплатный счет, новый ЛС, почти просроченная задолженность',
                    '.yb-user-content',
                    { hideElements }
                );

                await browser.click('input[value="Пополнить сейчас, оплатить позже"]');
                await browser.waitForVisible('#invoice_consumes-list-container .show-order');
                await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);
                await browser.ybAssertView(
                    'success -> выствление предоплатного счета, когда есть почти просроченная задолженность',
                    '.yb-user-content',
                    {
                        hideElements: [
                            '.b-page-title',
                            '.show-order',
                            '.oper-details',
                            '.report .date',
                            '#invoice_consumes-list-container .date',
                            '.yb-user-content div:nth-child(12) .date',
                            'h1',
                            'td.t-act'
                        ]
                    }
                );
            });
            it('постоплатный счет, новый ЛС, есть просроченная задолженность', async function () {
                const { browser } = this;

                await browser.ybSignIn({
                    baseRole: Roles.Support,
                    include: [],
                    exclude: []
                });
                const [, , request_id_2] = await browser.ybRun(
                    'test_request_fictive_pa_overdue_debt'
                );
                await browser.ybUrl('user', `paypreview.xml?request_id=${request_id_2}`);

                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'paypreview постоплатный счет, новый ЛС, просроченная задолженность',
                    '.yb-user-content',
                    { hideElements }
                );

                await browser.click('input[id="gensub"]');
                await browser.ybWaitForInvisible('img[alt="Waiting for data"]');
                await browser.ybAssertView(
                    'success -> выствление предоплатного счета, когда есть просроченная задолженность',
                    '.yb-user-content',
                    {
                        hideElements: [
                            ...hideElements,
                            '.b-page-title',
                            '.show-order',
                            '.oper-details',
                            '.report .date',
                            '#invoice_consumes-list-container .date',
                            '.yb-user-content div:nth-child(12) .date',
                            'h1',
                            'form[action="send-invoice.xml"] > div',
                            '.b-payment-details'
                        ]
                    }
                );
            });
        });
    });
});
