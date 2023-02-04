const assert = require('chai').assert;
const path = require('path');
const { elements } = require('./elements');
const { assertViewOpts } = require('./config');

module.exports.openPersonCreation = async function (
    browser,
    { personType, partner, signInOpts = { isAdmin: true, isReadonly: false } }
) {
    await browser.ybSignIn(signInOpts);
    const { client_id } = await browser.ybRun('create_client');

    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
    await browser.waitForVisible(elements.select);
    await browser.click(elements.select);
    await browser.click(elements.menu[`${personType}_${partner}`]);
    await browser.click(elements.btnSubmitAddPerson);

    await browser.waitForVisible(elements.formChangePerson);
};

module.exports.directOpenPersonCreation = async function (
    browser,
    { personType, partner, signInOpts = { isAdmin: true, isReadonly: false } }
) {
    await browser.ybSignIn(signInOpts);
    const { client_id } = await browser.ybRun('create_client');

    await browser.ybUrl(
        'admin',
        `change-person.xml?type=${personType}&partner=${partner}&client_id=${client_id}`
    );
    await browser.waitForVisible(elements.formChangePerson);
};

module.exports.openPersonEdit = async function (
    browser,
    { personType, partner, signInOpts = { isAdmin: true, isReadonly: false } }
) {
    const { login } = await browser.ybSignIn(signInOpts);
    const { client_id } = await browser.ybRun('create_client_with_person_for_user', [
        login,
        personType,
        partner
    ]);

    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
    await browser.waitForVisible(elements.personsList);
    await browser.click(elements.editLink);
    await browser.waitForVisible(elements.formChangePerson);
};

module.exports.assertMandatoryFields = async function (browser) {
    await browser.ybAssertView(
        'форма - создание, заполнены обязательные поля',
        elements.formChangePerson,
        assertViewOpts
    );

    await browser.click(elements.btnSubmit);

    await browser.waitForVisible(elements.personsList);
    await browser.ybAssertView(
        'subpersons.xml - карточка плательщика с обязательными полями',
        elements.personDetails,
        assertViewOpts
    );
};

module.exports.assertAllFields = async function (browser) {
    await browser.ybAssertView(
        'форма - создание, заполнены все поля',
        elements.formChangePerson,
        assertViewOpts
    );

    await browser.click(elements.btnSubmit);

    await browser.waitForVisible(elements.personDetails);
    await browser.ybAssertView(
        'subpersons.xml - карточка плательщика со всеми полями',
        elements.personDetails,
        assertViewOpts
    );
};

module.exports.assertPersonPostAddressEditDetails = async function (browser) {
    await browser.ybAssertView(
        'форма - создание, заполнены обязательные поля - нет права PersonPostAddressEdit',
        elements.formChangePerson,
        assertViewOpts
    );

    await browser.click(elements.btnSubmit);

    await browser.waitForVisible(elements.personsList);
};

module.exports.assertEditedDetails = async function (browser) {
    await browser.ybAssertView('форма - редактирование', elements.formChangePerson, assertViewOpts);

    await browser.click(elements.btnSubmit);

    await browser.waitForVisible(elements.personsList);
    await browser.ybAssertView(
        'subpersons.xml - карточка плательщика с измененными полями',
        elements.personDetails,
        assertViewOpts
    );
};

module.exports.assertDetailsValidation = async function (browser) {
    await browser.click(elements.btnSubmit);
    await browser.ybAssertView(
        `форма - валидация, обязательные поля`,
        elements.formChangePerson,
        assertViewOpts
    );
};

module.exports.assertPersonPostAddressEditDetailsEdit = async function (browser) {
    await browser.ybAssertView(
        'форма - редактирование, нет права PersonPostAddressEdit',
        elements.formChangePerson,
        assertViewOpts
    );
};

module.exports.setInnValue = async function (browser, selector, text, value) {
    await browser.setValue(selector, text);
    const valueSelector = `.yb-search-item__city*=${value}`;
    await browser.waitForVisible(valueSelector);
    await browser.click(valueSelector);
};

async function setSuggestValue(browser, selector, text, value, custom = false) {
    await browser.ybReplaceValue(selector, text);
    const valueSelector = custom
        ? `div.yb-search-item__main[data-text*=${value}]`
        : `div.yb-search-item[data-search*=${value}]`;
    await browser.waitForVisible(valueSelector);
    await browser.click(valueSelector);
}

module.exports.setSuggestValue = setSuggestValue;

const Types = {
    text: 'text',
    textarea: 'textarea',
    checkbox: 'checkbox',
    radio: 'radio',
    select: 'select',
    file: 'file',
    date: 'date',
    suggest: 'suggest'
};

module.exports.Types = Types;

/**
 *
 * @param browser
 * @param id
 * @param type
 * @param value
 * @param newValue
 * @param suggestValue
 * @param customSuggest означает, что для компонента переопределно отображение подсказок
 * @param edit
 * @param ownSuggestValue
 * @returns {Promise<void>}
 */
