const common = require('./common');
const { fillAllFieldsUr, fillRequiredDetailsUr } = require('./helpers');

const { setCurrency, setPaymethod } = require('../../helpers');

const { elements, hideElements, selectorToScroll } = require('../../elements');

describe('user', () => {
    describe('paystep', () => {
        describe('new', () => {
            describe('persons', () => {
                describe(`${common.personType.name}`, () => {
                    describe('создание', () => {
                        it(`создание только с обязательными полями под клиентом, банк, ${common.details.currency.value}`, async function () {
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

                            await setCurrency(browser, common.details);

                            await browser.ybSetSteps(`Ставим способ оплаты по счету`);
                            await setPaymethod(browser, 'bank');

                            await browser.click('input[value="ur"]');
                            await browser.ybWaitForInvisible(elements.paystepPreload);
                            await browser.ybWaitForInvisible(elements.preload);

                            await browser.ybSetSteps(`Нажимаем добавить плательщика`);
                            await browser.click('input[value="ur"]');
                            await browser.ybWaitForLoad();
                            await browser.ybWaitForInvisible(elements.paystepPreload);
                            await browser.ybWaitForInvisible(elements.preload);
                            await browser.click(elements.mainButtons.person);
                            await browser.click('[data-detail-id="person-type"] div span button');
                            await browser.click('span=Юр. лицо');

                            await browser.ybWaitForLoad();

                            await browser.waitForVisible(elements.changePerson.submitButton);

                            await browser.scroll(elements.popup);
                            await browser.ybAssertView(
                                'paystep форма для создания плательщика, ur, обяз. поля, user, часть 1',
                                elements.page,
                                { hideElements: [...hideElements] }
                            );
                            await browser.scroll('div[data-detail-id="legalAddrType"]');
                            await browser.ybAssertView(
                                'paystep форма для создания плательщика, ur, обяз. поля, user, часть 2',
                                elements.page,
                                { hideElements: [...hideElements] }
                            );
                            await browser.click('span=по справочнику');
                            await browser.click('span=по адресу');
                            await browser.scroll('div[data-detail-id="envelopeAddress"]');
                            await browser.ybAssertView(
                                'paystep форма для создания плательщика, ur, обяз. поля, user, часть 3',
                                elements.page,
                                { hideElements: [...hideElements] }
                            );

                            await browser.ybSetSteps(`Заполняем обязательные поля`);

                            await fillRequiredDetailsUr(browser, common.details, 'user');

                            await browser.scroll(elements.popup);
                            await browser.ybAssertView(
                                'paystep заполненная форма для создания плательщика, ur, обяз. поля, user, часть 1',
                                elements.page,
                                { hideElements: [...hideElements] }
                            );
                            await browser.scroll('div[data-detail-id="legalAddrType"]');
                            await browser.ybAssertView(
                                'paystep заполненная форма для создания плательщика, ur, обяз. поля, user, часть 2',
                                elements.page,
                                { hideElements: [...hideElements] }
                            );
                            await browser.scroll('div[data-detail-id="envelopeAddress"]');
                            await browser.ybAssertView(
                                'paystep заполненная форма для создания плательщика, ur, обяз. поля, user, часть 3',
                                elements.page,
                                { hideElements: [...hideElements] }
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
                                `paystep, информация о плательщике, ${common.personType.name}, обяз. поля, клиент`,
                                elements.page,
                                {
                                    hideElements: [...hideElements],
                                    selectorToScroll
                                }
                            );
                        });

                        it(`создание со всеми полями под клиентом, банк, ${common.details.currency.value}`, async function () {
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

                            await setCurrency(browser, common.details);

                            await browser.ybSetSteps(`Ставим способ оплаты по счету`);
                            await setPaymethod(browser, 'bank');

                            await browser.ybSetSteps(`Нажимаем добавить плательщика`);
                            await browser.click('input[value="ur"]');
                            await browser.ybWaitForLoad();
                            await browser.ybWaitForInvisible(elements.paystepPreload);
                            await browser.ybWaitForInvisible(elements.preload);
                            await browser.click(elements.mainButtons.person);
                            await browser.click('[data-detail-id="person-type"] div span button');
                            await browser.click('span=Юр. лицо');

                            await browser.ybWaitForLoad();

                            await browser.waitForVisible(elements.changePerson.submitButton);
                            await browser.ybAssertView(
                                'paystep форма для создания плательщика, ur, все поля, user, часть 1',
                                elements.page,
                                { hideElements: [...hideElements] }
                            );

                            await browser.scroll('div[data-detail-id="representative"]');
                            await browser.ybAssertView(
                                'paystep форма для создания плательщика, ur, все поля, user, часть 2',
                                elements.page,
                                { hideElements: [...hideElements] }
                            );
                            await browser.scroll('input[name="bik"]');
                            await browser.ybAssertView(
                                'paystep форма для создания плательщика, ur, все поля, user, часть 3',
                                elements.page,
                                { hideElements: [...hideElements] }
                            );

                            await browser.ybSetSteps(`Заполняем все поля`);

                            await fillAllFieldsUr(browser, common.details, 'user');

                            await browser.scroll(elements.popup);
                            await browser.ybAssertView(
                                'paystep заполненная форма для создания плательщика, ur, все. поля, user, часть 1',
                                elements.page,
                                { hideElements: [...hideElements] }
                            );
                            await browser.scroll('div[data-detail-id="representative"]');
                            await browser.ybAssertView(
                                'paystep заполненная форма для создания плательщика, ur, все. поля, user, часть 2',
                                elements.page,
                                { hideElements: [...hideElements, '[name=envelope-address]'] }
                            );

                            await browser.scroll('input[name="bik"]');
                            await browser.ybAssertView(
                                'paystep заполненная форма для создания плательщика, ur, все. поля, user, часть 3',
                                elements.page,
                                { hideElements: [...hideElements, '[name=envelope-address]'] }
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
                                `paystep информация о плательщике, ${common.personType.name}, все поля, клиент`,
                                elements.page,
                                {
                                    hideElements: [...hideElements],
                                    selectorToScroll
                                }
                            );
                        });

                        it(`создание только с обязательными полями под админом, банк, ${common.details.currency.value}`, async function () {
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

                            await browser.ybWaitForInvisible(elements.preload);

                            await browser.ybSetSteps(`Нажимаем добавить плательщика`);
                            await browser.click('input[value="ur"]');
                            await browser.ybWaitForLoad();
                            await browser.ybWaitForInvisible(elements.paystepPreload);
                            await browser.ybWaitForInvisible(elements.preload);
                            await browser.click(elements.mainButtons.person);
                            await browser.click('[data-detail-id="person-type"] div span button');
                            await browser.click('span=' + common.personType.value);
                            await browser.ybWaitForLoad();

                            await browser.waitForVisible(elements.changePerson.submitButton);
                            await browser.click('span=по справочнику');
                            await browser.click('span=по адресу');
                            await browser.scroll(elements.popup);
                            await browser.ybAssertView(
                                'paystep форма для создания плательщика, ur, обяз. поля, admin, часть 1',
                                elements.page,
                                { hideElements: [...hideElements] }
                            );
                            await browser.scroll('div[data-detail-id="legalAddrType"]');
                            await browser.ybAssertView(
                                'paystep форма для создания плательщика, ur, обяз. поля, admin, часть 2',
                                elements.page,
                                { hideElements: [...hideElements] }
                            );
                            await browser.scroll('input[name="invalid-bankprops"]');
                            await browser.ybAssertView(
                                'paystep форма для создания плательщика, ur, обяз. поля, admin, часть 3',
                                elements.page,
                                { hideElements: [...hideElements] }
                            );

                            await browser.ybSetSteps(`Заполняем обязательные поля`);

                            await fillRequiredDetailsUr(browser, common.details, 'admin');

                            await browser.scroll(elements.popup);
                            await browser.ybAssertView(
                                'paystep заполненная форма для создания плательщика, ur, обяз. поля, admin, часть 1',
                                elements.page,
                                { hideElements: [...hideElements] }
                            );
                            await browser.scroll('div[data-detail-id="legalAddrType"]');
                            await browser.ybAssertView(
                                'paystep заполненная форма для создания плательщика, ur, обяз. поля, admin, часть 2',
                                elements.page,
                                { hideElements: [...hideElements] }
                            );
                            await browser.scroll('input[name="invalid-bankprops"]');
                            await browser.ybAssertView(
                                'paystep заполненная форма для создания плательщика, ur, обяз. поля, admin, часть 3',
                                elements.page,
                                { hideElements: [...hideElements] }
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
                                `paystep информация о плательщике, ${common.personType.name}, обяз. поля, admin`,
                                elements.page,
                                {
                                    hideElements: [...hideElements],
                                    selectorToScroll
                                }
                            );
                        });
                    });
                    describe('редактирование', () => {
                        it('редактирование под клиентом', async function () {
                            const { browser } = this;
                            const { login } = await browser.ybSignIn({});
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
                                `paystep, список плательщиков, ${common.personType.name}, клиент`,
                                elements.page,
                                { hideElements: [...hideElements] }
                            );

                            await browser.click('.yb-paystep-main-persons-list-person__btn_edit');
                            await browser.waitForVisible(elements.changePerson.submitButton);

                            await browser.waitForVisible(elements.changePerson.submitButton);
                            await browser.scroll('.yb-paystep-main-change-person h1');
                            await browser.ybAssertView(
                                'paystep форма для создания плательщика, ur, клиент, часть 1',
                                elements.page,
                                { hideElements: [...hideElements] }
                            );
                            await browser.scroll('div[data-detail-id="legalAddrType"]');
                            await browser.ybAssertView(
                                'paystep форма для создания плательщика, ur, клиент, часть 2',
                                elements.page,
                                { hideElements: [...hideElements] }
                            );
                            await browser.scroll('div[data-detail-id="bik"]');
                            await browser.ybAssertView(
                                'paystep форма для создания плательщика, ur, клиент, часть 3',
                                elements.page,
                                { hideElements: [...hideElements] }
                            );

                            await browser.ybReplaceValue(
                                'input[name="representative"]',
                                common.details.representative.newValue
                            );

                            await browser.click(elements.changePerson.submitButton);
                            await browser.ybWaitAnimation('Modal-Content_theme_normal_visible');

                            await browser.click('.yb-paystep-main-persons-list-person__btn_view');
                            await browser.ybWaitForInvisible(elements.spinLoader);

                            await browser.ybAssertView(
                                `paystep, просмотр плательщика, ${common.personType.name}, клиент`,
                                elements.page,
                                { hideElements: [...hideElements], selectorToScroll }
                            );
                        });

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
                            await browser.scroll('.yb-paystep-main-change-person h1');
                            await browser.ybAssertView(
                                'paystep форма для создания плательщика, ur, админ, часть 1',
                                elements.page,
                                { hideElements: [...hideElements] }
                            );
                            await browser.scroll('div[data-detail-id="legalAddrType"]');
                            await browser.ybAssertView(
                                'paystep форма для создания плательщика, ur, админ, часть 2',
                                elements.page,
                                { hideElements: [...hideElements] }
                            );
                            await browser.scroll('div[data-detail-id="bik"]');
                            await browser.ybAssertView(
                                'paystep форма для создания плательщика, ur, админ, часть 3',
                                elements.page,
                                { hideElements: [...hideElements] }
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
