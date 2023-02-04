const { elements, hideElements } = require('./elements');
const { Roles, Perms } = require('../../../../helpers/role_perm');
const assert = require('chai').assert;

describe('user', () => {
    describe('paystep', () => {
        describe('new', () => {
            describe('overdraft', () => {
                it('выставление овердрафтного счета, под админом (на директ)', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const [, request_id] = await browser.ybRun('test_request_ph_with_overdraft', {
                        login
                    });
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep выставление овердрафтного счета, под админом (на директ) страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await browser.click(elements.mainButtons.tumbler);

                    await browser.ybAssertView(
                        'paystep выставление овердрафтного счета, под админом (на директ) боковое окно',
                        elements.right
                    );

                    await browser.click(elements.mainButtons.submit);
                    await browser.ybWaitAnimation('Modal-Content_theme_normal_visible');
                    await browser.waitForVisible(elements.popup);

                    await browser.ybAssertView(
                        'paystep выставление овердрафтного счета, под админом (на директ), модальное окно подтверждение',
                        elements.page,
                        {
                            hideElements: [
                                ...hideElements,
                                '.yb-cart-row__eid',
                                elements.mainButtons.submit
                            ]
                        }
                    );

                    await browser.click(elements.modal.payConfirmButton);
                    await browser.ybWaitForLoad();
                    await browser.waitForVisible(elements.popup);

                    await browser.ybAssertView(
                        'paystep выставление овердрафтного счета, под админом (на директ), модальное окно с success',
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

                it('Оповещение о просроченной задолженности, под админом', async function () {
                    const { browser } = this;

                    const [, request_id] = await browser.ybRun(
                        'test_overdraft_overdue_invoice_request'
                    );

                    await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep оповещение о просроченной задолженности, под админом, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    // получаем span внутри тумблера, чтобы  проверить его состояние
                    // aria-disabled ? 'всё правильно': 'почему-то тумблер доступен'
                    const tumblerSpan = await browser.$('.yb-paystep-main__tumbler span');
                    const attr = await tumblerSpan.getAttribute('aria-disabled');
                    await browser.ybSetSteps('Проверяет, что тумблер недоступен');
                    assert(attr, 'Проверка должен быть недоступен');

                    await browser.click('.yb-debt-rows button');

                    await browser.ybAssertView(
                        'paystep оповещение о просроченной задолженности, под админом, просмотр задолженности',
                        '.yb-debt-rows__content',
                        { hideElements: [...hideElements, '.yb-cart-row__eid'] }
                    );
                });

                it('Выставление овердрафтного счета, юр.лицо, банк, под админом + есть задолженность (на директ)', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const [, request_id] = await browser.ybRun(
                        'test_almost_overdue_overdraft_invoice_request'
                    );
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep выставление овердрафтного счета, юр.лицо, банк, под админом + есть задолженность (на директ) страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await browser.click('.yb-debt-rows button');

                    await browser.ybAssertView(
                        'paystep выставление овердрафтного счета, юр.лицо, банк, под админом + есть задолженность (на директ), просмотр задолженности',
                        '.yb-debt-rows__content',
                        { hideElements: [...hideElements, '.yb-cart-row__eid'] }
                    );

                    await browser.click(elements.mainButtons.submit);

                    await browser.ybWaitForLoad();
                    await browser.waitForVisible(elements.popup);

                    await browser.ybAssertView(
                        'paystep выставление овердрафтного счета, юр.лицо, банк, под админом + есть задолженность (на директ), модальное окно обычный счет',
                        elements.page,
                        {
                            hideElements: [
                                ...hideElements,
                                '.yb-success-header__id',
                                '.yb-cart-row__eid'
                            ]
                        }
                    );
                });

                it('Выставление овердрафтного счета, превышен лимит овердрафта, под админом (на директ)', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const [, request_id] = await browser.ybRun(
                        'test_request_ph_with_overdraft_under_limit'
                    );
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep выставление овердрафтного счета, превышен лимит овердрафта, под админом (на директ) страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );
                });

                it('Выставление овердрафтного счета, под админом (на директ) без права IssueInvoices', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        baseRole: Roles.Support,
                        include: [],
                        exclude: [Perms.IssueInvoices]
                    });

                    const [, request_id] = await browser.ybRun('test_request_ph_with_overdraft', {
                        login
                    });
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep выставление овердрафтного счета, под админом (на директ) без права IssueInvoices страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await browser.click(elements.mainButtons.tumbler);

                    await browser.ybAssertView(
                        'paystep выставление овердрафтного счета, под админом (на директ) без права IssueInvoices боковое окно',
                        elements.right
                    );

                    await browser.click(elements.mainButtons.submit);
                    await browser.ybWaitAnimation('Modal-Content_theme_normal_visible');
                    await browser.waitForVisible(elements.popup);

                    await browser.ybAssertView(
                        'paystep выставление овердрафтного счета, под админом (на директ) без права IssueInvoices, модальное окно подтверждение',
                        elements.page,
                        { hideElements: [...hideElements, elements.mainButtons.submit] }
                    );
                });

                it('Выставление овердрафтного счета, под админом (на директ) просроченные и почти просроченные счета (вперемешку)', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const [, invoice_id] = await browser.ybRun(
                        'test_overdraft_almost_overdue_and_overdue_invoice',
                        {
                            login
                        }
                    );
                    await browser.ybUrl('user', `paystep.xml?invoice_id=${invoice_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep выставление овердрафтного счета, под админом (на директ) просроченные и почти просроченные счета страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await browser.click('.yb-debt-rows button');

                    await browser.ybAssertView(
                        'paystep выставление овердрафтного счета, под админом (на директ) просроченные и почти просроченные счета, просмотр задолженности',
                        '.yb-debt-rows__content',
                        { hideElements: [...hideElements, '.yb-cart-row__eid'] }
                    );
                });

                it('Выставление овердрафтного счета, юр.лицо, карта, под админом + почти просроченная задолженность (на маркет)', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const [, request_id] = await browser.ybRun(
                        'test_almost_overdue_overdraft_invoice_market_request'
                    );
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybSetSteps('Выставляем способ оплаты картой');
                    await browser.click(elements.mainButtons.payMethod);
                    await browser.click(elements.menu.card);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep выставление овердрафтного счета, юр.лицо, карта, под админом + почти просроченная задолженность страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await browser.ybSetSteps('Просмотриваем список задолжностей');
                    await browser.click('.yb-debt-rows button');

                    await browser.ybAssertView(
                        'paystep выставление овердрафтного счета, юр.лицо, карта, под админом + почти просроченная задолженность, просмотр задолженности',
                        '.yb-debt-rows__content',
                        { hideElements: [...hideElements, '.yb-cart-row__eid'] }
                    );

                    await browser.click(elements.mainButtons.tumbler);

                    await browser.ybAssertView(
                        'paystep выставление овердрафтного счета, юр.лицо, карта, под админом + почти просроченная задолженность боковое окно',
                        elements.right
                    );

                    await browser.click(elements.mainButtons.submit);
                    await browser.ybWaitAnimation('Modal-Content_theme_normal_visible');
                    await browser.waitForVisible(elements.popup);

                    await browser.ybAssertView(
                        'paystep выставление овердрафтного счета, юр.лицо, карта, под админом + почти просроченная задолженность, модальное окно подтверждение',
                        elements.page,
                        {
                            hideElements: [
                                ...hideElements,
                                '.yb-cart-row__eid',
                                elements.mainButtons.submit
                            ]
                        }
                    );

                    await browser.click(elements.modal.payConfirmButton);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitAnimation('Modal-Content_theme_normal_visible');
                    await browser.waitForVisible(elements.popup);

                    await browser.ybAssertView(
                        'paystep выставление овердрафтного счета, юр.лицо, карта, под админом + почти просроченная задолженность, модальное окно с success',
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
            });
        });
    });
});
