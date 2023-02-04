const common = require('./common');
const { fillRequiredFieldsForPhDetails, fillAllFieldsPhUser } = require('./helpers');

const { setCurrency, setPaymethod } = require('../../helpers');

const { elements, selectorToScroll, hideElements } = require('../../elements');

describe('user', () => {
    describe('paystep', () => {
        describe('new', () => {
            describe('persons', () => {
                describe('ph', () => {
                    describe('создание', () => {
                        it('создание только с обязательными полями под клиентом, картой, рубли', async function () {
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

                            await browser.ybSetSteps(`Ставим способ оплаты картой`);
                            await setPaymethod(browser, 'card');

                            await browser.ybSetSteps(`Нажимаем добавить плательщика`);
                            await browser.click(elements.mainButtons.person);
                            await browser.click('[data-detail-id="person-type"] div span button');
                            await browser.click('span=Физ. лицо');
                            await browser.ybWaitForLoad();

                            await browser.scroll(elements.popup);

                            await browser.ybAssertView(
                                'paystep, форма для добавления плательщика, ph, обяз. поля, клиент часть 1',
                                elements.page,
                                {
                                    selectorToScroll,
                                    hideElements: [
                                        ...hideElements,
                                        '.yb-paystep-preview',
                                        '.yb-paystep__right'
                                    ]
                                }
                            );

                            await browser.scroll('div[data-detail-id="bik"]');

                            await browser.ybAssertView(
                                'paystep, форма для добавления плательщика, ph, обяз. поля, клиент часть 2',
                                elements.page,
                                {
                                    selectorToScroll,
                                    hideElements: [
                                        ...hideElements,
                                        '.yb-paystep-preview',
                                        '.yb-paystep__right'
                                    ]
                                }
                            );

                            await browser.ybSetSteps(`Заполняем обязательные поля`);

                            await fillRequiredFieldsForPhDetails(browser, common.details);

                            await browser.scroll(elements.popup);

                            await browser.ybAssertView(
                                'paystep, заполненная форма создания плательщика, ph, обяз. поля, клиент часть 1',
                                elements.page,
                                {
                                    selectorToScroll,
                                    hideElements: [
                                        ...hideElements,
                                        'div[data-detail-id="agree"] span span',
                                        '.yb-paystep__right'
                                    ]
                                }
                            );

                            await browser.scroll('div[data-detail-id="bik"]');

                            await browser.ybAssertView(
                                'paystep, заполненная форма создания плательщика, ph, обяз. поля, клиент часть 2',
                                elements.page,
                                {
                                    selectorToScroll,
                                    hideElements: [
                                        ...hideElements,
                                        'div[data-detail-id="agree"] span span',
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
                                'paystep, информация о плательщике, ph, обяз. поля, клиент',
                                elements.page,
                                {
                                    hideElements: [...hideElements],
                                    selectorToScroll,
                                    screenshotDelay: 2000
                                }
                            );
                        });

                        it('создание со всеми полями под клиентом, картой, рубли', async function () {
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

                            await browser.ybSetSteps(`Ставим способ оплаты картой`);
                            await setPaymethod(browser, 'card');

                            await browser.ybSetSteps(`Нажимаем добавить плательщика`);
                            await browser.click(elements.mainButtons.person);
                            await browser.click('[data-detail-id="person-type"] div span button');
                            await browser.click('span=Физ. лицо');
                            await browser.ybWaitForLoad();

                            await browser.scroll(elements.popup);

                            await browser.ybAssertView(
                                'paystep, форма для добавления плательщика, ph, все поля, клиент часть 1',
                                elements.page,
                                {
                                    selectorToScroll,
                                    hideElements: [
                                        ...hideElements,
                                        '.yb-paystep-preview',
                                        '.yb-paystep__right'
                                    ]
                                }
                            );

                            await browser.scroll('div[data-detail-id="bik"]');

                            await browser.ybAssertView(
                                'paystep, форма для добавления плательщика, ph, все поля, клиент часть 2',
                                elements.page,
                                {
                                    selectorToScroll,
                                    hideElements: [
                                        ...hideElements,
                                        '.yb-paystep-preview',
                                        '.yb-paystep__right'
                                    ]
                                }
                            );

                            await browser.ybSetSteps(`Заполняем все поля`);

                            await fillAllFieldsPhUser(browser, common.details);

                            await browser.scroll(elements.popup);

                            await browser.ybAssertView(
                                'paystep, заполненная форма для добавления плательщика, ph, все поля, клиент часть 1',
                                elements.page,
                                {
                                    selectorToScroll,
                                    hideElements: [
                                        ...hideElements,
                                        'div[data-detail-id="agree"] span span',
                                        '.yb-paystep__right'
                                    ]
                                }
                            );

                            await browser.scroll('div[data-detail-id="bik"]');

                            await browser.ybAssertView(
                                'paystep, заполненная форма для добавления плательщика, ph, все поля, клиент часть 2',
                                elements.page,
                                {
                                    selectorToScroll,
                                    hideElements: [
                                        ...hideElements,
                                        'div[data-detail-id="agree"] span span',
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
                                'paystep информация о плательщике, ph, все поля, клиент',
                                elements.page,
                                {
                                    hideElements: [...hideElements],
                                    selectorToScroll,
                                    screenshotDelay: 2000
                                }
                            );
                        });

                        it('создание только с обязательными полями под админом, картой, рубли', async function () {
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

                            await browser.ybSetSteps(`Ставим способ оплаты картой`);
                            await setPaymethod(browser, 'card');

                            await browser.ybSetSteps(`Нажимаем добавить плательщика`);
                            await browser.click(elements.mainButtons.person);
                            await browser.click('[data-detail-id="person-type"] div span button');
                            await browser.click('span=Физ. лицо');
                            await browser.ybWaitForLoad();

                            await browser.scroll(elements.popup);

                            await browser.ybAssertView(
                                'paystep, форма для создания плательщика, ph, обяз. поля, admin часть 1',
                                elements.page,
                                {
                                    selectorToScroll,
                                    hideElements: [
                                        ...hideElements,
                                        '.yb-paystep-preview',
                                        '.yb-paystep__right'
                                    ]
                                }
                            );

                            await browser.scroll('div[data-detail-id="postaddress"]');

                            await browser.ybAssertView(
                                'paystep, форма для создания плательщика, ph, обяз. поля, admin часть 2',
                                elements.page,
                                {
                                    selectorToScroll,
                                    hideElements: [
                                        ...hideElements,
                                        '.yb-paystep-preview',
                                        '.yb-paystep__right'
                                    ]
                                }
                            );

                            await browser.ybSetSteps(`Заполняем обязательные поля`);

                            await fillRequiredFieldsForPhDetails(browser, common.details);

                            await browser.scroll(elements.popup);

                            await browser.ybAssertView(
                                'paystep, заполненная форма создания плательщика, ph, обяз. поля, admin часть 1',
                                elements.page,
                                {
                                    selectorToScroll,
                                    hideElements: [
                                        ...hideElements,
                                        'div[data-detail-id="agree"] span span',
                                        '.yb-paystep__right'
                                    ]
                                }
                            );

                            await browser.scroll('div[data-detail-id="postaddress"]');

                            await browser.ybAssertView(
                                'paystep, заполненная форма создания плательщика, ph, обяз. поля, admin часть 2',
                                elements.page,
                                {
                                    selectorToScroll,
                                    hideElements: [
                                        ...hideElements,
                                        'div[data-detail-id="agree"] span span',
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
                                'paystep информация о плательщике, ph, обяз. поля, admin',
                                elements.page,
                                {
                                    hideElements: [...hideElements],
                                    selectorToScroll,
                                    screenshotDelay: 2000
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

                            await browser.ybAssertView(
                                `paystep, форма редактирования, ${common.personType.name}, клиент часть 1`,
                                elements.page,
                                { hideElements: [...hideElements], selectorToScroll }
                            );

                            await browser.scroll('div[data-detail-id="bik"]');

                            await browser.ybAssertView(
                                `paystep, форма редактирования, ${common.personType.name}, клиент часть 2`,
                                elements.page,
                                { hideElements: [...hideElements], selectorToScroll }
                            );

                            await browser.ybReplaceValue(
                                'input[name="fname"]',
                                common.details.fname.newValue
                            );

                            await browser.click(elements.changePerson.submitButton);
                            await browser.ybWaitAnimation('Modal-Content_theme_normal_visible');

                            await browser.click('.yb-paystep-main-persons-list-person__btn_view');
                            await browser.ybWaitForInvisible(elements.spinLoader);

                            await browser.ybAssertView(
                                `paystep, просмотр измененного плательщика, ${common.personType.name}, клиент`,
                                elements.page,
                                { hideElements: [...hideElements] }
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
                                'ph',
                                login
                            ]);

                            await browser.ybUrl('user', `paystep.xml?request_id=${request_id}`);

                            await browser.ybWaitForLoad();
                            await browser.ybWaitForInvisible(elements.paystepPreload);
                            await browser.ybWaitForInvisible(elements.preload);

                            await browser.click(elements.mainButtons.person);
                            await browser.ybWaitAnimation('Modal-Content_theme_normal_visible');

                            await browser.ybAssertView(
                                `paystep, список плательщиков, ph, админ`,
                                elements.page,
                                { hideElements: [...hideElements] }
                            );

                            await browser.click('.yb-paystep-main-persons-list-person__btn_edit');
                            await browser.waitForVisible(elements.changePerson.submitButton);

                            await browser.ybAssertView(
                                `paystep, форма редактирования, ph, админ, часть 1`,
                                elements.page,
                                { hideElements: [...hideElements], selectorToScroll }
                            );

                            await browser.scroll('div[data-detail-id="bik"]');

                            await browser.ybAssertView(
                                `paystep, форма редактирования, ph, админ, часть 2`,
                                elements.page,
                                { hideElements: [...hideElements], selectorToScroll }
                            );

                            await browser.ybReplaceValue(
                                'input[name="fname"]',
                                common.details.fname.newValue
                            );

                            await browser.click(elements.changePerson.submitButton);
                            await browser.ybWaitAnimation('Modal-Content_theme_normal_visible');

                            await browser.click('.yb-paystep-main-persons-list-person__btn_view');
                            await browser.ybWaitForInvisible(elements.spinLoader);

                            await browser.ybAssertView(
                                `paystep, просмотр измененного плательщика, ph, админ`,
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
