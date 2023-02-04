const addClientData = {
    name: 'Клиент Клиентов',
    onlyManualUpdate: true,
    phone: '82349873456',
    fax: '82349873456',
    url: 'www.ya.ru',
    mail: 'client-mail@yandex.ru',
    byCompletion: true,
    manualSuspect: true,
    manualSuspectComment: 'Новый клиент',
    reliableCcPayer: true,
    denyCreditCard: true,
    isFraud: true,
    fraudType: 'SAFE (MasterCard)',
    direct25: true,
    isNonResident: true,
    isAcquiring: true,
    fullName: 'Full clients name',
    currency: 'EUR',
    intercompany: 'Yandex.Taxi AM'
};

module.exports.addClientData = addClientData;

const editClientData = {
    name: 'Измененный Клиент Клиентов',
    onlyManualUpdate: false,
    phone: '81234567890',
    fax: '81234567890',
    url: 'www.yandex.ru',
    mail: 'client-mail@yandex-team.ru',
    byCompletion: false,
    manualSuspect: false,
    manualSuspectComment: 'Редактирование клиента',
    reliableCcPayer: false,
    denyCreditCard: false,
    isFraud: true,
    fraudType: 'Другой',
    fraudText: 'Фрод',
    availableDocTypes: 'Отдельные',
    direct25: false,
    isNonResident: false,
    isAcquiring: false,
    fullName: 'New full clients name',
    currency: 'USD',
    region: 'Австрия',
    denyOverdraft: false,
    forceContractlessInvoice: false,
    intercompany: 'Uber Azerbaijan'
};

module.exports.editClientData = editClientData;

const elements = {
    cancelBtn: '.yb-edit-client__buttons button[type=button]',
    clientComponent:
        '.src-common-components-Client-___style-module__client-component .yb-page-section:first-child table',
    clientId:
        '.src-common-components-Client-___style-module__client-component .yb-page-section:first-child table tr:first-child td',
    editForm: '#edit-client-form',
    editLink: '.src-common-components-Client-___style-module__client-header-item a',
    fetching: '.yb-fetching',
    oldForm: 'form#set-client table',
    page: '.yb-page',
    saveBtn: '.yb-edit-client__buttons button[type=submit]'
};

module.exports.elements = elements;

const options = {
    tolerance: 8,
    antialiasingTolerance: 10,
    ignoreElements: ['.yb-edit-client__login']
};

module.exports.options = options;

module.exports.helpers = {
    save: async function (browser) {
        await browser.ybSetSteps('Нажимает кнопку Сохранить и дожидается загрузки страницы');
        await browser.click(elements.saveBtn);
        await browser.waitForVisible(elements.clientComponent);
    },
    trySave: async function (browser) {
        await browser.ybSetSteps('Нажимает кнопку Сохранить');
        await browser.click(elements.saveBtn);
    },
    cancel: async function (browser, waitElement) {
        await browser.ybSetSteps('Нажимает кнопку Отменить');
        await browser.click(elements.cancelBtn);
        if (waitElement) {
            await browser.ybSetSteps(`Дожидается загрузки ${waitElement}`);
            await browser.waitForVisible(waitElement);
        }
    },
    unlink: async function (browser, uid) {
        await browser.ybSetSteps(`Отвязыват всех клиентов для пользователя uid=${uid}`);
        await browser.ybRun('unlink_all_clients_from_login', [uid]);
    },
    fillFraud: async function (browser, setCheckBox, fraudType, fraudText) {
        await browser.ybSetSteps(`Поставить галку Пометить как фрод, задать значение ${fraudType}`);
        if (setCheckBox) {
            await browser.ybSetLcomCheckboxValue(
                '.yb-edit-client__fraud-field .yb-edit-client__fraud-item:first-child',
                true
            );
        }
        await browser.ybSetLcomSelectValue(
            '.yb-edit-client__fraud-field .yb-edit-client__fraud-item:nth-child(2) button',
            fraudType
        );
        if (fraudText) {
            await browser.ybReplaceValue(
                '.yb-edit-client__fraud-field .yb-edit-client__fraud-item:nth-child(3) input',
                fraudText
            );
        }
    },
    createFillAll: async function (browser, { isAgency, login }) {
        await browser.ybSetSteps(
            `Заполняет все поля клиента${
                isAgency ? '-агентства' : ''
            }, в т.ч. устанавливает 'Не должен получать овердрафт', 'Выставление счетов по оферте'`
        );

        await browser.ybReplaceValue('.yb-edit-client__name input', addClientData.name);
        await browser.ybSetLcomCheckboxValue(
            '.yb-edit-client__onlyManualNameUpdate',
            addClientData.manualSuspectComment
        );
        await browser.ybReplaceValue('.yb-edit-client__login input', login);
        await browser.ybSetLcomCheckboxValue('.yb-edit-client__isAgency', isAgency);
        await browser.ybReplaceValue('.yb-edit-client__phone input', addClientData.phone);
        await browser.ybReplaceValue('.yb-edit-client__fax input', addClientData.fax);
        await browser.ybReplaceValue('.yb-edit-client__url input', addClientData.url);
        await browser.ybReplaceValue('.yb-edit-client__email input', addClientData.mail);
        await browser.ybSetLcomCheckboxValue(
            '.yb-edit-client__byCompletion',
            addClientData.byCompletion
        );
        await browser.ybSetLcomCheckboxValue(
            '.yb-edit-client__reliableCcPayer',
            addClientData.reliableCcPayer
        );
        await browser.ybSetLcomCheckboxValue(
            '.yb-edit-client__denyCc',
            addClientData.denyCreditCard
        );
        await browser.ybSetLcomCheckboxValue(
            '.yb-edit-client__fraud-field .yb-edit-client__fraud-item:first-child',
            addClientData.isFraud
        );
        await browser.ybSetLcomSelectValue(
            '.yb-edit-client__fraud-field .yb-edit-client__fraud-item:nth-child(2) button',
            addClientData.fraudType
        );
        await browser.ybSetLcomCheckboxValue('.yb-edit-client__direct25', addClientData.direct25);
        await browser.ybSetLcomCheckboxValue(
            '.yb-edit-client__isNonResident',
            addClientData.isNonResident
        );
        await browser.ybSetLcomCheckboxValue(
            '.yb-edit-client__isAcquiring',
            addClientData.isAcquiring
        );
        await browser.ybReplaceValue('.yb-edit-client__fullname input', addClientData.fullName);
        await browser.ybSetLcomSelectValue(
            '.yb-edit-client__isoCurrencyPayment button',
            addClientData.currency
        );
        await browser.ybSetLcomSelectValue(
            '.yb-edit-client__intercompany button',
            addClientData.intercompany
        );
        await browser.ybAssertView(
            `addtclient.xml - создание клиента${isAgency ? '-агентства' : ''} - заполненная форма`,
            elements.page,
            options
        );
    },
    waitForOldFormLoad: async function (browser) {
        await browser.ybWaitForInvisible(elements.fetching);
        await browser.waitForVisible(elements.oldForm);
    }
};
