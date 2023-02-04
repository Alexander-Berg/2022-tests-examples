const assert = require('chai').assert;
const { basicIgnore } = require('../../../helpers');

const hideElements = [
    '.yb-copyable-header__external-id',
    '.yb-consumes-list__invoice-id',
    '.yb-consumes-list__dt',
    '.yb-consumes-list__id',
    '.yb-general-info__dt',
    '.yb-general-info__id',
    '.yb-general-info__client-id',
    '.yb-general-info__agency-id',
    '.yb-invoice-orders__invoice-external-id',
    '.yb-invoice-orders__invoice-dt',
    '.yb-operations__dt',
    '.yb-operations__invoice-id',
    '.yb-operations__details'
];

module.exports.hideElements = hideElements;

module.exports.assertViewOpts = {
    ignoreElements: basicIgnore,
    hideElements,
    expandWidth: true
};

module.exports.openOrder = async function (browser, builderName, { isReadonly = false } = {}) {
    const data = await browser.ybRun(builderName);
    const [, serviceId, serviceOrderId] = data;

    assert.equal(String(serviceId), '7', 'Тест написан для директа');
    await browser.ybSignIn({ isAdmin: true, isReadonly });
    await browser.ybUrl(
        'admin',
        'order.xml?service_cc=PPC&service_order_id=' + String(serviceOrderId)
    );
    await browser.ybWaitForLoad();

    return data;
};

module.exports.transferSetArbitraryOrder = async function (browser, order) {
    await browser.ybSetSteps('Выбирает произвольный заказ номер ' + order);
    await browser.ybReplaceValue('.yb-transfer-orders__arbitrary-order', order);
};

module.exports.transferSetOrder = async function (browser, order) {
    await browser.ybSetSteps('Выбирает заказ из выпадашки начинающийся на ' + order);
    await browser.ybReplaceValue('.yb-transfer-orders__arbitrary-order', '');
    await browser.ybSetLcomSelectValue('.yb-transfer-orders__order', order, { isAccurate: false });
};

module.exports.transferSetAmount = async function (browser, amount) {
    await browser.ybSetSteps('Выбирает сумму ' + amount);
    await browser.click('label.transfer-orders__src-pts-radio');
    await browser.ybReplaceValue('input#transfer-orders__amount', amount);
};

module.exports.transferSetDiscount = async function (browser, amount) {
    await browser.ybSetSteps('Выбирает скидку ' + amount);
    await browser.click('label.transfer-orders__set-discount-radio');
    await browser.ybReplaceValue('input#transfer-orders__discount', amount);
};

module.exports.transferSumbit = async function (browser) {
    await browser.ybSetSteps('Кликает на перевод средств');
    await browser.click('.yb-transfer-orders__button-transfer');
};

module.exports.transferWait = async function (browser) {
    await browser.ybSetSteps('Дожидается загрузки страницы');
    await browser.ybWaitForInvisible('.yb-transfer-orders__button-transfer_progress');
    await browser.ybWaitForLoad();
    await browser.ybWaitForInvisible('.yb-operations-filter__button-show_progress');
};

module.exports.assertNotTransferable = async function (browser) {
    await browser.ybSetSteps('Проверяет, что перенос между сервисами невозможен');
    const text = await browser.ybMessageGetText();

    assert.match(text, /Перенос между сервисами запрещен в счете./, 'Неправильный текст модалки');
};

module.exports.operationsWaitLoad = async function (browser) {
    await browser.ybSetSteps(`Дожидается загрузки таблицы операций`);
    await browser.ybWaitForInvisible('.yb-operations__table_progress');
};

module.exports.operationsLoadMore = async function (browser) {
    await browser.ybSetSteps(`Нажимает загрузить ещё`);
    await browser.click('button*=Загрузить ещё');
    await browser.scroll('.yb-operations');
    await browser.ybWaitForInvisible('.yb-operations__table_progress');
    await browser.scroll('.yb-operations');
};

module.exports.operationsShowByDate = async function (browser) {
    await browser.ybSetSteps(`Заполняет даты и нажимает показать в операциях`);
    await browser.ybReplaceValue('.yb-operations-filter__from-dt', '01.06.2014 г.');
    await browser.click('.yb-operations .yb-page-section__title');
    await browser.ybReplaceValue('.yb-operations-filter__to-dt', '31.10.2014 г.');
    await browser.click('button*=Показать');
    await browser.ybWaitForInvisible('.yb-operations__table_progress');
};

module.exports.paySetType = async function (browser, type) {
    await browser.ybSetSteps(`Выбирает способ оплаты ` + type);
    await browser.ybLcomSelect('#pay-order-form', type);
};

module.exports.paySetAmount = async function (browser, amount) {
    await browser.ybSetSteps(`Выбирает количество ` + amount);
    await browser.ybReplaceValue('input#order-pay-sum-input', amount);
};

module.exports.payClick = async function (browser) {
    await browser.ybSetSteps(`Нажимает внести оплату`);
    await browser.waitForVisible('button*=Внести оплату');
    await browser.click('button*=Внести оплату');
};

module.exports.payConfirm = async function (browser) {
    await browser.ybSetSteps(`Нажимает подтвердить`);
    await browser.waitForVisible('button*=Подтвердить');
    await browser.click('button*=Подтвердить');
};

module.exports.payAbort = async function (browser) {
    await browser.ybSetSteps(`Нажимает отменить`);
    await browser.waitForVisible('button*=Отменить');
    await browser.click('button*=Отменить');
};

module.exports.payWaitLoad = async function (browser) {
    await browser.ybSetSteps(`Дожидается загрузки`);
    await browser.ybWaitForInvisible('.yb-pay-order__pay-button_progress');
    await browser.ybWaitForLoad();
};
