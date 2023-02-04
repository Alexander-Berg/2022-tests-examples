const { basicHide, basicIgnore } = require('../../../helpers');

module.exports.setAllValues = async function (browser) {
    await browser.ybReplaceValue(
        '.yb-product-catalog-search__product-name',
        'Рекламная кампания РРС'
    );
    await browser.ybReplaceValue('.yb-product-catalog-search__product-id', '503162');
    await browser.ybSetLcomSelectValue(
        '.yb-product-catalog-search__service-id',
        'Директ: Рекламные кампании'
    );
};

module.exports.assertViewOpts = {
    ignoreElements: basicIgnore,
    hideElements: basicHide
};

module.exports.valuesUrl =
    'product-catalog.xml?product_id=503162&product_name=%D0%A0%D0%B5%D0%BA%D0%BB%D0%B0%D0%BC%D0%BD%D0%B0%D1%8F%20%D0%BA%D0%B0%D0%BC%D0%BF%D0%B0%D0%BD%D0%B8%D1%8F%20%D0%A0%D0%A0%D0%A1&service_id=7&pn=1&ps=10&sf=ID&so=1';

module.exports.clearFilter = async function (browser) {
    await browser.ybSetSteps('Сбрасывает фильтр');
    await browser.click('.yb-search-filter__button-clear');
};

module.exports.paginationUrl = 'product-catalog.xml?service_id=470&pn=1&ps=10&sf=ID&so=1';
