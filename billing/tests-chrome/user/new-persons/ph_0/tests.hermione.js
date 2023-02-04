const { personType } = require('../common');

const { elements, ignoreElements } = require('../elements');

const { fillRequiredFields, fillAllFields, takeScreenshots, ReplaceValues } = require('./helpers');

describe('user', () => {
    describe('new-persons', () => {
        describe(`${personType.ph_0.name}`, () => {
            describe('создание', () => {
                describe('клиент', () => {
                    it(`Создание с обязательными полями`, async function () {
                        const { browser } = this;
                        const { login } = await browser.ybSignIn({ isAdmin: false });
                        await browser.ybRun('create_client_for_user', {
                            login
                        });

                        await browser.ybUrl('user', `new-persons.xml`);

                        await browser.waitForVisible(elements.addEmptyPersons);

                        await browser.click(elements.addEmptyPersons);

                        await browser.ybWaitForLoad();

                        await browser.click(elements.personsType.radioButton.ph);

                        await browser.ybWaitForInvisible('button[role="listbox"][disabled]');

                        await browser.click(elements.personsType.listBox);
                        await browser.click(personType.ph_0.selector);

                        await browser.click(elements.continueButton);

                        await browser.waitForVisible(elements.submitButton);

                        await takeScreenshots(browser, `форма создания (обяз поля), клиент`);

                        await fillRequiredFields(browser);

                        await takeScreenshots(
                            browser,
                            'заполненная форма создания (обяз поля), клиент'
                        );

                        await browser.click(elements.submitButton);

                        await browser.ybWaitForInvisible(elements.submitButton);

                        await browser.waitForVisible('.yb-person-detail_email');

                        await browser.ybAssertView(
                            `new-persons, ${personType.ph_0.name}, карточка плательщика (обяз поля), клиент`,
                            elements.page
                        );
                    });
                    it(`Создание со всеми полями`, async function () {
                        const { browser } = this;
                        const { login } = await browser.ybSignIn({ isAdmin: false });
                        await browser.ybRun('create_client_for_user', {
                            login
                        });

                        await browser.ybUrl('user', `new-persons.xml`);

                        await browser.waitForVisible(elements.addEmptyPersons);

                        await browser.click(elements.addEmptyPersons);

                        await browser.ybWaitForLoad();

                        await browser.click(elements.personsType.radioButton.ph);

                        await browser.ybWaitForInvisible('button[role="listbox"][disabled]');

                        await browser.click(elements.personsType.listBox);
                        await browser.click(personType.ph_0.selector);

                        await browser.click(elements.continueButton);

                        await browser.waitForVisible(elements.submitButton);

                        await takeScreenshots(browser, `форма создания (все поля), клиент`);

                        await fillAllFields(browser);

                        await browser.scroll('.yb-change-person-root__header');
                        await takeScreenshots(
                            browser,
                            'заполненная форма создания (все поля), клиент'
                        );

                        await browser.click(elements.submitButton);

                        await browser.ybWaitForInvisible(elements.submitButton);

                        await browser.waitForVisible('.yb-person-detail_email');

                        await browser.ybAssertView(
                            `new-persons, ${personType.ph_0.name}, карточка плательщика (все поля), клиент`,
                            elements.page
                        );
                    });
                });
                describe('админ', () => {
                    it(`Создание с обязательными полями`, async function () {
                        const { browser } = this;
                        const { login } = await browser.ybSignIn({
                            isAdmin: true,
                            isReadonly: false
                        });
                        await browser.ybRun('create_client_for_user', {
                            login
                        });

                        await browser.ybUrl('user', `new-persons.xml`);

                        await browser.waitForVisible(elements.addEmptyPersons);

                        await browser.click(elements.addEmptyPersons);

                        await browser.ybWaitForLoad();

                        await browser.click(elements.personsType.radioButton.ph);

                        await browser.ybWaitForInvisible('button[role="listbox"][disabled]');

                        await browser.click(elements.personsType.listBox);
                        await browser.click(personType.ph_0.selector);

                        await browser.click(elements.continueButton);

                        await browser.waitForVisible(elements.submitButton);

                        await takeScreenshots(browser, `форма создания (обяз поля), админ`);

                        await fillRequiredFields(browser);

                        await takeScreenshots(
                            browser,
                            'заполненная форма создания (обяз поля), админ'
                        );

                        await browser.click(elements.submitButton);

                        await browser.ybWaitForInvisible(elements.submitButton);

                        await browser.waitForVisible('.yb-person-detail_email');

                        await browser.ybAssertView(
                            `new-persons, ${personType.ph_0.name}, карточка плательщика (обяз поля) админ`,
                            elements.page
                        );
                    });
                    it(`Создание со всеми полями`, async function () {
                        const { browser } = this;
                        const { login } = await browser.ybSignIn({
                            isAdmin: true,
                            isReadonly: false
                        });
                        await browser.ybRun('create_client_for_user', {
                            login
                        });

                        await browser.ybUrl('user', `new-persons.xml`);

                        await browser.waitForVisible(elements.addEmptyPersons);

                        await browser.click(elements.addEmptyPersons);

                        await browser.ybWaitForLoad();

                        await browser.click(elements.personsType.radioButton.ph);

                        await browser.ybWaitForInvisible('button[role="listbox"][disabled]');

                        await browser.click(elements.personsType.listBox);
                        await browser.click(personType.ph_0.selector);

                        await browser.click(elements.continueButton);

                        await browser.waitForVisible(elements.submitButton);

                        await takeScreenshots(browser, `форма создания (все поля), админ`);

                        await fillAllFields(browser, true);

                        await browser.scroll('.yb-change-person-root__header');
                        await takeScreenshots(
                            browser,
                            'заполненная форма создания (все поля), админ'
                        );

                        await browser.click(elements.submitButton);

                        await browser.ybWaitForInvisible(elements.submitButton);

                        await browser.waitForVisible('.yb-person-detail_email');

                        await browser.ybAssertView(
                            `new-persons, ${personType.ph_0.name}, карточка плательщика (все поля), админ`,
                            elements.page
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
                            person_type: 'ph',
                            is_partner: false
                        });

                        await browser.ybUrl('user', `new-persons.xml`);

                        await browser.waitForVisible('.yb-persons__table-row');
                        await browser.click('.yb-persons__table-row');

                        await browser.waitForVisible('.yb-person-detail_email');

                        await browser.ybAssertView(
                            `new-persons, ${personType.ph_0.name} карточка плательщика клиент (перед редактированием)`,
                            elements.page
                        );

                        await browser.ybSetSteps(`Открываем форму для редактирования`);
                        await browser.click(elements.editPerson);
                        await browser.waitForVisible(elements.submitButton);
                        await takeScreenshots(browser, 'форма для редактирования, клиент');

                        await ReplaceValues(browser);

                        await browser.scroll('h1');

                        await takeScreenshots(browser, 'форма после редактирования, клиент');

                        await browser.click(elements.submitButton);

                        await browser.ybWaitForInvisible(elements.submitButton);

                        await browser.waitForVisible('.yb-person-detail_email');

                        await browser.ybAssertView(
                            `new-persons, ${personType.ph_0.name} карточка плательщика после редактирования, клиент`,
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
                            person_type: 'ph',
                            is_partner: false
                        });

                        await browser.ybUrl('user', `new-persons.xml`);

                        await browser.waitForVisible('.yb-persons__table-row');

                        await browser.click('.yb-persons__table-row');

                        await browser.waitForVisible('.yb-person-detail_email');

                        await browser.ybAssertView(
                            `new-persons, ${personType.ph_0.name} карточка плательщика админ (перед редактированием)`,
                            elements.page
                        );

                        await browser.ybSetSteps(`Открываем форму для редактирования`);
                        await browser.click(elements.editPerson);
                        await browser.waitForVisible(elements.submitButton);
                        await takeScreenshots(browser, 'форма для редактирования, админ');

                        await ReplaceValues(browser, true);

                        await browser.scroll('h1');

                        await takeScreenshots(browser, 'форма после редактирования, админ');

                        await browser.click(elements.submitButton);

                        await browser.ybWaitForInvisible(elements.submitButton);

                        await browser.waitForVisible('.yb-person-detail_email');

                        await browser.ybAssertView(
                            `new-persons, ${personType.ph_0.name} карточка плательщика после редактирования, админ`,
                            elements.page
                        );
                    });
                });
            });
            describe('валидация', () => {
                // как в vhost/hermione/tests-chrome/admin/change-person/ph/tests.hermione.js
                it('Напрямую открытие создания плательищка', async function () {
                    const { browser } = this;
                    const { login } = await browser.ybSignIn({ isAdmin: false });
                    await browser.ybRun('create_client_for_user', {
                        login
                    });

                    await browser.ybUrl('user', `new-persons.xml#/new`);
                    await browser.waitForVisible(elements.continueButton);

                    await browser.ybAssertView(
                        `new-persons, ${personType.ph_0.name}, поле созданния плательщика`,
                        elements.page
                    );
                });

                it('Обязательные поля', async function () {
                    const { browser } = this;
                    const { login } = await browser.ybSignIn({ isAdmin: false });
                    await browser.ybRun('create_client_for_user', {
                        login
                    });

                    await browser.ybUrl('user', `new-persons.xml`);

                    await browser.waitForVisible(elements.addEmptyPersons);

                    await browser.click(elements.addEmptyPersons);

                    await browser.ybWaitForLoad();

                    await browser.click(elements.personsType.radioButton.ph);

                    await browser.ybWaitForInvisible('button[role="listbox"][disabled]');

                    await browser.click(elements.personsType.listBox);
                    await browser.click(personType.ph_0.selector);

                    await browser.click(elements.continueButton);

                    await browser.waitForVisible(elements.submitButton);

                    await browser.ybSetSteps(
                        `Проверяем, что поля являются обязательными для заполнения`
                    );
                    await browser.click(elements.submitButton);
                    await browser.waitForVisible('[data-detail-id="lname"] ' + elements.error);

                    await takeScreenshots(
                        browser,
                        'Форма для создания плательщика, обязательные поля, клиент'
                    );

                    await browser.ybSetSteps(`Заполняем обязательные поля и создаем плательщика`);
                    await fillRequiredFields(browser);

                    await browser.click(elements.submitButton);

                    await browser.ybWaitForInvisible(elements.submitButton);

                    await browser.ybSetSteps(
                        `Открывает форму для редктирования плательщика и стираем обязательные поля`
                    );

                    await browser.click(elements.editPerson);

                    await browser.waitForVisible(elements.submitButton);

                    await browser.ybClearValue('[name="lname"]');
                    await browser.ybClearValue('[name="fname"]');
                    await browser.ybClearValue('[name="mname"]');
                    await browser.ybClearValue('[name="phone"]');
                    await browser.ybClearValue('[name="email"]');

                    await browser.click(elements.submitButton);

                    await takeScreenshots(
                        browser,
                        'Форма для редакитрования плательщиков, проверка обязательности, клиент'
                    );

                    await browser.setValue('[name="lname"]', 'Тестов');
                    await browser.setValue('[name="fname"]', 'Тест');
                    await browser.setValue('[name="mname"]', 'Тестович');

                    await browser.setValue('[name="phone"]', '81231231212');

                    await browser.setValue('[name="email"]', 'test@test.test');

                    await browser.scroll('h1');

                    await takeScreenshots(
                        browser,
                        'Форма редактирования после заполнения обязательных полей, клиент, обязательные поля'
                    );

                    await browser.click(elements.submitButton);

                    await browser.ybWaitForInvisible(elements.submitButton);

                    await browser.waitForVisible('.yb-person-detail_email');

                    await browser.ybAssertView(
                        `new-persons, ${personType.ph_0.name}, карточка плательщика после редактирования, обязательные поля`,
                        elements.page
                    );
                });
            });
        });
    });
});
