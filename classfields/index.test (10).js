const prepareComplectation = require('./index');

const GROUPED_OFFERS = [
    {
        vehicle_info: {
            mark_info: {
                code: 'SKODA',
                name: 'Skoda',
            },
            model_info: {
                code: 'KODIAQ',
                name: 'Kodiaq',
                ru_name: 'Кодиак',
            },
            super_gen: {
                id: '20839003',
                name: 'I',
            },
            configuration: {
                id: '20839055',
                body_type: 'ALLROAD_5_DOORS',
            },
            tech_param: {
                id: '20839377',
                displacement: 1968,
            },
            complectation: {
                id: '21288531',
                name: 'Style',
            },
        },
        groupping_info: {
            base_equipment_count: 25,
            price_from: {
                eur_price: 26362,
                rur_price: 1921200,
                usd_price: 29109,
            },
            price_to: {
                eur_price: 36332,
                rur_price: 2647737,
                usd_price: 40118,
            },
            size: 40,
            colors: [
                '040001',
                'CACECB',
            ],
        },
    },
];

it('должен вернуть список комплектаций, если переданы офферы', () => {
    expect(
        prepareComplectation({
            groupedOffers: GROUPED_OFFERS,
        }),
    ).toStrictEqual([
        {
            tech_info: {
                mark_info: {
                    code: 'SKODA',
                    name: 'Skoda',
                },
                model_info: {
                    code: 'KODIAQ',
                    name: 'Kodiaq',
                    ru_name: 'Кодиак',
                },
                super_gen: {
                    id: '20839003',
                    name: 'I',
                },
                configuration: {
                    id: '20839055',
                    body_type: 'ALLROAD_5_DOORS',
                },
                tech_param: {
                    id: '20839377',
                    displacement: 1968,
                },
                complectation: {
                    id: '21288531',
                    name: 'Style',
                },
            },
            complectation_id: '21288531',
            complectation_name: 'Style',
            option_count: 25,
            price_from: {
                eur_price: 26362,
                rur_price: 1921200,
                usd_price: 29109,
            },
            price_to: {
                eur_price: 36332,
                rur_price: 2647737,
                usd_price: 40118,
            },
            offer_count: 40,
            colors_hex: [
                '040001',
                'CACECB',
            ],
        },
    ]);
});
