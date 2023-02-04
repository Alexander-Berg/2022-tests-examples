import merge from 'lodash/merge';

const credit = {
    contractId: '3249677',
    spent: 3120,
    total: 60000000,
    remain: 59996880,
    remainsWithoutNds: 18199013,
    unpaid: 0,
    personId: 14843329,
    expired: 0
};

export const noPersonBillingRequisites = {
    byId: {},
    ids: []
};

export const singlePersonBillingRequisites = {
    byId: {
        12001231: {
            id: '12001231',
            name: 'ztuzmax1',
            longname: 'ztuzmax1',
            phone: '+77777777777',
            email: 'ztuzmax1@yandex.ru',
            legaladdress: 'ываываыва',
            inn: '7715400440',
            kpp: '771501001',
            postcode: '555555',
            postaddress: 'ываываываы',
            hidden: false,
            personType: 'ur',
            clientId: 1337971312,
            date: '2020-07-20T16:14:53+03:00',
            credit
        }
    },
    ids: [ '12001231' ]
};

export const longNameBillingRequisites = {
    byId: {
        12001231: {
            id: '12001231',
            // eslint-disable-next-line max-len
            name: 'zagentUL11 ПЕРВИЧНАЯ ПРОФСОЮЗНАЯ ОРГАНИЗАЦИЯ ГОСУДАРСТВЕННОГО ЛЕЧЕБНО-ПРОФИЛАКТИЧЕСКОГО УЧРЕЖДЕНИЯ ДЕТСКОЙ ГОРОДСКОЙ ПОЛИКЛИНИКИ № 75 УПРАВЛЕНИЯ ЗДРАВООХРАНЕНИЯ СВАО КОМИТЕТА ЗДРАВООХРАНЕНИЯ Г.МОСКВЫ',
            longname: 'ztuzmax1',
            phone: '+77777777777',
            email: 'ztuzmax1@yandex.ru',
            legaladdress: 'ываываыва',
            inn: '7715400440',
            kpp: '771501001',
            postcode: '555555',
            postaddress: 'ываываываы',
            hidden: false,
            personType: 'ur',
            clientId: 1337971312,
            date: '2020-07-20T16:14:53+03:00',
            credit
        }
    },
    ids: [ '12001231' ]
};

export const twoPersonBillingRequisites = {
    byId: {
        12001231: {
            id: '12001231',
            name: 'ztuzmax1',
            longname: 'ztuzmax1',
            phone: '+77777777777',
            email: 'ztuzmax1@yandex.ru',
            legaladdress: 'ываываыва',
            inn: '7715400440',
            kpp: '771501001',
            postcode: '555555',
            postaddress: 'ываываываы',
            hidden: false,
            personType: 'ur',
            clientId: 1337971312,
            date: '2020-07-20T16:14:53+03:00',
            credit
        },
        12001232: {
            id: '12001232',
            name: 'ztuzmax2',
            longname: 'ztuzmax1',
            phone: '+77777777777',
            email: 'ztuzmax1@yandex.ru',
            legaladdress: 'ываываыва',
            inn: '7715400440',
            kpp: '771501001',
            postcode: '555555',
            postaddress: 'ываываываы',
            hidden: false,
            personType: 'ur',
            clientId: 1337971312,
            date: '2020-07-20T16:14:53+03:00',
            credit
        }
    },
    ids: [ '12001231', '12001232' ]
};

export const getState = (stateOverrides = {}) => {
    return merge({
        wallet: {
            status: 'loaded', // 'errored'
            balance: {
                balance: 9120500
            },
            creditStatus: 'loaded' // 'pending'
        },
        page: { name: 'dashboard' },
        billingRequisites: twoPersonBillingRequisites,
        user: { crc: '1' }
    }, stateOverrides);
};
