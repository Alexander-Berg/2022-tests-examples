const assert = require('chai').assert;

module.exports.saveGoodDebt = async function (browser) {
    await browser.ybSetSteps('Признает долг хорошим');
    await browser.click('#label-act-saver__checkbox-is-good-debt');
    await browser.click('#act-saver__btn-save-good-debt');
    await browser.waitForVisible('span=Изменение учета долга сохранено');
};

module.exports.hideElements = [
    '.yb-copyable-header__external-id',
    '.src-common-components-Table-___table-module__table-container',
    '.src-common-components-ExportState-___styles-module__reexportStatus'
];

module.exports.reexportStatusSelector =
    '.src-common-components-ExportState-___styles-module__reexportStatus';

module.exports.assertPageEmpty = async function (browser) {
    await browser.ybSetSteps('Проверяет, что страница пуста');
    const text = browser.getText('.yb-content');
    assert(text, 'Эксперимент UI', 'Страница не пуста');
};
