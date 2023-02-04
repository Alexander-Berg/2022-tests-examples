const { hideElements, ignoreElements } = require('./elements');

describe('user', () => {
    describe('paystep', () => {
        describe('old', () => {
            it('постоплатный счет, старый ЛС Yandex_Inc', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({});
                const [
                    ,
                    person_id,
                    contract_id,
                    request_id
                ] = await browser.ybRun('test_request_old_pa_Yandex_Inc', { login });
                await browser.ybUrl('user', `paychoose.xml?request_id=${request_id}`);

                await browser.ybWaitForLoad();

                await browser.click('input[id="is_ur_1"]');
                await browser.click('input[id="paysys_id_1028"]');
                await browser.click(`input[id="person_id_p${person_id}_${contract_id}c"]`);
                await browser.ybAssertView(
                    'paychoose постоплатный счет, старый ЛС Yandex_Inc',
                    '.yb-user-content',
                    { hideElements }
                );

                await browser.click('input[id="sub"]');
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'paypreview постоплатный счет, старый ЛС Yandex_Inc',
                    '.yb-user-content',
                    { hideElements }
                );

                await browser.click('input[value="Пополнить сейчас, оплатить позже"]');
                await browser.waitForVisible('#invoice_consumes-list-container .show-order');
                await browser.waitForVisible('#invoice_acts_data div');
                await browser.ybAssertView(
                    'success -> invoice постоплатный счет, старый ЛС Yandex_Inc',
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
            it('постоплатный счет, старый ЛС Yandex_Europe_AG', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({});
                const [
                    ,
                    person_id,
                    contract_id,
                    request_id
                ] = await browser.ybRun('test_request_old_pa_Yandex_Europe_AG', { login });
                await browser.ybUrl('user', `paychoose.xml?request_id=${request_id}`);

                await browser.ybWaitForLoad();

                await browser.click('input[id="is_ur_1"]');
                await browser.click('input[id="paysys_id_1044"]');
                await browser.click(`input[id="person_id_p${person_id}_${contract_id}c"]`);
                await browser.ybAssertView(
                    'paychoose постоплатный счет, старый ЛС Yandex_Europe_AG',
                    '.yb-user-content',
                    { hideElements }
                );

                await browser.click('input[id="sub"]');
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'paypreview постоплатный счет, старый ЛС Yandex_Europe_AG',
                    '.yb-user-content',
                    { hideElements }
                );

                await browser.click('input[value="Пополнить сейчас, оплатить позже"]');
                await browser.waitForVisible('#invoice_consumes-list-container .show-order');
                await browser.waitForVisible('#invoice_acts_data div');
                await browser.ybAssertView(
                    'success -> invoice постоплатный счет, старый ЛС Yandex_Europe_AG',
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
            it('постоплатный счет, старый ЛС сумма больше доступного кредита + задолжненность', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({});
                const [
                    ,
                    person_id,
                    contract_id,
                    ,
                    request_id_2
                ] = await browser.ybRun('test_request_old_pa_big_sum_and_debt', { login });
                await browser.ybUrl('user', `paychoose.xml?request_id=${request_id_2}`);

                await browser.ybWaitForLoad();

                await browser.click('input[id="is_ur_1"]');
                await browser.click('input[id="paysys_id_1044"]');
                await browser.click(`input[id="person_id_p${person_id}_${contract_id}c"]`);
                await browser.ybAssertView(
                    'paychoose постоплатный счет, старый ЛС сумма больше доступного кредита + задолжненность',
                    '.yb-user-content',
                    { hideElements }
                );

                await browser.click('input[id="sub"]');
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'paypreview постоплатный счет, старый ЛС сумма больше доступного кредита + задолжненность',
                    '.yb-user-content',
                    { hideElements: [...hideElements, 'div:nth-child(2) > div.blc_content'] }
                );
            });
            it('постоплатный счет, старый ЛС почти просроченная задолжность', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({});

                const [
                    ,
                    ,
                    request_id_2
                ] = await browser.ybRun('test_request_old_pa_almost_overdue_debt', { login });
                await browser.ybUrl('user', `paypreview.xml?request_id=${request_id_2}`);

                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'paypreview постоплатный счет, старый ЛС почти просроченная задолжность',
                    '.yb-user-content',
                    { hideElements: [...hideElements, 'div:nth-child(2) > div.blc_content'] }
                );

                await browser.click('input[value="Пополнить сейчас, оплатить позже"]');
                await browser.waitForVisible('#invoice_consumes-list-container .show-order');
                await browser.ybAssertView(
                    'success -> invoice постоплатный счет, старый ЛС почти просроченная задолжность',
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
                            'td.t-act',
                            'tr:nth-child(4) > td.l-success__r > strong'
                        ]
                    }
                );
            });
            it('постоплатный счет, старый ЛС просроченная задолжность', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({});

                const [, , request_id_2] = await browser.ybRun('test_request_old_pa_overdue_debt', {
                    login
                });
                await browser.ybUrl('user', `paypreview.xml?request_id=${request_id_2}`);

                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'paypreview постоплатный счет, старый ЛС просроченная задолжность',
                    '.yb-user-content',
                    { hideElements: [...hideElements, 'div:nth-child(2) > div.blc_content'] }
                );
            });
            it('Квитанции. Выставление счета, предоплата, Такси [smoke]', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({});
                const [
                    ,
                    person_id,
                    contract_id,
                    request_id
                ] = await browser.ybRun('test_request_old_pa_taxi', { login });
                await browser.ybUrl('user', `paychoose.xml?request_id=${request_id}`);

                await browser.ybWaitForLoad();

                await browser.click('input[id="paysys_id_1301003"]');
                await browser.ybAssertView('paychoose предоплата, Такси', '.yb-user-content', {
                    hideElements: [...hideElements, `#lab_person_id_p${person_id}_${contract_id}c`]
                });

                await browser.click('input[id="sub"]');
                await browser.ybWaitForLoad();
                await browser.ybAssertView('paypreview предоплата, Такси', '.yb-user-content', {
                    hideElements: [...hideElements, 'div:nth-child(2) > div.blc_content']
                });

                await browser.click('input[id="gensub"]');
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'success -> invoice предоплата, Такси',
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
                            'tr:nth-child(4) > td.l-success__r > strong',
                            'tr:nth-child(4) > td.l-success__r > strong'
                        ]
                    }
                );
            });
            it('Квитанции. Выставление счета, предоплата, Заправки', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn({});
                const [
                    ,
                    person_id,
                    contract_id,
                    request_id
                ] = await browser.ybRun('test_request_old_pa_zapravki', { login });
                await browser.ybUrl('user', `paychoose.xml?request_id=${request_id}`);

                await browser.ybWaitForLoad();

                await browser.ybAssertView('paychoose предоплата, Заправки', '.yb-user-content', {
                    hideElements: [...hideElements, `#lab_person_id_p${person_id}_${contract_id}c`]
                });

                await browser.click('input[id="sub"]');
                await browser.ybWaitForLoad();
                await browser.ybAssertView('paypreview предоплата, Заправки', '.yb-user-content', {
                    hideElements: [...hideElements, 'div:nth-child(2) > div.blc_content']
                });

                await browser.click('input[id="gensub"]');
                await browser.ybWaitForLoad();
                await browser.ybAssertView(
                    'success -> invoice предоплата, Заправки',
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
                            'tr:nth-child(4) > td.l-success__r > strong'
                        ]
                    }
                );
            });
        });
    });
});
