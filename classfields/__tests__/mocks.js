const omit = require('lodash/omit');

const getUserV1ResponseJuridical = {
    user: {
        status: 'active',
        login: '4037541989',
        type: 3,
        email: 'roman.agency4@yandex.ru',
        name: 'Роман агентсвтво',
        ogrn: '1177746415857',
        phones: [
            '+75555555555',
            '+74343434343'
        ],
        licenseAgreement: true,
        redirectPhones: true,
        paymentType: 'JURIDICAL_PERSON',
        // https://github.com/YandexClassifieds/realty-frontend/blob/1ab812b784d8ca11711e4b0cc3267035aa304ec9/realty-core/app/resource/realty3/user.js#L29
        userType: 'AGENCY'
    }
};

const getUserV1ResponseNatural = {
    user: {
        status: 'active',
        login: '4036010583',
        type: 0,
        email: 'roman.fizik@yandex.ru',
        name: 'Роман3',
        phones: [
            '+79992134916'
        ],
        redirectPhones: true,
        licenseAgreement: false,
        paymentType: 'NATURAL_PERSON',
        // https://github.com/YandexClassifieds/realty-frontend/blob/1ab812b784d8ca11711e4b0cc3267035aa304ec9/realty-core/app/resource/realty3/user.js#L29
        userType: 'OWNER'
    }
};

const getUserV1ResponseWithoutNameAndEmailAndPhones = {
    user: {
        status: 'active',
        login: '4040765462',
        type: 8,
        name: '',
        phones: [],
        licenseAgreement: false,
        redirectPhones: true,
        paymentType: 'JURIDICAL_PERSON',
        userType: 'AD_AGENCY'
    }
};

const getUserV2ResponseJuridical = {
    id: '4037541989',
    userStatus: 'ACTIVE',
    userSettings: {
        redirectPhones: true,
        licenseAgreement: true
    },
    userContacts: {
        name: 'Роман агентсвтво',
        email: 'roman.agency4@yandex.ru',
        phones: [
            {
                wholePhoneNumber: '+75555555555'
            },
            {
                wholePhoneNumber: '+74343434343'
            }
        ],
        ogrn: '1177746415857'
    },
    userInfo: {
        paymentType: 'PT_JURIDICAL_PERSON',
        userType: 'AGENCY'
    }
};

const getUserV2ResponseNatural = {
    id: '4036010583',
    userStatus: 'ACTIVE',
    userSettings: {
        redirectPhones: true,
        licenseAgreement: false
    },
    userContacts: {
        name: 'Роман3',
        email: 'roman.fizik@yandex.ru',
        phones: [
            {
                wholePhoneNumber: '+79992134916'
            }
        ]
    },
    userInfo: {
        paymentType: 'PT_NATURAL_PERSON',
        userType: 'OWNER'
    }
};

const getUserV2ResponseNaturalWithUnknownPT = {
    id: '4036010583',
    userStatus: 'ACTIVE',
    userSettings: {
        redirectPhones: true
    },
    userContacts: {
        name: 'Роман3',
        email: 'roman.fizik@yandex.ru',
        phones: [
            {
                wholePhoneNumber: '+79992134916'
            }
        ]
    },
    userInfo: {
        paymentType: 'PT_UNKNOWN',
        userType: 'OWNER'
    }
};

const getUserV2ResponseWithoutNameAndEmailAndPhones = {
    id: '4040765462',
    userStatus: 'ACTIVE',
    userSettings: {
        redirectPhones: true
    },
    userContacts: {},
    userInfo: {
        paymentType: 'PT_JURIDICAL_PERSON',
        userType: 'AD_AGENCY'
    }
};

module.exports = {
    get: {
        juridical: {
            v2: getUserV2ResponseJuridical,
            v1: getUserV1ResponseJuridical
        },
        natural: {
            v2: getUserV2ResponseNatural,
            v1: getUserV1ResponseNatural
        },
        unknownPT: {
            v2: getUserV2ResponseNaturalWithUnknownPT,
            v1: getUserV1ResponseNatural
        },
        withoutNameAndEmailAndPhones: {
            v2: getUserV2ResponseWithoutNameAndEmailAndPhones,
            v1: getUserV1ResponseWithoutNameAndEmailAndPhones
        }
    },
    updade: {
        juridical: {
            v2: omit(getUserV2ResponseJuridical, 'userStatus', 'id'),
            v1: {
                ...getUserV1ResponseJuridical.user,
                telephones: getUserV1ResponseJuridical.user.phones,
                phones: undefined
            }
        },
        natural: {
            v2: omit(getUserV2ResponseNatural, 'userStatus', 'id'),
            v1: {
                ...getUserV1ResponseNatural.user,
                telephones: getUserV1ResponseNatural.user.phones,
                phones: undefined
            }
        }
    }
};
