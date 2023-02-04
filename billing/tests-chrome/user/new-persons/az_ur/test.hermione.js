const { personType } = require('../common');
const { elements } = require('../elements');

const {
    fillRequiredFields,
    fillAllFields,
    takeScreenshots,
    ReplaceAllPossibleFields
} = require('./helpers');

describe(`user`, () => {
    describe('new-persons', () => {
        describe(`${personType.az_ur.name}`, () => {
            describe(`создание`, () => {
                describe('клиент', () => {
                    it(`создание с обязательными полями`, async function () {
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
                        await browser.click(personType.az_ur.selector);

                        await browser.click(elements.continueButton);

                        await browser.waitForVisible(elements.submitButton);

                        await takeScreenshots(
                            browser,
                            `${personType.az_ur.name} создание с обяз. полями, форма, клиент`
                        );

                        await fillRequiredFields(browser);

                        await takeScreenshots(
                            browser,
                            `${personType.az_ur.name} создание с обяз. полями, заполненная форма, клиент`
                        );

                        await browser.click(elements.submitButton);

                        await browser.ybWaitForInvisible(elements.submitButton);

                        await browser.waitForVisible('.yb-person-detail_email');

                        await browser.ybAssertView(
                            `new-persons, ${personType.az_ur.name}, создание с обяз. полями, карточка плательщика клиент`,
                            elements.page
                        );
                    });
                    it('создание со всеми полями, IBAN', async function () {
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
                        await browser.click(personType.az_ur.selector);

                        await browser.click(elements.continueButton);

                        await browser.waitForVisible(elements.submitButton);

                        await browser.click('[data-detail-id="payType"]');
                        await browser.click(elements.payType.iban.list);

                        await takeScreenshots(
                            browser,
                            `${personType.az_ur.name} создание со всеми полями (IBAN), форма, клиент`
                        );

                        await fillAllFields(browser, 'IBAN');

                        await takeScreenshots(
                            browser,
                            `${personType.az_ur.name} создание со всеми полями (IBAN), заполненная форма, клиент`
                        );

                        await browser.click(elements.submitButton);

                        await browser.ybWaitForInvisible(elements.submitButton);

                        await browser.waitForVisible('.yb-person-detail_email');

                        await browser.ybAssertView(
                            `new-persons, ${personType.az_ur.name}, создание со всеми полями (IBAN), карточка плательщика клиент`,
                            elements.page
                        );
                    });
                    it('создание со всеми полями, р.с.', async function () {
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
                        await browser.click(personType.az_ur.selector);

                        await browser.click(elements.continueButton);

                        await browser.waitForVisible(elements.submitButton);

                        await browser.click('[data-detail-id="payType"]');
                        await browser.click(elements.payType.account.list);

                        await takeScreenshots(
                            browser,
                            `${personType.az_ur.name} создание со всеми полями (Расчетный счет), форма, клиент`
                        );

                        await fillAllFields(browser, 'Расчетный счет');

                        await takeScreenshots(
                            browser,
                            `${personType.az_ur.name} создание со всеми полями (Расчетный счет), заполненная форма, клиент`
                        );

                        await browser.click(elements.submitButton);

                        await browser.ybWaitForInvisible(elements.submitButton);

                        await browser.waitForVisible('.yb-person-detail_email');

                        await browser.ybAssertView(
                            `new-persons, ${personType.az_ur.name}, создание со всеми полями (Расчетный счет), карточка плательщика клиент`,
                            elements.page
                        );
                    });
                    it('создание со всеми полями, вид счета: прочее', async function () {
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
                        await browser.click(personType.az_ur.selector);

                        await browser.click(elements.continueButton);

                        await browser.waitForVisible(elements.submitButton);

                        await browser.click('[data-detail-id="payType"]');
                        await browser.click(elements.payType.other.list);

                        await takeScreenshots(
                            browser,
                            `${personType.az_ur.name} создание со всеми полями (Прочее), форма, клиент`
                        );

                        await fillAllFields(browser, 'Прочее');

                        await takeScreenshots(
                            browser,
                            `${personType.az_ur.name} создание со всеми полями (Прочее), заполненная форма, клиент`
                        );

                        await browser.click(elements.submitButton);

                        await browser.ybWaitForInvisible(elements.submitButton);

                        await browser.waitForVisible('.yb-person-detail_email');

                        await browser.ybAssertView(
                            `new-persons, ${personType.az_ur.name}, создание со всеми полями (Прочее), карточка плательщика клиент`,
                            elements.page
                        );
                    });
                });
            });
            describe('редактирование', () => {
                describe('клиент', () => {
                    it('редактирование всех возможный полей', async function () {
                        const { browser } = this;
                        const { login } = await browser.ybSignIn({
                            isAdmin: false
                        });
                        const { person_id } = await browser.ybRun(
                            'create_client_with_person_for_user',
                            {
                                login,
                                person_type: 'az_ur',
                                is_partner: false
                            }
                        );

                        await browser.ybUrl('user', `new-persons.xml`);

                        await browser.waitForVisible('.yb-persons__table-row');

                        await browser.ybUrl('user', `new-persons.xml#/${person_id}`);

                        await browser.waitForVisible('.yb-person-detail_email');

                        await browser.ybAssertView(
                            `new-persons, ${personType.az_ur.name} карточка плательщика клиент (перед редактированием)`,
                            elements.page
                        );

                        await browser.click(elements.editPerson);

                        await browser.waitForVisible(elements.submitButton);

                        await takeScreenshots(
                            browser,
                            `${personType.az_ur.name} редактирование, форма, клиент`
                        );

                        await ReplaceAllPossibleFields(browser);

                        await takeScreenshots(
                            browser,
                            `${personType.az_ur.name}, форма после редактирования, клиент`
                        );

                        await browser.click(elements.submitButton);

                        await browser.ybWaitForInvisible(elements.submitButton);

                        await browser.waitForVisible('.yb-person-detail_email');

                        await browser.ybAssertView(
                            `new-persons, ${personType.az_ur.name}, карточка после редакитрования, клиент`,
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

                await browser.click(elements.personsType.radioButton.ur);

                await browser.ybWaitForInvisible('button[role="listbox"][disabled]');

                await browser.click(elements.personsType.listBox);
                await browser.click(personType.az_ur.selector);

                await browser.click(elements.continueButton);

                await browser.waitForVisible(elements.submitButton);

                await browser.ybSetSteps(
                    `Проверяем, что поля являются обязательными для заполнения`
                );

                await browser.click(elements.submitButton);
                await browser.waitForVisible('[data-detail-id="name"] ' + elements.error);

                await takeScreenshots(
                    browser,
                    `обязательные поля при создании плательщика (без вида счета`
                );

                await browser.ybSetSteps(`Проверяем, обязательность разлияных видов счета`);

                await browser.click(elements.payType.button);
                await browser.click(elements.payType.iban.list);
                await browser.click(elements.submitButton);
                await browser.waitForVisible('[data-detail-id="iban"] ' + elements.error);
                await browser.scroll(elements.payType.button);
                await browser.ybAssertView(
                    `new-persons, ${personType.az_ur.name}, создание плательщика, IBAN обязателен, клиент`,
                    elements.page
                );

                await browser.click(elements.payType.button);
                await browser.click(elements.payType.account.list);
                await browser.click(elements.submitButton);
                await browser.waitForVisible('[data-detail-id="account"] ' + elements.error);
                await browser.scroll(elements.payType.button);
                await browser.ybAssertView(
                    `new-persons, ${personType.az_ur.name}, создание плательщика, р.с. обязателен, клиент`,
                    elements.page
                );

                await browser.click(elements.payType.button);
                await browser.click(elements.payType.other.list);
                await browser.ybClearValue(elements.payType.other.input);
                await browser.click(elements.submitButton);
                await browser.waitForVisible('[data-detail-id="other"] ' + elements.error);
                await browser.scroll(elements.payType.button);
                await browser.ybAssertView(
                    `new-persons, ${personType.az_ur.name}, создание плательщика, прочее обязателен, клиент`,
                    elements.page
                );

                await browser.click(elements.payType.button);
                await browser.click(elements.payType.nothing);

                await browser.ybSetSteps(`Заполняем  обязательные поля и создаем плательщика`);

                await fillRequiredFields(browser);

                await browser.click(elements.submitButton);

                await browser.ybWaitForInvisible(elements.submitButton);

                await browser.ybSetSteps(
                    `Открывает форму для редктирования плательщика и стираем обязательные поля`
                );

                await browser.click(elements.editPerson);

                await browser.waitForVisible(elements.submitButton);

                await browser.ybClearValue(elements.phone);
                await browser.ybClearValue(elements.email);
                await browser.ybClearValue(elements.ben_bank_code);

                await browser.click(elements.submitButton);
                await browser.waitForVisible('[data-detail-id="phone"] ', elements.error);
                await browser.ybAssertView(
                    `new-persons, ${personType.az_ur.name}, редактирование плательщика, доступные обязательные поля обязательны, клиент`,
                    elements.page
                );

                await browser.click(elements.payType.button);
                await browser.click(elements.payType.iban.list);
                await browser.click(elements.submitButton);
                await browser.waitForVisible('[data-detail-id="iban"] ' + elements.error);
                await browser.scroll(elements.payType.button);
                await browser.ybAssertView(
                    `new-persons, ${personType.az_ur.name}, редактирование  плательщика, IBAN обязателен, клиент`,
                    elements.page
                );

                await browser.click(elements.payType.button);
                await browser.click(elements.payType.account.list);
                await browser.click(elements.submitButton);
                await browser.waitForVisible('[data-detail-id="account"] ' + elements.error);
                await browser.scroll(elements.payType.button);
                await browser.ybAssertView(
                    `new-persons, ${personType.az_ur.name}, редактирование плательщика, р.с. обязателен, клиент`,
                    elements.page
                );

                await browser.click(elements.payType.button);
                await browser.click(elements.payType.other.list);
                await browser.ybClearValue(elements.payType.other.input);
                await browser.click(elements.submitButton);
                await browser.waitForVisible('[data-detail-id="other"] ' + elements.error);
                await browser.scroll(elements.payType.button);
                await browser.ybAssertView(
                    `new-persons, ${personType.az_ur.name}, редактирование плательщика, прочее обязателен, клиент`,
                    elements.page
                );
            });
        });
    });
});
