import merge from 'lodash/merge';

import { AnyObject } from 'realty-core/types/utils';

import { SETTINGS_REQUISITES_FIELDS, getRequisites } from 'app/lib/settings/requisites';

const defaultVosUserData = {
    name: '',
    email: '',
    phones: [],
};

const defaultBillingRequisites = {
    byId: {
        '1': {
            personType: 'ur',
            name: '',
            email: '',
            phone: '',
            legaladdress: '',
            postaddress: '',
            postcode: '',
            kpp: '',
            inn: '',
        },
    },
    ids: ['1'],
};

const defaultBillingNaturalRequisites = {
    byId: {
        '1': {
            id: '1',
            lname: '',
            fname: '',
            mname: '',
            phone: '',
            email: '',
            legaladdress: '',
            is_partner: false,
            hidden: false,
            personType: 'ph',
        },
    },
    ids: ['1'],
};

const filledVosUserData = {
    name: 'Евгений',
    email: '123@list.ru',
    phones: ['+79992134916'],
};

const savedBillingRequisites = {
    byId: {
        '1': {
            longname: 'Уникальный Роман',
            name: 'Уникальный Роман',
            email: 'roman.agency21@yandex.ru',
            phone: '+79992134916',
            legaladdress: 'Улица Пушкина, дом Колотушкина',
            postaddress: 'Дом Колотушкина, улица Пушкина',
            postcode: '192000',
            kpp: '770401001',
            inn: '7704407589',
            personType: 'ur',
        },
    },
    ids: ['1'],
};

const savedNaturalBillingRequisites = {
    byId: {
        '1': {
            id: '14170277',
            lname: 'Башмачкин',
            fname: 'Акакий',
            mname: 'Акакиевич',
            phone: '+74953222222',
            email: 'ztuzmin6@yandex.ru',
            legaladdress: '',
            is_partner: false,
            hidden: false,
            personType: 'ph',
            clientId: 187700886,
            date: '2021-02-15T12:11:58+03:00',
        },
    },
    ids: ['1'],
};

const showAllRequisitesMessagesOverrides = {
    [SETTINGS_REQUISITES_FIELDS.NAME]: { shouldDisplayMessage: true },
    [SETTINGS_REQUISITES_FIELDS.EMAIL]: { shouldDisplayMessage: true },
    [SETTINGS_REQUISITES_FIELDS.PHONE]: { shouldDisplayMessage: true },
    [SETTINGS_REQUISITES_FIELDS.LEGAL_ADDRESS]: { shouldDisplayMessage: true },
    [SETTINGS_REQUISITES_FIELDS.POSTAL_ADDRESS]: { shouldDisplayMessage: true },
    [SETTINGS_REQUISITES_FIELDS.POSTAL_CODE]: { shouldDisplayMessage: true },
    [SETTINGS_REQUISITES_FIELDS.KPP]: { shouldDisplayMessage: true },
    [SETTINGS_REQUISITES_FIELDS.REQUIRED_KPP]: { shouldDisplayMessage: true },
    [SETTINGS_REQUISITES_FIELDS.INN]: { shouldDisplayMessage: true },
};

export const requisitesStateProducerFactory = (
    overrideBillingRequisites: AnyObject,
    overrideVosUserData: AnyObject
) => {
    const billingRequisites = merge({}, defaultBillingRequisites, overrideBillingRequisites);
    const vosUserData = merge({}, defaultVosUserData, overrideVosUserData);

    return (overrideRequisites: AnyObject) => {
        const mainBillingPerson = billingRequisites.byId['1'];
        const requisites = getRequisites(vosUserData, mainBillingPerson || {});

        return merge({}, requisites, overrideRequisites);
    };
};

export const settingsStateProducer = (
    vosUserData: AnyObject,
    billingRequisites: AnyObject,
    requisitesOverrides = {}
) => {
    const requisitesProducer = requisitesStateProducerFactory(billingRequisites, vosUserData);

    return {
        requisites: requisitesProducer(requisitesOverrides),
        network: {
            requisitesSaveStatus: 'success',
        },
    };
};

const getState = (
    vosUserData: AnyObject,
    billingRequisites: AnyObject,
    requisitesOverrides = {},
    stateOverrides = {}
) => {
    return merge(
        {
            billingRequisites,
            vosUserData,
            settings: settingsStateProducer(vosUserData, billingRequisites, requisitesOverrides),
        },
        stateOverrides
    );
};

export const getStore = () => {
    return {
        default: getState({}, defaultBillingRequisites),
        defaultNatural: getState({}, defaultBillingNaturalRequisites),
        filledNatural: getState({}, savedNaturalBillingRequisites),
        withFilledVosUserData: getState(filledVosUserData, defaultBillingRequisites),
        notFilled: getState({}, { byId: {}, ids: [] }, showAllRequisitesMessagesOverrides),
        notLoaded: getState(
            {},
            {
                byId: {},
                ids: [],
                isLoadingError: true,
            },
            showAllRequisitesMessagesOverrides
        ),
        saved: getState({}, savedBillingRequisites),
    };
};
