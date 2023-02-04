import merge from 'lodash/merge';

import { SETTINGS_PROFILE_FIELDS, PROFILE_STATUSES } from 'app/lib/settings/profile';
import { getProfile } from 'app/controller/lib/settings';
import { MODERATION_MESSAGES, CUSTOM_MESSAGES, getCustomMessage } from 'app/lib/settings/libs';

import { contactsStateProducerFactory } from '../../SettingsContacts/__tests__/mocks';

const vosFilledAgencyProfile = {
    name: 'Уникальный',
    ogrn: '1177746415857',
    phones: [
        {
            wholePhoneNumber: '+79992134916'
        }
    ],
    logoUrl: 'https://avatars.mdst.yandex.net/get-realty/3274/add.159050324415822da49ccc6/orig',
    foundationDate: '2020-03-02T21:00:00Z',
    address: {
        unifiedAddress: 'Иркутск, улица Баррикад, 32',
        point: {
            latitude: 52.29173,
            longitude: 104.313545
        }
    },
    workSchedule: [
        {
            day: 'MONDAY',
            minutesFrom: 510,
            minutesTo: 1200
        },
        {
            day: 'FRIDAY',
            minutesFrom: 510,
            minutesTo: 1200
        }
    ],
    description: 'Заполненное агентсвто'
};

const defaultVosUserData = {
    agencyProfileEnabled: false,
    currentAgencyProfile: undefined,
    approvedAgencyProfile: undefined
};

export const filledProfileVosUserData = {
    agencyProfileEnabled: false,
    currentAgencyProfile: vosFilledAgencyProfile
};

const approvedProfileVosUserData = {
    agencyProfileEnabled: true,
    approvedAgencyProfile: vosFilledAgencyProfile
};

const onModerationProfileVosUserData = {
    agencyProfileEnabled: true,
    currentAgencyProfile: vosFilledAgencyProfile
};

export const vosUserDataWithModerationErrorsFactory = (moderationErrors, userType = 'AGENCY') => ({
    userType,
    agencyProfileEnabled: true,
    currentAgencyProfile: vosFilledAgencyProfile,
    currentAgencyProfileErrors: moderationErrors
});

const showAllProfileMessagesOverrides = {
    [SETTINGS_PROFILE_FIELDS.IS_ENABLED]: { shouldDisplayMessage: true },
    [SETTINGS_PROFILE_FIELDS.LOGO_URL]: { shouldDisplayMessage: true },
    [SETTINGS_PROFILE_FIELDS.PHOTO_URL]: { shouldDisplayMessage: true },
    [SETTINGS_PROFILE_FIELDS.FOUNDATION_DATE]: { shouldDisplayMessage: true },
    [SETTINGS_PROFILE_FIELDS.ADDRESS]: { shouldDisplayMessage: true },
    [SETTINGS_PROFILE_FIELDS.AGENT_ADDRESS]: { shouldDisplayMessage: true },
    [SETTINGS_PROFILE_FIELDS.WORKING_DAYS]: { shouldDisplayMessage: true },
    [SETTINGS_PROFILE_FIELDS.WORKING_HOURS]: { shouldDisplayMessage: true },
    [SETTINGS_PROFILE_FIELDS.DESCRIPTION]: { shouldDisplayMessage: true }
};

// из воза генерим стейт, как на ноде
export const profileStateProducerFactory = (
    overrideVosUserData,
    profileStatus = PROFILE_STATUSES.NOT_FILLED_PROFILE
) => {
    const vosUserData = merge({}, defaultVosUserData, overrideVosUserData);

    return profileOverrides => {
        const profile = getProfile(vosUserData, profileStatus);

        return merge({}, profile, profileOverrides);
    };
};

const getState = ({
    vosUserData = {},
    profileOverrides = {},
    stateOverrides = {},
    profileStatus
} = {}) => {
    if (! vosUserData.userType) {
        vosUserData.userType = 'AGENCY';
    }

    const profileProducer = profileStateProducerFactory(vosUserData, profileStatus);
    const contactsProducer = contactsStateProducerFactory(vosUserData);

    return merge({
        user: {
            isJuridical: false
        },
        vosUserData,
        settings: {
            profile: profileProducer(profileOverrides),
            contacts: contactsProducer()
        }
    }, stateOverrides);
};

