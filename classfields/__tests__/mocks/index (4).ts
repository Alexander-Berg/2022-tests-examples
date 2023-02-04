import merge from 'lodash/merge';

// @ts-ignore
import profilePageDefaultStore from './profilePage';
import defaultStore from './store';
import offers from './offers';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const getStore = ({ profileCardOverrides = {}, storeOverrides = {} }: any = {}) => {
    const profileCard = merge({}, profilePageDefaultStore, profileCardOverrides);
    const store = merge({}, defaultStore, storeOverrides, { profiles: { profileCard } });

    return store;
};

const defaultAgent = getStore({
    profileCardOverrides: { card: { userType: 'AGENT', name: 'Сергей Пантелеевич Мавроди' } },
});

const withManyOffers = getStore({
    profileCardOverrides: {
        card: {
            offerCounters: {
                filteredOffers: 10,
            },
        },
        offers: {
            items: offers.apartment,
            total: 6,
        },
    },
});

const withOneOffer = getStore({
    profileCardOverrides: {
        card: {
            offerCounters: {
                totalOffers: 1,
                filteredOffers: 1,
            },
        },
        offers: {
            items: offers.apartment.slice(0, 1),
            total: 1,
        },
    },
});

const withoutOffers = getStore({
    profileCardOverrides: {
        card: {
            offerCounters: {
                totalOffers: 1,
                filteredOffers: 10,
            },
        },
        offers: {
            items: [],
            total: 0,
        },
    },
});

const shortDescription = getStore({
    profileCardOverrides: {
        card: {
            description: 'Привет, кажется, что это короткое описание, которое не должно обрезаться',
        },
    },
});

const sellCommercial = getStore({
    profileCardOverrides: {
        card: {
            offerCounters: {
                totalOffers: 1,
                filteredOffers: 1,
            },
        },
        offers: {
            items: offers.commercial.slice(0, 1),
            total: 1,
        },
        searchParams: {
            type: 'SELL',
            category: 'COMMERCIAL',
        },
    },
});

export default {
    defaultAgency: getStore(),
    defaultAgent,
    withManyOffers,
    withOneOffer,
    withoutOffers,
    shortDescription,
    sellCommercial,
};
