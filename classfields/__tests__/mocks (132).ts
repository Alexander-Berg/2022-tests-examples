import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { ISiteCard, ISiteDeliveryDate } from 'realty-core/types/siteCard';

import { newbuildingOffer } from '../../__tests__/stubs/offer';

export { newbuildingOffer as offer };

const generateSite = ({
    locativeFullName,
    buildingClass,
    buildingState,
    finishedApartments,
    deliveryDates,
}: {
    locativeFullName: string;
    finishedApartments?: boolean;
    buildingClass?: 'ECONOM' | 'STANDART' | 'COMFORT' | 'COMFORT_PLUS' | 'BUSINESS' | 'ELITE';
    buildingState?: string;
    deliveryDates?: ISiteDeliveryDate[];
}) =>
    (({
        locativeFullName,
        buildingFeatures: {
            class: buildingClass,
            finishedApartments,
            state: buildingState,
        },
        deliveryDates,
        images: {
            list: [
                {
                    full: generateImageUrl({ width: 900, height: 200 }),
                    viewType: 'GENERAL',
                },
            ],
        },
    } as unknown) as ISiteCard);

export const site = generateSite({
    locativeFullName: 'в ЖК Ромашка',
    buildingClass: 'ECONOM',
    finishedApartments: true,
});

export const siteWithLongTitle = generateSite({
    locativeFullName: 'в ЖК Ромашка с очень-очень-очень длинным названием, которое должно быть на две строки',
    buildingClass: 'COMFORT',
    finishedApartments: true,
});

export const siteWithoutBadges = generateSite({ locativeFullName: 'в ЖК Ромашка' });

export const siteWithManyBadges = generateSite({
    locativeFullName: 'в ЖК Ромашка',
    buildingClass: 'BUSINESS',
    buildingState: 'UNFINISHED',
    finishedApartments: true,
    deliveryDates: ([
        {
            finished: false,
            quarter: 3,
            year: 2021,
            buildingState: 'UNFINISHED',
        },
        {
            finished: false,
            quarter: 3,
            year: 2023,
            buildingState: 'UNFINISHED',
        },
    ] as unknown) as ISiteDeliveryDate[],
});

export const siteWithOneFinishDate = generateSite({
    locativeFullName: 'в ЖК Ромашка',
    buildingClass: 'ELITE',
    buildingState: 'UNFINISHED',
    finishedApartments: true,
    deliveryDates: ([
        {
            finished: false,
            quarter: 3,
            year: 2023,
            buildingState: 'UNFINISHED',
        },
    ] as unknown) as ISiteDeliveryDate[],
});
