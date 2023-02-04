module.exports.valuesUrl =
    'print-form-rules.xml?contract_type=DISTRIBUTION&rule_type=contract&is_published=true&pn=1&ps=10';

module.exports.waitTimeoutForExtensiveQuery = 60000;

module.exports.waitForEnabled = async function (browser) {
    await browser.waitForVisible('.yb-print-form-rules-search__contract-type button');
    await browser.waitForVisible(
        '.yb-print-form-rules-search__contract-type button[aria-disabled=false]'
    );
};
