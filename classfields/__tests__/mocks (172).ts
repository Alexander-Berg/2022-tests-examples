import { generateImageUrl, generateImageAliases } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

const commonPhoneModalData = {
    isError: false,
    isOpened: true,
    isLoading: false,
};

const commonOfferData = {
    offerId: '7567892541657818327',
    offerType: 'SELL',
    offerCategory: 'APARTMENT',
    uid: '4071110006',
    phones: ['+74950000002'],
    trustedOfferInfo: {
        isFullTrustedOwner: false,
        ownerTrustedStatus: 'NOT_LINKED_MOSRU',
        isCadastrPersonMatched: false,
    },
    socialPessimization: false,
    image: generateImageUrl({ width: 240, height: 180 }),
    author: {
        id: '4071110006',
        category: 'DEVELOPER',
        organization: 'ТПУ Рассказовка',
        phones: ['+74950000002'],
        creationDate: '2021-04-27T00:15:13Z',
        allowedCommunicationChannels: ['COM_CALLS'],
        name: 'ТПУ Рассказовка',
        redirectPhonesFailed: false,
        redirectPhones: true,
        encryptedPhones: [{ phoneWithMask: '+7 495 000 ●● ●●', phoneHash: 'KzcF0OHTUJwMLDANwMPDARy' }],
    },
    shouldShowRedirectIndicator: false,
    salesDepartment: {
        id: '344969',
        name: 'Savills',
        phones: ['+74950000002'],
        isRedirectPhones: true,
        weekTimetable: [{ dayFrom: 1, dayTo: 7, timePattern: [{ open: '09:00', close: '21:00' }] }],
        logo: generateImageUrl({ width: 225, height: 75 }),
        dump: {},
        phonesWithTag: [],
        timetableZoneMinutes: 180,
        statParams: 'qwerty',
        encryptedPhones: [{ phoneWithMask: '+7 495 000 ●● ●●', phoneHash: 'KzcF0OHTUJwMLDANwMPDARy' }],
        encryptedDump: 'qwerty',
    },
    yandexRent: false,
    price: {
        value: 10914691,
        period: 'WHOLE_LIFE',
        trend: 'UNCHANGED',
        currency: 'RUR',
        price: { value: 10914691, currency: 'RUB', priceType: 'PER_OFFER', pricingPeriod: 'WHOLE_LIFE' },
    },
    openPlan: false,
    roomsTotal: 2,
    livingSpace: { value: 28.9, unit: 'SQUARE_METER' },
    area: { value: 67.2, unit: 'SQUARE_METER' },
    house: { apartments: false, housePart: false },
    location: {
        rgid: 62823,
        geoId: 145714,
        subjectFederationId: 1,
        subjectFederationRgid: 741964,
        settlementRgid: 62823,
        settlementGeoId: 121774,
        address: 'Москва, поселение Внуковское, улица Анны Ахматовой, 11к3',
        geocoderAddress: 'Россия, Москва, поселение Внуковское, улица Анны Ахматовой, 11к3',
        structuredAddress: {
            component: [],
        },
        point: { latitude: 55.63358, longitude: 37.328915, precision: 'EXACT' },
        metro: {
            metroGeoId: 190121,
            name: 'Рассказовка',
            metroTransport: 'ON_FOOT',
            timeToMetro: 5,
            latitude: 55.63397,
            longitude: 37.33475,
            minTimeToMetro: 5,
            lineColors: ['ffe400'],
            rgbColor: 'ffe400',
        },
        highway: { name: 'Боровское шоссе', distanceKm: 10.192 },
        station: { name: 'Мичуринец', distanceKm: 1.617 },
        streetAddress: 'улица Анны Ахматовой, 11к3',
        metroList: [],
        ponds: [],
        airports: [],
        heatmaps: [],
        allHeatmaps: [],
        routeDistances: [],
        subjectFederationName: 'Москва и МО',
        cityCenter: [],
    },
    floorsOffered: [3],
    floorsTotal: 25,
    newBuilding: true,
    building: {
        siteId: 1706831,
        siteDisplayName: 'ЖК «Городские истории»',
        siteName: 'Городские истории',
        builtYear: 2022,
        builtQuarter: 4,
    },
    partnerId: '1069252863',
    flatType: 'NEW_FLAT',
    primarySaleV2: true,
    queryId: '9908745066a11c8cc4da145c6b587888',
    statParams: '',
    vas: {
        raised: true,
        premium: true,
        placement: false,
        promoted: true,
        turboSale: false,
        raisingSale: false,
        campaignType: 'RAISE',
        tuzFeatured: false,
        vasAvailable: true,
    },
    siteSpecialProposals: {},
    backCallTrafficInfo: {},
};

