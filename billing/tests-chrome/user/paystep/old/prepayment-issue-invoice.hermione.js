const { hideElements, ignoreElements } = require('./elements');
const { Roles } = require('../../../../helpers/role_perm');

describe('user', () => {
    describe('paystep', () => {
        describe('old', () => {
            it('предоплатный счет без договора выставление под клиентом [smoke]', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({});
                const [
                    ,
                    ,
                    service_order_id,
                    ,
                    request_id
                ] = await browser.ybRun('test_client_empty_order_no_person', { login });
                await browser.ybUrl('user', `paychoose.xml?request_id=${request_id}`);

                await browser.ybWaitForLoad();

                await browser.click('input[id="is_ur_0"]');
                await browser.ybAssertView(
                    'paychoose предоплатный без договора способы оплаты ФЛ [smoke]',
                    '.yb-user-content',
                    { hideElements }
                );
                await browser.ybAssertLink(
                    `a=7-${service_order_id}`,
                    `/order.xml?service_cc=PPC&service_order_id=${service_order_id}`
                );

                await browser.click('input[id="is_ur_1"]');
                await browser.click('input[id="paysys_id_1003"]');
                await browser.setValue('input[id="inn"]', '7723886841');
                await browser.waitForVisible('.suggestions-suggestions');
                await browser.click('.suggestions-suggestions');
                await browser.setValue('input[id="phone"]', '123123123');
                await browser.setValue('input[id="email"]', 'woop@woop.woop');
                await browser.setValue('input[id="city"]', 'Москва');
                await browser.waitForVisible('.ac_over');
                await browser.click('.ac_over');
                await browser.setValue('input[id="postcode-simple"]', '123123');
                await browser.setValue('input[id="postbox"]', 'какой-то текст');
                await browser.ybAssertView(
                    'paychoose заполненная форма ЮЛ [smoke]',
                    '.l-payments-choose tr:nth-child(5)',
                    { hideElements: [hideElements, '#envelope-address'] }
                );
                await browser.click('input[id="sub"]');
                await browser.waitForVisible('.blc_change_payment_method_or_person_button');
                await browser.ybAssertView(
                    'paypreview предоплатный без договора ЮЛ [smoke]',
                    '.yb-user-content',
                    { hideElements }
                );

                await browser.click('input[id="gensub"]');
                await browser.waitForVisible('div[id="details_div_small"]');
                await browser.click('div[id="details_div_small"]');
                await browser.ybAssertView(
                    'success предоплатный без договора ФЛ [smoke]',
                    '.yb-user-content',
                    {
                        hideElements: [
                            ...hideElements,
                            '.b-page-title',
                            '.b-payments-person__name__label'
                        ]
                    }
                );
            });

            it('предоплатный счет без договора выставление под админом + ограничение способов оплаты при больших суммах', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({
                    baseRole: Roles.Support,
                    include: [],
                    exclude: []
                });
                const [, request_id] = await browser.ybRun('test_client_ph_market_order_max_sum');
                await browser.ybUrl('user', `paychoose.xml?request_id=${request_id}`);

                await browser.ybWaitForLoad();

                await browser.click('input[id="is_ur_0"]');
                await browser.ybAssertView(
                    'paychoose предоплатный без договора ограничение способов оплаты ФЛ',
                    '.yb-user-content',
                    { hideElements: [hideElements, '.b-payments-person'] }
                );

                await browser.click('input[id="sub"]');
                await browser.waitForVisible('.blc_change_payment_method_or_person_button');
                await browser.ybAssertView(
                    'paypreview предоплатный без договора ФЛ, большая сумма',
                    '.yb-user-content',
                    { hideElements: [hideElements, '.b-payments-person__name__label'] }
                );

                await browser.click('input[id="gensub"]');
                await browser.waitForVisible('div[id="details_div_small"]');
                await browser.click('div[id="details_div_small"]');
                await browser.ybAssertView(
                    'success предоплатный без договора ФЛ, большая сумма',
                    '.yb-user-content',
                    {
                        hideElements: [
                            ...hideElements,
                            '.b-page-title',
                            '.b-payments-person__name__label'
                        ]
                    }
                );
            });

            it('предоплатный счет с договором выставление под клиентом + проверка реквизитов', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({});
                const [, request_id, person_id, contract_id] = await browser.ybRun(
                    'test_request_with_contract_client',
                    {
                        login
                    }
                );
                await browser.ybUrl('user', `paychoose.xml?request_id=${request_id}`);

                await browser.ybWaitForLoad();

                await browser.click('input[id="is_ur_1"]');
                await browser.ybAssertView(
                    'paychoose предоплатный с договором способы оплаты ЮЛ',
                    '.yb-user-content',
                    { hideElements }
                );

                await browser.click(`input[id='person_id_p${person_id}_${contract_id}c']`);
                await browser.click('input[id="sub"]');
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'paypreview предоплатный с договором',
                    '.yb-user-content',
                    {
                        hideElements
                    }
                );

                await browser.click('input[id="gensub"]');
                await browser.ybWaitForLoad();
                await browser.ybAssertView('success предоплатный с договором', '.yb-user-content', {
                    hideElements: [...hideElements, '.b-page-title']
                });

                await browser.click('span.b-link_inner');
                await browser.click('span.b-payments-person__data-link');
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'проверка подробностей счета и реквизитов плательщика',
                    '.yb-user-content',
                    {
                        hideElements: [...hideElements, '.b-page-title']
                    }
                );
            });

            it('агентство без договора', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                const [, request_id] = await browser.ybRun(
                    'test_request_for_agency_no_contract_with_nonrez'
                );
                await browser.ybUrl('user', `paychoose.xml?request_id=${request_id}`);

                await browser.ybWaitForLoad();

                await browser.ybAssertView(
                    'paychoose для агентства без договора',
                    '.yb-user-content',
                    {
                        hideElements
                    }
                );
            });

            it('предоплатный счет, выставление под менеджером по карте + смена способа оплаты', async function () {
                const { browser } = this;

                await browser.ybSignIn({ manager: true });
                const [, request_id, person_id, contract_id, person_id_new] = await browser.ybRun(
                    'test_request_with_contract_client'
                );
                await browser.ybUrl('user', `paychoose.xml?request_id=${request_id}`);

                await browser.ybWaitForLoad();

                await browser.click('input[id="is_ur_1"]');
                await browser.click('input[id="paysys_id_1033"]');
                await browser.click(`input[id="person_id_p${person_id}_${contract_id}c"]`);
                await browser.ybAssertView(
                    'paychoose предоплатный с договором, менеджер',
                    '.yb-user-content',
                    { hideElements }
                );

                await browser.click('input[id="sub"]');
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'paypreview предоплатный с договором, менеджер',
                    '.yb-user-content',
                    { hideElements }
                );

                await browser.click('input[id="gensub"]');
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'success предоплатный с договором, менеджер',
                    '.yb-user-content',
                    { hideElements: [...hideElements, '.b-page-title'] }
                );

                await browser.click('a=изменить способ оплаты');
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'paychoose изменение способа оплаты и плательщика, менеджер',
                    '.yb-user-content',
                    { hideElements }
                );

                await browser.click('input[id="paysys_id_1003"]');
                await browser.click(`input[id="person_id_p${person_id_new}_c"]`);
                await browser.click('input[id="sub"]');
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'paypreview измененный способ оплаты и плательщик, менеджер',
                    '.yb-user-content',
                    { hideElements }
                );

                await browser.click('input[id="gensub"]');
                await browser.ybWaitForInvisible('img[alt="Waiting for data"]');
                await browser.waitForVisible('#invoice_acts_data div');
                await browser.ybAssertView(
                    'success -> invoice измененный способ оплаты и плательщик, менеджер',
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

            it('Невозможность выставления под бухлогином', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({});
                const [client_id, request_id] = await browser.ybRun('test_request_market_with_ph');
                await browser.ybRun('test_unlink_client', { login });
                await browser.ybRun('test_delete_every_accountant_role', { login });
                await browser.ybRun('test_add_accountant_role', { login, client_id });
                await browser.ybUrl('user', `paypreview.xml?request_id=${request_id}`);
                await browser.waitForVisible('.error');
                await browser.ybRun('test_delete_every_accountant_role', { login });
            });
        });
    });
});
