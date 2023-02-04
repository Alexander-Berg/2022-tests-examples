const { basicIgnore } = require('../../../helpers');

const hideElements = [
    'input[id="dt"]',
    'input[id="service-start-dt"]',
    'input[id="print-form-dt"]',
    'input[id="is-booked-dt"]',
    'input[id="is-faxed-date"]',
    'input[id="is-signed-date"]',
    'input[id="sent-dt-date"]',
    'input[id="is-suspended-date"]',
    'input[id="col-new-dt"]',
    'input[id="col-new-group02-grp-90-finish-dt"]',
    'input[id="col-new-dt"]',
    'input[id="col-new-is-faxed-date"]',
    'input[id="col-new-is-signed-date"]',
    'div[id="alert-message"]',
    'div[id="col-new-form-alert"]'
];

module.exports.assertViewOpts = {
    ignoreElements: [...basicIgnore, 'input[id="external-id"]'],
    hideElements
};

module.exports.hideElements = hideElements;

module.exports.getAssertViewOptsCol = async function (browser) {
    const collateralIdElem = await browser.$('.collateral');
    const collateralId = await collateralIdElem.getAttribute('id');
    let assertViewOptsCol = {
        ignoreElements: [...basicIgnore, 'input[id="external-id"]'],
        hideElements: [
            hideElements,
            'input[id="' + collateralId.replace('form', 'dt') + '"]',
            'input[id="' + collateralId.replace('form', 'group02-grp-90-finish-dt') + '"]',
            'input[id="' + collateralId.replace('form', 'is-booked-dt') + '"]',
            'input[id="' + collateralId.replace('form', 'is-faxed-date') + '"]',
            'input[id="' + collateralId.replace('form', 'is-signed-date') + '"]'
        ]
    };
    return assertViewOptsCol;
};

module.exports.setClient = async function (browser, client_id, mainTabId) {
    await browser.ybSetSteps(`Выбирает клиента ` + client_id);
    await browser.click('#client-id-ob a:nth-child(1)');
    let tabIds = await browser.getWindowHandles();
    const clientModalTabId = tabIds.filter(tab => tab !== mainTabId)[0];
    await browser.switchToWindow(clientModalTabId);
    await browser.setValue('input[name="client_id"]', client_id);
    await browser.click('input[type="submit"]');
    await browser.waitForVisible('table[class="report"] tr:nth-child(2)');
    await browser.click('tr:nth-child(2) a:nth-child(1)');
    await browser.switchToWindow(mainTabId);
};

module.exports.setPerson = async function (browser, person_id, mainTabId) {
    await browser.ybSetSteps(`Выбирает плательщика ` + person_id);
    await browser.click('#person-id-ob a:nth-child(1)');
    tabIds = await browser.getWindowHandles();
    const personModalTabId = tabIds.filter(tab => tab !== mainTabId)[0];
    await browser.switchToWindow(personModalTabId);
    await browser.waitForVisible('div[class="subcontent b-persons-list"]');
    await browser.click('a:nth-child(4)');
    await browser.waitForVisible('div[class="subcontent b-persons-list"] > a');
    await browser.click('div[class="subcontent b-persons-list"] > a');
    await browser.switchToWindow(mainTabId);
};

module.exports.setDate = async function (browser, selector) {
    await browser.click('input[id="' + selector + '"]');
    await browser.waitForVisible('tr.datepick-days-row:nth-child(2) > td:nth-child(1)');
    await browser.click('tr.datepick-days-row:nth-child(2) > td:nth-child(1)');
    await browser.ybWaitForInvisible('#datepick-div');
};

module.exports.setManager = async function (browser, managerName) {
    await browser.setValue('input[id="manager-code_ref"]', managerName);
    await browser.waitForVisible('.ui-menu-item');
    await browser.click('.ui-menu-item a');
};
