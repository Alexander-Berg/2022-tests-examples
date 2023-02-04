import { LINK_BLOCKS } from 'realty-core/view/react/modules/seo-links/constants/offers-link-blocks';

export const geo = {
    rgid: 587795,
    locative: 'в Москве'
};

const linkStrings = [];

// Подцепляем все алиасы из LINK_BLOCKS
Object.values(LINK_BLOCKS).forEach(item => {
    if (item.titleLink) {
        linkStrings.push(item.titleLink);
    }
    item.subLinks.forEach(link => linkStrings.push(link));
});

export const links = linkStrings.reduce((result, key, index) => {
    result[key] = {
        count: (index || 1) * 100,
        params: {}
    };

    return result;
}, {});

export const sites = [
    {
        id: 271768,
        name: 'Большевик',
        fullName: 'апарт-комплекс «Большевик»',
        price: {
            from: 41200000,
            currency: 'RUR'
        },
        location: {
            rgid: 193328,
            metro: {
                lineColors: [
                    'ed9f2d',
                    '4f8242',
                    '794835'
                ],
                metroGeoId: 20470,
                rgbColor: 'ed9f2d',
                metroTransport: 'ON_FOOT',
                name: 'Белорусская',
                timeToMetro: 15
            }
        }
    },
    {
        id: 175644,
        name: 'Царская площадь',
        fullName: 'МФК «Царская площадь»',
        price: {
            from: 16000000,
            currency: 'RUR'
        },
        location: {
            rgid: 193328,
            metro: {
                lineColors: [
                    '4f8242'
                ],
                metroGeoId: 20558,
                rgbColor: '4f8242',
                metroTransport: 'ON_FOOT',
                name: 'Динамо',
                timeToMetro: 7
            }
        }
    },
    {
        id: 846002,
        name: 'Prime Park',
        fullName: 'ЖК «Prime Park»',
        price: {
            from: 41200000,
            currency: 'RUR'
        },
        location: {
            rgid: 12441,
            metro: {
                lineColors: [
                    'ffe400',
                    '6fc1ba'
                ],
                metroGeoId: 189492,
                rgbColor: 'ffe400',
                metroTransport: 'ON_FOOT',
                name: 'ЦСКА',
                timeToMetro: 14
            }
        }
    },
    {
        id: 2073003,
        name: 'Alcon Tower',
        fullName: 'апарт-комплекс «Alcon Tower»',
        price: {
            from: 41200000,
            currency: 'RUR'
        },
        location: {
            rgid: 193328,
            metro: {
                lineColors: [
                    '4f8242'
                ],
                metroGeoId: 20558,
                rgbColor: '4f8242',
                metroTransport: 'ON_FOOT',
                name: 'Динамо',
                timeToMetro: 11
            }
        }
    }
];
