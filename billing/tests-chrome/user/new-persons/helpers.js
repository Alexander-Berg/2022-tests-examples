const { elements } = require('./elements');
const { personType: personInfoByType } = require('./common');
const { assertViewOpts } = require('./config');
const { setValues } = require('../../admin/change-person/helpers');

module.exports.openPersonCreation = async function (
    browser,
    { personType, isUr, signInOpts = { isAdmin: false } }
) {
    const { login } = await browser.ybSignIn(signInOpts);
    await browser.ybRun('create_client_for_user', {
        login
    });

    await browser.ybUrl('user', `new-persons.xml#/new`);

    await browser.ybWaitForLoad();

    await browser.click(elements.personsType.radioButton[isUr ? 'ur' : 'ph']);

    await browser.ybWaitForInvisible('button[role="listbox"][disabled]');

    await browser.click(elements.personsType.listBox);
    await browser.click(personInfoByType[personType].selector);

    await browser.click(elements.continueButton);
};

module.exports.assertMandatoryFieldsValidation = async function (browser, { personType }) {
    await browser.waitForVisible(elements.submitButton);
    await browser.click(elements.submitButton);

    await browser.ybAssertView(
        `new-persons, ${personInfoByType[personType].name} создание с обяз. полями, форма, клиент`,
        elements.page,
        assertViewOpts
    );
};

module.exports.assertMandatoryFields = async function (browser, { personType }) {
    await browser.ybAssertView(
        `new-persons, ${personInfoByType[personType].name} создание с обяз. полями, заполненная форма, клиент`,
        elements.page,
        assertViewOpts
    );
};

module.exports.assertAllFields = async function (browser, { personType }) {
    await browser.ybAssertView(
        `new-persons, ${personInfoByType[personType].name} создание со всеми полями, заполненная форма, клиент`,
        elements.page,
        assertViewOpts
    );
};

module.exports.save = async function (browser) {
    await browser.click(elements.submitButton);

    await browser.ybWaitForInvisible(elements.submitButton);
};

module.exports.assertMandatoryCard = async function (browser, { personType }) {
    await browser.waitForVisible('.yb-person-detail_email');

    await browser.ybAssertView(
        `new-persons, ${personInfoByType[personType].name}, создание с обяз. полями, карточка плательщика клиент`,
        elements.page,
        assertViewOpts
    );
};

module.exports.assertAllCard = async function (browser, { personType }) {
    await browser.waitForVisible('.yb-person-detail_email');

    await browser.ybAssertView(
        `new-persons, ${personInfoByType[personType].name}, создание со всеми полями, карточка плательщика клиент`,
        elements.page,
        assertViewOpts
    );
};

module.exports.openPersonCard = async function (
    browser,
    { personType, signInOpts = { isAdmin: false } }
) {
    const { login } = await browser.ybSignIn(signInOpts);
    const { person_id } = await browser.ybRun('create_client_with_person_for_user', {
        login,
        person_type: personType,
        is_partner: false
    });

    await browser.ybUrl('user', `new-persons.xml#/${person_id}`);
};

module.exports.assertCardBeforeEdit = async function (browser, { personType }) {
    await browser.waitForVisible('.yb-person-detail_email');

    await browser.ybAssertView(
        `new-persons, ${personInfoByType[personType].name}, карточка плательщика клиент (перед редактированием)`,
        elements.page,
        assertViewOpts
    );
};

module.exports.clickEdit = async function (browser) {
    await browser.click(elements.editPerson);

    await browser.waitForVisible(elements.submitButton);
};

module.exports.assertFormBeforeEdit = async function (browser, { personType }) {
    await browser.ybAssertView(
        `new-persons, ${personInfoByType[personType].name}, форма перед редактированием, клиент`,
        elements.page,
        assertViewOpts
    );
};

module.exports.assertFormAfterEdit = async function (browser, { personType }) {
    await browser.ybAssertView(
        `new-persons, ${personInfoByType[personType].name}, форма после редактирования, клиент`,
        elements.page,
        assertViewOpts
    );
};

module.exports.assertCardAfterEdit = async function (browser, { personType }) {
    await browser.waitForVisible('.yb-person-detail_email');

    await browser.ybAssertView(
        `new-persons, ${personInfoByType[personType].name}, карточка плательщика клиент (после редактирования)`,
        elements.page,
        assertViewOpts
    );
};

module.exports.setValues = setValues;
