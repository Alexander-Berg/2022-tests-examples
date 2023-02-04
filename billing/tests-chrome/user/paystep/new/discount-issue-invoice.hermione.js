const { hideElements, ignoreElements, elements } = require('./elements');
const { Roles } = require('../../../../helpers/role_perm');
const { submitAndWaitPopUp, waitUntilTimeout } = require('./helpers');

describe('user', () => {
    describe('paystep', () => {
        describe('new', () => {
            describe('discounts', () => {
                it('Выставление счета на физ.лицо с разовой клиентской скидкой', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({});
                    const [
                        ,
                        ,
                        request_id
                    ] = await browser.ybRun('test_request_onetime_client_discount', { login });
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload, elements.preload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep разовая клиентская скидка',
                        '.yb-user-content',
                        {
                            hideElements
                        }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybWaitForLoad();
                    await browser.ybAssertView(
                        'paystep, модальное окно success, разовая клиентская скидка',
                        '.yb-user-content',
                        {
                            hideElements: [hideElements, '.b-page-title']
                        }
                    );

                    await browser.click(elements.modal.close);

                    await browser.ybWaitForInvisible(elements.mainButtons.submit);

                    await browser.ybWaitForInvisible(
                        'img[alt="Waiting for data"]',
                        waitUntilTimeout
                    );

                    await browser.ybAssertView(
                        'paystep, разовая клиентская скидка, страница счета',
                        elements.page,
                        {
                            hideElements: [
                                '.date',
                                '.show-order',
                                '.oper-details',
                                'h1',
                                '.show-client'
                            ]
                        }
                    );
                });
                it('Выставление счета с агентской фиксированной скидкой', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        baseRole: Roles.Support,
                        include: [],
                        exclude: []
                    });
                    const [, , , request_id] = await browser.ybRun(
                        'test_request_fix_agency_discount'
                    );
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload, elements.preload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep, агентская фиксированная скидка',
                        '.yb-user-content',
                        {
                            hideElements
                        }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep, модальное окно success, агентская фиксированная скидка',
                        '.yb-user-content',
                        {
                            hideElements: [hideElements, '.b-page-title']
                        }
                    );
                    await browser.click(elements.modal.close);

                    await browser.ybWaitForInvisible(elements.mainButtons.submit);

                    await browser.ybWaitForInvisible(
                        'img[alt="Waiting for data"]',
                        waitUntilTimeout
                    );

                    await browser.ybAssertView(
                        'paystep, агентская фиксированная скидка, страница счета',
                        elements.page,
                        { hideElements: ['.date', '.show-order', '.oper-details', 'h1', 'form'] }
                    );
                });
            });
        });
    });
});