export const storeWithOnePhoneDeveloperOffer = {
    phoneModal: {
        ...commonPhoneModalData,
        offer: {
            ...commonOfferData,
        },
    },
};

export const storeWithThreePhoneDeveloperOffer = {
    phoneModal: {
        ...commonPhoneModalData,
        offer: {
            ...commonOfferData,
            salesDepartment: {
                ...commonOfferData.salesDepartment,
                logo: generateImageUrl({ width: 400, height: 75 }),
            },
            phones: ['+74950000002', '+74950000003', '+74950000004'],
        },
    },
};

export const storeWithOnePhoneAgencyOffer = {
    phoneModal: {
        ...commonPhoneModalData,
        offer: {
            ...commonOfferData,
            salesDepartment: undefined,
            author: {
                id: '1164466882',
                category: 'AGENCY',
                organization: 'CENTURY 21 Milestone',
                agentName: 'Ланченков Алексей',
                phones: ['+79647978773'],
                creationDate: '2021-03-31T14:54:47Z',
                profile: {
                    userType: 'AGENCY',
                    name: 'С21 Milestone',
                    logo: generateImageAliases({ width: 120, height: 120 }),
                },
                allowedCommunicationChannels: ['COM_CALLS'],
                name: 'CENTURY 21 Milestone',
                redirectPhonesFailed: false,
                redirectPhones: true,
                encryptedPhones: [{ phoneWithMask: '+7 964 797 ●● ●●', phoneHash: 'KzcF5NHjQJ3OLTcN4NPzcRz' }],
            },
        },
    },
};

export const storeWithOnePhoneAuthorOffer = {
    phoneModal: {
        ...commonPhoneModalData,
        offer: {
            ...commonOfferData,
            salesDepartment: undefined,
            author: {
                id: '47497757',
                category: 'OWNER',
                agentName: 'Ирина',
                phones: ['+79057628049'],
                creationDate: '2020-07-03T12:06:06Z',
                humanPhoto: generateImageUrl({ width: 42, height: 42 }),
                allowedCommunicationChannels: ['COM_CALLS'],
                redirectPhonesFailed: false,
                redirectPhones: true,
                encryptedPhones: [{ phoneWithMask: '+7 905 762 ●● ●●', phoneHash: 'KzcF5MHDUJ3NLjIN4MPDQR5' }],
            },
            trustedOfferInfo: {
                isFullTrustedOwner: true,
                ownerTrustedStatus: 'TRUSTED_MOSRU_AND_MATCHED_FLAT',
                isCadastrPersonMatched: true,
            },
            shouldShowRedirectIndicator: true,
        },
    },
};

export const storeWithOnePhoneAuthorWithoutProtectOffer = {
    phoneModal: {
        ...commonPhoneModalData,
        offer: {
            ...commonOfferData,
            salesDepartment: undefined,
            author: {
                id: '1421863476',
                category: 'OWNER',
                agentName: 'Борисова Татьяна',
                phones: ['+79587834405'],
                creationDate: '2021-06-29T12:56:20Z',
                allowedCommunicationChannels: ['COM_CALLS', 'COM_CHATS'],
                redirectPhonesFailed: false,
                redirectPhones: true,
                encryptedPhones: [{ phoneWithMask: '+7 958 783 ●● ●●', phoneHash: 'KzcF5NHTgJ3OLDMN0NPDAR1' }],
            },
        },
    },
};

