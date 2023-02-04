const { elements, hideElements } = require('./elements');
const assert = require('chai').assert;
const { waitUntilTimeout, submitAndWaitPopUp } = require('./helpers');

describe('user', () => {
    describe('paystep', () => {
        describe('new', () => {
            describe('fictive pa', () => {
                it('новый ЛС, выставление кредитного счета, админ', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const [, , , request_id] = await browser.ybRun('test_request_fictive_pa', {
                        login
                    });
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep новый ЛС, выставление кредитного счета, админ, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await browser.click(elements.mainButtons.tumbler);

                    await browser.ybAssertView(
                        'paystep новый ЛС, выставление кредитного счета, админ, боковое окно ',
                        elements.right,
                        { hideElements: [...hideElements] }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep новый ЛС, выставление кредитного счета, админ, модальное окно',
                        elements.popup
                    );

                    await browser.click(elements.modal.payConfirmButton);

                    await browser.ybWaitForInvisible(elements.mainButtons.submit);

                    await browser.ybWaitForInvisible(
                        'img[alt="Waiting for data"]',
                        waitUntilTimeout
                    );

                    await browser.ybAssertView(
                        'invoice, новый ЛС, выставление кредитного счета, админ, страница счета',
                        elements.page,
                        { hideElements: ['.date', '.show-order', '.oper-details', 'h1'] }
                    );
                });

                it('новый ЛС, выставление кредитного счета, есть задолженность, админ', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const [, , , , request_id] = await browser.ybRun(
                        'test_request_fictive_pa_debt',
                        {
                            login
                        }
                    );
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);
                    await browser.waitForVisible('.yb-debt-rows');

                    await browser.ybAssertView(
                        'paystep новый ЛС, выставление кредитного счета, есть задолженность, админ, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await browser.click(elements.mainButtons.tumbler);

                    await browser.ybAssertView(
                        'paystep новый ЛС, выставление кредитного счета, есть задолженность, админ, боковое окно ',
                        elements.right,
                        { hideElements: [...hideElements] }
                    );

                    await browser.click('.yb-debt-rows');

                    await browser.ybAssertView(
                        'paystep новый ЛС, выставление кредитного счета, есть задолженность, админ, спиcок задолженостей',
                        '.yb-debt-rows__content',
                        { hideElements: [...hideElements, '.yb-cart-row__badge'] }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep новый ЛС, выставление кредитного счета, есть задолженность, админ, модальное окно',
                        elements.popup
                    );

                    await browser.click(elements.modal.payConfirmButton);

                    await browser.ybWaitForInvisible(elements.mainButtons.submit);

                    await browser.ybWaitForInvisible(
                        'img[alt="Waiting for data"]',
                        waitUntilTimeout
                    );

                    await browser.ybAssertView(
                        'invoice, новый ЛС, выставление кредитного счета, есть задолженность, админ, страница счета',
                        elements.page,
                        { hideElements: ['.date', '.show-order', '.oper-details', 'h1', '.t-act'] }
                    );
                });

                it('новый ЛС, выставление кредитного счета, есть почти просроченная задолженность, админ', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const [, , request_id] = await browser.ybRun(
                        'test_request_fictive_pa_almost_overdue_debt',
                        {
                            login
                        }
                    );
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);
                    await browser.waitForVisible('.yb-debt-rows');

                    await browser.ybAssertView(
                        'paystep новый ЛС, выставление кредитного счета, есть почти просроченная задолженность, админ, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await browser.click(elements.mainButtons.tumbler);

                    await browser.ybAssertView(
                        'paystep новый ЛС, выставление кредитного счета, есть почти просроченная задолженность, админ, боковое окно ',
                        elements.right,
                        { hideElements: [...hideElements] }
                    );

                    await browser.click('.yb-debt-rows');

                    await browser.ybAssertView(
                        'paystep новый ЛС, выставление кредитного счета, есть почти просроченная задолженность, админ, спиcок задолженостей',
                        '.yb-debt-rows__content',
                        { hideElements: [...hideElements, '.yb-cart-row__badge'] }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep новый ЛС, выставление кредитного счета, есть почти просроченная задолженность, админ, модальное окно ',
                        elements.popup
                    );

                    await browser.click(elements.modal.payConfirmButton);

                    await browser.ybWaitForInvisible(elements.mainButtons.submit);

                    await browser.ybWaitForInvisible(
                        'img[alt="Waiting for data"]',
                        waitUntilTimeout
                    );

                    await browser.ybAssertView(
                        'invoice, новый ЛС, выставление кредитного счета, есть почти просроченная задолженность, админ, страница счета',
                        elements.page,
                        { hideElements: ['.date', '.show-order', '.oper-details', 'h1', '.t-act'] }
                    );
                });

                it('новый ЛС, выставление кредитного счета, есть просроченная задолженность, админ', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const [
                        ,
                        ,
                        request_id
                    ] = await browser.ybRun('test_request_fictive_pa_overdue_debt', { login });
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);
                    await browser.waitForVisible('.yb-debt-rows');

                    await browser.ybAssertView(
                        'paystep новый ЛС, выставление кредитного счета, есть просроченная задолженность, админ, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    // получаем span внутри тумблера, чтобы  проверить его состояние
                    // aria-disabled ? 'всё правильно': 'почему-то тумблер доступен'
                    const tumblerSpan = await browser.$('.yb-paystep-main__tumbler span');
                    const attr = await tumblerSpan.getAttribute('aria-disabled');
                    await browser.ybSetSteps('Проверяет, что тумблер недоступен');
                    assert(attr, 'Тублер доступен при наличии задолженности');

                    await browser.click('.yb-debt-rows');

                    await browser.ybAssertView(
                        'paystep новый ЛС, выставление кредитного счета, есть просроченная задолженность, админ, спиcок задолженостей',
                        '.yb-debt-rows__content',
                        { hideElements: [...hideElements, '.yb-cart-row__badge'] }
                    );
                });
            });
        });
    });
});
