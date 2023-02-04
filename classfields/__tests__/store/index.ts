import merge from 'lodash/merge';
import { DeepPartial } from 'utility-types';

import { ICoreStore } from 'realty-core/view/react/common/reducers/types';
import { REGIONS } from 'realty-core/app/lib/constants';

import { defaultStore as store } from './defaultStore';

const getStore = (mergeParams?: DeepPartial<ICoreStore>, overrideParams?: DeepPartial<ICoreStore>) => {
    return { ...merge({}, store, mergeParams), ...overrideParams };
};

export const defaultStore = getStore();
export const notMoscow = getStore({ geo: { isInMO: false, hasYandexRent: false } });
export const withoutSpecialProjects = getStore({ siteSpecialProject: null });
export const withLogoSpecialProjects = {
    1: getStore(),
    10174: getStore({
        siteSpecialProject: {
            developerId: 15947,
            developerName: 'LEGENDA',
            showTab: true,
            hasLogoInTab: true,
            tabUrl: '/sankt-peterburg_i_leningradskaya_oblast/zastroyschik/legenda-intelligent-development-15947/',
            hideAds: true,
            sideMenuText: 'LEGENDA',
        },
    }),
    11119: getStore({
        siteSpecialProject: {
            developerId: 1525537,
            developerName: 'Суварстроит',
            showTab: true,
            hasLogoInTab: true,
            tabUrl: '/tatarstan/zastroyschik/suvarstroit-1525537/',
            hideAds: true,
            sideMenuText: 'Суварстроит',
        },
    }),
    11079: getStore({
        siteSpecialProject: {
            developerId: 442486,
            developerName: 'Столица Нижний',
            showTab: true,
            tabUrl: '/nizhegorodskaya_oblast/zastroyschik/stolica-nizhnij-442486/',
            showPin: false,
            hideAds: true,
            showFilter: true,
            hasLogoInTab: true,
            filterText: 'Только ЖК от Столица Нижний',
            developerFullName: 'Столица Нижний',
            sideMenuText: 'Квартиры от Столица Нижний',
        },
    }),
    463788: getStore({
        siteSpecialProject: {
            developerId: 463788,
            developerName: 'Унистрой',
            showTab: true,
            tabUrl: '/tatarstan/zastroyschik/unistroj-463788/',
            hideAds: true,
            showFilter: true,
            filterText: 'Только ЖК от Унистрой',
            developerFullName: 'Унистрой',
            sideMenuText: 'Квартиры от Унистрой',
        },
    }),
    102320: getStore({
        siteSpecialProject: {
            developerId: 102320,
            developerName: 'Группа «Самолет»',
            showTab: true,
            tabUrl: '/samolet/?from=main_menu',
            showPin: true,
            showFilter: true,
            filterText: 'Только ЖК от Самолет',
            developerFullName: 'Группа «Самолет»',
            sideMenuText: 'Квартиры от Самолет',
        },
    }),
    241299: getStore({
        siteSpecialProject: {
            developerId: 241299,
            developerName: 'Семья',
            showTab: true,
            hasLogoInTab: true,
            tabUrl: '/krasnodarskiy_kray/zastroyschik/semya-241299/',
            sideMenuText: 'Семья',
        },
    }),
    896162: getStore({
        siteSpecialProject: {
            developerId: 896162,
            developerName: 'КОМОССТРОЙ',
            showTab: true,
            hasLogoInTab: true,
            tabUrl: '/udmurtiya/zastroyschik/komosstroj-896162/',
            sideMenuText: 'КОМОССТРОЙ',
        },
    }),
    230663: getStore({
        siteSpecialProject: {
            developerId: 230663,
            developerName: 'ЮгСтройИнвест',
            showTab: true,
            hasLogoInTab: true,
            tabUrl: '/rostovskaya_oblast/zastroyschik/yugstrojinvest-230663/',
            sideMenuText: 'ЮгСтройИнвест',
        },
    }),
    75122: getStore({
        siteSpecialProject: {
            developerId: 75122,
            developerName: 'ГК Садовое кольцо',
            showTab: true,
            hasLogoInTab: true,
            tabUrl: '/bashkortostan/zastroyschik/gk-sadovoe-kolco-75122/',
            sideMenuText: 'ГК Садовое кольцо',
        },
    }),
    650428: getStore({
        siteSpecialProject: {
            developerId: 650428,
            developerName: '4D',
            showTab: true,
            hasLogoInTab: true,
            tabUrl: '/tyumenskaya_oblast/zastroyschik/4d-650428/',
            sideMenuText: 'Квартиры от 4D',
        },
    }),
    524385: getStore({
        siteSpecialProject: {
            developerId: 524385,
            developerName: 'Кронверк',
            showTab: true,
            hasLogoInTab: true,
            tabUrl: '/saratovskaya_oblast/zastroyschik/kronverk-524385/',
            sideMenuText: 'Кронверк',
        },
    }),
};
export const withoutVillages = getStore({ geo: { hasVillages: false } });
export const withoutCommercial = getStore({}, { geo: { ...store.geo, searchFilters: {} } });
export const tabletStore = getStore({ config: { isMobile: 'iPad', os: 'ios' } });

export enum regionTypes {
    MOSCOW = 'MOSCOW',
    MOSCOW_OBLAST = 'MOSCOW_OBLAST',
    REGION = 'REGION',
}

export const getStoreWithDifferentRegions = (region: regionTypes): DeepPartial<ICoreStore> => {
    switch (region) {
        case regionTypes.MOSCOW:
            return getStore({ geo: { rgid: REGIONS.MOSCOW, isMsk: true, isInMO: false, hasYandexRent: true } });
        case regionTypes.MOSCOW_OBLAST:
            return getStore({ geo: { rgid: REGIONS.MOSCOW_OBLAST, isMsk: false, isInMO: true, hasYandexRent: true } });
        case regionTypes.REGION:
            return getStore({ geo: { rgid: REGIONS.VORONEZH, isMsk: false, isInMO: false, hasYandexRent: false } });
    }
};