async function setValue(
    browser,
    { id, type, value, newValue, suggestValue, customSuggest, scrollIntoView },
    edit = false,
    ownSuggestValue = undefined
) {
    if (scrollIntoView && type !== Types.select) {
        throw new Error(
            'NotImplementedError: Чтобы пользоваться такой функцией нужно добавить ее в setValue'
        );
    }

    const nextValue = ownSuggestValue || (edit ? newValue : value);
    switch (type) {
        case Types.text: {
            try {
                await browser.ybSetSteps(`Заполняет поле '${id}' значением '${nextValue}'`);
                const selector = `div[data-detail-id="${id}"] input[type=text]`;
                await browser.ybReplaceValue(selector, nextValue);
            } catch (err) {
                console.log(
                    `Could not use input selector: 'div[data-detail-id="${id}"] input[type=text]': ${err.message}. Try to use textarea`
                );
                const selector = `div[data-detail-id="${id}"] textarea`;
                await browser.ybReplaceValue(selector, nextValue, 'textarea');
            }
            break;
        }
        case Types.textarea: {
            await browser.ybSetSteps(`Заполняет поле '${id}' значением '${nextValue}'`);
            const selector = `div[data-detail-id="${id}"]`;
            await browser.ybReplaceValue(selector, nextValue, 'textarea');
            break;
        }
        case Types.checkbox: {
            await browser.ybSetSteps(`Кликает на '${id}'`);
            const selector = `div[data-detail-id="${id}"] input[type=checkbox]`;
            await browser.click(selector);
            break;
        }
        case Types.radio: {
            await browser.ybSetSteps(
                `Кликает на радио-кнопку для поля '${id}' со значением поля '${nextValue}'`
            );
            await browser.click(
                `div[data-detail-id="${id}"] input[type=radio][value="${nextValue}"]`
            );
            break;
        }
        case Types.select: {
            await browser.ybSetSteps(
                `Выбирает значение из списка для поля '${id}': '${nextValue}'`
            );
            const selector = `div[data-detail-id="${id}"] .src-common-presentational-components-Field-___field-module__detail__value`;
            // Иногда селект находится внизу и может открыться только вниз, тогда нужно насколлить его к верху
            // Чтобы не переснимать все остальные скриншоты где это работает нормально, выносим под флаг
            if (scrollIntoView) {
                const item = await browser.waitForVisible(selector);
                await item.scrollIntoView();
            }
            await browser.ybLcomSelect(selector, nextValue);
            break;
        }
        case Types.file: {
            await browser.ybSetSteps(`Выбирает для поля '${id}' файл '${nextValue}'`);
            const selector = `div[data-detail-id="${id}"] input[type=file]`;
            const filePath = path.join(__dirname, nextValue);
            const remotePath = await browser.uploadFile(filePath);
            const elem = await browser.$(selector);
            await elem.addValue(remotePath);
            await browser.ybSetSteps(
                'Проверить файл можно в рассылке https://ml.yandex-team.ru/lists/test-balance-notify/'
            );
            break;
        }
        case Types.date: {
            await browser.ybSetSteps(`Заполняет поле '${id}' значением '${nextValue}'`);
            const selector = `div[data-detail-id="${id}"]`;
            await browser.ybSetDatepickerValue(selector, nextValue);
            await browser.click(`${selector} > div:first-child`);
            break;
        }
        case Types.suggest: {
            if (!suggestValue) {
                throw new Error('suggestValue is required');
            }
            // если задано ownSuggestValue, то не дожидаемся подсказок
            if (ownSuggestValue) {
                await setValue(browser, { id, type: Types.text, value: ownSuggestValue });
                return;
            }
            await browser.ybSetSteps(
                `Вводит в поле '${id}' значение '${suggestValue}' и выбирает '${nextValue}'`
            );
            const selector = `div[data-detail-id="${id}"] input[name]`;
            await setSuggestValue(browser, selector, suggestValue, nextValue, customSuggest);
            break;
        }
        default: {
            throw new Error(`Unsupported detail type: '${type}'`);
        }
    }
}

module.exports.setValue = setValue;

module.exports.setValues = async function (browser, details, { edit = false } = {}) {
    // of не использую тк он не гарантирует порядок элементов
    for (let i = 0; i < details.length; i++) {
        await setValue(browser, details[i], edit);
    }
};

module.exports.assertErrorMessage = async function (browser, { expectedText, strict = false }) {
    const text = await browser.ybMessageGetText();
    if (strict) {
        assert(expectedText === text, `Сообщение об ошибке равно '${expectedText}'`);
        return;
    }
    assert(text.indexOf(expectedText) >= 0, `Сообщение об ошибке содержит текст '${expectedText}'`);
};

module.exports.processDetails = details => {
    const process = (details, [id, value]) => {
        details[id] = typeof value === 'string' ? { id, value } : { id, ...value };
        if (!details[id].type) {
            details[id].type = 'text';
        }
        return details;
    };

    return Object.entries(details).reduce(process, {});
};
