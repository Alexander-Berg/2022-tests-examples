const { elements, selectorToScroll, hideElements } = require('../../elements');
const common = require('./common');
const { fillRequiredFieldsForYTDetails, fillAllFields } = require('./helpers');

const { setCurrency, setPaymethod } = require('../../helpers');

describe('user', () => {
    describe('paystep', () => {
        describe('new', () => {
            describe('persons', () => {
                describe(`${common.personType.name}`, () => {
                    describe('создание', () => {
                        it(`создание только с обязательными полями под админом, банк, рубли`, async function () {
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
                            ] = await browser.ybRun('test_client_empty_order_no_person', { login });

                            await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                            await browser.ybWaitForLoad();
                            await browser.ybWaitForInvisible(elements.paystepPreload);
                            await browser.ybWaitForInvisible(elements.preload);

                            await setCurrency(browser, common.details);

                            await browser.ybSetSteps(`Ставим способ оплаты по счету`);
                            await setPaymethod(browser, 'bank');
                            await browser.ybWaitForInvisible(elements.paystepPreload);
                            await browser.ybWaitForInvisible(elements.preload);

                            await browser.click('input[value="ur"]');
                            await browser.ybWaitForInvisible(elements.paystepPreload);
                            await browser.ybWaitForInvisible(elements.preload);

                            await browser.ybSetSteps(`Нажимаем добавить плательщика`);
                            await browser.click(elements.mainButtons.person);
                            await browser.click('[data-detail-id="person-type"] div span button');
                            await browser.click('span=' + common.personType.value);
                            await browser.ybWaitForLoad();

                            await browser.waitForVisible(elements.changePerson.submitButton);
                            await browser.ybAssertView(
                                'paystep форма для создания плательщика, yt, обяз. поля, admin, часть 1',
                                elements.page,
                                {
                                    hideElements: [
                                        ...hideElements,
                                        '.yb-user-copyright__year',
                                        '.yb-paystep__right'
                                    ]
                                }
                            );
                            await browser.scroll('div[data-detail-id="legaladdress"]');
                            await browser.ybAssertView(
                                'paystep форма для создания плательщика, yt, обяз. поля, admin, часть 2',
                                elements.page,
                                {
                                    hideElements: [
                                        ...hideElements,
                                        '.yb-user-copyright__year',
                                        '.yb-paystep__right'
                                    ]
                                }
                            );

                            await browser.ybSetSteps(`Заполняем обязsательные поля`);

                            await fillRequiredFieldsForYTDetails(browser, common.details);

                            await browser.scroll(elements.popup);
                            await browser.ybAssertView(
                                'paystep заполненная форма для создания плательщика, yt, обяз. поля, admin, часть 1',
                                elements.page,
                                {
                                    hideElements: [
                                        ...hideElements,
                                        '.yb-user-copyright__year',
                                        '.yb-paystep__right'
                                    ]
                                }
                            );
                            await browser.scroll('div[data-detail-id="legaladdress"]');
                            await browser.ybAssertView(
                                'paystep заполненная форма для создания плательщика, yt, обяз. поля, admin, часть 2',
                                elements.page,
                                {
                                    hideElements: [
                                        ...hideElements,
                                        '.yb-user-copyright__year',
                                        '.yb-paystep__right'
                                    ]
                                }
                            );

                            await browser.ybSetSteps(`Регистрация плательщика`);
                            await browser.click(elements.changePerson.submitButton);
                            await browser.ybWaitForInvisible(elements.popup);
                            await browser.ybWaitForInvisible(elements.spinLoader);
                            await browser.ybWaitForInvisible(
                                elements.mainButtons.submit + '[aria-disabled="true"]'
                            );

                            await browser.click(elements.mainButtons.person);
                            await browser.ybWaitForLoad();
                            await browser.click(
                                '.yb-paystep-main-persons-list-person__buttons span'
                            );
                            await browser.ybWaitForInvisible(elements.spinLoader);

                            await browser.ybAssertView(
                                `paystep информация о плательщике, yt, обяз. поля, admin`,
                                elements.page,
                                {
                                    hideElements: [...hideElements],
                                    selectorToScroll,
                                    screenshotDelay: 2000
                                }
                            );
                        });

                        it(`создание только со всеми полями под админом, банк, рубли`, async function () {
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
                            ] = await browser.ybRun('test_client_empty_order_no_person', { login });

                            await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                            await browser.ybWaitForLoad();
                            await browser.ybWaitForInvisible(elements.paystepPreload);
                            await browser.ybWaitForInvisible(elements.preload);

                            await setCurrency(browser, common.details);

                            await browser.ybSetSteps(`Ставим способ оплаты по счету`);
                            await setPaymethod(browser, 'bank');
                            await browser.ybWaitForInvisible(elements.paystepPreload);
                            await browser.ybWaitForInvisible(elements.preload);

                            await browser.click('input[value="ur"]');
                            await browser.ybWaitForInvisible(elements.paystepPreload);
                            await browser.ybWaitForInvisible(elements.preload);

                            await browser.ybSetSteps(`Нажимаем добавить плательщика`);
                            await browser.click(elements.mainButtons.person);
                            await browser.click('[data-detail-id="person-type"] div span button');
                            await browser.click('span=' + common.personType.value);
                            await browser.ybWaitForLoad();

                            await browser.waitForVisible(elements.changePerson.submitButton);
                            await browser.ybAssertView(
                                'paystep форма для создания плательщика, yt, все поля, admin, часть 1',
                                elements.page,
                                {
                                    hideElements: [
                                        ...hideElements,
                                        '.yb-user-copyright__year',
                                        '.yb-paystep__right'
                                    ]
                                }
                            );
                            await browser.scroll('div[data-detail-id="legaladdress"]');
                            await browser.ybAssertView(
                                'paystep форма для создания плательщика, yt, все поля, admin, часть 2',
                                elements.page,
                                {
                                    hideElements: [
                                        ...hideElements,
                                        '.yb-user-copyright__year',
                                        '.yb-paystep__right'
                                    ]
                                }
                            );

                            await browser.ybSetSteps(`Заполняем обязsательные поля`);

                            await fillAllFields(browser, common.details);

                            await browser.scroll(elements.popup);
                            await browser.ybAssertView(
                                'paystep заполненная форма для создания плательщика, yt, все поля, admin, часть 1',
                                elements.page,
                                { hideElements: [...hideElements, '.yb-user-copyright__year'] }
                            );
                            await browser.scroll('div[data-detail-id="legaladdress"]');
                            await browser.ybAssertView(
                                'paystep заполненная форма для создания плательщика, yt, все поля, admin, часть 2',
                                elements.page,
                                { hideElements: [...hideElements, '.yb-user-copyright__year'] }
                            );

                            await browser.ybSetSteps(`Регистрация плательщика`);
                            await browser.click(elements.changePerson.submitButton);
                            await browser.ybWaitForInvisible(elements.popup);
                            await browser.ybWaitForInvisible(elements.spinLoader);
                            await browser.ybWaitForInvisible(
                                elements.mainButtons.submit + '[aria-disabled="true"]'
                            );

                            await browser.click(elements.mainButtons.person);
                            await browser.ybWaitForLoad();
                            await browser.click(
                                '.yb-paystep-main-persons-list-person__buttons span'
                            );
                            await browser.ybWaitForInvisible(elements.spinLoader);

                            await browser.ybAssertView(
                                `paystep информация о плательщике, yt, все поля, admin`,
                                elements.page,
                                {
                                    hideElements: [...hideElements, '.yb-user-copyright__year'],
                                    selectorToScroll,
                                    screenshotDelay: 2000
                                }
                            );
                        });
                    });
                    describe('редактирование', () => {
                        it('редактирование под админом', async function () {
                            const { browser } = this;
                            const { login } = await browser.ybSignIn({
                                isAdmin: true,
                                isReadonly: false
                            });
                            const [
                                ,
                                ,
                                request_id
                            ] = await browser.ybRun('test_create_request_with_person', [
                                `${common.personType.name}`,
                                login
                            ]);

                            await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                            await browser.ybWaitForLoad();
                            await browser.ybWaitForInvisible(elements.paystepPreload);
                            await browser.ybWaitForInvisible(elements.preload);

                            await browser.click(elements.mainButtons.person);
                            await browser.ybWaitAnimation('Modal-Content_theme_normal_visible');

                            await browser.ybAssertView(
                                `paystep, список плательщиков, ${common.personType.name}, админ`,
                                elements.page,
                                { hideElements: [...hideElements] }
                            );

                            await browser.click('.yb-paystep-main-persons-list-person__btn_edit');
                            await browser.waitForVisible(elements.changePerson.submitButton);

                            await browser.waitForVisible(elements.changePerson.submitButton);
                            await browser.ybAssertView(
                                'paystep форма для создания плательщика, yt, admin, часть 1',
                                elements.page,
                                { hideElements: [...hideElements, '.yb-user-copyright__year'] }
                            );
                            await browser.scroll('div[data-detail-id="legaladdress"]');
                            await browser.ybAssertView(
                                'paystep форма для создания плательщика, yt, admin, часть 2',
                                elements.page,
                                { hideElements: [...hideElements, '.yb-user-copyright__year'] }
                            );

                            await browser.ybReplaceValue(
                                'input[name="name"]',
                                common.details.name.newValue
                            );

                            await browser.click(elements.changePerson.submitButton);
                            await browser.ybWaitAnimation('Modal-Content_theme_normal_visible');

                            await browser.click('.yb-paystep-main-persons-list-person__btn_view');
                            await browser.ybWaitForInvisible(elements.spinLoader);

                            await browser.ybAssertView(
                                `paystep, просмотр плательщика, ${common.personType.name}, админ`,
                                elements.page,
                                { hideElements: [...hideElements], selectorToScroll }
                            );
                        });
                    });
                });
            });
        });
    });
});
