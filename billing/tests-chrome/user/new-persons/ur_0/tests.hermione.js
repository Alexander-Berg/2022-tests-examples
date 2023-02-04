const common = require('../common');
const { personType } = require('../common');
const {
    fillAllFieldsAutoName,
    fillAllFieldsAutoINN,
    fillRequiredFields,
    takeScreenshots,
    replaceValues,
    openChangePersonForm,
    fillInn
} = require('./helpers');

const { elements, ignoreElements } = require('../elements');
const { assertViewOpts } = require('../config');

describe('user', () => {
    describe('new-persons', () => {
        describe(`${common.personType.ur_0.name}`, () => {
            describe('создание', () => {
                describe('клиент', () => {
                    it('создание со всеми полями, автозаполнение, по справочнику', async function () {
                        const { browser } = this;
                        const { login } = await browser.ybSignIn({ isAdmin: false });
                        await browser.ybRun('create_client_for_user', {
                            login
                        });
                        const role = 'client';

                        await browser.ybUrl('user', `new-persons.xml`);

                        await browser.waitForVisible(elements.addEmptyPersons);

                        await browser.click(elements.addEmptyPersons);

                        await browser.ybWaitForLoad();

                        await browser.click(elements.personsType.radioButton.ur);

                        await browser.ybWaitForInvisible('button[role="listbox"][disabled]');

                        await browser.click(elements.personsType.listBox);
                        await browser.click(personType.ur_0.selector);

                        await browser.click(elements.continueButton);

                        await browser.waitForVisible('input[name="inn"]');
                        await browser.click('span=по справочнику');

                        await takeScreenshots(
                            browser,
                            common.personType.ur_0.name,
                            'создание со всеми полями, форма',
                            role
                        );

                        await browser.scroll('div[data-detail-id="inn"]');
                        await fillAllFieldsAutoName(browser, 'РОГАЧЕВЪ');

                        await browser.scroll('h1');
                        await takeScreenshots(
                            browser,
                            common.personType.ur_0.name,
                            'создание со всеми полями, заполненная форма',
                            role
                        );

                        await browser.click(elements.submitButton);

                        await browser.ybWaitForInvisible('input[name="inn"]');

                        await browser.ybAssertView(
                            `new-persons, ${common.personType.ur_0.name} создание со всеми полями, просмотр плательщица часть 1`,
                            elements.page
                        );

                        await browser.scroll('div:nth-child(10) .yb-person-details-user__title ');
                        await browser.ybAssertView(
                            `new-persons, ${common.personType.ur_0.name} создание со всеми полями, просмотр плательщица часть 2`,
                            elements.page
                        );
                    });
                    it('создание с обязательными полями, 12значный ИНН (с автозаполнение)', async function () {
                        const { browser } = this;
                        const { login } = await browser.ybSignIn({ isAdmin: false });
                        await browser.ybRun('create_client_for_user', {
                            login
                        });
                        const role = 'client';

                        await browser.ybUrl('user', `new-persons.xml`);

                        await browser.waitForVisible(elements.addEmptyPersons);

                        await browser.click(elements.addEmptyPersons);

                        await browser.ybWaitForLoad();

                        await browser.click(elements.personsType.radioButton.ur);

                        await browser.ybWaitForInvisible('button[role="listbox"][disabled]');

                        await browser.click(elements.personsType.listBox);
                        await browser.click(personType.ur_0.selector);

                        await browser.click(elements.continueButton);

                        await browser.waitForVisible('input[name="inn"]');

                        await takeScreenshots(
                            browser,
                            common.personType.ur_0.name,
                            'создание с обязательными полями, форма',
                            role
                        );

                        await browser.scroll('div[data-detail-id="inn"]');
                        await fillAllFieldsAutoINN(browser, '503612526896');

                        await browser.scroll('h1');
                        await takeScreenshots(
                            browser,
                            common.personType.ur_0.name,
                            'создание c обязательными полям, заполненная форма',
                            role
                        );

                        await browser.click(elements.submitButton);

                        await browser.ybWaitForInvisible('input[name="inn"]');

                        await browser.ybAssertView(
                            `new-persons, ${common.personType.ur_0.name} создание c обязательными полям, просмотр плательщица`,
                            elements.page
                        );
                    });
                    it('создание c обязательными полями, 10значный ИНН (без автозаполнения), без справочника', async function () {
                        const { browser } = this;
                        const { login } = await browser.ybSignIn({ isAdmin: false });
                        await browser.ybRun('create_client_for_user', {
                            login
                        });
                        const role = 'client';

                        await browser.ybUrl('user', `new-persons.xml`);

                        await browser.waitForVisible(elements.addEmptyPersons);

                        await browser.click(elements.addEmptyPersons);

                        await browser.ybWaitForLoad();

                        await browser.click(elements.personsType.radioButton.ur);

                        await browser.ybWaitForInvisible('button[role="listbox"][disabled]');

                        await browser.click(elements.personsType.listBox);
                        await browser.click(personType.ur_0.selector);

                        await browser.click(elements.continueButton);

                        await browser.waitForVisible('input[name="inn"]');

                        await takeScreenshots(
                            browser,
                            common.personType.ur_0.name,
                            'создание с обязательными полями без автозаполнения, форма',
                            role
                        );

                        await browser.scroll('div[data-detail-id="inn"]');
                        await fillRequiredFields(browser);

                        await browser.scroll('h1');
                        await takeScreenshots(
                            browser,
                            common.personType.ur_0.name,
                            'создание c обязательными полями без автозаполнения, заполненная форма',
                            role
                        );

                        await browser.click(elements.submitButton);

                        await browser.ybWaitForInvisible('input[name="inn"]');

                        await browser.ybAssertView(
                            `new-persons, ${common.personType.ur_0.name} создание c обязательными полям без автозаполнения 10знач ИНН, просмотр плательщица`,
                            elements.page
                        );
                    });
                });
                describe('админ', () => {
                    it('создание со всеми полями, автозаполнение, по справочнику', async function () {
                        const { browser } = this;
                        const role = 'admin';

                        await openChangePersonForm(browser, 'ur_0');

                        await browser.click('span=по справочнику');

                        await takeScreenshots(
                            browser,
                            common.personType.ur_0.name,
                            'создание со всеми полями, форма',
                            role
                        );

                        await browser.scroll('div[data-detail-id="inn"]');
                        await fillAllFieldsAutoName(browser, 'РОГАЧЕВЪ', true);

                        await browser.scroll('h1');
                        await takeScreenshots(
                            browser,
                            common.personType.ur_0.name,
                            'создание со всеми полями, заполненная форма, автозаполнение',
                            role
                        );

                        await browser.click(elements.submitButton);

                        await browser.ybWaitForInvisible('input[name="inn"]');

                        await browser.ybAssertView(
                            `new-persons, ${common.personType.ur_0.name} создание со всеми полями, ${role} просмотр плательщица часть 1`,
                            elements.page
                        );

                        await browser.scroll('div:nth-child(10) .yb-person-details-user__title ');
                        await browser.ybAssertView(
                            `new-persons, ${common.personType.ur_0.name} создание со всеми полями, ${role} просмотр плательщица часть 2`,
                            elements.page
                        );
                    });
                    it('создание с обязательными полями, 12значный ИНН (с автозаполнение)', async function () {
                        const { browser } = this;
                        const role = 'admin';

                        await openChangePersonForm(browser, 'ur_0');

                        await takeScreenshots(
                            browser,
                            common.personType.ur_0.name,
                            'создание с обязательными полями, форма 12знач ИНН',
                            role
                        );

                        await browser.scroll('div[data-detail-id="inn"]');
                        await fillAllFieldsAutoINN(browser, '503612526896');

                        await browser.scroll('h1');
                        await takeScreenshots(
                            browser,
                            common.personType.ur_0.name,
                            'создание c обязательными полям, заполненная форма 12знач ИНН',
                            role
                        );

                        await browser.click(elements.submitButton);

                        await browser.ybWaitForInvisible('input[name="inn"]');

                        await browser.ybAssertView(
                            `new-persons, ${common.personType.ur_0.name} создание c обязательными полям 12знач ИНН, ${role} просмотр плательщица часть 1`,
                            elements.page
                        );
                        await browser.scroll('.yb-person-details-user__title');
                        await browser.ybAssertView(
                            `new-persons, ${common.personType.ur_0.name} создание c обязательными полям 12знач ИНН, ${role} просмотр плательщица часть 2`,
                            elements.page
                        );
                    });
                    it('создание c обязательными полями, 10значный ИНН (без автозаполнения), без справочника', async function () {
                        const { browser } = this;
                        const role = 'admin';

                        await openChangePersonForm(browser, 'ur_0');

                        await takeScreenshots(
                            browser,
                            common.personType.ur_0.name,
                            'создание с обязательными полями без автозаполнения, форма',
                            role
                        );

                        await browser.scroll('div[data-detail-id="inn"]');
                        await fillRequiredFields(browser);

                        await browser.scroll('h1');
                        await takeScreenshots(
                            browser,
                            common.personType.ur_0.name,
                            'создание c обязательными полями без автозаполнения, заполненная форма',
                            role
                        );

                        await browser.click(elements.submitButton);

                        await browser.ybWaitForInvisible('input[name="inn"]');

                        await browser.ybAssertView(
                            `new-persons, ${common.personType.ur_0.name} создание c обязательными полям без автозаполнения 10знач ИНН, ${role} просмотр плательщица часть 1`,
                            elements.page
                        );
                        await browser.scroll('.yb-person-details-user__title');
                        await browser.ybAssertView(
                            `new-persons, ${common.personType.ur_0.name} создание c обязательными полям 10знач ИНН, ${role} просмотр плательщица часть 2`,
                            elements.page
                        );
                    });

                    it('создание с галкой "Почтовый адрес совпадает с юридическим"', async function () {
                        const { browser } = this;

                        await openChangePersonForm(browser, 'ur_0');

                        await browser.setValue(elements.phone, '12345678');
                        await browser.setValue(elements.email, 'test@test.test');
                        await fillInn(browser, '6905000819');

                        await browser.ybAssertView(
                            'форма - создание, копируется почтовый адрес (до галки)',
                            elements.page,
                            {
                                ...assertViewOpts,
                                expandedHeight: 3500
                            }
                        );

                        await browser.click('span=по адресу');
                        await browser.click(elements.isSamePostaddress);

                        await browser.ybAssertView(
                            'форма - создание, копируется почтовый адрес (после галки)',
                            elements.page,
                            {
                                ...assertViewOpts,
                                expandedHeight: 3500
                            }
                        );

                        await browser.click(elements.submitButton);

                        await browser.ybWaitForInvisible(elements.inn);

                        await browser.ybAssertView(
                            'карточка плательщика с совпадающими адресами',
                            elements.page,
                            assertViewOpts
                        );
                    });
                });
            });
            describe('редактирование', () => {
                describe('клиент', () => {
                    it('изменение всех доступных полей', async function () {
                        const { browser } = this;
                        const { login } = await browser.ybSignIn({ isAdmin: false });
                        await browser.ybRun('create_client_with_person_for_user', {
                            login,
                            person_type: 'ur',
                            is_partner: false
                        });

                        const role = 'client';

                        await browser.ybUrl('user', `new-persons.xml`);

                        await browser.waitForVisible('.yb-persons__table-row');

                        await browser.click('.yb-persons__table-row');

                        await browser.waitForVisible('.yb-person-detail_email');

                        await browser.ybAssertView(
                            `new-persons, ${personType.ur_0.name} карточка плательщика часть 1`,
                            elements.page
                        );
                        await browser.scroll('div:nth-child(10) .yb-person-details-user__title ');
                        await browser.ybAssertView(
                            `new-persons, ${common.personType.ur_0.name} карточка плательщика часть 2`,
                            elements.page
                        );
                        await browser.ybSetSteps(`ОТкрываем форму для редактирования`);
                        await browser.click(elements.editPerson);
                        await browser.waitForVisible('input[name="inn"]');
                        await takeScreenshots(
                            browser,
                            personType.ur_0.name,
                            'форма для редактирования',
                            role
                        );

                        await replaceValues(browser);

                        await browser.scroll('h1');

                        await takeScreenshots(
                            browser,
                            personType.ur_0.name,
                            'форма после редактирования',
                            role
                        );

                        await browser.click(elements.submitButton);

                        await browser.ybWaitForInvisible('input[name="inn"]');

                        await browser.waitForVisible('.yb-person-detail_email');

                        await browser.ybAssertView(
                            `new-persons, ${personType.ur_0.name} карточка плательщика после редактирования часть 1`,
                            elements.page
                        );
                        await browser.scroll('div:nth-child(10) .yb-person-details-user__title ');
                        await browser.ybAssertView(
                            `new-persons, ${common.personType.ur_0.name} карточка плательщика после редактирования часть 2`,
                            elements.page
                        );
                    });
                });
                describe('админ', () => {
                    it('изменение всех доступных полей', async function () {
                        const { browser } = this;
                        const { login } = await browser.ybSignIn({
                            isAdmin: true,
                            isReadonly: false
                        });
                        await browser.ybRun('create_client_with_person_for_user', {
                            login,
                            person_type: 'ur',
                            is_partner: false
                        });

                        const role = 'admin';

                        await browser.ybUrl('user', `new-persons.xml`);

                        await browser.waitForVisible('.yb-persons__table-row');

                        await browser.click('.yb-persons__table-row');

                        await browser.waitForVisible('.yb-person-detail_email');

                        await browser.ybAssertView(
                            `new-persons, ${personType.ur_0.name} ${role} карточка плательщика часть 1`,
                            elements.page
                        );
                        await browser.scroll('div:nth-child(10) .yb-person-details-user__title ');
                        await browser.ybAssertView(
                            `new-persons, ${common.personType.ur_0.name} ${role} карточка плательщика часть 2`,
                            elements.page
                        );
                        await browser.ybSetSteps(`Оnкрываем форму для редактирования`);
                        await browser.click(elements.editPerson);
                        await browser.waitForVisible('input[name="inn"]');
                        await takeScreenshots(
                            browser,
                            personType.ur_0.name,
                            'форма для редактирования',
                            role
                        );

                        await replaceValues(browser, true);

                        await browser.scroll('h1');

                        await takeScreenshots(
                            browser,
                            personType.ur_0.name,
                            'форма после редактирования',
                            role
                        );

                        await browser.click(elements.submitButton);

                        await browser.ybWaitForInvisible('input[name="inn"]');

                        await browser.waitForVisible('.yb-person-detail_email');

                        await browser.ybAssertView(
                            `new-persons, ${personType.ur_0.name} ${role} карточка плательщика после редактирования часть 1`,
                            elements.page
                        );
                        await browser.scroll('div:nth-child(10) .yb-person-details-user__title ');
                        await browser.ybAssertView(
                            `new-persons, ${common.personType.ur_0.name} ${role} карточка плательщика после редактирования часть 2`,
                            elements.page
                        );
                    });

                    it('создание/редактирование с галкой "Почтовый адрес совпадает с юридическим"', async function () {
                        const { browser } = this;

                        await openChangePersonForm(browser, 'ur_0');

                        await fillInn(browser, '503612526896');

                        await browser.setValue(elements.phone, '12345678');
                        await browser.setValue(elements.email, 'test@test.test');

                        await browser.setValue(elements.legalAddressStreet, 'ул уличная');
                        await browser.setValue(elements.legalAddressPostcode, '142100');
                        await browser.setValue(
                            elements.legalAddressHome,
                            'дом для тестировщиков 1'
                        );

                        await browser.click('span=по адресу');
                        await browser.click(elements.isSamePostaddress);

                        await browser.ybAssertView(
                            'форма - создание, копируется почтовый адрес (до сохранения)',
                            elements.page,
                            {
                                ...assertViewOpts,
                                expandedHeight: 3500
                            }
                        );

                        await browser.click(elements.submitButton);

                        await browser.ybWaitForInvisible(elements.inn);

                        await browser.click(elements.editPerson);
                        await browser.waitForVisible(elements.inn);

                        await browser.ybAssertView(
                            'форма - редактирование, скопирован почтовый адрес (после сохранения)',
                            elements.page,
                            {
                                ...assertViewOpts,
                                expandedHeight: 3500
                            }
                        );

                        await browser.click(elements.isSamePostaddress);

                        await browser.ybAssertView(
                            'форма - редактирование, галка копирования адреса снята',
                            elements.page,
                            {
                                ...assertViewOpts,
                                expandedHeight: 3500
                            }
                        );
                    });
                });
            });
            describe('валидация', () => {
                it('ИНН, КПП, email', async function () {
                    const { browser } = this;
                    const { login } = await browser.ybSignIn({ isAdmin: false });
                    await browser.ybRun('create_client_for_user', {
                        login
                    });

                    await browser.ybUrl('user', `new-persons.xml`);

                    await browser.waitForVisible(elements.addEmptyPersons);

                    await browser.click(elements.addEmptyPersons);

                    await browser.ybWaitForLoad();

                    await browser.click(elements.personsType.radioButton.ur);

                    await browser.ybWaitForInvisible('button[role="listbox"][disabled]');

                    await browser.click(elements.personsType.listBox);
                    await browser.click(personType.ur_0.selector);

                    await browser.click(elements.continueButton);

                    await browser.waitForVisible('input[name="inn"]');

                    await browser.ybSetSteps(
                        `Заполнить поле "ИНН" строкой несоответствующего формата (т.е. не 10-значным или 12-значным числом). Заполнить остальные обязательные поля (помечены красной звездочкой). Нажать на кнопку "Зарегистрировать"`
                    );
                    await fillRequiredFields(browser, { inn: '12345678901' });

                    await browser.click(elements.submitButton);
                    await browser.waitForVisible('[data-detail-id="inn"] ' + elements.error);
                    await browser.ybAssertView(
                        `new-persons, ${common.personType.ur_0.name} ИНН неправильной длины`,
                        elements.page
                    );

                    await browser.ybSetSteps(
                        `Заполнить поле "ИНН" числом, не являющимся ИНН (например, 1234567890). Заполнить остальные обязательные поля (помечены красной звездочкой). Нажать на кнопку “Зарегистрировать”`
                    );
                    await browser.ybReplaceValue('input[name="inn"]', '1234567890');
                    await browser.ybWaitForInvisible('.suggest__spin-container');
                    await browser.click(elements.submitButton);
                    await browser.waitForVisible('[data-detail-id="inn"] ' + elements.error);
                    await browser.ybAssertView(
                        `new-persons, ${common.personType.ur_0.name} ИНН число, которое не является ИНН`,
                        elements.page
                    );

                    await browser.ybSetSteps(
                        `Заполнить поле "ИНН" валидным значением (например, 3358359869)`
                    );
                    await browser.ybReplaceValue('input[name="inn"]', '3358359869');
                    await browser.ybWaitForInvisible('.suggest__spin-container');
                    await browser.ybReplaceValue('input[name="kpp"]', '12345678');
                    await browser.click(elements.submitButton);
                    await browser.waitForVisible('[data-detail-id="kpp"] ' + elements.error);
                    await browser.ybAssertView(
                        `new-persons, ${common.personType.ur_0.name} ИНН валиден, КПП нет`,
                        elements.page
                    );
                    await browser.ybSetSteps(`Заполнить поле "КПП" строкой несоответствующего формата (т.е. не 9-значным числом). Заполнить остальные обязательные поля (помечены красной звездочкой). Нажать на кнопку “Зарегистрировать”.
Заполнить поле “Электронная почта” форматом, не соответствующим email’y. Заполнить остальные обязательные поля (помечены красной звездочкой). Нажать на кнопку “Зарегистрировать”.`);
                    await browser.ybReplaceValue('input[name="kpp"]', '1');
                    await browser.ybReplaceValue('input[name="email"]', 'я не почта');
                    await browser.click(elements.submitButton);
                    await browser.waitForVisible('[data-detail-id="email"] ' + elements.error);
                    await browser.ybAssertView(
                        `new-persons, ${common.personType.ur_0.name} Неправильный формат КПП, неправильный EMAIL`,
                        elements.page
                    );
                });
                it('ОГРН, почтовый индекс', async function () {
                    const { browser } = this;
                    const { login } = await browser.ybSignIn({ isAdmin: false });
                    await browser.ybRun('create_client_for_user', {
                        login
                    });

                    const role = 'client';

                    await browser.ybUrl('user', `new-persons.xml`);

                    await browser.waitForVisible(elements.addEmptyPersons);

                    await browser.click(elements.addEmptyPersons);

                    await browser.ybWaitForLoad();

                    await browser.click(elements.personsType.radioButton.ur);

                    await browser.ybWaitForInvisible('button[role="listbox"][disabled]');

                    await browser.click(elements.personsType.listBox);
                    await browser.click(personType.ur_0.selector);

                    await browser.click(elements.continueButton);

                    await browser.waitForVisible('input[name="inn"]');

                    await browser.ybSetSteps(`Ввести в поля "ИНН" и “КПП” не цифровые символы.
Заполнить поле “ОГРН” числом несоответствующего формата (т.е. не 13-значным или 15-значным числом). Заполнить остальные обязательные поля (помечены красной звездочкой). Нажать на кнопку “Зарегистрировать”`);
                    await fillRequiredFields(browser, {
                        inn: 'не число',
                        kpp: 'не число',
                        ogrn: '1234567890'
                    });
                    await browser.click(elements.submitButton);
                    await browser.waitForVisible('[data-detail-id="inn"] ' + elements.error);
                    await browser.ybAssertView(
                        `new-persons, валидация ${common.personType.ur_0.name} Не цифровые ИНН и КПП, ОГРН неправильного формата`,
                        elements.page
                    );

                    await browser.ybSetSteps(
                        `Заполнить поле “ОГРН” 13- или 15-значной строкой, состоящей не только из цифр. Заполнить остальные обязательные поля (помечены красной звездочкой). Нажать на кнопку “Зарегистрировать”`
                    );
                    await browser.ybReplaceValue('input[name="inn"]', '3358359869');
                    await browser.ybWaitForInvisible('.suggest__spin-container');
                    await browser.ybReplaceValue('input[name="kpp"]', '1234567890');
                    await browser.ybReplaceValue('input[name="ogrn"]', '123456TEST123');
                    await browser.click(elements.submitButton);
                    await browser.waitForVisible('[data-detail-id="ogrn"] ' + elements.error);
                    await browser.ybAssertView(
                        `new-persons, валидация ${common.personType.ur_0.name} ОГРН не только цифры в поле`,
                        elements.page
                    );

                    await browser.ybSetSteps(
                        `Заполнить поле "Почтовый индекс" в блоке "Юридический адрес" числом несоответствующего формата (т.е. не 6-значным числом).`
                    );

                    await browser.ybClearValue('input[name="ogrn"]');
                    await browser.scroll('[data-detail-id="legalAddrType"]');

                    await browser.click('span=по справочнику');
                    await browser.setValue('[name="legal-address-city"]', 'Москва');
                    let valueSelector = `div[data-search="г Москва"]`;
                    await browser.waitForVisible(valueSelector);
                    await browser.click(valueSelector);
                    await browser.setValue('[name="legal-address-postcode"]', '123');

                    await browser.click(elements.submitButton);
                    await browser.waitForVisible(
                        '[data-detail-id="legalAddressPostcode"] ' + elements.error
                    );
                    await browser.ybAssertView(
                        `new-persons, валидация Юридический адрес по справочнику ${common.personType.ur_0.name} Почтовый индекс неправильной длинны`,
                        elements.page,
                        { ignoreElements }
                    );

                    await browser.ybSetSteps(
                        `Заполнить поле “Почтовый индекс” в блоке “Юридический адрес” 6-значной строкой, состоящей не только из цифр. Заполнить остальные обязательные поля (помечены красной звездочкой). Нажать на кнопку “Зарегистрировать”`
                    );

                    await browser.ybClearValue('[name="legal-address-postcode"]');
                    await browser.setValue('[name="legal-address-postcode"]', 'TEST12');
                    await browser.click(elements.submitButton);
                    await browser.waitForVisible(
                        '[data-detail-id="legalAddressPostcode"] ' + elements.error
                    );
                    await browser.ybAssertView(
                        `new-persons, валидация Юридический адрес по справочнику ${common.personType.ur_0.name} Почтовый индекс не только цифры`,
                        elements.page,
                        { ignoreElements }
                    );

                    // дозаполняем обязательные поля, которые образовалиьс после последних проверок
                    await browser.click('span=без справочника');
                    await browser.setValue('textarea[name="legaladdress"]', 'ул уличная');

                    await browser.ybSetSteps(
                        `Повторить предыдущие 2 шага в поле "Почтовый индекс" блоке "Почтовый адрес" для обоих вариантов доставки.`
                    );

                    await browser.scroll('[data-detail-id="envelopeAddress"]');
                    await browser.setValue('[name="postsuffix"]', 'а123');

                    await browser.ybReplaceValue('input[name="postcode"]', '123');
                    await browser.click(elements.submitButton);
                    await browser.waitForVisible(
                        '[data-detail-id="postcodeSimple"] ' + elements.error
                    );
                    await browser.ybAssertView(
                        `new-persons, валидация Почтовый адрес, абонентский ящик ${common.personType.ur_0.name} Почтовый индекс неправильной длинны`,
                        elements.page,
                        { ignoreElements }
                    );

                    await browser.ybReplaceValue('input[name="postcode"]', 'TEST12');
                    await browser.click(elements.submitButton);
                    await browser.waitForVisible(
                        '[data-detail-id="postcodeSimple"] ' + elements.error
                    );
                    await browser.ybAssertView(
                        `new-persons, валидация Почтовый адрес абонентский ящик ${common.personType.ur_0.name} Почтовый индекс не только цифры`,
                        elements.page,
                        { ignoreElements }
                    );

                    await browser.click('span=по адресу');

                    await browser.ybClearValue('[name="city"]');
                    await browser.setValue('[name="city"]', 'Москва');
                    valueSelector = `div[data-search="г Москва"]`;
                    await browser.waitForVisible(valueSelector);
                    await browser.click(valueSelector);
                    await browser.setValue('[name="postcode"]', 'TEST12');
                    await browser.setValue('[name="postsuffix"]', 'а123');
                    await browser.click(elements.submitButton);
                    await browser.waitForVisible('[data-detail-id="postcode"] ' + elements.error);
                    await browser.ybAssertView(
                        `new-persons, валидация Почтовый адрес по адресу ${common.personType.ur_0.name} Почтовый индекс не только цифры`,
                        elements.page,
                        { ignoreElements }
                    );

                    await browser.ybClearValue('[name="postcode"]');
                    await browser.setValue('[name="postcode"]', '123');
                    await browser.click(elements.submitButton);
                    await browser.waitForVisible('[data-detail-id="postcode"] ' + elements.error);
                    await browser.ybAssertView(
                        `new-persons, валидация Почтовый адрес по адресу ${common.personType.ur_0.name} Почтовый индекс неправильной длинны`,
                        elements.page,
                        { ignoreElements }
                    );
                });
                it('БИК, расчетный счет', async function () {
                    const { browser } = this;
                    const { login } = await browser.ybSignIn({ isAdmin: false });
                    await browser.ybRun('create_client_for_user', {
                        login
                    });

                    await browser.ybUrl('user', `new-persons.xml`);

                    await browser.waitForVisible(elements.addEmptyPersons);

                    await browser.click(elements.addEmptyPersons);

                    await browser.ybWaitForLoad();

                    await browser.click(elements.personsType.radioButton.ur);

                    await browser.ybWaitForInvisible('button[role="listbox"][disabled]');

                    await browser.click(elements.personsType.listBox);
                    await browser.click(personType.ur_0.selector);

                    await browser.click(elements.continueButton);

                    await browser.waitForVisible('input[name="inn"]');

                    await browser.ybSetSteps(
                        `Заполнить поле “БИК” числом несоответствующего формата (т.е. не 9-значным числом). Заполнить остальные обязательные поля (помечены красной звездочкой). Нажать на кнопку “Зарегистрировать”`
                    );
                    await fillRequiredFields(browser, { bik: '1234567' });
                    await browser.scroll('[data-detail-id="postbox"]');
                    await browser.click(elements.submitButton);
                    await browser.waitForVisible('[data-detail-id="bik"] ' + elements.error);
                    await browser.ybAssertView(
                        `new-persons, валидация ${common.personType.ur_0.name} БИК неправильного размера`,
                        elements.page,
                        { ignoreElements }
                    );

                    await browser.ybSetSteps(
                        `Заполнить поле “БИК” числом, не являющимся БИК (например, 123456789). Заполнить остальные обязательные поля (помечены красной звездочкой). Нажать на кнопку “Зарегистрировать”`
                    );
                    await browser.ybReplaceValue('input[name="bik"]', '123456789');
                    await browser.click(elements.submitButton);
                    await browser.waitForVisible('[data-detail-id="bik"] ' + elements.error);
                    await browser.ybAssertView(
                        `new-persons, валидация ${common.personType.ur_0.name} несуществующий БИК`,
                        elements.page,
                        { ignoreElements }
                    );

                    await browser.ybSetSteps(
                        `Заполнить поле “Расчетный счет” числом несоответствующего формата (т.е. не 20-значным числом). Заполнить поле “БИК” корректным значением. Заполнить остальные обязательные поля (помечены красной звездочкой). Нажать на кнопку “Зарегистрировать”`
                    );
                    await browser.ybReplaceValue('input[name="bik"]', '044030653');
                    await browser.setValue('[name="account"]', '123');
                    await browser.click(elements.submitButton);
                    await browser.waitForVisible('[data-detail-id="account"] ' + elements.error);
                    await browser.ybAssertView(
                        `new-persons, валидация ${common.personType.ur_0.name} корректный БИК, неправильный размер рассчетного счета`,
                        elements.page,
                        { ignoreElements }
                    );

                    await browser.ybSetSteps(
                        `Заполнить поле “Расчетный счет” числом, не являющимся р/с (например, 12345123451234512345). Заполнить поле “БИК” корректным значением. Заполнить остальные обязательные поля (помечены красной звездочкой). Нажать на кнопку “Зарегистрировать”`
                    );
                    await browser.ybReplaceValue('input[name="bik"]', '044030653');
                    await browser.ybClearValue('[name="account"]');
                    await browser.setValue('[name="account"]', '12345123451234512345');
                    await browser.click(elements.submitButton);
                    await browser.waitForVisible('[data-detail-id="account"] ' + elements.error);
                    await browser.ybAssertView(
                        `new-persons, валидация ${common.personType.ur_0.name} корректный БИК, несуществующий р.с.`,
                        elements.page,
                        { ignoreElements }
                    );

                    await browser.ybSetSteps(
                        `Заполнить поле “Расчетный счет” корректным значением. Не заполнять поле “БИК”. Заполнить остальные обязательные поля (помечены красной звездочкой). Нажать на кнопку “Зарегистрировать”. Проверяем, что требует заполнить поле “БИК”. Провекра р/с при этом не происходит, поле не подствечивается ошибкой.`
                    );
                    await browser.ybClearValue('input[name="bik"]');
                    await browser.ybClearValue('[name="account"]');
                    await browser.setValue('[name="account"]', '40817810455000000131');
                    await browser.click(elements.submitButton);
                    await browser.waitForVisible('[data-detail-id="bik"] ' + elements.error);
                    await browser.ybAssertView(
                        `new-persons, валидация ${common.personType.ur_0.name} правильный расчетный счет, пустой БИК`,
                        elements.page,
                        { ignoreElements }
                    );

                    await browser.ybSetSteps(
                        `Заполнить поле “БИК” числом, не являющимся БИК (например, 123456789). Заполнить поле “Расчетный счет” корректным значениме. Нажать на кнопку “Зарегистрировать”. Проверяем, что провекра р/с при этом не происходит, поле не подствечивается ошибкой.`
                    );
                    await browser.ybReplaceValue('input[name="bik"]', '123456789');
                    await browser.ybClearValue('[name="account"]');
                    await browser.setValue('[name="account"]', '40817810455000000131');
                    await browser.click(elements.submitButton);
                    await browser.waitForVisible('[data-detail-id="bik"] ' + elements.error);
                    await browser.ybAssertView(
                        `new-persons, валидация ${common.personType.ur_0.name} неправильный БИК, правильный расчетный счет`,
                        elements.page,
                        { ignoreElements }
                    );
                });
                it('Не требовать РС, если заполнен БИК', async function () {
                    const { browser } = this;
                    const { login } = await browser.ybSignIn({ isAdmin: false });
                    await browser.ybRun('create_client_for_user', {
                        login
                    });

                    await browser.ybUrl('user', `new-persons.xml`);

                    await browser.waitForVisible(elements.addEmptyPersons);

                    await browser.click(elements.addEmptyPersons);

                    await browser.ybWaitForLoad();

                    await browser.click(elements.personsType.radioButton.ur);

                    await browser.ybWaitForInvisible('button[role="listbox"][disabled]');

                    await browser.click(elements.personsType.listBox);
                    await browser.click(personType.ur_0.selector);

                    await browser.click(elements.continueButton);

                    await browser.waitForVisible('input[name="inn"]');
                    await fillRequiredFields(browser, { bik: '044030001' });
                    await browser.click(elements.submitButton);
                    await browser.ybWaitForInvisible('[name="inn"]');
                    await browser.ybAssertView(
                        `new-persons, валидация ${common.personType.ur_0.name} БИК заполнен, р.с. не требуется`,
                        elements.page
                    );
                });
            });
            it('проверка обязательности', async function () {
                const { browser } = this;
                const { login } = await browser.ybSignIn({ isAdmin: false });
                await browser.ybRun('create_client_for_user', {
                    login
                });
                const role = 'client';
                await browser.ybUrl('user', `new-persons.xml`);

                await browser.waitForVisible(elements.addEmptyPersons);

                await browser.click(elements.addEmptyPersons);

                await browser.ybWaitForLoad();

                await browser.click(elements.personsType.radioButton.ur);

                await browser.ybWaitForInvisible('button[role="listbox"][disabled]');

                await browser.click(elements.personsType.listBox);
                await browser.click(personType.ur_0.selector);

                await browser.click(elements.continueButton);

                await browser.waitForVisible('input[name="inn"]');

                await takeScreenshots(
                    browser,
                    common.personType.ur_0.name,
                    'проверка обязательности, форма',
                    role
                );

                await browser.click(elements.submitButton);

                await takeScreenshots(
                    browser,
                    common.personType.ur_0.name,
                    'проверка обязательности, форма, ошибки',
                    role
                );
            });
        });
    });
});