export const storeWithOnePhoneAuthorNotTrustOffer = {
    phoneModal: {
        ...commonPhoneModalData,
        offer: {
            ...commonOfferData,
            salesDepartment: undefined,
            author: {
                id: '164323507',
                category: 'OWNER',
                agentName: 'собственник',
                phones: ['+79311107645'],
                creationDate: '2019-09-27T10:50:14Z',
                allowedCommunicationChannels: ['COM_CALLS', 'COM_CHATS'],
                redirectPhonesFailed: false,
                redirectPhones: true,
                encryptedPhones: [{ phoneWithMask: '+7 931 110 ●● ●●', phoneHash: 'KzcF5MHzEJxMLTAN3NPjQR1' }],
            },
            socialPessimization: true,
            shouldShowRedirectIndicator: true,
        },
    },
};

export const storeWithOnePhoneAuthorToAuthorOffer = {
    user: {
        uid: '4071110006',
    },
    phoneModal: {
        ...commonPhoneModalData,
        offer: {
            ...commonOfferData,
            salesDepartment: undefined,
            author: {
                id: '36185884',
                category: 'AGENT',
                agentName: 'Сергей',
                profile: {
                    userType: 'AGENT',
                    name: 'Сергей',
                    logo: generateImageAliases({ width: 120, height: 120 }),
                },
                phones: ['+79587824152'],
                creationDate: '2021-06-27T08:07:35Z',
                humanPhoto: generateImageUrl({ width: 42, height: 42 }),
                allowedCommunicationChannels: ['COM_CALLS', 'COM_CHATS'],
                redirectPhones: true,
                redirectPhonesFailed: false,
                encryptedPhones: [{ phoneWithMask: '+7 958 782 ●● ●●', phoneHash: 'KzcF5NHTgJ3OLDIN0MPTURy' }],
            },
            shouldShowRedirectIndicator: true,
        },
    },
};

export const storeWithThreePhoneAuthorOffer = {
    phoneModal: {
        ...commonPhoneModalData,
        offer: {
            ...commonOfferData,
            salesDepartment: undefined,
            author: {
                id: '36185884',
                category: 'PRIVATE_AGENT',
                agentName: 'Сергей',
                phones: ['+79587824152'],
                creationDate: '2021-06-27T08:07:35Z',
                humanPhoto: generateImageUrl({ width: 42, height: 42 }),
                allowedCommunicationChannels: ['COM_CALLS', 'COM_CHATS'],
                redirectPhones: true,
                redirectPhonesFailed: false,
                encryptedPhones: [{ phoneWithMask: '+7 958 782 ●● ●●', phoneHash: 'KzcF5NHTgJ3OLDIN0MPTURy' }],
            },
            phones: ['+74950000002', '+74950000003', '+74950000004'],
            shouldShowRedirectIndicator: true,
        },
    },
};

export const storeWithFourPhoneAuthorWithoutProtectOffer = {
    phoneModal: {
        ...commonPhoneModalData,
        offer: {
            ...commonOfferData,
            salesDepartment: undefined,
            author: {
                id: '36185884',
                category: 'OWNER',
                agentName: 'Виктория',
                phones: ['+79587824152'],
                creationDate: '2021-06-27T08:07:35Z',
                allowedCommunicationChannels: ['COM_CALLS', 'COM_CHATS'],
                redirectPhones: true,
                redirectPhonesFailed: false,
                encryptedPhones: [{ phoneWithMask: '+7 958 782 ●● ●●', phoneHash: 'KzcF5NHTgJ3OLDIN0MPTURy' }],
            },
            phones: ['+74950000002', '+74950000003', '+74950000004', '+74950000005'],
        },
    },
};

export const storeWithOriginalSellerDeveloperOnePhoneOffer = {
    phoneModal: {
        ...commonPhoneModalData,
        offer: {
            ...commonOfferData,
            originalSeller: {
                name: 'MR Group',
                logo: generateImageUrl({ width: 60, height: 60 }),
                encryptedPhones: [{ phoneWithMask: '+7 495 231 ●● ●●', phoneHash: 'KzcF0OHTUJyMLzEN4MPjcR1' }],
                encryptedDump: '123',
                weekTimetable: [{ dayFrom: 1, dayTo: 7, timePattern: [{ open: '09:00', close: '21:00' }] }],
                timetableZoneMinutes: 180,
            },
        },
    },
};

