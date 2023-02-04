const elements = {
    select: '.yb-create-person button[type=button]',
    menu: {
        am_jp_1: '.Menu-Text=Юр. лицо РСЯ, Армения',
        am_jp_0: '.Menu-Text=Юр. лицо-резидент, Армения',
        az_ur_0: '.Menu-Text=Юр. лицо, Азербайджан',
        by_ytph_0: '.Menu-Text=Физ. лицо-нерезидент, СНГ',
        byp_0: '.Menu-Text=Физ. лицо, Республика Беларусь',
        byu_1: '.Menu-Text=Юр. лицо РСЯ, Республика Беларусь',
        byu_0: '.Menu-Text=Юр. лицо, Республика Беларусь',
        de_ur_0: '.Menu-Text=Юр. лицо, Германия',
        de_ph_0: '.Menu-Text=Физ. лицо, Германия',
        de_yt_0: '.Menu-Text=Нерезидент, Германия',
        de_ytph_0: '.Menu-Text=Физ. лицо-нерезидент, Германия',
        eu_yt_1: '.Menu-Text=Нерезидент РСЯ, Нидерланды',
        eu_yt_0: '.Menu-Text=Нерезидент, Нидерланды',
        fr_ur_1: '.Menu-Text=Юр. лицо РСЯ, Франция',
        fr_ur_0: '.Menu-Text=Юр. лицо, Франция',
        gb_ur_1: '.Menu-Text=Юр. лицо РСЯ, Великобритания',
        gb_ur_0: '.Menu-Text=Юр. лицо, Великобритания',
        hk_yt_1: '.Menu-Text=Нерезидент РСЯ, Гонконг',
        hk_ytph_1: '.Menu-Text=Физ. лицо РСЯ (нерезидент, Гонконг)',
        il_ur_1: '.Menu-Text=Юр. лицо РСЯ, Израиль',
        il_ur_0: '.Menu-Text=Юр. лицо, Израиль',
        kzp_0: '.Menu-Text=Физ. лицо, Казахстан',
        kzu_1: '.Menu-Text=Юр. лицо РСЯ, Казахстан',
        kzu_0: '.Menu-Text=Юр. лицо, Казахстан',
        ph_0: '.Menu-Text=Физ. лицо',
        ph_1: '.Menu-Text=Физ. лицо РСЯ',
        ro_ur_1: '.Menu-Text=Юр. лицо РСЯ, Румыния',
        ro_ur_0: '.Menu-Text=Юр. лицо, Румыния',
        sk_ur_0: '.Menu-Text=Юр. лицо, Южная Корея',
        sw_ph_0: '.Menu-Text=Физ. лицо, Швейцария',
        sw_ur_0: '.Menu-Text=Юр. лицо, Швейцария',
        sw_yt_1: '.Menu-Text=Нерезидент РСЯ, Швейцария',
        sw_yt_0: '.Menu-Text=Нерезидент, Швейцария',
        sw_ytph_1: '.Menu-Text=Физ. лицо РСЯ (нерезидент, Швейцария)',
        sw_ytph_0: '.Menu-Text=Физ. лицо-нерезидент, Швейцария',
        ur: '.Menu-Text=Юр. лицо',
        ur_1: '.Menu-Text=Юр. лицо или ИП РСЯ',
        ur_autoru_0: '.Menu-Text=Юр. лицо Авто.ру',
        usp_0: '.Menu-Text=Физ. лицо, США',
        ur_ytkz_0: '.Menu-Text=Юр. лицо, Нерезидент Казахстан',
        usu_0: '.Menu-Text=Юр. лицо, США',
        us_yt_0: '.Menu-Text=Нерезидент, США',
        us_ytph_0: '.Menu-Text=Физ. лицо-нерезидент, США',
        yt_0: '.Menu-Text=Нерезидент',
        yt_1: '.Menu-Text=Нерезидент РСЯ',
        yt_kzp_0: '.Menu-Text=Физ. лицо, Нерезидент РФ, Казахстан',
        yt_kzu_0: '.Menu-Text=Юр. лицо, Нерезидент РФ, Казахстан',
        ytph_1: '.Menu-Text=Физ. лицо-нерезидент РСЯ',
        ytph_0: '.Menu-Text=Физ. лицо-нерезидент'
    },
    btnSubmitAddPerson: '.yb-create-person button[type=submit]',
    personExport: '.yb-person-export',
    personName: '.yb-person-id__name',
    personsList: '.yb-subpersons-persons',
    personDetails: '.yb-person-details',
    formChangePerson:
        'form.src-common-modules-change-person-components-ChangePerson-___styles-module__personForm',

    legaladdress: 'div[data-detail-id="legaladdress"] textarea',
    name: 'div[data-detail-id="name"] input',

    editLink: '.yb-person-id__edit-link',
    archiveLink: '.yb-person-id__archive-link',
    updated: '.yb-person-detail__updated',
    suggestSpin: '.suggest__spin-container',
    yamoneyWallet: 'div[data-detail-id="yamoneyWallet"]',
    serverErrorCode: '.yb-error__error-code'
};

elements.btnSubmit = `${elements.formChangePerson} [type=submit]`;

for (let id of [
    // text
    'account',
    'account',
    'address',
    'authorityDocDetails',
    'bank',
    'bankcity',
    'bik',
    'city',
    'corraccount',
    'email',
    'fax',
    'fname',
    'inn',
    'kbk',
    'kpp',
    'legalAddressCity',
    'legalAddressHome',
    'legalAddressPostcode',
    'legalAddressStreet',
    'lname',
    'longname',
    'mname',
    'ogrn',
    'oktmo',
    'paymentPurpose',
    'phone',
    'postaddress',
    'postbox',
    'postcode',
    'postcodeSimple',
    'postsuffix',
    'purchaseOrder',
    'representative',
    'signerPersonName'
]) {
    elements[id] = `div[data-detail-id="${id}"] input[type=text]`;
}

for (let id of [
    'invalidAddres',
    'liveSignature',
    'invalidBankprops',
    'vip',
    'invalidAddress',
    'earlyDocs'
]) {
    elements[id] = `div[data-detail-id="${id}"] input[type=checkbox]`;
}

elements.mnameCheckbox = 'div[data-detail-id="mname"] input[type=checkbox]';

for (let id of [
    // dropdowns
    'countryId',
    'region',
    'deliveryType',
    'deliveryCity',
    'signerPersonGender',
    'signerPositionName',
    'authorityDocType',
    'reviseActPeriodType'
]) {
    elements[id] = `div[data-detail-id="${id}"] button`;
}

elements.error = '.src-common-presentational-components-Field-___field-module__detail__error';

elements.detail = detailId => `div[data-detail-id="${detailId}"]`;
elements.detailError = detailId => `div[data-detail-id="${detailId}"] ${elements.error}`;

module.exports.elements = elements;
