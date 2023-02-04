const { elements } = require('../../../../../admin/change-person/elements');

module.exports.fillRequiredFieldsForYTDetails = async function (browser, details) {
    await browser.setValue(elements.name, details.name.value);

    await browser.setValue(elements.phone, details.phone.value);
    await browser.setValue(elements.email, details.email.value);

    await browser.click('span=выберите страну');
    await browser.click(`span=${details.country.name}`);

    await browser.setValue(elements.address, details.address.value);
    await browser.setValue(elements.legaladdress, details.legaladdress.value);
    await browser.setValue(elements.longname, details.longname.value);
};

module.exports.fillAllFields = async function (browser, details) {
    await browser.setValue(elements.name, details.name.value);

    await browser.setValue(elements.phone, details.phone.value);
    await browser.setValue(elements.email, details.email.value);

    await browser.click('span=выберите страну');
    await browser.click(`span=${details.country.name}`);

    await browser.setValue(elements.address, details.address.value);
    await browser.setValue(elements.legaladdress, details.legaladdress.value);
    await browser.setValue(elements.longname, details.longname.value);

    await browser.setValue(elements.fax, details.fax.value);
    await browser.setValue(elements.representative, details.representative.value);
    await browser.setValue(elements.postcode, details.postcode.value);

    await browser.click('input[name="invalid-address"]');
    await browser.click('input[name="invalid-bankprops"]');

    await browser.setValue(elements.legalAddressPostcode, details.legal_address_postcode.value);
    await browser.setValue(elements.bank, details.bank.value);
    await browser.setValue(elements.account, details.account.value);

    await browser.click('div[data-detail-id="deliveryType"] button');
    await browser.click('span=почта');

    await browser.click('input[name="live-signature"]');

    await browser.setValue(elements.signerPersonName, details.signer_person_name.value);

    await browser.click('div[data-detail-id="signerPersonGender"] button');
    await browser.click('span=мужской');

    await browser.click('div[data-detail-id="signerPositionName"] button');
    await browser.click('span=Генеральный директор');

    await browser.click('div[data-detail-id="authorityDocType"] button');
    await browser.click('span=Устав');

    await browser.setValue(
        'input[name="authority-doc-details"]',
        details.authority_doc_details.value
    );

    await browser.click('input[name="vip"]');

    await browser.click('input[name="early-docs"]');
};

module.exports.hideElements = ['.yb-user-copyright__year'];
