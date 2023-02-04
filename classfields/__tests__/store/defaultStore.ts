import { DeepPartial } from 'utility-types';

import { ICoreStore } from 'realty-core/view/react/common/reducers/types';
import { MainMenuPromoBlockType } from 'realty-core/types/header';

export const defaultStore: DeepPartial<ICoreStore> = {
    geo: {
        rgid: 587795,
        id: 1,
        isInMO: true,
        hasVillages: true,
        hasYandexRent: true,
        searchFilters: {
            'COMMERCIAL:SELL': {},
        },
        parents: [],
    },
    config: {
        isMobile: undefined,
        os: undefined,
        yaArendaAgencyId: 1260401477,
        yaArendaUrl: 'https://realty.yandex.ru',
    },
    page: {
        name: 'index',
    },
    siteSpecialProject: {
        developerId: 52308,
        developerName: 'ПИК',
        showTab: true,
        hasLogoInTab: true,
        tabUrl: '/pik/?from=main_menu',
        showPin: true,
        hideAds: true,
        showFilter: true,
        filterText: 'Только ЖК от ПИК',
        developerFullName: 'ПИК',
        sideMenuText: 'Квартиры от ПИК',
    },
    header: {
        promoblocks: {
            SELL: MainMenuPromoBlockType.CHECK_APARTMENT,
            RENT: MainMenuPromoBlockType.DEVELOPERS_MAP,
            SITES: MainMenuPromoBlockType.FIND_AGENT,
            COMMERCIAL: MainMenuPromoBlockType.FIND_AGENT,
            MORTGAGE: MainMenuPromoBlockType.MORTGAGE_CALCULATOR,
            FOR_PROFESSIONAL: MainMenuPromoBlockType.MORTGAGE_CALCULATOR,
        },
    },
};
