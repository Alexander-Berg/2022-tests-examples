const { personType } = require('../common');
const { elements } = require('../elements');

const {
    fillRequiredFields,
    fillAllFields,
    takeScreenshots,
    ReplaceAllPossibleFields
} = require('./helpers');

describe('user', () => {
    describe('new-persons', () => {
        describe(`${personType.by_ytph.name}`, () => {
            describe('создание', () => {
                describe('клиент', () => {
                    it('создание со обязательными полямм', async function () {
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
                        await browser.click(personType.by_ytph.selector);

                        await browser.click(elements.continueButton);

                        await browser.waitForVisible(elements.submitButton);

                        await takeScreenshots(
                            browser,
                            `${personType.by_ytph.name} создание с обяз. полями, форма, клиент`
                        );

                        await fillRequiredFields(browser);

                        await takeScreenshots(
                            browser,
                            `${personType.by_ytph.name} создание с обяз. полями, заполненная форма, клиент`
                        );

                        await browser.click(elements.submitButton);

                        await browser.ybWaitForInvisible(elements.submitButton);

                        await browser.waitForVisible('.yb-person-detail_email');

                        await browser.ybAssertView(
                            `new-persons, ${personType.by_ytph.name}, создание с обяз. полями, карточка плательщика клиент`,
                            elements.page
                        );
                    });
                    it('создание со всеми полями', async function () {
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
                        await browser.click(personType.by_ytph.selector);

                        await browser.click(elements.continueButton);

                        await browser.waitForVisible(elements.submitButton);

                        await takeScreenshots(
                            browser,
                            `${personType.by_ytph.name} создание со всеми полями, форма, клиент`
                        );

                        await fillAllFields(browser);

                        await takeScreenshots(
                            browser,
                            `${personType.by_ytph.name} создание со всеми  полями, заполненная форма, клиент`
                        );

                        await browser.click(elements.submitButton);

                        await browser.ybWaitForInvisible(elements.submitButton);

                        await browser.waitForVisible('.yb-person-detail_email');

                        await browser.ybAssertView(
                            `new-persons, ${personType.by_ytph.name}, создание со всеми полями, карточка плательщика клиент`,
                            elements.page
                        );
                    });
                });
            });
            describe('редактирование', () => {
                describe('клиент', () => {
                    it('редактирование всех доступных полей', async function () {
                        const { browser } = this;
                        const { login } = await browser.ybSignIn({
                            isAdmin: false
                        });
                        const { person_id } = await browser.ybRun(
                            'create_client_with_person_for_user',
                            {
                                login,
                                person_type: 'by_ytph',
                                is_partner: false
                            }
                        );

                        await browser.ybUrl('user', `new-persons.xml`);

                        await browser.waitForVisible('.yb-persons__table-row');

                        await browser.ybUrl('user', `new-persons.xml#/${person_id}`);

                        await browser.waitForVisible('.yb-person-detail_email');

                        await browser.ybAssertView(
                            `new-persons, ${personType.by_ytph.name} карточка плательщика клиент (перед редактированием)`,
                            elements.page
                        );

                        await browser.click(elements.editPerson);

                        await browser.waitForVisible(elements.submitButton);

                        await takeScreenshots(
                            browser,
                            `${personType.by_ytph.name}, форма перед редакитрованием, клиент`
                        );

                        await ReplaceAllPossibleFields(browser);

                        await browser.scroll('h1');
                        await browser.ybAssertView(
                            `new-persons, ${personType.by_ytph.name}, форма после редакитрованием, клиент, часть 1`,
                            elements.page
                        );
                        await browser.scroll(elements.city);
                        await browser.ybAssertView(
                            `new-persons, ${personType.by_ytph.name}, форма после редакитрованием, клиент, часть 2`,
                            elements.page
                        );

                        await browser.click(elements.submitButton);

                        await browser.ybWaitForInvisible(elements.submitButton);

                        await browser.waitForVisible('.yb-person-detail_email');

                        await browser.ybAssertView(
                            `new-persons, ${personType.by_ytph.name}, карточка после редакитрования, клиент`,
                            elements.page
                        );
                    });
                });
            });
            it('Проверка обязательности', async function () {
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
                await browser.click(personType.by_ytph.selector);

                await browser.click(elements.continueButton);

                await browser.waitForVisible(elements.submitButton);

                await browser.ybSetSteps(
                    `Проверяем, что поля являются обязательными для заполнения при создании`
                );

                await browser.click(elements.submitButton);
                await browser.waitForVisible('[data-detail-id="fname"] ' + elements.error);
                await takeScreenshots(
                    browser,
                    `${personType.by_ytph.name}, проверка обязтельности полей при создании плательщика, клиент`
                );

                await browser.ybSetSteps(`Заполняем обязательные поля и создаем плательщика`);

                await fillRequiredFields(browser);
                await browser.click(elements.submitButton);
                await browser.ybWaitForInvisible(elements.submitButton);
                await browser.click(elements.editPerson);
                await browser.waitForVisible(elements.submitButton);

                await browser.ybSetSteps(`Проверяем обязательность полей при редактировании`);

                await browser.ybClearValue(elements.fname);
                await browser.ybClearValue(elements.lname);
                await browser.ybClearValue(elements.phone);
                await browser.ybClearValue(elements.email);
                await browser.ybClearValue(elements.postaddress);
                await browser.ybClearValue(elements.city);

                await browser.click(elements.submitButton);
                await browser.waitForVisible('[data-detail-id="fname"] ' + elements.error);
                await takeScreenshots(
                    browser,
                    `${personType.by_ytph.name}, проверка обязательности при редактировании, клиент`
                );
            });
        });
    });
});
