export const params = {
    pageSize: 10,
    rgid: '741964',
    sort: 'RELEVANCE',
    type: 'SELL',
    category: 'APARTMENT',
} as const;

export type ParamsType = typeof params;

export const sortDecl = {
    control: 'select',
    defaultValue: 'RELEVANCE',
    values: ['RELEVANCE', 'DATE_DESC', 'PRICE', 'PRICE_DESC', 'AREA', 'AREA_DESC'],
    deps: {
        values: [
            [{ newFlat: 'YES' }, ['RELEVANCE', 'PRICE', 'COMMISSIONING_DATE']],
            [{ objectType: 'VILLAGE' }, ['RELEVANCE', 'PRICE', 'COMMISSIONING_DATE']],
            [
                { objectType: 'OFFER', villageOfferType: ['TOWNHOUSE', 'COTTAGE'] },
                ['RELEVANCE', 'PRICE', 'PRICE_DESC', 'HOUSE_AREA', 'HOUSE_AREA_DESC'],
            ],
            [
                { objectType: 'OFFER', villageOfferType: ['LAND'] },
                ['RELEVANCE', 'PRICE', 'PRICE_DESC', 'LAND_AREA', 'LAND_AREA_DESC'],
            ],
            [
                { ctype: 'SELL', category: ['APARTMENT', 'GARAGE', 'LOT'] },
                [
                    'RELEVANCE',
                    'DATE_DESC',
                    'PRICE',
                    'PRICE_DESC',
                    'AREA',
                    'AREA_DESC',
                    'PRICE_PER_SQUARE',
                    'PRICE_PER_SQUARE_DESC',
                ],
            ],
        ],
    },
} as const;
