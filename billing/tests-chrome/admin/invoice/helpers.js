const { basicIgnore } = require('../../../helpers');

const hideElements = [
    '.yb-copyable-header__external-id',
    '.yb-page-header__column.yb-page-header__column_right',
    '.src-common-components-Person-___person-module__export-oebs-form',
    '.yb-invoice-status__contract',
    '.src-common-components-Person-___person-module__person-id__type-id',
    '.yb-invoice-info .yb-table__client',
    '.yb-invoice-info .yb-table__date',
    '.yb-invoice-contract__contract-selector',
    '.yb-invoice-orders .yb-table__order-id',
    '.yb-invoice-orders .yb-table__client',
    '.yb-invoice-consumes .yb-table__order-id',
    '.yb-invoice-consumes .yb-table__client',
    '.yb-invoice-consumes .yb-table__date',
    '.yb-invoice-operations .yb-table__date',
    '.yb-invoice-operations .yb-table__extras',
    '.yb-invoice-operations .yb-table__id',
    '.yb-invoice-fictive-invoices .yb-table__id',
    '.yb-invoice-fictive-invoices .yb-table__date',
    '.yb-invoice-acts .yb-table__act-id',
    '.yb-invoice-acts .yb-table__date',
    '.yb-invoice-acts .yb-table__payment-term-date',
    '.yb-invoice-info .yb-table__doc-date',
    '.src-admin-pages-invoice-components-InvoiceInfo-___invoice-info-module__repayment-dt'
];

module.exports.assertViewOpts = {
    ignoreElements: [...basicIgnore, '.yb-invoice-patch-date input'],
    hideElements,
    captureElementFromTop: true,
    allowViewportOverflow: true,
    compositeImage: true,
    expandWidth: true
};

module.exports.hideElements = hideElements;

module.exports.openConfirm = async function (browser, buttonText, buttonName) {
    await browser.ybSetSteps('Нажимает кнопку ' + buttonText + ' и ждет открытия модалки');
    await browser.click(buttonName);
    await browser.ybWaitAnimation('Modal-Content_theme_normal_visible');
};
