import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { IDevelopersListData } from 'realty-core/types/developer';

export const geo = {
    rgid: 587795,
    locative: 'в Москве',
};

const getLogoMock = () => ({
    minicard: generateImageUrl({ width: 300, height: 250 }),
    main: generateImageUrl({ width: 300, height: 250 }),
    alike: generateImageUrl({ width: 300, height: 250 }),
    large: generateImageUrl({ width: 300, height: 250 }),
    cosmic: generateImageUrl({ width: 300, height: 250 }),
    appMiddle: generateImageUrl({ width: 300, height: 250 }),
    appLarge: generateImageUrl({ width: 300, height: 250 }),
    appSnippetMini: generateImageUrl({ width: 300, height: 250 }),
    appSnippetSmall: generateImageUrl({ width: 300, height: 250 }),
    appSnippetMiddle: generateImageUrl({ width: 300, height: 250 }),
    appSnippetLarge: generateImageUrl({ width: 300, height: 250 }),
    optimize: generateImageUrl({ width: 300, height: 250 }),
    large1242: generateImageUrl({ width: 300, height: 250 }),
});

export const getDevelopersList = (): IDevelopersListData[] => {
    return [
        {
            id: '11111',
            name: 'Застройщик 1',
            logo: generateImageUrl({ width: 100, height: 70 }),
            born: '1999-12-12T21:00:00Z',
            description: '«Застройщик 1» — описание',
            link: 'http://www.developer1.ru/',
            phones: ['+7 (111) 111-11-11'],
            additionalSales: false,
            statistic: {
                allHouses: 1,
                allSites: 1,
                suspended: {
                    houses: 1,
                    sites: 1,
                },
            },
            logotype: getLogoMock(),
            sites: 1,
        },
        {
            id: '22222',
            name: 'Застройщик 2',
            logo: generateImageUrl({ width: 100, height: 70 }),
            born: '1992-12-12T21:00:00Z',
            description: '«Застройщик 2» — описание',
            link: 'http://www.developer2.ru/',
            phones: ['+7 (222) 222-22-22'],
            additionalSales: false,
            statistic: {
                allHouses: 5,
                allSites: 2,
                finished: {
                    houses: 3,
                    sites: 1,
                },
                unfinished: {
                    houses: 2,
                    sites: 1,
                },
            },
            logotype: getLogoMock(),
            sites: 2,
        },
        {
            id: '33333',
            name: 'Застройщик 3',
            description: '«Застройщик 3» — описание',
            link: 'http://www.developer3.ru/',
            phones: ['+7 (333) 333-33-33'],
            additionalSales: false,
            statistic: {
                allHouses: 0,
                allSites: 0,
            },
            logotype: getLogoMock(),
            sites: 0,
        },
    ];
};
