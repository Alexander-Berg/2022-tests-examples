import { SiteStatisticTypes } from 'types/siteStatistics';

export const LESS_THAN_1E6_VALUES = [
    {
        date: '2021-03-01',
        value: 0,
    },
    {
        date: '2021-03-02',
        value: 5000,
    },
    {
        date: '2021-03-03',
        value: 6000,
    },
    {
        date: '2021-03-04',
        value: 10000,
    },
    {
        date: '2021-03-05',
        value: 25000,
    },
];

export const ZERO_VALUES = [
    {
        date: '2021-03-01',
        value: 0,
    },
    {
        date: '2021-03-02',
        value: 0,
    },
    {
        date: '2021-03-03',
        value: 0,
    },
];

export const GREATER_THAN_1E6_VALUES = [
    {
        date: '2021-03-01',
        value: 0,
    },
    {
        date: '2021-03-02',
        value: 1000000,
    },
    {
        date: '2021-03-03',
        value: 1500000,
    },
    {
        date: '2021-03-04',
        value: 2000000,
    },
    {
        date: '2021-03-05',
        value: 2500000,
    },
];

export const PERCENT_VALUES = [
    {
        date: '2021-03-01',
        value: 0,
    },
    {
        date: '2021-03-02',
        value: 0.20001,
    },
    {
        date: '2021-03-03',
        value: 0.1,
    },
    {
        date: '2021-03-04',
        value: 0.3,
    },
];

export const MORE_THAN_ONE_MONTH = [
    {
        date: '2021-03-01',
        value: 0.1,
    },
    {
        date: '2021-04-02',
        value: 0.5,
    },
    {
        date: '2021-04-03',
        value: 0,
    },
];

export const VERY_SMALL_VALUES = [
    { date: '2021-11-24', value: 0 },
    { date: '2021-11-25', value: 0.00010435932 },
    { date: '2021-11-26', value: 0.00014623624 },
    { date: '2021-11-27', value: 0.000024407746 },
    { date: '2021-11-28', value: 0.00006844058 },
    { date: '2021-11-29', value: 0.00015774672 },
    { date: '2021-11-30', value: 0.0001227324 },
    { date: '2021-12-01', value: 0.0001957941 },
    { date: '2021-12-02', value: 0.000025984149 },
    { date: '2021-12-03', value: 0.000036711528 },
];

export const DEFAULT_DATA_BY_TYPE = {
    [SiteStatisticTypes.OffersNumberByDay]: [
        {
            date: '2021-03-01',
            value: 300,
        },
        {
            date: '2021-03-02',
            value: 250,
        },
        {
            date: '2021-03-03',
            value: 255,
        },
        {
            date: '2021-03-04',
            value: 310,
        },
    ],
    [SiteStatisticTypes.PricePerMeterByDay]: [
        {
            date: '2021-03-01',
            value: 100000,
        },
        {
            date: '2021-03-02',
            value: 120000,
        },
        {
            date: '2021-03-03',
            value: 101000,
        },
        {
            date: '2021-03-04',
            value: 123000,
        },
    ],
    [SiteStatisticTypes.CallsByDay]: [
        {
            date: '2021-03-01',
            value: 30,
        },
        {
            date: '2021-03-02',
            value: 20,
        },
        {
            date: '2021-03-03',
            value: 19,
        },
        {
            date: '2021-03-04',
            value: 8,
        },
    ],
    [SiteStatisticTypes.BidByDay]: [
        {
            date: '2021-03-01',
            value: 8000,
        },
        {
            date: '2021-03-02',
            value: 9043,
        },
        {
            date: '2021-03-03',
            value: 9312,
        },
        {
            date: '2021-03-04',
            value: 9423,
        },
    ],
    [SiteStatisticTypes.CtrByDay]: [
        {
            date: '2021-03-01',
            value: 87,
        },
        {
            date: '2021-03-02',
            value: 85,
        },
        {
            date: '2021-03-03',
            value: 73,
        },
        {
            date: '2021-03-04',
            value: 71,
        },
    ],
    [SiteStatisticTypes.PlaceInSubjectByDay]: [
        {
            date: '2021-03-01',
            value: 3,
        },
        {
            date: '2021-03-02',
            value: 3,
        },
        {
            date: '2021-03-03',
            value: 10,
        },
        {
            date: '2021-03-04',
            value: 3,
        },
    ],
    [SiteStatisticTypes.ClicksPerSubjectByDay]: [
        {
            date: '2021-03-01',
            value: 0.4,
        },
        {
            date: '2021-03-02',
            value: 0.40005,
        },
        {
            date: '2021-03-03',
            value: 0.399,
        },
        {
            date: '2021-03-04',
            value: 0.3,
        },
    ],
    [SiteStatisticTypes.ShowsPerSubjectByDay]: [
        {
            date: '2021-03-01',
            value: 0,
        },
        {
            date: '2021-03-02',
            value: 0.20001,
        },
        {
            date: '2021-03-03',
            value: 0.1,
        },
        {
            date: '2021-03-04',
            value: 0.3,
        },
    ],
    [SiteStatisticTypes.OfferShowsPerSubjectByDay]: [
        {
            date: '2021-03-01',
            value: 0,
        },
        {
            date: '2021-03-02',
            value: 0.01,
        },
        {
            date: '2021-03-03',
            value: 0.02,
        },
        {
            date: '2021-03-04',
            value: 0.01,
        },
    ],
    [SiteStatisticTypes.OfferClicksPerSubjectByDay]: [
        {
            date: '2021-03-01',
            value: 0.88,
        },
        {
            date: '2021-03-02',
            value: 0.88,
        },
        {
            date: '2021-03-03',
            value: 0.87,
        },
        {
            date: '2021-03-04',
            value: 0.88,
        },
    ],
};

export const MORE_THAN_ONE_YEAR = [
    { date: '2021-12-18', value: 0 },
    { date: '2021-12-19', value: 0 },
    { date: '2021-12-20', value: 0 },
    { date: '2021-12-21', value: 0 },
    { date: '2021-12-22', value: 20 },
    { date: '2021-12-23', value: 0 },
    { date: '2021-12-24', value: 0 },
    { date: '2021-12-25', value: 0 },
    { date: '2021-12-26', value: 0 },
    { date: '2021-12-27', value: 0 },
    { date: '2021-12-28', value: 40 },
    { date: '2021-12-29', value: 0 },
    { date: '2021-12-30', value: 0 },
    { date: '2021-12-31', value: 0 },
    { date: '2022-01-01', value: 0 },
    { date: '2022-01-02', value: 0 },
];

export const SITE_PLACE_WITHOUT_SOME_VALUES = [
    {
        date: '2021-03-01',
        value: 0,
    },
    {
        date: '2021-03-02',
        value: 3,
    },
    {
        date: '2021-03-03',
        value: 1,
    },
    {
        date: '2021-03-04',
        value: 3,
    },
    {
        date: '2021-03-05',
        value: 0,
    },
    {
        date: '2021-03-06',
        value: 0,
    },
    {
        date: '2021-03-07',
        value: 3,
    },
    {
        date: '2021-03-08',
        value: 3,
    },
    {
        date: '2021-03-09',
        value: 4,
    },
    {
        date: '2021-03-10',
        value: 4,
    },
];
