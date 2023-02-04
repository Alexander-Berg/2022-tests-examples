const { elements, hideElements } = require('./elements');

const { waitUntilTimeout, submitAndWaitPopUp } = require('./helpers');

describe('user', () => {
    describe('paystep', () => {
        describe('new', () => {
            describe('promocode', () => {
                it('paystep, фиксированная скидка, количество, банк', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});
                    const [
                        client_id,
                        ,
                        ,
                        ,
                        request_id
                    ] = await browser.ybRun('test_client_empty_order_with_ph', { login });

                    const promocode = await browser.ybRun('test_fix_discount_amount_promo');

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.payMethod);
                    await browser.click(elements.menu.bank);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.promocode);

                    await browser.waitForVisible(elements.popup);

                    await browser.setValue(
                        '.yb-paystep-main-promocode-popup__input input',
                        promocode
                    );

                    await browser.click('.yb-paystep-main-promocode-popup__input button');

                    await browser.ybWaitForInvisible(elements.popup);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep, фиксированная скидка, количество, банк, страница актив. промокод',
                        elements.page,
                        {
                            hideElements: [...hideElements, '.yb-paystep-main-promocode']
                        }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep, фиксированная скидка, количество, банк, модальное окно',
                        elements.page,
                        {
                            hideElements: [
                                ...hideElements,
                                '.yb-paystep-main-promocode',
                                '.yb-success-header__id'
                            ]
                        }
                    );

                    const invoice_id = await browser.ybRun('test_pay_invoice', { client_id });
                    await browser.ybUrl('user', `invoice.xml?invoice_id=${invoice_id}`);

                    await browser.ybWaitForInvisible(
                        'img[alt="Waiting for data"]',
                        waitUntilTimeout
                    );

                    await browser.ybAssertView(
                        'invoice, фиксированная скидка, количество, банк, зачисления на заказы',
                        'div.sub:nth-last-child(6)',
                        { hideElements: ['.show-order', '.date'] }
                    );

                    await browser.ybAssertView(
                        'invoice, фиксированная скидка, количество, банк, операции',
                        'div.sub:nth-last-child(2)',
                        {
                            hideElements: ['.show-order', '.date', 'a']
                        }
                    );
                });

                it('paystep, фиксированная скидка, сумма, банк', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});
                    const [client_id, , , , request_id] = await browser.ybRun(
                        'test_client_3_orders_with_ur',
                        {
                            login
                        }
                    );

                    const promocode = await browser.ybRun('test_fix_discount_sum_promo');

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.payMethod);
                    await browser.click(elements.menu.bank);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.promocode);

                    await browser.waitForVisible(elements.popup);

                    await browser.setValue(
                        '.yb-paystep-main-promocode-popup__input input',
                        promocode
                    );

                    await browser.click('.yb-paystep-main-promocode-popup__input button');

                    await browser.ybWaitForInvisible(elements.popup);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep, фиксированная скидка, сумма, банк, страница актив. промокод',
                        elements.page,
                        {
                            hideElements: [...hideElements, '.yb-paystep-main-promocode']
                        }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep, фиксированная скидка, сумма, банк, модальное окно',
                        elements.page,
                        {
                            hideElements: [
                                ...hideElements,
                                '.yb-paystep-main-promocode',
                                '.yb-success-header__id'
                            ]
                        }
                    );

                    const invoice_id = await browser.ybRun('test_pay_invoice', { client_id });
                    await browser.ybUrl('user', `invoice.xml?invoice_id=${invoice_id}`);

                    await browser.ybWaitForInvisible(
                        'img[alt="Waiting for data"]',
                        waitUntilTimeout
                    );

                    await browser.ybAssertView(
                        'invoice, фиксированная скидка, сумма, банк, подробности по счету',
                        'div.sub:nth-last-child(7)',
                        { hideElements: ['.show-order'] }
                    );

                    await browser.ybAssertView(
                        'invoice, фиксированная скидка, сумма, банк, зачисления на заказы',
                        'div.sub:nth-last-child(6)',
                        { hideElements: ['.show-order', '.date'] }
                    );

                    await browser.ybAssertView(
                        'invoice, фиксированная скидка, сумма, банк, операции',
                        'div.sub:nth-last-child(2)',
                        {
                            hideElements: ['.show-order', '.date', 'a']
                        }
                    );
                });

                it('paystep, бонус в количестве, увеличение количества в строках счета, банк', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});
                    const [
                        client_id,
                        ,
                        ,
                        ,
                        request_id
                    ] = await browser.ybRun('test_client_empty_order_with_ph', { login });

                    const promocode = await browser.ybRun(
                        'test_qty_bonus_increase_qty_invoice_promo'
                    );

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.payMethod);
                    await browser.click(elements.menu.bank);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.promocode);

                    await browser.waitForVisible(elements.popup);

                    await browser.setValue(
                        '.yb-paystep-main-promocode-popup__input input',
                        promocode
                    );

                    await browser.click('.yb-paystep-main-promocode-popup__input button');

                    await browser.ybWaitForInvisible(elements.popup);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep, бонус в количестве, увеличение количества в строках счета, банк, страница актив. промокод',
                        elements.page,
                        {
                            hideElements: [...hideElements, '.yb-paystep-main-promocode']
                        }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep, бонус в количестве, увеличение количества в строках счета, банк, модальное окно',
                        elements.page,
                        {
                            hideElements: [
                                ...hideElements,
                                '.yb-paystep-main-promocode',
                                '.yb-success-header__id'
                            ]
                        }
                    );

                    const invoice_id = await browser.ybRun('test_pay_invoice', { client_id });
                    await browser.ybUrl('user', `invoice.xml?invoice_id=${invoice_id}`);

                    await browser.ybWaitForInvisible(
                        'img[alt="Waiting for data"]',
                        waitUntilTimeout
                    );

                    await browser.ybAssertView(
                        'invoice, бонус в количестве, увеличение количества в строках счета, подробности по счету',
                        'div.sub:nth-last-child(7)',
                        { hideElements: ['.show-order'] }
                    );

                    await browser.ybAssertView(
                        'invoice, бонус в количестве, увеличение количества в строках счета, зачисления на заказы',
                        'div.sub:nth-last-child(6)',
                        { hideElements: ['.show-order', '.date'] }
                    );

                    await browser.ybAssertView(
                        'invoice, бонус в количестве, увеличение количества в строках счета, операции',
                        'div.sub:nth-last-child(2)',
                        {
                            hideElements: ['.show-order', '.date', 'a']
                        }
                    );
                });

                it('paystep, бонус в количестве, уменьшение суммы в строках счета, банк', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});
                    const [client_id, , , , request_id] = await browser.ybRun(
                        'test_client_3_orders_with_ph',
                        {
                            login
                        }
                    );

                    const promocode = await browser.ybRun(
                        'test_qty_bonus_decrease_sum_invoice_promo'
                    );

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.payMethod);
                    await browser.click(elements.menu.bank);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.promocode);

                    await browser.waitForVisible(elements.popup);

                    await browser.setValue(
                        '.yb-paystep-main-promocode-popup__input input',
                        promocode
                    );

                    await browser.click('.yb-paystep-main-promocode-popup__input button');

                    await browser.ybWaitForInvisible(elements.popup);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep, бонус в количестве, уменьшение суммы в строках счета, банк, страница актив. промокод',
                        elements.page,
                        {
                            hideElements: [...hideElements, '.yb-paystep-main-promocode']
                        }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep, бонус в количестве, уменьшение суммы в строках счета, банк, модальное окно',
                        elements.page,
                        {
                            hideElements: [
                                ...hideElements,
                                '.yb-paystep-main-promocode',
                                '.yb-success-header__id'
                            ]
                        }
                    );

                    const invoice_id = await browser.ybRun('test_pay_invoice', { client_id });
                    await browser.ybUrl('user', `invoice.xml?invoice_id=${invoice_id}`);

                    await browser.ybWaitForInvisible(
                        'img[alt="Waiting for data"]',
                        waitUntilTimeout
                    );

                    await browser.ybAssertView(
                        'invoice, бонус в количестве, уменьшение суммы в строках счета, банк, подробности по счету',
                        'div.sub:nth-last-child(7)',
                        { hideElements: ['.show-order'] }
                    );

                    await browser.ybAssertView(
                        'invoice, бонус в количестве, уменьшение суммы в строках счета, банк, зачисления на заказы',
                        'div.sub:nth-last-child(6)',
                        { hideElements: ['.show-order', '.date'] }
                    );

                    await browser.ybAssertView(
                        'invoice, бонус в количестве, уменьшение суммы в строках счета, банк, операции',
                        'div.sub:nth-last-child(2)',
                        {
                            hideElements: ['.show-order', '.date', 'a']
                        }
                    );
                });

                it('paystep, бонус в количестве, увеличение количества в заявках, банк', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});
                    const [
                        client_id,
                        ,
                        ,
                        ,
                        request_id
                    ] = await browser.ybRun('test_client_empty_order_with_ph', { login });

                    const promocode = await browser.ybRun(
                        'test_qty_bonus_increase_qty_consume_promo'
                    );

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.payMethod);
                    await browser.click(elements.menu.bank);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.promocode);

                    await browser.waitForVisible(elements.popup);

                    await browser.setValue(
                        '.yb-paystep-main-promocode-popup__input input',
                        promocode
                    );

                    await browser.click('.yb-paystep-main-promocode-popup__input button');

                    await browser.ybWaitForInvisible(elements.popup);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep, бонус в количестве, увеличение количества в заявках, банк, страница актив. промокод',
                        elements.page,
                        {
                            hideElements: [...hideElements, '.yb-paystep-main-promocode']
                        }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep, бонус в количестве, увеличение количества в заявках, банк, модальное окно',
                        elements.page,
                        {
                            hideElements: [
                                ...hideElements,
                                '.yb-paystep-main-promocode',
                                '.yb-success-header__id'
                            ]
                        }
                    );

                    const invoice_id = await browser.ybRun('test_pay_invoice', { client_id });
                    await browser.ybUrl('user', `invoice.xml?invoice_id=${invoice_id}`);

                    await browser.ybWaitForInvisible(
                        'img[alt="Waiting for data"]',
                        waitUntilTimeout
                    );

                    await browser.ybAssertView(
                        'invoice, бонус в количестве, увеличение количества в заявках, банк, зачисления на заказы',
                        'div.sub:nth-last-child(6)',
                        { hideElements: ['.show-order', '.date'] }
                    );

                    await browser.ybAssertView(
                        'invoice, бонус в количестве, увеличение количества в заявках, банк, операции',
                        'div.sub:nth-last-child(2)',
                        {
                            hideElements: ['.show-order', '.date', 'a']
                        }
                    );
                });

                it('paystep, бонус в деньгах, увеличение количества в строках счета, банк', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});
                    const [client_id, , , , request_id] = await browser.ybRun(
                        'test_client_3_orders_with_ur',
                        {
                            login
                        }
                    );

                    const promocode = await browser.ybRun(
                        'test_money_bonus_increase_qty_invoice_promo'
                    );

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.payMethod);
                    await browser.click(elements.menu.bank);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.promocode);

                    await browser.waitForVisible(elements.popup);

                    await browser.setValue(
                        '.yb-paystep-main-promocode-popup__input input',
                        promocode
                    );

                    await browser.click('.yb-paystep-main-promocode-popup__input button');

                    await browser.ybWaitForInvisible(elements.popup);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep, бонус в деньгах, увеличение количества в строках счета, банк, страница актив. промокод',
                        elements.page,
                        {
                            hideElements: [...hideElements, '.yb-paystep-main-promocode']
                        }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep, бонус в деньгах, увеличение количества в строках счета, банк, модальное окно',
                        elements.page,
                        {
                            hideElements: [
                                ...hideElements,
                                '.yb-paystep-main-promocode',
                                '.yb-success-header__id'
                            ]
                        }
                    );

                    const invoice_id = await browser.ybRun('test_pay_invoice', { client_id });
                    await browser.ybUrl('user', `invoice.xml?invoice_id=${invoice_id}`);

                    await browser.ybWaitForInvisible(
                        'img[alt="Waiting for data"]',
                        waitUntilTimeout
                    );

                    await browser.ybAssertView(
                        'invoice, бонус в деньгах, увеличение количества в строках счета, банк, подробности по счету',
                        'div.sub:nth-last-child(7)',
                        { hideElements: ['.show-order'] }
                    );

                    await browser.ybAssertView(
                        'invoice, бонус в деньгах, увеличение количества в строках счета, банк, зачисления на заказы',
                        'div.sub:nth-last-child(6)',
                        { hideElements: ['.show-order', '.date'] }
                    );

                    await browser.ybAssertView(
                        'invoice, бонус в деньгах, увеличение количества в строках счета, банк, операции',
                        'div.sub:nth-last-child(2)',
                        {
                            hideElements: ['.show-order', '.date', 'a']
                        }
                    );
                });

                it('paystep, бонус в деньгах, уменьшение суммы в строках счета, банк', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});
                    const [
                        client_id,
                        ,
                        ,
                        ,
                        request_id
                    ] = await browser.ybRun('test_client_empty_order_with_ph', { login });

                    const promocode = await browser.ybRun(
                        'test_money_bonus_decrease_sum_invoice_promo'
                    );

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.payMethod);
                    await browser.click(elements.menu.bank);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.promocode);

                    await browser.waitForVisible(elements.popup);

                    await browser.setValue(
                        '.yb-paystep-main-promocode-popup__input input',
                        promocode
                    );

                    await browser.click('.yb-paystep-main-promocode-popup__input button');

                    await browser.ybWaitForInvisible(elements.popup);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep, бонус в деньгах, уменьшение суммы в строках счета, банк, страница актив. промокод',
                        elements.page,
                        {
                            hideElements: [...hideElements, '.yb-paystep-main-promocode']
                        }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep, бонус в деньгах, уменьшение суммы в строках счета, банк, модальное окно',
                        elements.page,
                        {
                            hideElements: [
                                ...hideElements,
                                '.yb-paystep-main-promocode',
                                '.yb-success-header__id'
                            ]
                        }
                    );

                    const invoice_id = await browser.ybRun('test_pay_invoice', { client_id });
                    await browser.ybUrl('user', `invoice.xml?invoice_id=${invoice_id}`);

                    await browser.ybWaitForInvisible(
                        'img[alt="Waiting for data"]',
                        waitUntilTimeout
                    );

                    await browser.ybAssertView(
                        'invoice, бонус в деньгах, уменьшение суммы в строках счета, банк, подробности по счету',
                        'div.sub:nth-last-child(7)',
                        { hideElements: ['.show-order'] }
                    );

                    await browser.ybAssertView(
                        'invoice, бонус в деньгах, уменьшение суммы в строках счета, банк, зачисления на заказы',
                        'div.sub:nth-last-child(6)',
                        { hideElements: ['.show-order', '.date'] }
                    );

                    await browser.ybAssertView(
                        'invoice, бонус в деньгах, уменьшение суммы в строках счета, банк, операции',
                        'div.sub:nth-last-child(2)',
                        {
                            hideElements: ['.show-order', '.date', 'a']
                        }
                    );
                });

                it('paystep, бонус в деньгах, увеличение количества в заявках, банк', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});
                    const [client_id, , , , request_id] = await browser.ybRun(
                        'test_client_3_orders_with_ph',
                        {
                            login
                        }
                    );

                    const promocode = await browser.ybRun(
                        'test_money_bonus_increase_qty_consume_promo'
                    );

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.payMethod);
                    await browser.click(elements.menu.bank);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.promocode);

                    await browser.waitForVisible(elements.popup);

                    await browser.setValue(
                        '.yb-paystep-main-promocode-popup__input input',
                        promocode
                    );

                    await browser.click('.yb-paystep-main-promocode-popup__input button');

                    await browser.ybWaitForInvisible(elements.popup);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep, бонус в деньгах, увеличение количества в заявках, банк, страница актив. промокод',
                        elements.page,
                        {
                            hideElements: [...hideElements, '.yb-paystep-main-promocode']
                        }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep, бонус в деньгах, увеличение количества в заявках, банк, модальное окно',
                        elements.page,
                        {
                            hideElements: [
                                ...hideElements,
                                '.yb-paystep-main-promocode',
                                '.yb-success-header__id'
                            ]
                        }
                    );

                    const invoice_id = await browser.ybRun('test_pay_invoice', { client_id });
                    await browser.ybUrl('user', `invoice.xml?invoice_id=${invoice_id}`);

                    await browser.ybWaitForInvisible(
                        'img[alt="Waiting for data"]',
                        waitUntilTimeout
                    );

                    await browser.ybAssertView(
                        'invoice, бонус в деньгах, увеличение количества в заявках, банк, зачисления на заказы',
                        'div.sub:nth-last-child(6)',
                        { hideElements: ['.show-order', '.date'] }
                    );

                    await browser.ybAssertView(
                        'invoice, бонус в деньгах, увеличение количества в заявках, банк, операции',
                        'div.sub:nth-last-child(2)',
                        {
                            hideElements: ['.show-order', '.date', 'a']
                        }
                    );
                });

                it('paystep, использованный промокод', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});
                    const [
                        ,
                        ,
                        ,
                        ,
                        request_id
                    ] = await browser.ybRun('test_client_empty_order_with_ph', { login });

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.promocode);

                    await browser.waitForVisible(elements.popup);

                    await browser.setValue(
                        '.yb-paystep-main-promocode-popup__input input',
                        'TESTRBHA3Z44XP3O'
                    );

                    await browser.click('.yb-paystep-main-promocode-popup__input button');
                    await browser.waitForVisible(elements.popupError);

                    await browser.ybAssertView('paystep, использованный промокод', elements.popup, {
                        hideElements: [...hideElements]
                    });
                });

                it('paystep, просроченный промокод', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});
                    const [
                        ,
                        ,
                        ,
                        ,
                        request_id
                    ] = await browser.ybRun('test_client_empty_order_with_ph', { login });

                    const promocode = await browser.ybRun('test_overdue_promo');

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.promocode);

                    await browser.waitForVisible(elements.popup);

                    await browser.setValue(
                        '.yb-paystep-main-promocode-popup__input input',
                        promocode
                    );

                    await browser.click('.yb-paystep-main-promocode-popup__input button');
                    await browser.waitForVisible(elements.popupError);

                    await browser.ybAssertView('paystep, просроченный промокод', elements.popup, {
                        hideElements: [...hideElements, '.yb-paystep-main-promocode-popup__input']
                    });
                });

                it('paystep, промокод для другой фирмы', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});
                    const [
                        ,
                        ,
                        ,
                        ,
                        request_id
                    ] = await browser.ybRun('test_client_empty_order_with_ph', { login });

                    const promocode = await browser.ybRun('test_kinopoisk_promo');

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.promocode);

                    await browser.waitForVisible(elements.popup);

                    await browser.setValue(
                        '.yb-paystep-main-promocode-popup__input input',
                        promocode
                    );

                    await browser.click('.yb-paystep-main-promocode-popup__input button');

                    await browser.ybWaitForInvisible(elements.popup);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);
                    await browser.waitForVisible(elements.popupNotification);

                    await browser.ybAssertView(
                        'paystep, промокод для другой фирмы',
                        elements.page,
                        {
                            hideElements: [...hideElements, '.yb-paystep-main-promocode']
                        }
                    );
                });
            });
        });
    });
});
