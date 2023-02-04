const { partner } = require('./common');

module.exports.changePersonTypeToUrApikeys = async function (browser) {
    const url = await browser.url();
    await browser.ybAbsoluteUrl(url.replace('type=ur', `type=ur&from_service=apikeys`));
};

module.exports.navigateToNewUrApikeysChangePerson = async function (browser, clientId) {
    await browser.ybUrl(
        'admin',
        `change-person.xml?type=ur&from_service=apikeys&partner=${partner}&client_id=${clientId}`
    );
};
