import { PriceStatisticsType } from 'realty-core/types/priceStatistics';

const defaultDots = [
    {
        meanPrice: '8037652',
        meanPricePerM2: '212709',
        timestamp: '2021-05-31T00:00:00Z',
    },
    {
        meanPrice: '8210057',
        meanPricePerM2: '219990',
        timestamp: '2021-06-30T00:00:00Z',
    },
    {
        meanPrice: '8479833',
        meanPricePerM2: '229978',
        timestamp: '2021-07-31T00:00:00Z',
    },
    {
        meanPrice: '8245896',
        meanPricePerM2: '226772',
        timestamp: '2021-08-31T00:00:00Z',
    },
    {
        meanPrice: '8303322',
        meanPricePerM2: '228836',
        timestamp: '2021-09-30T00:00:00Z',
    },
    {
        meanPrice: '8214773',
        meanPricePerM2: '228032',
        timestamp: '2021-10-31T00:00:00Z',
    },
];

const dotsWithBlankData = [
    {
        meanPrice: '8037652',
        meanPricePerM2: '212709',
        timestamp: '2021-05-31T00:00:00Z',
    },
    {
        meanPrice: '8210057',
        meanPricePerM2: '219990',
        timestamp: '2021-06-30T00:00:00Z',
    },
    {
        meanPrice: '8303322',
        meanPricePerM2: '228836',
        timestamp: '2022-09-30T00:00:00Z',
    },
    {
        meanPrice: '8214773',
        meanPricePerM2: '228032',
        timestamp: '2022-10-31T00:00:00Z',
    },
];

export const priceStatistics = {
    TWO: defaultDots,
    ONE: defaultDots,
    STUDIO: defaultDots,
    PLUS_4: defaultDots,
    THREE: defaultDots,
} as PriceStatisticsType;

export const priceStatisticsSingle = {
    TWO: defaultDots,
} as PriceStatisticsType;

export const priceStatisticsWithBlankData = {
    TWO: dotsWithBlankData,
} as PriceStatisticsType;