export const storeWithOriginalSellerAgencyThreePhoneOffer = {
    phoneModal: {
        ...commonPhoneModalData,
        offer: {
            ...commonOfferData,
            salesDepartment: undefined,
            author: {
                id: '1164466882',
                category: 'AGENCY',
                organization: 'CENTURY 21 Milestone',
                agentName: 'Ланченков Алексей',
                phones: ['+79647978773'],
                creationDate: '2021-03-31T14:54:47Z',
                profile: {
                    userType: 'AGENCY',
                    name: 'С21 Milestone',
                    logo: generateImageAliases({ width: 120, height: 120 }),
                },
                allowedCommunicationChannels: ['COM_CALLS'],
                name: 'CENTURY 21 Milestone',
                redirectPhonesFailed: false,
                redirectPhones: true,
                encryptedPhones: [{ phoneWithMask: '+7 964 797 ●● ●●', phoneHash: 'KzcF5NHjQJ3OLTcN4NPzcRz' }],
            },
            phones: ['+74950000002', '+74950000003', '+74950000004'],
            originalSeller: {
                name: 'MR Group',
                logo: generateImageUrl({ width: 60, height: 60 }),
                encryptedPhones: [{ phoneWithMask: '+7 495 231 ●● ●●', phoneHash: 'KzcF0OHTUJyMLzEN4MPjcR1' }],
                encryptedDump: '123',
                weekTimetable: [{ dayFrom: 1, dayTo: 7, timePattern: [{ open: '09:00', close: '21:00' }] }],
                timetableZoneMinutes: 180,
            },
        },
    },
};

export const storeWithOriginalSellerOwnerOnePhoneOffer = {
    phoneModal: {
        ...commonPhoneModalData,
        offer: {
            ...commonOfferData,
            salesDepartment: undefined,
            author: {
                id: '47497757',
                category: 'OWNER',
                agentName: 'Ирина',
                phones: ['+79057628049'],
                creationDate: '2020-07-03T12:06:06Z',
                humanPhoto: generateImageUrl({ width: 42, height: 42 }),
                allowedCommunicationChannels: ['COM_CALLS'],
                redirectPhonesFailed: false,
                redirectPhones: true,
                encryptedPhones: [{ phoneWithMask: '+7 905 762 ●● ●●', phoneHash: 'KzcF5MHDUJ3NLjIN4MPDQR5' }],
            },
            originalSeller: {
                name: 'MR Group',
                logo: generateImageUrl({ width: 60, height: 60 }),
                encryptedPhones: [{ phoneWithMask: '+7 495 231 ●● ●●', phoneHash: 'KzcF0OHTUJyMLzEN4MPjcR1' }],
                encryptedDump: '123',
                weekTimetable: [{ dayFrom: 1, dayTo: 7, timePattern: [{ open: '09:00', close: '21:00' }] }],
                timetableZoneMinutes: 180,
            },
        },
    },
};

export const storeWithOriginalSellerAgentTwoPhoneOffer = {
    phoneModal: {
        ...commonPhoneModalData,
        offer: {
            ...commonOfferData,
            salesDepartment: undefined,
            author: {
                id: '47497757',
                category: 'AGENT',
                agentName: 'Mister X',
                phones: ['+79057628049'],
                creationDate: '2020-07-03T12:06:06Z',
                humanPhoto: generateImageUrl({ width: 42, height: 42 }),
                allowedCommunicationChannels: ['COM_CALLS'],
                redirectPhonesFailed: false,
                redirectPhones: true,
                encryptedPhones: [{ phoneWithMask: '+7 905 762 ●● ●●', phoneHash: 'KzcF5MHDUJ3NLjIN4MPDQR5' }],
            },
            shouldShowRedirectIndicator: true,
            phones: ['+74959990324', '+749566666663'],
            originalSeller: {
                name: 'MR Group',
                logo: generateImageUrl({ width: 60, height: 60 }),
                encryptedPhones: [{ phoneWithMask: '+7 495 231 ●● ●●', phoneHash: 'KzcF0OHTUJyMLzEN4MPjcR1' }],
                encryptedDump: '123',
                weekTimetable: [{ dayFrom: 1, dayTo: 7, timePattern: [{ open: '09:00', close: '21:00' }] }],
                timetableZoneMinutes: 180,
            },
        },
    },
};
