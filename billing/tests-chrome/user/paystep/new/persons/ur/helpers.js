const { elements } = require('../../../../../admin/change-person/elements');
module.exports.fillRequiredDetailsUr = async function (browser, details, role) {
    await browser.setValue(elements.inn, details.inn.value);
    await browser.setValue(elements.name, details.name.value);
    await browser.setValue(elements.longname, details.longname.value);
    await browser.setValue(elements.kpp, details.kpp.value);
    await browser.setValue(elements.phone, details.phone.value);
    await browser.setValue(elements.email, details.email.value);

    await browser.click('span=по справочнику');
    await browser.setValue(elements.legalAddressCity, 'москва');
    await browser.waitForVisible(`.Suggest-Item*=Москва`);
    await browser.click(`.Suggest-Item*=Москва`);
    await browser.setValue(elements.legalAddressStreet, 'ул. генерала Тюленева');
    await browser.setValue(elements.legalAddressPostcode, '117465');
    await browser.setValue(elements.legalAddressHome, 'д. 5');

    await browser.click('span=по адресу');
    await browser.setValue(elements.city, 'москва');
    await browser.waitForVisible(`.Suggest-Item*=Москва`);
    await browser.click(`.Suggest-Item*=Москва`);
    await browser.setValue('input[name="street"]', 'ул. генерала Тюленева');
    await browser.setValue(elements.postcode, '117465');
    await browser.setValue(elements.postsuffix, 'д. 5');
};

module.exports.fillAllFieldsUr = async function (browser, details, role) {
    await browser.setValue(elements.inn, details.inn.value);
    await browser.setValue(elements.name, details.name.value);
    await browser.setValue(elements.longname, details.longname.value);
    await browser.setValue(elements.kpp, details.kpp.value);
    await browser.setValue(elements.phone, details.phone.value);
    await browser.setValue(elements.email, details.email.value);
    await browser.setValue(elements.legaladdress, details.legaladdress.value);
    await browser.setValue(elements.city, 'москва');
    await browser.waitForVisible(`.Suggest-Item*=Москва`);
    await browser.click(`.Suggest-Item*=Москва`);
    await browser.setValue('input[name="postcode"]', details.postcode.value);
    await browser.setValue('input[name="postsuffix"]', details.postsuffix.value);
    await browser.setValue(elements.ogrn, details.ogrn.value);

    await browser.setValue(elements.fax, details.fax.value);
    await browser.click('span=выберите страну');
    await browser.click(`span=${details.country.name}`);
    await browser.setValue(elements.representative, details.representative.value);

    if (role === 'admin') {
        await browser.click('input[name="invalid-address"]');
    }

    await browser.setValue(elements.bik, details.bik.value);
    await browser.setValue(elements.account, details.account.value);

    await browser.setValue(elements.kbk, details.kbk.value);
    await browser.setValue(elements.oktmo, details.oktmo.value);
};
