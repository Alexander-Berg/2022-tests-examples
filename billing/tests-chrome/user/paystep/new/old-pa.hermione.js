const { elements, ignoreElements, hideElements } = require('./elements');
const { waitUntilTimeout, waitNavigationToInvoice, submitAndWaitPopUp } = require('./helpers');

describe('user', () => {
    describe('paystep', () => {
        describe('new', () => {
            describe('old pa', () => {
                it('старый ЛС, выставление кредитного счета, Yandex Inc, admin', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const [, , , request_id] = await browser.ybRun(
                        'test_request_old_pa_Yandex_Inc',
                        {
                            login
                        }
                    );
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep старый ЛС, выставление кредитного счета, Yandex Inc, админ, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep старый ЛС, выставление кредитного счета, Yandex Inc, админ, модальное окно ',
                        elements.popup
                    );

                    await browser.click(elements.modal.payConfirmButton);

                    await browser.ybWaitForInvisible(elements.mainButtons.submit);

                    await browser.ybWaitForInvisible(
                        'img[alt="Waiting for data"]',
                        waitUntilTimeout
                    );

                    await browser.ybAssertView(
                        'invoice, старый ЛС, выставление кредитного счета, Yandex Inc, админ, страница счета',
                        elements.page,
                        { hideElements: ['.date', '.show-order', '.oper-details', 'h1'] }
                    );
                });

                it('старый ЛС, выставление кредитного счета, Yandex Europe AG, админ', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const [
                        ,
                        ,
                        ,
                        request_id
                    ] = await browser.ybRun('test_request_old_pa_Yandex_Europe_AG', { login });
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep старый ЛС, выставление кредитного счета, Yandex Europe AG, админ, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep старый ЛС, выставление кредитного счета, Yandex Europe AG, админ, модальное окно ',
                        elements.popup
                    );

                    await browser.click(elements.modal.payConfirmButton);

                    await browser.ybWaitForInvisible(elements.mainButtons.submit);

                    await browser.ybWaitForInvisible(
                        'img[alt="Waiting for data"]',
                        waitUntilTimeout
                    );

                    await browser.ybAssertView(
                        'invoice, старый ЛС, выставление кредитного счета, Yandex Europe AG, админ, страница счета',
                        elements.page,
                        { hideElements: ['.date', '.show-order', '.oper-details', 'h1'] }
                    );
                });

                it('старый ЛС, выставление кредитного счета, сумма больше лимита, есть задолженность, админ', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const [
                        ,
                        ,
                        ,
                        ,
                        request_id
                    ] = await browser.ybRun('test_request_old_pa_big_sum_and_debt', { login });
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.preload);
                    await browser.waitForVisible('.yb-debt-rows');

                    await browser.ybAssertView(
                        'paystep старый ЛС, выставление кредитного счета, сумма больше лимита, есть задолженность, админ, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await browser.click('.yb-debt-rows');

                    await browser.ybAssertView(
                        'paystep старый ЛС, выставление кредитного счета, сумма больше лимита, есть задолженность, админ, спиcок задолженостей',
                        '.yb-debt-rows__content',
                        { hideElements: [...hideElements, '.yb-cart-row__badge'] }
                    );
                });

                it('старый ЛС, выставление кредитного счета, есть почти просроченная задолженность, админ', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const [
                        ,
                        ,
                        request_id
                    ] = await browser.ybRun('test_request_old_pa_almost_overdue_debt', { login });
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.preload);
                    await browser.waitForVisible('.yb-debt-rows');

                    await browser.ybAssertView(
                        'paystep старый ЛС, выставление кредитного счета, есть почти просроченная задолженность, админ, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await browser.click('.yb-debt-rows');

                    await browser.ybAssertView(
                        'paystep старый ЛС, выставление кредитного счета, есть почти просроченная задолженность, админ, спиcок задолженостей',
                        '.yb-debt-rows__content',
                        { hideElements: [...hideElements, '.yb-cart-row__badge'] }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep старый ЛС, выставление кредитного счета, есть почти просроченная задолженность, админ, модальное окно ',
                        elements.popup
                    );

                    await browser.click(elements.modal.payConfirmButton);

                    await browser.ybWaitForInvisible(elements.mainButtons.submit);

                    await browser.ybWaitForInvisible(
                        'img[alt="Waiting for data"]',
                        waitUntilTimeout
                    );

                    await browser.ybAssertView(
                        'invoice, старый ЛС, выставление кредитного счета, сумма больше лимита, есть задолженность, админ, страница счета',
                        elements.page,
                        { hideElements: ['.date', '.show-order', '.oper-details', 'h1', '.t-act'] }
                    );
                });

                it('старый ЛС, выставление кредитного счета, есть просроченная задолженность, админ', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const [, , request_id] = await browser.ybRun(
                        'test_request_old_pa_overdue_debt',
                        {
                            login
                        }
                    );
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.preload);
                    await browser.waitForVisible('.yb-debt-rows');

                    await browser.ybAssertView(
                        'paystep старый ЛС, выставление кредитного счета, есть просроченная задолженность, админ, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await browser.click('.yb-debt-rows');

                    await browser.ybAssertView(
                        'paystep старый ЛС, выставление кредитного счета, есть просроченная задолженность, админ, спиcок задолженостей',
                        '.yb-debt-rows__content',
                        { hideElements: [...hideElements, '.yb-cart-row__badge'] }
                    );
                });

                it('Выставление кредитного счета, старый ЛС, под админом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                    const [, , , request_id] = await browser.ybRun('test_request_fictive_pa', {
                        login
                    });
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep Выставление кредитного счета, старый ЛС, под админом',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await browser.click(elements.mainButtons.tumbler);

                    await browser.ybAssertView(
                        'paystep Выставление кредитного счета, старый ЛС, под админом, правый блок',
                        elements.right
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep Выставление кредитного счета, старый ЛС, под админом, модальное окно',
                        elements.popup
                    );

                    await browser.click(elements.modal.payConfirmButton);

                    await waitNavigationToInvoice(browser);
                });
            });
        });
    });
});
