const waitTimeoutForExtensiveQuery = 120000;
module.exports.waitTimeoutForExtensiveQuery = waitTimeoutForExtensiveQuery;

module.exports.setValues = async function (browser) {
    await browser.ybSetSteps('Заполняет наименование, тип, id, инн, email, партнер, только vip');
    await browser.ybReplaceValue('.yb-persons-search__name', 'КУМИТ');
    await browser.ybLcomSelect('.yb-persons-search__person-type', 'Юр. лицо');
    await browser.ybReplaceValue('.yb-persons-search__person-id', '5687027');
    await browser.ybReplaceValue('.yb-persons-search__inn', '7707813050');
    await browser.ybReplaceValue('.yb-persons-search__email', 'coomeetyour@yandex.ru');
    await browser.ybLcomSelect('.yb-persons-search__is-partner', 'Только не партнеры');
    await browser.ybSetLcomCheckboxValue('.yb-persons-search__vip-only', true);
};

module.exports.valuesUrl =
    'persons.xml?name=%D0%9A%D0%A3%D0%9C%D0%98%D0%A2&type=ur&inn=7707813050&id=5687027&email=coomeetyour%40yandex.ru&is_partner=false&vip_only=true&pn=1&ps=10';

module.exports.openPerson = async function (browser) {
    await browser.ybSetSteps(`Кликает "Подробности", чтобы открыть информацию о плательщике`);
    const elem = await browser.ybWaitForExist(['.yb-persons-table', 'a=Подробности']);
    await elem.click();

    await browser.waitForVisible('.yb-person-id__name');
};

module.exports.paginationUrl =
    'persons.xml?name=ООО "Гермионовый плательщик"&type=ur&inn=2473746582&email=hermione_test_pers%40ya.ru&pn=1&ps=10';

module.exports.scrollToEnd = async function (browser) {
    await browser.ybSetSteps(`Скроллит до конца таблицы`);
    await browser.scroll('.src-common-components-Table-___table-module__page-size-selector');
    await browser.waitForVisible(
        '.src-common-components-Table-___table-module__page-size-selector'
    );
};

const oebsStatusSelector = '.src-common-components-Person-___person-module__person-row';
const typeIdSelector = '.src-common-components-Person-___person-module__person-id__type-id';

module.exports.hideModalElements = [oebsStatusSelector, typeIdSelector];
module.exports.hideElements = ['.yb-persons-table__person', '.yb-persons-table__client'];
