import merge from 'lodash/merge';

import { SETTINGS_CONTACTS_FIELDS } from 'app/lib/settings/contacts';
import { getContacts } from 'app/controller/lib/settings';
import {
    MODERATION_MESSAGES,
    BACKEND_ERRORS,
    CUSTOM_MESSAGES,
    getBackendError,
    getCustomMessage
} from 'app/lib/settings/libs';

export const notFilledVosUserDataFactory = userType => ({
    userType,
    name: '',
    email: '',
    ogrn: '',
    phones: [],
    redirectPhones: true
});

export const filledVosUserDataFactory = (userType, extendedUserType = userType) => ({
    userType,
    extendedUserType,
    name: [ 'OWNER', 'AGENT' ].includes(userType) ? 'Ким Чен Ын' : 'Google Inc',
    email: 'email@mail.ru',
    ogrn: ! [ 'OWNER' ].includes(userType) ? '1053600591197' : '',
    phones: [ '+79992134920', '+79992134915' ],
    redirectPhones: true
});

export const filledPassportData = {
    passportDefaultEmail: 'passport@emai.ru',
    passportPhones: [ '+79992134920', '+79992134915' ]
};

export const defaultPassportData = {
    passportDefaultEmail: '',
    passportPhones: []
};

const showAllContactsMessagesOverrides = {
    [SETTINGS_CONTACTS_FIELDS.USER_TYPE]: { shouldDisplayMessage: true },
    [SETTINGS_CONTACTS_FIELDS.OWNER_AND_AGENT_NAME]: { shouldDisplayMessage: true },
    [SETTINGS_CONTACTS_FIELDS.AGENCY_AND_DEVELOPER_NAME]: { shouldDisplayMessage: true },
    [SETTINGS_CONTACTS_FIELDS.TRADEMARK]: { shouldDisplayMessage: true },
    [SETTINGS_CONTACTS_FIELDS.EMAIL]: { shouldDisplayMessage: true },
    [SETTINGS_CONTACTS_FIELDS.AGENT_OGRN]: { shouldDisplayMessage: true },
    [SETTINGS_CONTACTS_FIELDS.OGRN]: { shouldDisplayMessage: true },
    [SETTINGS_CONTACTS_FIELDS.REDIRECT_PHONES]: { shouldDisplayMessage: true },
    [SETTINGS_CONTACTS_FIELDS.PHONES]: [ { shouldDisplayMessage: true } ],
    [SETTINGS_CONTACTS_FIELDS.PHONES_WITH_CONFIRMATION]: [ { shouldDisplayMessage: true } ]
};

// из воза генерим стейт, как на ноде
export const contactsStateProducerFactory = (overrideVosUserData, overridePassport) => {
    const vosUserData = merge({}, notFilledVosUserDataFactory('OWNER'), overrideVosUserData);
    const passportData = merge({}, defaultPassportData, overridePassport);

    return overrideContacts => {
        const contacts = getContacts(vosUserData, passportData);

        return merge({}, contacts, overrideContacts);
    };
};

const getState = (vosUserData, passportData, contactsOverrides = {}, stateOverrides = {}) => {
    const contactsProducer = contactsStateProducerFactory(vosUserData, passportData);

    return merge({
        user: {
            isJuridical: false,
            ...passportData
        },
        vosUserData: {
            trustedUserInfo: {
                mosRuTrustedStatus: 'NOT_PROCESSED',
                mosRuAvailable: false
            },
            ...vosUserData
        },
        settings: {
            contacts: contactsProducer(contactsOverrides)
        },
        config: {
            mosruRedirectLinks: {
                url: ''
            }
        }
    }, stateOverrides);
};

const getStateWithModerationMessages = (moderationMessages, userType = 'AGENT') => {
    return getState(
        {
            ...filledVosUserDataFactory(userType),
            currentAgencyProfileErrors: moderationMessages
        },
        filledPassportData
    );
};

const ownerDefaultState = getState({ userType: 'OWNER' });
const ownerFilledState = getState(
    filledVosUserDataFactory('OWNER'),
    filledPassportData
);
const ownerWithMosruAvailableState = getState(
    {
        ...filledVosUserDataFactory('OWNER'),
        trustedUserInfo: {
            mosRuTrustedStatus: 'NOT_PROCESSED',
            mosRuAvailable: true
        }
    },
    filledPassportData
);

const ownerDefaultWithVisibleErrors = getState(
    notFilledVosUserDataFactory('OWNER'),
    {},
    showAllContactsMessagesOverrides
);

const agentNaturalDefaultState = getState(
    notFilledVosUserDataFactory('AGENT')
);
const agentNaturalFilledState = getState(
    filledVosUserDataFactory('AGENT'),
    filledPassportData
);

const agentJuridicalDefaultState = getState(
    notFilledVosUserDataFactory('AGENT'),
    {},
    {},
    { user: { isJuridical: true } }
);
const agentJuridicalFilledState = getState(
    filledVosUserDataFactory('AGENT'),
    filledPassportData,
    {},
    { user: { isJuridical: true } }
);

const agencyDefaultState = getState(
    notFilledVosUserDataFactory('AGENCY'),
    {},
    {},
    { user: { isJuridical: true } }
);

