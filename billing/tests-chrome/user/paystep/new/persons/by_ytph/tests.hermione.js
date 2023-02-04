const common = require('./common');
const { fillAllFields, fillRequiredFields } = require('./helpers');

const { setCurrency, setPaymethod } = require('../../helpers');

const { elements, selectorToScroll, hideElements } = require('../../elements');
const path = require('path');
const localHideElements = [
    ...hideElements,
    '.yb-paystep-preview',
    '.yb-cart-row__col-sum',
    '.yb-paystep-preview__sum',
    '.yb-paystep-main-total__sum',
    '.Button2-Text>span+span'
];

describe('user', () => {
    describe('paystep', () => {
        describe('new', () => {
            describe('persons', () => {
                describe(`${common.personType.name}`, () => {
                    describe('создание', () => {
                        it(`создание только с обязательными полями под клиентом, карта, ${common.details.currency.value}`, async function () {
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

                            await browser.ybSetSteps(`Ставим способ оплаты по карте`);
                            await setPaymethod(browser, 'card');

                            await browser.ybSetSteps(`Нажимаем добавить плательщика`);
                            await browser.click(elements.mainButtons.person);

                            await browser.ybWaitForLoad();

                            await browser.scroll(elements.popup);

                            await browser.ybAssertView(
                                `paystep, форма для добавления плательщика, ${common.personType.name}, обяз. поля, клиент часть 1`,

                                elements.page,
                                {
                                    hideElements: [
                                        ...hideElements,
                                        '.yb-paystep-preview',
                                        '.yb-paystep__right'
                                    ],
                                    selectorToScroll
                                }
                            );

                            await browser.scroll('div[data-detail-id="countryId"]');

                            await browser.ybAssertView(
                                `paystep, форма для добавления плательщика, ${common.personType.name}, обяз. поля, клиент часть 2`,

                                elements.page,
                                {
                                    hideElements: [
                                        ...hideElements,
                                        '.yb-paystep-preview',
                                        '.yb-paystep__right'
                                    ],
                                    selectorToScroll
                                }
                            );

                            await browser.ybSetSteps(`Заполняем обязательные поля`);

                            await fillRequiredFields(browser, common.details, 'user');

                            await browser.scroll(elements.popup);

                            await browser.ybAssertView(
                                `paystep, заполненная форма создания плательщика, ${common.personType.name}, обяз. поля, клиент часть 1`,
                                elements.page,
                                {
                                    hideElements: [...hideElements, '.yb-paystep__right'],
                                    selectorToScroll
                                }
                            );

                            await browser.scroll('div[data-detail-id="countryId"]');

                            await browser.ybAssertView(
                                `paystep, заполненная форма создания плательщика, ${common.personType.name}, обяз. поля, клиент часть 2`,
                                elements.page,
                                {
                                    hideElements: [...hideElements, '.yb-paystep__right'],
                                    selectorToScroll
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
                            await browser.waitForVisible('.yb-person-id__name');

                            await browser.ybAssertView(
                                `paystep, информация о плательщике, ${common.personType.name}, обяз. поля, клиент`,
                                elements.page,
                                {
                                    hideElements: localHideElements,
                                    screenshotDelay: 2000
                                }
                            );
                        });

                        it(`создание со всеми полями под клиентом, карта, ${common.details.currency.value}`, async function () {
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

                            await browser.ybSetSteps(`Ставим способ оплаты по карте`);
                            await setPaymethod(browser, 'card');

                            await browser.ybSetSteps(`Нажимаем добавить плательщика`);
                            await browser.click(elements.mainButtons.person);

                            await browser.ybWaitForLoad();

                            await browser.scroll(elements.popup);

                            await browser.ybAssertView(
                                `paystep, форма для добавления плательщика, ${common.personType.name}, все поля, клиент часть 1`,
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

                            await browser.scroll('div[data-detail-id="countryId"]');

                            await browser.ybAssertView(
                                `paystep, форма для добавления плательщика, ${common.personType.name}, все поля, клиент часть 2`,
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

                            await fillAllFields(browser, common.details);

                            await browser.scroll(elements.popup);

                            await browser.ybAssertView(
                                `paystep, заполненная форма для добавления плательщика, ${common.personType.name}, все поля, клиент часть 1`,
                                elements.page,
                                {
                                    hideElements: [...hideElements, '.yb-paystep__right'],
                                    selectorToScroll
                                }
                            );

                            await browser.scroll('div[data-detail-id="countryId"]');

                            await browser.ybAssertView(
                                `paystep, заполненная форма для добавления плательщика, ${common.personType.name}, все поля, клиент часть 2`,
                                elements.page,
                                {
                                    hideElements: [...hideElements, '.yb-paystep__right'],
                                    selectorToScroll
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
                            await browser.waitForVisible('.yb-person-id__name');

                            await browser.ybAssertView(
                                `paystep информация о плательщике, ${common.personType.name}, все поля, клиент`,
                                elements.page,
                                {
                                    hideElements: localHideElements,
                                    screenshotDelay: 2000
                                }
                            );
                        });

                        it(`создание только с обязательными полями под админом, карта, ${common.details.currency.value}`, async function () {
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

                            await browser.ybSetSteps(`Ставим способ оплаты по карте`);
                            await setPaymethod(browser, 'card');

                            await browser.ybSetSteps(`Нажимаем добавить плательщика`);
                            await browser.click(elements.mainButtons.person);

                            await browser.ybWaitForLoad();

                            await browser.scroll(elements.popup);

                            await browser.ybAssertView(
                                `paystep, форма для создания плательщика, ${common.personType.name}, обяз. поля, admin часть 1`,
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

                            await browser.scroll('div[data-detail-id="countryId"]');

                            await browser.ybAssertView(
                                `paystep, форма для создания плательщика, ${common.personType.name}, обяз. поля, admin часть 2`,
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

                            await fillRequiredFields(browser, common.details, 'admin');

                            await browser.scroll(elements.popup);

                            await browser.ybAssertView(
                                `paystep, заполненная форма создания плательщика, ${common.personType.name}, обяз. поля, admin часть 1`,
                                elements.page,
                                {
                                    hideElements: [...hideElements, 'yb-paystep__right'],
                                    selectorToScroll
                                }
                            );

                            await browser.scroll('div[data-detail-id="countryId"]');

                            await browser.ybAssertView(
                                `paystep, заполненная форма создания плательщика, ${common.personType.name}, обяз. поля, admin часть 2`,
                                elements.page,
                                {
                                    hideElements: [...hideElements, 'yb-paystep__right'],
                                    selectorToScroll
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
                            await browser.waitForVisible('.yb-person-id__name');

                            await browser.ybAssertView(
                                `paystep информация о плательщике, ${common.personType.name}, обяз. поля, admin`,
                                elements.page,
                                {
                                    hideElements: localHideElements,
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

                            await browser.scroll('.yb-user-popup__content h1');

                            await browser.ybAssertView(
                                `paystep, форма редактирования, ${common.personType.name}, клиент`,
                                elements.page,
                                { hideElements: [...hideElements], selectorToScroll }
                            );

                            await browser.ybReplaceValue(
                                'input[name="email"]',
                                common.details.email.newValue
                            );

                            const selector = `div input[type=file]`;
                            const filePath = path.join(__dirname, '/testfile.docx');
                            const remotePath = await browser.uploadFile(filePath);
                            const elem = await browser.$(selector);
                            await elem.addValue(remotePath);

                            await browser.click(elements.changePerson.submitButton);
                            await browser.ybWaitAnimation('Modal-Content_theme_normal_visible');

                            await browser.click('.yb-paystep-main-persons-list-person__btn_view');
                            await browser.ybWaitForInvisible(elements.spinLoader);
                            await browser.waitForVisible('.yb-person-id__name');

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
                                `paystep, список плательщиов, ${common.personType.name}, админ`,
                                elements.page,
                                { hideElements: [...hideElements], selectorToScroll }
                            );

                            await browser.click('.yb-paystep-main-persons-list-person__btn_edit');
                            await browser.waitForVisible(elements.changePerson.submitButton);

                            await browser.scroll('.yb-user-popup__content h1');

                            await browser.ybAssertView(
                                `paystep, форма редактирования, ${common.personType.name}, админ`,
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
                            await browser.waitForVisible('.yb-person-id__name');

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
