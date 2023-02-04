import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import getMaskPhone from 'realty-core/view/react/libs/get-phone-mask';
import { encrypt } from 'realty-core/app/lib/crypto_phone';

const commonDeveloperData = {
    id: '1',
    name: 'Самолет',
    isExtended: true
};

const site1 = {
    id: 1,
    name: 'Элитная новостройка',
    fullName: 'ЖК "Элитная новостройка"',
    price: {
        from: 16252256,
        to: 46883644,
        currency: 'RUR',
        minPricePerMeter: 265805,
        maxPricePerMeter: 333217,
        rooms: {
            1: {
                soldout: false,
                from: 16252256,
                to: 17272538,
                currency: 'RUR',
                areas: { from: '58.3', to: '61.7' },
                hasOffers: false,
                priceRatioToMarket: 0,
                status: 'ON_SALE'
            },
            2: {
                soldout: false,
                from: 18526644,
                to: 23611502,
                currency: 'RUR',
                areas: { from: '69.7', to: '76.1' },
                hasOffers: false,
                priceRatioToMarket: 0,
                status: 'ON_SALE'
            },
            3: {
                soldout: false,
                from: 24619080,
                to: 36405524,
                currency: 'RUR',
                areas: { from: '83.5', to: '134.5' },
                hasOffers: false,
                priceRatioToMarket: 0,
                status: 'ON_SALE'
            },
            OPEN_PLAN: { soldout: false, currency: 'RUR', hasOffers: false, priceRatioToMarket: 0 },
            STUDIO: { soldout: false, currency: 'RUR', hasOffers: false, priceRatioToMarket: 0 },
            PLUS_4: {
                soldout: false,
                from: 35169420,
                to: 46883644,
                currency: 'RUR',
                areas: { from: '110.7', to: '140.7' },
                hasOffers: false,
                priceRatioToMarket: 0,
                status: 'ON_SALE'
            }
        },
        totalOffers: 0,
        priceRatioToMarket: 0
    },
    phone: {
        phoneWithMask: getMaskPhone('+79876543210', '××\u00a0××'),
        phoneHash: encrypt('+79876543210')
    },
    flatStatus: 'ON_SALE',
    viewTypes: [ 'GENERAL' ],
    salesDepartment: {
        name: 'Офис'
    },
    location: {
        address: 'Москва, Ореховый бул., вл. 24, к. 2',
        subjectFederationRgid: 741964,
        metro: {
            metroGeoId: 20424,
            rgbColor: '4f8242',
            metroTransport: 'ON_FOOT',
            name: 'Красногвардейская',
            timeToMetro: 8
        }
    },
    images: [ 'http://site.com/image.png' ],
    appLargeImages: [ generateImageUrl({ width: 1000, height: 560 }) ],
    developers: [],
    siteSpecialProposals: [ {
        specialProposalType: 'mortgage',
        description: 'Ипотека 4.5%',
        minRate: '4.5',
        mainProposal: true
    } ],
    buildingClass: 'ELITE',
    state: 'UNFINISHED',
    withBilling: true,
    awards: {}
};

const site2 = {
    id: 2,
    name: 'Новостройка бизнес класса',
    fullName: 'ЖК "Новостройка бизнес класса"',
    price: {
        from: 5663000,
        to: 45395180,
        currency: 'RUR',
        minPricePerMeter: 172774,
        maxPricePerMeter: 290994,
        rooms: {
            1: {
                soldout: false,
                from: 7605750,
                to: 14742460,
                currency: 'RUR',
                areas: { from: '32', to: '53' },
                hasOffers: false,
                priceRatioToMarket: 0,
                status: 'ON_SALE'
            },
            2: {
                soldout: false,
                from: 9988600,
                to: 23250570,
                currency: 'RUR',
                areas: { from: '49', to: '92' },
                hasOffers: false,
                priceRatioToMarket: 0,
                status: 'ON_SALE'
            },
            3: {
                soldout: false,
                from: 14948900,
                to: 29742080,
                currency: 'RUR',
                areas: { from: '77', to: '114' },
                hasOffers: false,
                priceRatioToMarket: 0,
                status: 'ON_SALE'
            },
            OPEN_PLAN: { soldout: false, currency: 'RUR', hasOffers: false, priceRatioToMarket: 0 },
            STUDIO: {
                soldout: false,
                from: 5663000,
                to: 7018800,
                currency: 'RUR',
                areas: { from: '23', to: '26' },
                hasOffers: false,
                priceRatioToMarket: 0,
                status: 'ON_SALE'
            },
            PLUS_4: {
                soldout: false,
                from: 16759100,
                to: 45395180,
                currency: 'RUR',
                areas: { from: '97', to: '156' },
                hasOffers: false,
                priceRatioToMarket: 0,
                status: 'ON_SALE'
            }
        },
        totalOffers: 0,
        priceRatioToMarket: 0
    },
    phone: {
        phoneWithMask: getMaskPhone('+79877743210', '××\u00a0××'),
        phoneHash: encrypt('+79877743210')
    },
    flatStatus: 'SOLD',
    viewTypes: [ 'GENERAL' ],
    salesDepartment: {
        name: 'Офис'
    },
    location: {
        address: 'Москва, ул. Золоторожский Вал, 11, стр. 20',
        subjectFederationRgid: 741964,
        metro: {
            metroGeoId: 20408,
            rgbColor: 'ff66e8',
            metroTransport: 'ON_FOOT',
            name: 'Авиамоторная',
            timeToMetro: 18
        }
    },
    images: [ 'http://site.com/image.png' ],
    appLargeImages: [ generateImageUrl({ width: 1000, height: 560 }) ],
    developers: [],
    siteSpecialProposals: [ {
        specialProposalType: 'mortgage',
        description: 'Ипотека 4.5%',
        minRate: '4.5',
        mainProposal: true
    }, {
        specialProposalType: 'installment',
        description: 'Беспроцентная рассрочка',
        durationMonths: 24,
        mainProposal: false
    } ],
    buildingClass: 'BUSINESS',
    state: 'UNFINISHED',
    withBilling: true,
    awards: {}
};

