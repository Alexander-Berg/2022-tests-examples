import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { ISiteCard } from 'realty-core/types/siteCard';
const gateRes = {
    rooms: [
        {
            roomType: 'STUDIO',
            priceFrom: '5194763',
            priceTo: '5878993',
            areaFrom: 22.3,
            areaTo: 27.44,
            flatPlansCount: 4,
            offersCount: 21,
        },
        {
            roomType: '1',
            priceFrom: '5773461',
            priceTo: '7555833',
            areaFrom: 28.56,
            areaTo: 40.05,
            flatPlansCount: 4,
            offersCount: 21,
        },
        {
            roomType: '2',
            priceFrom: '6554228',
            priceTo: '7261228',
            areaFrom: 33.28,
            areaTo: 35.65,
            flatPlansCount: 5,
            offersCount: 17,
        },
        {
            roomType: '3',
            priceFrom: '8330280',
            priceTo: '9613801',
            areaFrom: 52.08,
            areaTo: 57.24,
            flatPlansCount: 4,
            offersCount: 16,
        },
    ],
};

export const Gate = {
    get: () => Promise.resolve(gateRes),
};

export const initialState = {
    cookies: {},
    genPlan: {
        housesMap: {},
        image: generateImageUrl({
            width: 800,
            height: 300,
        }),
        houses: [
            {
                finished: true,
                year: 2022,
                phaseName: '1 очередь',
                id: '1839212',
                buildingSiteName: 'корпус 58',
                hasPlans: true,
                commissioningDate: { year: 2022, quarter: 3, constructionState: 'CONSTRUCTION_STATE_UNFINISHED' },
                maxFloor: 22,
                genplanPolygon: {
                    points: [
                        {
                            x: 0.2,
                            y: 0.2,
                        },
                        {
                            x: 0.5,
                            y: 0.2,
                        },
                        {
                            x: 0.5,
                            y: 0.5,
                        },
                        {
                            x: 0.2,
                            y: 0.5,
                        },
                    ],
                    center: {
                        x: 0.3,
                        y: 0.3,
                    },
                },
            },
            {
                finished: false,
                year: 2028,
                phaseName: '1 очередь',
                id: '1839213',
                buildingSiteName: 'корпус 58',
                hasPlans: false,
                maxFloor: 22,
                genplanPolygon: {
                    points: [
                        {
                            x: 0.7,
                            y: 0.7,
                        },
                        {
                            x: 0.9,
                            y: 0.7,
                        },
                        {
                            x: 0.9,
                            y: 0.9,
                        },
                        {
                            x: 0.7,
                            y: 0.9,
                        },
                    ],
                    center: {
                        x: 0.8,
                        y: 0.8,
                    },
                },
            },
        ],
    },
};

export const siteCard = ({
    locativeFullName: 'в ЖК Фантазии',
    images: {
        list: [
            {
                viewType: 'GENPLAN',
                full: generateImageUrl({
                    width: 800,
                    height: 300,
                }),
            },
        ],
    },
    deliveryDates: [
        {
            finished: true,
            quarter: 4,
            year: 2020,
            phaseName: '1 очередь',
            houses: 8,
            housesInfo: [
                {
                    id: '1839212',
                    buildingSiteName: 'корпус 58',
                    name: '1',
                    maxFloor: 17,
                },
            ],
        },
    ],
} as unknown) as ISiteCard;

export const siteCardWithFlyover: ISiteCard = {
    ...siteCard,
    hasFlyover: true,
    permalink: '/',
};
