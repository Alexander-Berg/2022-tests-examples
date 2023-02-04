import { MainMenuItemType, MainMenuExpandedItemType, MainMenuPromotionItemType } from 'realty-core/types/header';
import { LinkProp } from 'realty-core/view/react/common/enhancers/withLink';

export const testCases: [
    MainMenuItemType,
    [MainMenuExpandedItemType, Parameters<LinkProp>][],
    [MainMenuPromotionItemType, Parameters<LinkProp> | string][]
][] = [
    [
        MainMenuItemType.SELL,
        [
            [MainMenuExpandedItemType.APARTMENT, ['search', { rgid: 587795, category: 'APARTMENT', type: 'SELL' }]],
            [
                MainMenuExpandedItemType.NEWBUILDING_APARTMENT,
                ['search', { rgid: 587795, category: 'APARTMENT', type: 'SELL', newFlat: 'YES' }],
            ],
            [MainMenuExpandedItemType.ROOM, ['search', { rgid: 587795, category: 'ROOMS', type: 'SELL' }]],
            [MainMenuExpandedItemType.HOUSE_OR_COTTAGE, ['search', { rgid: 587795, category: 'HOUSE', type: 'SELL' }]],
            [MainMenuExpandedItemType.LOT, ['search', { rgid: 587795, category: 'LOT', type: 'SELL' }]],
            [MainMenuExpandedItemType.GARAGE, ['search', { rgid: 587795, category: 'GARAGE', type: 'SELL' }]],
            [MainMenuExpandedItemType.COMMERCIAL, ['search', { rgid: 587795, category: 'COMMERCIAL', type: 'SELL' }]],
        ],
        [
            [MainMenuPromotionItemType.YANDEX_DEAL_VALUATION, ['ya-deal-valuation', { flatType: 'odnokomnatnaya' }]],
            [MainMenuPromotionItemType.CHECK_APARTMENT, ['egrn-address-purchase']],
            [MainMenuPromotionItemType.FIND_AGENT, ['profile-search', { rgid: 587795, profileUserType: 'AGENCY' }]],
            [MainMenuPromotionItemType.JOURNAL, ['journal-category', { categoryId: 'vtorichnoe-zhilyo' }]],
            [MainMenuPromotionItemType.OFFERS_HISTORY, ['offers-archive']],
            [MainMenuPromotionItemType.DOCUMENTS, ['documents']],
        ],
    ],
    [
        MainMenuItemType.RENT,
        [
            [MainMenuExpandedItemType.APARTMENT, ['search', { rgid: 587795, category: 'APARTMENT', type: 'RENT' }]],
            [MainMenuExpandedItemType.ROOM, ['search', { rgid: 587795, category: 'ROOMS', type: 'RENT' }]],
            [MainMenuExpandedItemType.HOUSE_OR_COTTAGE, ['search', { rgid: 587795, category: 'HOUSE', type: 'RENT' }]],
            [MainMenuExpandedItemType.GARAGE, ['search', { rgid: 587795, category: 'GARAGE', type: 'RENT' }]],
            [MainMenuExpandedItemType.COMMERCIAL, ['search', { rgid: 587795, category: 'COMMERCIAL', type: 'RENT' }]],
            [
                MainMenuExpandedItemType.SHORT_RENT,
                ['search', { rgid: 587795, category: 'APARTMENT', rentTime: 'SHORT', type: 'RENT' }],
            ],
        ],
        [
            [
                MainMenuPromotionItemType.YANDEX_ARENDA,
                'https://realty.yandex.ru/?from=main_menu&utm_source=header_dropdown_yarealty',
            ],
            [MainMenuPromotionItemType.FIND_AGENT, ['profile-search', { rgid: 587795, profileUserType: 'AGENCY' }]],
            [MainMenuPromotionItemType.JOURNAL, ['journal-category', { categoryId: 'arenda' }]],
            [MainMenuPromotionItemType.DOCUMENTS, ['documents']],
        ],
    ],
    [
        MainMenuItemType.SITES,
        [
            [
                MainMenuExpandedItemType.APARTMENT,
                ['search', { rgid: 587795, category: 'APARTMENT', type: 'SELL', newFlat: 'YES' }],
            ],
            [MainMenuExpandedItemType.SITES, ['newbuilding-search', { rgid: 587795, type: 'SELL' }]],
            [MainMenuExpandedItemType.HOUSE_OR_COTTAGE, ['search', { rgid: 587795, category: 'HOUSE', type: 'SELL' }]],
            [MainMenuExpandedItemType.VILLAGES, ['village-search', { rgid: 587795, type: 'SELL' }]],
        ],
        [
            [MainMenuPromotionItemType.JOURNAL, ['journal-category', { categoryId: 'novostroyki' }]],
            [MainMenuPromotionItemType.DOCUMENTS, ['documents']],
            [MainMenuPromotionItemType.DEVELOPERS_LIST, ['developers-list', { rgid: 587795 }]],
        ],
    ],
    [
        MainMenuItemType.COMMERCIAL,
        [
            [MainMenuExpandedItemType.SELL, ['search', { rgid: 587795, type: 'SELL', category: 'COMMERCIAL' }]],
            [MainMenuExpandedItemType.RENT, ['search', { rgid: 587795, type: 'RENT', category: 'COMMERCIAL' }]],
        ],
        [[MainMenuPromotionItemType.JOURNAL, ['journal']]],
    ],
    [
        MainMenuItemType.MORTGAGE,
        [
            [MainMenuExpandedItemType.MORTGAGE_PROGRAMS, ['mortgage-search', { rgid: 587795, flatType: 'NEW_FLAT' }]],
            [MainMenuExpandedItemType.MORTGAGE_CALCULATOR, ['mortgage-calculator', { rgid: 587795 }]],
        ],
        [[MainMenuPromotionItemType.JOURNAL, ['journal']]],
    ],
    [
        MainMenuItemType.FOR_PROFESSIONAL,
        [
            [MainMenuExpandedItemType.OPTIONS_AND_PLACEMENT_REQUIREMENTS, ['promotion']],
            [MainMenuExpandedItemType.PROFESSIONAL_SEARCH, ['profsearch']],
        ],
        [[MainMenuPromotionItemType.JOURNAL, ['journal-category', { categoryId: 'analitika' }]]],
    ],
];
