const { elements } = require('./elements');

const hideElements = [
    elements.ignoreElements.orderId,
    '.yb-success-header__id',
    '.yb-paystep-success__contract-date',
    '.yb-paystep-main__contract'
];

const { waitUntilTimeout, submitAndWaitPopUp } = require('./helpers');

describe('user', () => {
    describe('paystep', () => {
        describe('new', () => {
            describe('charge-note', () => {
                it('счет-квитанция, Такси', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});
                    const [, , , request_id] = await browser.ybRun('test_request_old_pa_taxi', {
                        login
                    });

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep счет-квитанция, Такси, ur, по счету, под клиентом, страница',
                        elements.page,
                        { hideElements }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep счет-квитанция, Такси, ur, по счету, под клиентом, модальное окно',
                        elements.page,
                        { hideElements }
                    );
                });

                it('счет-квитанция, Заправки', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});
                    const [, , , request_id] = await browser.ybRun('test_request_old_pa_zapravki', {
                        login
                    });

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep счет-квитанция, Заправки, ur, по счету, под клиентом, страница',
                        elements.page,
                        { hideElements }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep счет-квитанция, Заправки, ur, по счету, под клиентом, модальное окно',
                        elements.page,
                        { hideElements }
                    );
                });

                it('счет-квитанция, Облако Россия', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});

                    const [, , , request_id] = await browser.ybRun('test_request_old_pa_cloud', {
                        login
                    });

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep счет-квитанция, Облако Россия, ur, по счету, под клиентом, страница',
                        elements.page,
                        { hideElements }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep счет-квитанция, Облако Россия, ur, по счету, под клиентом, модальное окно',
                        elements.page,
                        { hideElements }
                    );
                });

                it('счет-квитанция, Облако Россия, оферта', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});

                    const [, , , request_id] = await browser.ybRun(
                        'test_request_old_pa_cloud_offer',
                        {
                            login
                        }
                    );

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep счет-квитанция, Облако Россия, оферта, ur, по счету, под клиентом, страница',
                        elements.page,
                        { hideElements }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep счет-квитанция, Облако Россия, оферта, ur, по счету, под клиентом, модальное окно',
                        elements.page,
                        { hideElements }
                    );
                });

                it('счет-квитанция, Корптакси Россия', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});

                    const [, , , request_id] = await browser.ybRun(
                        'test_request_old_pa_corp_taxi_ru',
                        {
                            login
                        }
                    );

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep счет-квитанция, Корптакси Россия, ur, по счету, под клиентом, страница',
                        elements.page,
                        { hideElements }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep счет-квитанция, Корптакси Россия, ur, по счету, под клиентом, модальное окно',
                        elements.page,
                        { hideElements }
                    );
                });

                it('счет-квитанция, Корптакси Казахстан', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});

                    const [, , , request_id] = await browser.ybRun(
                        'test_request_old_pa_corp_taxi_kz',
                        {
                            login
                        }
                    );

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep счет-квитанция, Корптакси Казахстан, kzu, по счету, под клиентом, страница',
                        elements.page,
                        { hideElements }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep счет-квитанция, Корптакси Казахстан, kzu, по счету, под клиентом, модальное окно',
                        elements.page,
                        { hideElements }
                    );
                });

                it('счет-квитанция, Корптакси Беларусь', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});

                    const [, , , request_id] = await browser.ybRun(
                        'test_request_old_pa_corp_taxi_by',
                        {
                            login
                        }
                    );

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep счет-квитанция, Корптакси Беларусь, by_ur, по счету, под клиентом, страница',
                        elements.page,
                        { hideElements }
                    );

                    await browser.click(elements.mainButtons.submit);
                    await browser.ybWaitAnimation('Modal-Content_theme_normal_visible');

                    await browser.ybAssertView(
                        'paystep счет-квитанция, Корптакси Беларусь, by_ur, по счету, под клиентом, модальное окно',
                        elements.page,
                        { hideElements }
                    );
                });

                it('счет-квитанция, Драйв Б2Б предоплата', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});

                    const [, , , request_id] = await browser.ybRun(
                        'test_request_old_pa_drive_b2b_prepay',
                        {
                            login
                        }
                    );

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep счет-квитанция, Драйв Б2Б предоплата, ur, по счету, под клиентом, страница',
                        elements.page,
                        { hideElements }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep счет-квитанция, Драйв Б2Б предоплата, ur, по счету, под клиентом, модальное окно',
                        elements.page,
                        { hideElements }
                    );
                });

                it('счет-квитанция, Драйв Б2Б постоплата', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});

                    const [, , , request_id] = await browser.ybRun(
                        'test_request_old_pa_drive_b2b_postpay',
                        {
                            login
                        }
                    );

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep счет-квитанция, Драйв Б2Б постоплата, ur, по счету, под клиентом, страница',
                        elements.page,
                        { hideElements }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep счет-квитанция, Драйв Б2Б постоплата, ur, по счету, под клиентом, модальное окно',
                        elements.page,
                        { hideElements }
                    );
                });

                it('счет-квитанция, Такси Камерун MLU постоплата', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const [, , , request_id] = await browser.ybRun(
                        'test_request_old_pa_taxi_cameroon_postpay',
                        {
                            login
                        }
                    );

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);
                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);
                    await browser.click('.yb-paystep-main__pay-method button');
                    await browser.click('div[id="card"]');
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep счет-квитанция, Такси Камерун MLU постоплата, картой, под админом, страница',
                        elements.page,
                        { hideElements }
                    );

                    await browser.click(elements.mainButtons.submit);
                    await browser.ybWaitForInvisible(elements.mainButtons.submit);

                    await browser.ybWaitForInvisible(
                        'img[alt="Waiting for data"]',
                        waitUntilTimeout
                    );

                    await browser.ybAssertView(
                        'invoice счет-квитанция, Такси Камерун MLU постоплата, картой, под админом, страница',
                        elements.page,
                        { hideElements: ['h1', '.date', '.show-order'] }
                    );
                });
            });
        });
    });
});