const site3 = {
    id: 3,
    name: 'Комфортная новостройка',
    fullName: 'ЖК "Элитная Комфортная"',
    price: {
        from: 5783400,
        to: 20941100,
        currency: 'RUR',
        minPricePerMeter: 166515,
        maxPricePerMeter: 228999,
        rooms: {
            1: {
                soldout: false,
                from: 7213050,
                to: 9640752,
                currency: 'RUR',
                areas: { from: '40.2', to: '42.8' },
                hasOffers: false,
                priceRatioToMarket: 0,
                status: 'ON_SALE'
            },
            2: {
                soldout: false,
                from: 10008900,
                to: 14792450,
                currency: 'RUR',
                areas: { from: '58.5', to: '69.7' },
                hasOffers: false,
                priceRatioToMarket: 0,
                status: 'ON_SALE'
            },
            3: {
                soldout: false,
                from: 12638550,
                to: 16447500,
                currency: 'RUR',
                areas: { from: '75.9', to: '80.2' },
                hasOffers: false,
                priceRatioToMarket: 0,
                status: 'ON_SALE'
            },
            OPEN_PLAN: { soldout: false, currency: 'RUR', hasOffers: false, priceRatioToMarket: 0 },
            STUDIO: {
                soldout: false,
                from: 5783400,
                to: 7648600,
                currency: 'RUR',
                areas: { from: '24.3', to: '33.4' },
                hasOffers: false,
                priceRatioToMarket: 0,
                status: 'ON_SALE'
            },
            PLUS_4: {
                soldout: false,
                from: 15288000,
                to: 20941100,
                currency: 'RUR',
                areas: { from: '90.6', to: '106.3' },
                hasOffers: false,
                priceRatioToMarket: 0,
                status: 'ON_SALE'
            }
        },
        totalOffers: 0,
        priceRatioToMarket: 0
    },
    phone: {
        phoneWithMask: getMaskPhone('+79997743210', '××\u00a0××'),
        phoneHash: encrypt('+79997743210')
    },
    flatStatus: 'ON_SALE',
    viewTypes: [ 'GENERAL' ],
    salesDepartment: {
        name: 'Офис'
    },
    location: {
        address: 'Москва, Шелепихинская наб., вл. 34',
        subjectFederationRgid: 741964,
        metro: {
            metroGeoId: 152948,
            rgbColor: 'ffa8af',
            metroTransport: 'ON_FOOT',
            name: 'Шелепиха',
            timeToMetro: 20
        }

    },
    images: [ 'http://site.com/image.png' ],
    appLargeImages: [ generateImageUrl({ width: 1000, height: 560 }) ],
    developers: [],
    siteSpecialProposals: [ {
        specialProposalType: 'discount',
        description: 'Скидка 4%',
        mainProposal: true
    }, { specialProposalType: 'mortgage', description: 'Ипотека 4.5%', minRate: '4.5', mainProposal: false } ],
    buildingClass: 'COMFORT',
    state: 'UNFINISHED',
    withBilling: true,
    awards: {}
};

export const developerWithOneSite = {
    ...commonDeveloperData,
    mapSites: [ site1 ]
};

export const developerWithTwoSite = {
    ...commonDeveloperData,
    mapSites: [ site2, site1 ]
};

export const developerWithThreeSite = {
    ...commonDeveloperData,
    mapSites: [ site1, site2, site3 ]
};

export const developerWithFourSite = {
    ...commonDeveloperData,
    mapSites: [ site1, site2, site3, site2 ]
};