const agencyFilledState = getState(
    filledVosUserDataFactory('AGENCY'),
    {},
    {},
    { user: { isJuridical: true } }
);

const agencyFilledWithLoadedTrademarkState = getState(
    filledVosUserDataFactory('AGENCY'),
    {},
    {
        [SETTINGS_CONTACTS_FIELDS.TRADEMARK]: { value: { url: 'http://123.ru', filename: 'Торговая марка.pdf' } }
    },
    { user: { isJuridical: true } }
);

const agencyFilledWithDuplicateOgrnErrorState = getState(
    filledVosUserDataFactory('AGENCY'),
    {},
    {
        [SETTINGS_CONTACTS_FIELDS.OGRN]: {
            message: getBackendError(SETTINGS_CONTACTS_FIELDS.OGRN, BACKEND_ERRORS.OGRN_DUPLICATE),
            shouldDisplayMessage: true
        } },
    { user: { isJuridical: true } }
);

const agencyFilledWithDuplicateNameErrorState = getState(
    filledVosUserDataFactory('AGENCY'),
    {},
    {
        [SETTINGS_CONTACTS_FIELDS.AGENCY_AND_DEVELOPER_NAME]: {
            message: getBackendError(SETTINGS_CONTACTS_FIELDS.AGENCY_AND_DEVELOPER_NAME, BACKEND_ERRORS.NAME_DUPLICATE),
            shouldDisplayMessage: true
        } },
    { user: { isJuridical: true } }
);

const developerDefaultState = getState(
    notFilledVosUserDataFactory('DEVELOPER'),
    {},
    {},
    { user: { isJuridical: true } }
);

const developerFilledState = getState(
    filledVosUserDataFactory('DEVELOPER'),
    {},
    {},
    { user: { isJuridical: true } }
);

// eslint-disable-next-line max-len
const wrongNameAndNoAnswerState = getStateWithModerationMessages([ MODERATION_MESSAGES.NO_ANSWER, MODERATION_MESSAGES.WRONG_NAME ]);
const nameNotRegisteredState = getStateWithModerationMessages([ MODERATION_MESSAGES.NAME_NOT_REGISTERED ]);
const stolenNameState = getStateWithModerationMessages([ MODERATION_MESSAGES.STOLEN_NAME ]);

const agencyFilledWithTrademarkUploadingFailedState = getState(
    filledVosUserDataFactory('AGENCY'),
    {},
    {
        [SETTINGS_CONTACTS_FIELDS.TRADEMARK]: {
            message: getCustomMessage(CUSTOM_MESSAGES.TRADEMARK_UPLOADING_FAILED),
            shouldDisplayMessage: true
        } },
    { user: { isJuridical: true } }
);

const agencyFilledWithTrademarkTooLargeState = getState(
    {
        ...filledVosUserDataFactory('AGENCY'),
        name: undefined,
        email: undefined
    },
    {},
    {
        [SETTINGS_CONTACTS_FIELDS.AGENCY_AND_DEVELOPER_NAME]: {
            shouldDisplayMessage: true
        },
        [SETTINGS_CONTACTS_FIELDS.EMAIL]: {
            shouldDisplayMessage: true
        },
        [SETTINGS_CONTACTS_FIELDS.TRADEMARK]: {
            value: {
                filename: 'Слишком большой файл',
                sizeError: true
            },
            message: getCustomMessage(CUSTOM_MESSAGES.TRADEMARK_TOO_LARGE),
            shouldDisplayMessage: true
        } },
    { user: { isJuridical: true } }
);

export default {
    owner: {
        default: ownerDefaultState,
        mosru: ownerWithMosruAvailableState,
        filled: ownerFilledState
    },
    agent: {
        defaultNatural: agentNaturalDefaultState,
        filledNatural: agentNaturalFilledState,
        defaultJuridical: agentJuridicalDefaultState,
        filledJuridical: agentJuridicalFilledState
    },
    agency: {
        withLoadedTrademark: agencyFilledWithLoadedTrademarkState,
        default: agencyDefaultState,
        filled: agencyFilledState
    },
    developer: {
        default: developerDefaultState,
        filled: developerFilledState,
        withModerationMessages: {
            ...developerFilledState,
            ...getStateWithModerationMessages([
                MODERATION_MESSAGES.NO_ANSWER,
                MODERATION_MESSAGES.WRONG_NAME
            ], 'DEVELOPER')
        }
    },
    errors: {
        validation: {
            notFilled: ownerDefaultWithVisibleErrors
        },
        moderation: {
            wrongNameAndNoAnswer: wrongNameAndNoAnswerState,
            nameNotRegistered: nameNotRegisteredState,
            stolenName: stolenNameState
        },
        backend: {
            duplicateOgrn: agencyFilledWithDuplicateOgrnErrorState,
            duplicateName: agencyFilledWithDuplicateNameErrorState
        },
        custom: {
            trademarkUploadingFailed: agencyFilledWithTrademarkUploadingFailedState,
            trademarkTooLarge: agencyFilledWithTrademarkTooLargeState
        }
    }
};