export default {
    defaultAgency: getState(),
    defaultAgent: getState({ vosUserData: { userType: 'AGENT' } }),
    defaultFilled: getState({ vosUserData: filledProfileVosUserData }),
    defaultNotFilledAgencyErrors: getState({ vosUserData: {}, profileOverrides: showAllProfileMessagesOverrides }),
    defaultNotFilledAgentErrors: getState({
        vosUserData: { userType: 'AGENT' },
        profileOverrides: showAllProfileMessagesOverrides
    }),
    uploadingAgencyLogoFailed: getState({
        vosUserData: {},
        profileOverrides: {
            [SETTINGS_PROFILE_FIELDS.LOGO_URL]: {
                message: getCustomMessage(
                    CUSTOM_MESSAGES.LOGO_OR_PHOTO_UPLOADING_FAILED,
                    SETTINGS_PROFILE_FIELDS.LOGO_URL
                ),
                shouldDisplayMessage: true
            }
        }
    }),
    uploadingAgentPhotoFailed: getState({
        vosUserData: { userType: 'AGENT' },
        profileOverrides: {
            [SETTINGS_PROFILE_FIELDS.PHOTO_URL]: {
                message: getCustomMessage(
                    CUSTOM_MESSAGES.LOGO_OR_PHOTO_UPLOADING_FAILED,
                    SETTINGS_PROFILE_FIELDS.PHOTO_URL
                ),
                shouldDisplayMessage: true
            }
        }
    }),
    tooLargeAgencyLogo: getState({
        vosUserData: filledProfileVosUserData,
        profileOverrides: {
            [SETTINGS_PROFILE_FIELDS.LOGO_URL]: {
                message: getCustomMessage(
                    CUSTOM_MESSAGES.LOGO_OR_PHOTO_TOO_LARGE,
                    SETTINGS_PROFILE_FIELDS.LOGO_URL
                ),
                shouldDisplayMessage: true
            }
        }
    }),
    tooLargeAgentPhoto: getState({
        vosUserData: {
            ...filledProfileVosUserData,
            userType: 'AGENT'
        },
        profileOverrides: {
            [SETTINGS_PROFILE_FIELDS.PHOTO_URL]: {
                message: getCustomMessage(
                    CUSTOM_MESSAGES.LOGO_OR_PHOTO_TOO_LARGE,
                    SETTINGS_PROFILE_FIELDS.PHOTO_URL
                ),
                shouldDisplayMessage: true
            }
        }
    }),
    published: getState({
        vosUserData: approvedProfileVosUserData,
        profileStatus: PROFILE_STATUSES.PUBLISHED
    }),
    onModeration: getState({
        vosUserData: onModerationProfileVosUserData,
        profileStatus: PROFILE_STATUSES.ON_MODERATION
    }),
    rejectedModeration: getState({
        vosUserData: approvedProfileVosUserData,
        profileStatus: PROFILE_STATUSES.REJECTED_MODERATION
    }),
    disabledProfile: getState({
        vosUserData: filledProfileVosUserData,
        profileStatus: PROFILE_STATUSES.DISABLED_PROFILE
    }),
    onModerationWithPublishedProfile: getState({
        vosUserData: approvedProfileVosUserData,
        profileStatus: PROFILE_STATUSES.ON_MODERATION_WITH_PUBLISHED_PROFILE
    }),
    rejectedModerationWithPublishedProfile: getState({
        vosUserData: approvedProfileVosUserData,
        profileStatus: PROFILE_STATUSES.REJECTED_MODERATION_WITH_PUBLISHED_PROFILE
    }),
    willBeDisabled: getState({
        vosUserData: approvedProfileVosUserData,
        profileStatus: PROFILE_STATUSES.WILL_BE_DISABLED
    }),
    willBePublished: getState({
        vosUserData: approvedProfileVosUserData,
        profileStatus: PROFILE_STATUSES.WILL_BE_PUBLISHED
    }),
    willBeRepublished: getState({
        vosUserData: approvedProfileVosUserData,
        profileStatus: PROFILE_STATUSES.WILL_BE_REPUBLISHED
    }),
    moderationErrors: {
        setOneAgency: getState({
            vosUserData: vosUserDataWithModerationErrorsFactory([
                MODERATION_MESSAGES.LOGO_LOW_DEFINITION,
                MODERATION_MESSAGES.WRONG_CREATE_TIME,
                MODERATION_MESSAGES.STOPWORD
            ])
        }),
        setOneAgent: getState({
            vosUserData: vosUserDataWithModerationErrorsFactory([
                MODERATION_MESSAGES.LOGO_LOW_DEFINITION,
                MODERATION_MESSAGES.WRONG_CREATE_TIME,
                MODERATION_MESSAGES.STOPWORD
            ], 'AGENT')
        }),
        setTwoAgency: getState({
            vosUserData: vosUserDataWithModerationErrorsFactory([
                MODERATION_MESSAGES.LOGO_INAPPROPRIATE,
                MODERATION_MESSAGES.CONTACT_IN_DESCRIPTION
            ])
        }),
        setTwoAgent: getState({
            vosUserData: vosUserDataWithModerationErrorsFactory([
                MODERATION_MESSAGES.LOGO_INAPPROPRIATE,
                MODERATION_MESSAGES.CONTACT_IN_DESCRIPTION
            ], 'AGENT')
        }),
        setThree: getState({
            vosUserData: vosUserDataWithModerationErrorsFactory([
                MODERATION_MESSAGES.UNKNOWN_ERROR
            ])
        })
    },
    enableProfileWarning: getState({
        vosUserData: {
            agencyProfileEnabled: true,
            currentAgencyProfile: {
                name: 'Иван Петров',
                ogrn: '1177746126040',
                logoUrl: 'https://avatars.mdst.yandex.net/get-realty/3022/add.1592297794982e08668afc9/orig',
                foundationDate: '2000-01-31T21:00:00Z',
                address: {
                    unifiedAddress: 'Петергоф, Санкт-Петербург, Санкт-Петербургский проспект, 60',
                    point: {
                        latitude: 59.88227,
                        longitude: 29.89025
                    }
                },
                workSchedule: [
                    {
                        day: 'THURSDAY',
                        minutesFrom: 600,
                        minutesTo: 1439
                    },
                    {
                        day: 'TUESDAY',
                        minutesFrom: 600,
                        minutesTo: 1439
                    }
                ],
                description: 'опрпорпрпор'
            },
            currentAgencyProfileErrors: [
                'WRONG_CREATE_TIME'
            ],
            userType: 'AGENCY',
            ogrn: '1177746126040',
            name: 'Иван Петров'
        },
        profileStatus: null
    })
};

