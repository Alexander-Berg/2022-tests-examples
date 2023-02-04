import { AnyObject } from 'realty-core/types/utils';

export const initialState = {
    presets: {
        menuPresets: {
            sell: 100,
            rent: 100,
            site: 100,
            commercial: 100,
            villages: 100,
            pik: 100,
        },
    },
    config: {
        yaArendaUrl: 'https://arenda.test.vertis.yandex.ru/',
    },
} as const;

export const initialStateWithoutPresets = {
    presets: {},
    config: {
        yaArendaUrl: 'https://arenda.test.vertis.yandex.ru/',
    },
} as const;

export const initialStateWithNewPresets = {
    presets: {
        menuPresets: {
            sell: 500,
            rent: 500,
            site: 500,
            commercial: 500,
            villages: 500,
            pik: 500,
        },
    },
    config: {
        yaArendaUrl: 'https://arenda.test.vertis.yandex.ru/',
    },
} as const;

export const initialStateForAuthorizedUser = {
    ...initialState,
    user: {
        isAuth: true,
    },
} as const;

function getInitialStateWithSpecialProject({ siteSpecialProject }: { siteSpecialProject: AnyObject }) {
    return {
        ...initialState,
        user: {
            isAuth: true,
        },
        siteSpecialProject,
    };
}

export const initialStateWithLogoSpecialProjects = {
    10174: getInitialStateWithSpecialProject({
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
    11119: getInitialStateWithSpecialProject({
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
    11079: getInitialStateWithSpecialProject({
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
    463788: getInitialStateWithSpecialProject({
        siteSpecialProject: {
            developerId: 463788,
            developerName: 'Унистрой',
            showTab: true,
            tabUrl: '/tatarstan/zastroyschik/unistroj-463788/',
            hideAds: true,
            showFilter: true,
            filterText: 'Только ЖК от Унистрой',
            developerFullName: 'Унистрой',
            hasLogoInTab: true,
            sideMenuText: 'Квартиры от Унистрой',
        },
    }),
    102320: getInitialStateWithSpecialProject({
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
            hasLogoInTab: true,
        },
    }),
    241299: getInitialStateWithSpecialProject({
        siteSpecialProject: {
            developerId: 241299,
            developerName: 'Семья',
            showTab: true,
            hasLogoInTab: true,
            tabUrl: '/krasnodarskiy_kray/zastroyschik/semya-241299/',
            sideMenuText: 'Семья',
        },
    }),
    896162: getInitialStateWithSpecialProject({
        siteSpecialProject: {
            developerId: 896162,
            developerName: 'КОМОССТРОЙ',
            showTab: true,
            hasLogoInTab: true,
            tabUrl: '/udmurtiya/zastroyschik/komosstroj-896162/',
            sideMenuText: 'КОМОССТРОЙ',
        },
    }),
    230663: getInitialStateWithSpecialProject({
        siteSpecialProject: {
            developerId: 230663,
            developerName: 'ЮгСтройИнвест',
            showTab: true,
            hasLogoInTab: true,
            tabUrl: '/rostovskaya_oblast/zastroyschik/yugstrojinvest-230663/',
            sideMenuText: 'ЮгСтройИнвест',
        },
    }),
    75122: getInitialStateWithSpecialProject({
        siteSpecialProject: {
            developerId: 75122,
            developerName: 'ГК Садовое кольцо',
            showTab: true,
            hasLogoInTab: true,
            tabUrl: '/bashkortostan/zastroyschik/gk-sadovoe-kolco-75122/',
            sideMenuText: 'ГК Садовое кольцо',
        },
    }),
    650428: getInitialStateWithSpecialProject({
        siteSpecialProject: {
            developerId: 650428,
            developerName: '4D',
            showTab: true,
            hasLogoInTab: true,
            tabUrl: '/tyumenskaya_oblast/zastroyschik/4d-650428/',
            sideMenuText: 'Квартиры от 4D',
        },
    }),
    123456: getInitialStateWithSpecialProject({
        siteSpecialProject: {
            developerId: 666,
            developerName: 'NoName',
            showTab: true,
            hasLogoInTab: true,
            tabUrl: '/tyumenskaya_oblast/zastroyschik/noname-650428/',
            sideMenuText: 'Квартиры от NoName',
        },
    }),
    524385: getInitialStateWithSpecialProject({
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

export const Gate = {
    get() {
        return Promise.resolve({});
    },
} as const;
