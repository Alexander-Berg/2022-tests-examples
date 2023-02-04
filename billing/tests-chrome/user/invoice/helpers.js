const { basicIgnore } = require('../../../helpers');

const hideElements = [
    'h1',
    '.report .date',
    '.show-order',
    'form:nth-child(1) img',
    '.b-payment-details',
    '#invoice_consumes-list-container .date',
    '#invoice_acts_data .t-act',
    '#invoice_acts_data .date',
    'div.sub:nth-child(17) .date',
    'select[name="dst_order_id"]',
    'td.oper-details'
];

module.exports.assertViewOpts = {
    ignoreElements: [...basicIgnore],
    hideElements,
    captureElementFromTop: true,
    allowViewportOverflow: true,
    compositeImage: true,
    expandWidth: true
};

module.exports.hideElements = hideElements;

module.exports.waitConsumes = async function (browser) {
    await browser.waitUntil(
        async function () {
            const url = await browser.getUrl();
            return url.includes('/invoice.xml?invoice_id=');
        },
        { timeout: 30000 }
    );
};
