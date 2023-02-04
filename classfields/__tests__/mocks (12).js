import merge from 'lodash/merge';

import { SETTINGS_CONTACTS_FIELDS } from 'app/lib/settings/contacts';
import { PROFILE_STATUSES } from 'app/lib/settings/profile';

import { BACKEND_ERRORS, MODERATION_MESSAGES, getBackendError } from 'app/lib/settings/libs';

import {
    contactsStateProducerFactory,
    notFilledVosUserDataFactory,
    filledVosUserDataFactory,
    filledPassportData
} from '../../SettingsContacts/__tests__/mocks';

import {
    profileStateProducerFactory,
    filledProfileVosUserData,
    vosUserDataWithModerationErrorsFactory
} from '../../SettingsProfile/__tests__/mocks';

const initialState = {
    vosUserData: {},
    passportData: {},
    contactsOverrides: {},
    profileOverrides: {},
    stateOverrides: {}
};

const getState = ({
    vosUserData,
    passportData,
    contactsOverrides,
    profileOverrides,
    profileStatus,
    stateOverrides
} = initialState) => {
    const contactsProducer = contactsStateProducerFactory(vosUserData, passportData);
    const profileProducer = profileStateProducerFactory(vosUserData, profileStatus);

    return merge({
        user: {
            isJuridical: false,
            ...passportData
        },
        vosUserData: {
            ...vosUserData,
            trustedUserInfo: {
                mosRuTrustedStatus: 'NOT_PROCESSED',
                mosRuAvailable: false
            }
        },
        settings: {
            contacts: contactsProducer(contactsOverrides),
            profile: profileProducer(profileOverrides),
            network: {
                contactsAndProfileSaveStatus: 'success',
                sendingConfirmationCodeStatus: 'success',
                uploadingTrademarkStatus: 'success'
            }
        },
        config: {
            mosruRedirectLinks: {
                url: ''
            }
        }
    }, stateOverrides);
};

export default {
    ownerWithoutCheckedPhones: getState({
        vosUserData: filledVosUserDataFactory('OWNER'),
        passportData: filledPassportData
    }),
    owner: getState({
        vosUserData: notFilledVosUserDataFactory('OWNER')
    }),
    agent: getState({
        vosUserData: notFilledVosUserDataFactory('AGENT')
    }),
    agentJuridical: getState({
        vosUserData: {
            ...notFilledVosUserDataFactory('AGENT'),
            ogrn: '1053600591197'
        }
    }),
    filledAgent: getState({
        vosUserData: filledVosUserDataFactory('AGENT')
    }),
    agency: getState({
        vosUserData: notFilledVosUserDataFactory('AGENCY')
    }),
    agencyWithBackendDuplicateErrors: getState({
        vosUserData: {
            ...filledVosUserDataFactory('AGENCY'),
            ...filledProfileVosUserData,
            agencyProfileEnabled: true
        },
        contactsOverrides: {
            [SETTINGS_CONTACTS_FIELDS.AGENCY_AND_DEVELOPER_NAME]: {
                message: getBackendError(
                    SETTINGS_CONTACTS_FIELDS.AGENCY_AND_DEVELOPER_NAME,
                    BACKEND_ERRORS.NAME_DUPLICATE
                ),
                shouldDisplayMessage: true
            },
            [SETTINGS_CONTACTS_FIELDS.OGRN]: {
                message: getBackendError(
                    SETTINGS_CONTACTS_FIELDS.OGRN,
                    BACKEND_ERRORS.OGRN_DUPLICATE
                ),
                shouldDisplayMessage: true
            }
        },
        profileStatus: null
    }),
    agencyWithModerationErrors: getState({
        vosUserData: {
            ...filledVosUserDataFactory('AGENCY'),
            ...vosUserDataWithModerationErrorsFactory(
                [
                    MODERATION_MESSAGES.CONTACT_IN_DESCRIPTION,
                    MODERATION_MESSAGES.LOGO_INAPPROPRIATE,
                    MODERATION_MESSAGES.NAME_NOT_REGISTERED,
                    MODERATION_MESSAGES.WRONG_CREATE_TIME,
                    MODERATION_MESSAGES.NO_ANSWER
                ],
                'AGENCY'
            )
        },
        profileStatus: PROFILE_STATUSES.REJECTED_MODERATION
    }),
    filledAgency: getState({
        vosUserData: filledVosUserDataFactory('AGENCY')
    })
};
