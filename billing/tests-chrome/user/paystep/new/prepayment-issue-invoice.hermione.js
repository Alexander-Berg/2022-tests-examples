const { elements } = require('./elements');
const { setCurrency, setPaymethod, submitAndWaitPopUp } = require('./helpers');
const assert = require('chai').assert;

// в BALANCE-38542 убрать и добавить импотр hideElements
const hideElements = [
    '.yb-cart-row__eid a',
    '.yb-success-header__id',
    '.yb-paystep-success__payment-date',
    '.yb-success-header__id'
];

describe('user', () => {
    describe('paystep', () => {
        describe('new', () => {
            describe('prepayment', () => {
                it('Выставление предоплатного счета, директ, ph, банк, под клиентом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: false });
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

                    await browser.click(elements.mainButtons.payMethod);
                    await browser.click(elements.menu.bank);

                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, директ, ph, банк, под клиентом, страница',
                        elements.page,
                        { hideElements }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, директ, ph, банк, под клиентом, модальное окно',
                        elements.popup,
                        { hideElements }
                    );
                });

                it('Выставление предоплатного счета, директ, ph, банк, под админом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: true });
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

                    await browser.click(elements.mainButtons.payMethod);
                    await browser.click(elements.menu.bank);

                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, директ, ph, банк, под админом, страница',
                        elements.page,
                        { hideElements }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, директ, ph, банк, под админом, модальное окно',
                        elements.popup,
                        { hideElements }
                    );
                });

                it('Выставление предоплатного счета, маркет, ph, банк, под клиентом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: false });
                    const [, request_id] = await browser.ybRun('test_request_market_with_ph', {
                        login
                    });

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.payMethod);
                    await browser.click(elements.menu.bank);

                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, маркет, ph, банк, под клиентом боковое окно',
                        elements.page,
                        { hideElements }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, маркет, ph, банк, под клиентом, модальное окно',
                        elements.popup,
                        { hideElements }
                    );
                });

                it('Выставление предоплатного счета, маркет, ur, банк, под админом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: true });
                    const [, request_id] = await browser.ybRun('test_request_market_with_ur', {
                        login
                    });
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.payMethod);
                    await browser.click(elements.menu.bank);

                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, маркет, ur, банк, под админом, страница',
                        elements.page,
                        { hideElements }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, маркет, ur, банк, под админом, модальное окно',
                        elements.popup,
                        { hideElements }
                    );
                });

                it('Выставление предоплатного счета, толока, usu, банк, под клиентом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: false });
                    const [, request_id] = await browser.ybRun('test_request_toloka_with_usu', {
                        login
                    });
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.payMethod);
                    await browser.click(elements.menu.bank);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, толока, usu, банк, под клиентом, страница',
                        elements.page,
                        { hideElements }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, толока, usu, банк, под клиентом, модальное окно',
                        elements.popup,
                        { hideElements }
                    );
                });

                it('Выставление предоплатного счета, директ, ph, юмани, под клиентом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: false });
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

                    await browser.click(elements.mainButtons.payMethod);
                    await browser.click(elements.menu.yamoney);

                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, директ, ph, юмани, под клиентом, страница',
                        elements.page,
                        { hideElements }
                    );
                });

                it('Выставление предоплатного счета, толока, usu, карта, под клиентом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: false });
                    const [, request_id] = await browser.ybRun('test_request_toloka_with_usu', {
                        login
                    });
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.payMethod);
                    await browser.click(elements.menu.card);

                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, толока, usu, карта, под клиентом, страница',
                        elements.page,
                        { hideElements }
                    );
                });

                it('Выставление предоплатного счета, практикум, usp, paypal, под админом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: true });
                    const [, request_id] = await browser.ybRun('test_request_practicum_with_usp', {
                        login
                    });
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, практикум, usp, paypal, под админом, страница',
                        elements.page,
                        { hideElements }
                    );
                });

                it('Выставление предоплатного счета, директ, ur, несколько плательщиков, под клиентом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: false });
                    const [
                        ,
                        ,
                        ,
                        ,
                        request_id,
                        person_id_1,
                        ,
                        person_id_3
                    ] = await browser.ybRun('test_client_empty_order_three_persons', { login });
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.person);
                    await browser.ybWaitForLoad();

                    await browser.click(elements.modal.personListItem + person_id_1);
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);
                    await browser.ybWaitForLoad();

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, толока, usu, карта, под клиентом, первый плательщик',
                        elements.page,
                        { hideElements }
                    );

                    await browser.click(elements.mainButtons.person);
                    await browser.ybWaitForLoad();

                    await browser.click(elements.modal.personListItem + person_id_3);
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);
                    await browser.ybWaitForLoad();

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, толока, usu, карта, под клиентом, страница, третий плательщик',
                        elements.page,
                        { hideElements }
                    );

                    await browser.click(elements.mainButtons.payMethod);
                    await browser.ybWaitForLoad();
                    await browser.click(elements.menu.card);

                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, толока, usu, карта, под клиентом, страница, другой способ оплаты',
                        elements.page,
                        { hideElements }
                    );
                });

                it('Выставление предоплатного счета, директ, sw_ur, смена валюты, банк, под клиентом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: false });

                    const [
                        ,
                        ,
                        ,
                        ,
                        request_id,
                        person_id_3
                    ] = await browser.ybRun('test_request_direct_with_sw_ur', { login });
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, директ, sw_ur, смена валюты, банк, под клиентом, страница',
                        elements.page,
                        { hideElements }
                    );

                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.currency);
                    await browser.ybWaitForLoad();

                    await browser.click('div[id="CHF"]');
                    await browser.click(elements.mainButtons.payMethod);
                    await browser.click(elements.menu.bank);
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, директ, sw_ur, смена валюты, банк, под клиентом, швейцарские франки как валюта',
                        elements.page,
                        { hideElements }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, директ, sw_ur, смена валюты, банк, под клиентом, модальное окно',
                        elements.popup,
                        { hideElements }
                    );
                });

                it('Выставление предоплатного счета, клиент без плательщика, добавить плательщика', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});
                    const [
                        ,
                        ,
                        ,
                        ,
                        request_id
                    ] = await browser.ybRun('test_client_empty_order_no_person', { login });

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, клиент без плательщика, добавить плательщика, боковое окно пустое',
                        elements.right,
                        { hideElements: [...hideElements] }
                    );

                    await setCurrency(browser, {
                        currency: { id: '#RUB', value: 'Российский рубль' }
                    });

                    await browser.ybSetSteps(`Ставим способ оплаты по счету`);
                    await setPaymethod(browser, 'bank');

                    await browser.ybSetSteps(`Нажимаем добавить плательщика`);
                    await browser.click(elements.mainButtons.person);
                    await browser.ybWaitForLoad();

                    await browser.ybSetSteps(`Заполняем обязательные поля`);

                    await browser.waitForVisible(elements.changePerson.submitButton);
                    await browser.setValue('input[name="lname"]', 'Иванов');
                    await browser.setValue('input[name="fname"]', 'Иван');
                    await browser.setValue('input[name="mname"]', 'Иванович');
                    await browser.setValue('input[name="phone"]', '81231231212');
                    await browser.setValue('input[name="email"]', 'hello@my.world');
                    await browser.click('div[data-detail-id="agree"] input');

                    await browser.ybSetSteps(`Регистрация плательщика`);
                    await browser.click(elements.changePerson.submitButton);
                    await browser.ybWaitForInvisible(elements.spinLoader);
                    await browser.ybWaitForInvisible(
                        elements.mainButtons.submit + '[aria-disabled="true"]'
                    );

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, клиент без плательщика, добавить плательщика, боковое окно готовое',
                        elements.right,
                        { hideElements: [...hideElements] }
                    );

                    await browser.click(elements.mainButtons.submit);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitAnimation('Modal-Content_theme_normal_visible');

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, клиент без плательщика, добавить плательщика, модальное окно',
                        '.yb-paystep-success',
                        { hideElements: [...hideElements, '.yb-success-header__id'] }
                    );
                });

                it('Выставление предоплатного счета, клиент с плательщиком, добавить плательщика и выставить счет ', async function () {
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

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, клиент с плательщиком, добавить плательщика и выставить счет, боковое окно пустое',
                        elements.right,
                        { hideElements: [...hideElements] }
                    );

                    await browser.ybSetSteps(`Ставим способ оплаты по счету`);
                    await setPaymethod(browser, 'bank');

                    await browser.ybSetSteps(`Нажимаем добавить плательщика`);
                    await browser.click(elements.mainButtons.person);
                    await browser.click('.yb-paystep-main-persons-add');
                    await browser.ybWaitForLoad();

                    await browser.ybSetSteps(`Заполняем обязательные поля`);

                    await browser.waitForVisible(elements.changePerson.submitButton);
                    await browser.setValue('input[name="lname"]', 'Иванов');
                    await browser.setValue('input[name="fname"]', 'Иван');
                    await browser.setValue('input[name="mname"]', 'Иванович');
                    await browser.setValue('input[name="phone"]', '81231231212');
                    await browser.setValue('input[name="email"]', 'hello@my.world');
                    await browser.click('div[data-detail-id="agree"] input');

                    await browser.ybSetSteps(`Регистрация плательщика`);
                    await browser.click(elements.changePerson.submitButton);
                    await browser.ybWaitForInvisible(elements.spinLoader);
                    await browser.ybWaitForInvisible(
                        elements.mainButtons.submit + '[aria-disabled="true"]'
                    );

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, клиент с плательщиком, добавить плательщика и выставить счет, боковое окно готовое',
                        elements.right,
                        { hideElements: [...hideElements] }
                    );

                    await browser.click(elements.mainButtons.submit);
                    await browser.ybWaitAnimation('Modal-Content_theme_normal_visible');

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, клиент с плательщиком, добавить плательщика и выставить счет, модальное окно',
                        '.yb-paystep-success',
                        { hideElements: [...hideElements, '.yb-success-header__id'] }
                    );
                });

                it('Выставление предоплатного счета, несколько заказов в реквесте и выставить счет ', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});
                    const [, , , , request_id] = await browser.ybRun(
                        'test_client_3_orders_with_ur',
                        {
                            login
                        }
                    );

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybSetSteps(`Ставим способ оплаты по счету`);
                    await setPaymethod(browser, 'bank');

                    await browser.ybWaitForLoad();

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, несколько заказов в реквесте и выставить счет, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, несколько заказов в реквесте, модальное окно',
                        '.yb-paystep-success',
                        { hideElements: [...hideElements, '.yb-success-header__id'] }
                    );
                });

                it('Выставление предоплатного счета, выставление счетв, когда не принимаешь оферту ', async function () {
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

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, выставление счетв, когда не принимаешь оферту, боковое окно по умолчанию',
                        elements.right,
                        { hideElements: [...hideElements] }
                    );

                    await browser.click('.yb-paystep-main__offer input');

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, выставление счетв, когда не принимаешь оферту, боковое окно не согласен с офертой',
                        elements.right,
                        { hideElements: [...hideElements] }
                    );

                    // aria-disabled ? 'всё правильно': 'почему-то могу выставить счет'
                    const submitInvoice = await browser.$(elements.mainButtons.submit);
                    const attr = await submitInvoice.getAttribute('aria-disabled');
                    await browser.ybSetSteps('Проверяет, что выставить счет нельзя');
                    assert(attr, `Выставить счет aria-disabled = ${attr}, ожидалось true`);
                });

                it('Выставление предоплатного счета, проверка ссылки на счет ', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});
                    const [
                        client_id,
                        ,
                        ,
                        ,
                        request_id
                    ] = await browser.ybRun('test_client_empty_order_with_ph', { login });

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, проверка ссылки, боковое окно по умолчанию',
                        elements.right,
                        { hideElements: [...hideElements] }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, проверка ссфлки, модальное окно',
                        elements.popup,
                        { hideElements: [...hideElements, '.yb-success-header__id'] }
                    );

                    // получаем номер нашего счета
                    const invoice_id = await browser.ybRun('test_get_invoice_id', { client_id });

                    // переходим на страницу оплаты при помощи закрытия окна со счетом
                    await browser.click('.yb-user-popup__btn-close');
                    await browser.ybWaitForLoad();

                    const url = await browser.getUrl();

                    await browser.ybSetSteps('Проверяем ссылку на заказ');

                    let exprectedUrl = `https://user-balance.greed-tm.paysys.yandex.ru/invoice.xml?invoice_id=${invoice_id}`;

                    // проверяем, что была правильная ссылка на заказ
                    assert(
                        url === exprectedUrl,
                        `Неправильная ссылка на заказ, мы ожидали ${exprectedUrl}, получили ${url}`
                    );
                });

                it('Выставление предоплатного счета, большая сумма в заказе (недоступность мгновенных способов оплаты)', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});
                    const [, , , , request_id] = await browser.ybRun('test_client_order_max_sum', {
                        login
                    });

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep ВВыставление предоплатного счета, большая сумма в заказе, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await browser.click(elements.mainButtons.payMethod);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, большая сумма заказа, модальное окно способов оплаты',
                        elements.right,
                        { hideElements: [...hideElements] }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета, большая сумма заказа, модальное окно',
                        elements.popup,
                        { hideElements: [...hideElements, '.yb-success-header__id'] }
                    );
                });

                it('Кнопка с мгновенными способами оплаты, есть плательщик, под клиентом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});
                    const [, , , , request_id] = await browser.ybRun(
                        'test_client_empty_order_with_ph',
                        {
                            login
                        }
                    );
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.payMethod);
                    await browser.click(elements.menu.card);
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView('paystep Способ оплаты картой', elements.right, {
                        hideElements: [...hideElements]
                    });

                    await browser.click(elements.mainButtons.payMethod);
                    await browser.click(elements.menu.yamoney);
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView('paystep Способ оплаты ЮMoney', elements.right, {
                        hideElements: [...hideElements]
                    });
                });

                it('Выставление предоплатного счета на клиента с договором, под клиентом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});
                    const [, request_id] = await browser.ybRun(
                        'test_request_with_contract_client',
                        {
                            login
                        }
                    );
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета на клиента с договором, под клиентом',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await browser.click(elements.mainButtons.submit);
                    await browser.ybWaitAnimation('Modal-Content_theme_normal_visible');

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета на клиента с договором, под клиентом, модальное окно',
                        elements.popup,
                        {
                            hideElements: [...hideElements, elements.hideElements.popupHeader],
                            screenshotDelay: 2000
                        }
                    );
                });

                it('Выставление предоплатного счета на клиента, банк, есть плательщик, под админом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: true });
                    const [, , , , request_id] = await browser.ybRun('test_client_empty_order', {
                        login
                    });
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.payMethod);
                    await browser.click(elements.menu.bank);

                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета на клиента, банк, есть плательщик, под админом',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep Выставленный предоплатнный счет на клиента, банк, есть плательщик, под админом, модальное окно',
                        elements.popup,
                        { hideElements: [...hideElements] }
                    );
                });

                it('Выставление предоплатного счета на клиента, банк, есть плательщик, под клиентом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: false });
                    const [, , , , request_id] = await browser.ybRun('test_client_empty_order', {
                        login
                    });
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click(elements.mainButtons.payMethod);
                    await browser.click(elements.menu.bank);

                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep Выставление предоплатного счета на клиента, банк, есть плательщик, под клиентом',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep Выставленный предоплатнный счет на клиента, банк, есть плательщик, под клиентом, модальное окно',
                        elements.popup,
                        { hideElements: [...hideElements, elements.hideElements.popupHeader] }
                    );
                });

                it('выставление предоплатного счёта с договором, ООО "Яндекс", директ, ur, банк, под клиентом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: false });
                    const [, request_id] = await browser.ybRun(
                        'test_prepayment_direct_ur_request',
                        {
                            login
                        }
                    );
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, ООО "Яндекс", директ, ur, банк, под клиентом, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, ООО "Яндекс", директ, ur, банк, под клиентом, модальное окно',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );
                });

                it('выставление предоплатного счёта с договором, ООО "Яндекс", директ, ur, банк, под админом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                    const [, request_id] = await browser.ybRun(
                        'test_prepayment_direct_ur_request',
                        {
                            login
                        }
                    );
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, ООО "Яндекс", директ, ur, банк, под админом, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await browser.click(elements.mainButtons.submit);
                    await browser.ybWaitAnimation('Modal-Content_theme_normal_visible');

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, ООО "Яндекс", директ, ur, банк, под админомм, модальное окно',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );
                });

                it('выставление предоплатного счёта с договором, "Яндекс.Реклама", директ, byu, банк, под клиентом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: false });
                    const [, request_id] = await browser.ybRun(
                        'test_prepayment_direct_byu_request',
                        {
                            login
                        }
                    );
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, "Яндекс.Реклама", директ, byu, банк, под клиентом, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, "Яндекс.Реклама", директ, byu, банк, под клиентом, модальное окно',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );
                });

                it('выставление предоплатного счёта с договором, "Яндекс.Реклама", директ, byu, банк, под админом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                    const [, request_id] = await browser.ybRun(
                        'test_prepayment_direct_byu_request',
                        {
                            login
                        }
                    );
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, "Яндекс.Реклама", директ, byu, банк, под админом, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, "Яндекс.Реклама", директ, byu, банк, под админом, модальное окно',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );
                });

                it('выставление предоплатного счёта с договором, ООО "Яндекс.Казахстан", директ, kzu, банк, под клиентом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: false });
                    const [, request_id] = await browser.ybRun(
                        'test_prepayment_direct_kzu_request',
                        {
                            login
                        }
                    );
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, ООО "Яндекс.Казахстан", директ, kzu, банк, под клиентом, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, ООО "Яндекс.Казахстан", директ, kzu, банк, под клиентом, модальное окно',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );
                });

                it('выставление предоплатного счёта с договором, "Yandex Europe AG", директ, sw_yt, банк, под админом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                    const [, request_id] = await browser.ybRun(
                        'test_prepayment_direct_sw_yt_request',
                        {
                            login
                        }
                    );
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, "Yandex Europe AG", директ, sw_yt, банк, под админом, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, "Yandex Europe AG", директ, sw_yt, банк, под админом, модальное окно',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );
                });

                it('выставление предоплатного счёта с договором, ООО "Яндекс.Маркет", маркет, ur, банк, под клиентом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: false });
                    const [, request_id] = await browser.ybRun(
                        'test_prepayment_market_ur_request',
                        {
                            login
                        }
                    );
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, ООО "Яндекс.Маркет", маркет, ur, банк, под клиентом, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, ООО "Яндекс.Маркет", маркет, ur, банк, под клиентом, модальное окно',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );
                });

                it('выставление предоплатного счёта с договором, ООО "Яндекс.Вертикали", недвижимость, ur, банк, под админом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                    const [, request_id] = await browser.ybRun(
                        'test_prepayment_realty_ur_request',
                        {
                            login
                        }
                    );
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, ООО "Яндекс.Вертикали", недвижимость, ur, банк, под админом, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, ООО "Яндекс.Вертикали", недвижимость, ur, банк, под админом, модальное окно',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );
                });

                it('выставление предоплатного счёта с договором, ООО "Яндекс", геоконтекст, ur, банк, под клиентом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: false });
                    const [, request_id] = await browser.ybRun('test_prepayment_geo_ur_request', {
                        login
                    });
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, ООО "Яндекс", геоконтекст, ur, банк, под клиентом, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, ООО "Яндекс", геоконтекст, ur, банк, под клиентом, модальное окно',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );
                });

                it('выставление предоплатного счёта с договором, ООО "Яндекс", навигатор, ur, банк, под админом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                    const [, request_id] = await browser.ybRun('test_prepayment_navi_ur_request', {
                        login
                    });
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, ООО "Яндекс", навигатор, ur, банк, под админом, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, ООО "Яндекс", навигатор, ur, банк, под админом, модальное окно',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );
                });

                it('выставление предоплатного счёта с договором, ООО "Яндекс.ОФД", яндекс.офд, ur, банк, под клиентом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: false });
                    const [, request_id] = await browser.ybRun('test_prepayment_ofd_ur_request', {
                        login
                    });
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, ООО "Яндекс.ОФД", яндекс.офд, ur, банк, под клиентом, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, ООО "Яндекс.ОФД", яндекс.офд, ur, банк, под клиентом, модальное окно',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );
                });

                it('выставление предоплатного счёта с договором, ООО "Яндекс", директ, ur, несколько плательщиков, под клиентом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: false });
                    const [, request_id, person_id_2] = await browser.ybRun(
                        'test_prepayment_direct_2_ur_request',
                        {
                            login
                        }
                    );
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, ООО "Яндекс", директ, ur, несколько плательщиков, под клиентом, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await browser.click(elements.mainButtons.person);

                    await browser.click(`.yb-paystep-main-persons-list-person_id_${person_id_2}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, ООО "Яндекс", директ, ur, несколько плательщиков, под клиентом, боковое окно',
                        elements.right,
                        { hideElements: [...hideElements] }
                    );

                    await browser.click(elements.mainButtons.submit);
                    await browser.ybWaitAnimation('Modal-Content_theme_normal_visible');

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, ООО "Яндекс", директ, ur, несколько плательщиков, под клиентом, модальное окно',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );
                });

                it('выставление предоплатного счёта с договором, ООО "Яндекс", директ, ur+ph, два разных плательщика, под клиентом', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({ isAdmin: false });
                    const [, request_id] = await browser.ybRun(
                        'test_prepayment_direct_ur_ph_request',
                        {
                            login
                        }
                    );
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.click('input[value="ur"]');

                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, ООО "Яндекс", директ, ur+ph, два разных плательщика, под клиентом, боковое окно ЮР. лицо',
                        elements.right,
                        { hideElements: [...hideElements] }
                    );

                    await browser.click('input[value="ph"]');

                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, ООО "Яндекс", директ, ur+ph, два разных плательщика, под клиентом, физик, страница',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep выставление предоплатного счёта с договором, ООО "Яндекс", директ, ur+ph, два разных плательщика, под клиентом, физик, модальное окно',
                        elements.page,
                        { hideElements: [...hideElements] }
                    );
                });

                it('Невозможность выставления под бухлогином', async function () {
                    const { browser } = this;
                    const { login } = await browser.ybSignIn({});
                    const [client_id, request_id] = await browser.ybRun(
                        'test_request_market_with_ph'
                    );
                    await browser.ybRun('test_unlink_client', { login });
                    await browser.ybRun('test_delete_every_accountant_role', { login });
                    await browser.ybRun('test_add_accountant_role', { login, client_id });
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);
                    await browser.waitForVisible('.yb-user-error__code');
                    await browser.ybRun('test_delete_every_accountant_role', { login });
                });
            });
        });
    });
});
