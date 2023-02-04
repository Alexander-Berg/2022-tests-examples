const { hideElements, ignoreElements, elements } = require('./elements');
const { submitAndWaitPopUp, waitUntilTimeout } = require('./helpers');

describe('user', () => {
    describe('paystep', () => {
        describe('new', () => {
            describe('agency', () => {
                it('счёт под агентством, заказ на субклиента-нереза, нет договора', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                    const [, request_id] = await browser.ybRun(
                        'test_request_for_agency_no_contract_with_nonrez'
                    );
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.waitForVisible('.yb-paystep-no-choice');

                    await browser.ybAssertView(
                        'paystep для агентства, заказ на субклиента-нереза, нет договора',
                        '.yb-user-content',
                        {
                            hideElements
                        }
                    );
                });
                it('счёт под агентством, заказ на субклиента + общий лимит агентства', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                    const [, request_id] = await browser.ybRun('test_request_for_agency');
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep для агентства, заказ на субклиента',
                        '.yb-user-content',
                        {
                            hideElements
                        }
                    );

                    await browser.click(elements.mainButtons.tumbler);

                    await browser.ybAssertView(
                        'paystep счёт на агентство, заказ на субклиента, общий лимит, боковое окно ',
                        elements.right,
                        { hideElements: [...hideElements] }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep, модальное окно success, счёт на агентство, заказ на субклиента',
                        '.yb-user-content',
                        {
                            hideElements: [
                                hideElements,
                                '.b-page-title',
                                elements.mainButtons.submit
                            ]
                        }
                    );
                    await browser.click(elements.modal.payConfirmButton);

                    await browser.ybWaitForInvisible(elements.mainButtons.submit);

                    await browser.ybWaitForInvisible(
                        'img[alt="Waiting for data"]',
                        waitUntilTimeout
                    );

                    await browser.ybAssertView(
                        'paystep, страница счета, счёт на агентство, заказ на субклиента',
                        elements.page,
                        { hideElements: ['.date', '.show-order', '.oper-details', 'h1', 'form'] }
                    );

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep счёт на агентство, заказ на субклиента, общий лимит после первичного выставления, боковое окно ',
                        elements.right,
                        { hideElements: [...hideElements] }
                    );
                });
                it('счет на агентство с договором, заказ на субклиента нереза', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                    const [, request_id] = await browser.ybRun(
                        'test_request_for_agency_with_nonrez'
                    );
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep, счет на агентство с договором, заказ на субклиента нереза',
                        '.yb-user-content',
                        {
                            hideElements: [
                                hideElements,
                                '.yb-paystep-main-total__sum',
                                '.yb-cart-row__col-sum',
                                '.yb-paystep-preview__sum',
                                '.yb-paystep-main__pay'
                            ]
                        }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep, модальное окно success, счет на агентство с договором, заказ на субклиента нереза',
                        '.yb-user-content',
                        {
                            hideElements: [
                                hideElements,
                                '.b-page-title',
                                '.yb-paystep-main-total__sum',
                                '.yb-cart-row__col-sum',
                                '.yb-paystep-preview__sum',
                                '.yb-paystep-main__pay',
                                '.yb-paystep-success__sum'
                            ]
                        }
                    );
                    await browser.click(elements.modal.close);

                    await browser.ybWaitForInvisible(elements.mainButtons.submit);

                    await browser.ybWaitForInvisible(
                        'img[alt="Waiting for data"]',
                        waitUntilTimeout
                    );

                    await browser.ybAssertView(
                        'paystep, страница счета, счет на агентство с договором, заказ на субклиента нереза',
                        elements.page,
                        {
                            hideElements: [
                                '.date',
                                '.show-order',
                                '.oper-details',
                                'h1',
                                'form',
                                '.t-price',
                                'td.invoice-footer'
                            ]
                        }
                    );
                });
                it('счёт под агентством, без договора, в разрешенный сервис', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                    const [, request_id] = await browser.ybRun(
                        'test_request_for_agency_no_contract'
                    );
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep, счёт под агентством, без договора, в разрешенный сервис',
                        '.yb-user-content',
                        {
                            hideElements
                        }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep, модальное окно success, счёт под агентством, без договора, в разрешенный сервис',
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
                        'paystep, страница счета, счёт под агентством, без договора, в разрешенный сервис',
                        elements.page,
                        { hideElements: ['.date', '.show-order', '.oper-details', 'h1', 'form'] }
                    );
                });
                it('счёт под агентством, заказ на субклиентов с индивидуальными лимитами', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                    const [, request_id_1, request_id_2, request_id_3] = await browser.ybRun(
                        'test_request_for_agency_with_ind_limits'
                    );
                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id_1}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep для агентства, заказ на субклиента 1',
                        '.yb-user-content',
                        {
                            hideElements
                        }
                    );

                    await browser.click(elements.mainButtons.tumbler);

                    await browser.ybAssertView(
                        'paystep счёт на агентство, заказ на субклиента 1, индивидуальный лимит, боковое окно ',
                        elements.right,
                        { hideElements: [...hideElements] }
                    );

                    await submitAndWaitPopUp(browser, elements);

                    await browser.ybAssertView(
                        'paystep, модальное окно success, счёт на агентство, заказ на субклиента 1 с индивидуальным лимитом',
                        '.yb-user-content',
                        {
                            hideElements: [
                                hideElements,
                                '.b-page-title',
                                elements.mainButtons.submit
                            ]
                        }
                    );
                    await browser.click(elements.modal.payConfirmButton);

                    await browser.ybWaitForInvisible(elements.mainButtons.submit);

                    await browser.ybWaitForInvisible(
                        'img[alt="Waiting for data"]',
                        waitUntilTimeout
                    );

                    await browser.ybAssertView(
                        'paystep, страница счета, счёт на агентство, заказ на субклиента 1 с индивидуальным лимитом',
                        elements.page,
                        { hideElements: ['.date', '.show-order', '.oper-details', 'h1', 'form'] }
                    );

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id_1}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep счёт на агентство, заказ на субклиента 1, инд. лимит после первичного выставления, боковое окно ',
                        elements.right,
                        { hideElements: [...hideElements] }
                    );

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id_2}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep счёт на агентство, заказ на субклиента 2, инд. лимит после выставления под 1 субклиентом, боковое окно ',
                        elements.right,
                        { hideElements: [...hideElements] }
                    );

                    await browser.ybUrl('user', `paystep.xml?request_id=${request_id_3}`);

                    await browser.ybWaitForLoad();
                    await browser.ybWaitForInvisible(elements.paystepPreload);
                    await browser.ybWaitForInvisible(elements.preload);

                    await browser.ybAssertView(
                        'paystep счёт на агентство, заказ на субклиента без инд. лимита, общий лимит после выставления под 1 субклиентом, боковое окно ',
                        elements.right,
                        { hideElements: [...hideElements] }
                    );
                });
            });
        });
    });
});
