import humanify from './humanify';

const filters = {
    in_stock: 'TO_ORDER',
    year_from: 2017,
    year_to: 2022,
    price_from: 100,
    price_to: 1000000,
    catalog_filter: [
        {
            mark: 'JAGUAR',
        },
        {
            mark: 'JEEP',
        },
        {
            mark: 'KIA',
            model: 'XCEED',
        },
        {
            mark: 'KIA',
            model: 'RIO',
            generation: '7694524',
        },
        {
            mark: 'KIA',
            model: 'RIO',
            generation: '20508999',
        },
        {
            mark: 'KIA',
            model: 'RIO',
            generation: '22500704',
        },
        {
            mark: 'KIA',
            model: 'CERATO',
            generation: '9343457',
        },
        {
            mark: 'KIA',
            model: 'CERATO',
            generation: '20888011',
        },
    ],
    vin_codes: [
        'XW8ZZZ61ZJG067457',
    ],
    vin_report_statuses: [
        'RED_REPORTS',
    ],
};

const markModels = [
    {
        mark: 'JAGUAR',
        models: [
            {
                model: 'XF',
                offers_count: 2,
                human_name: 'XF',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '7771114',
                        offers_count: 2,
                        human_name: 'I Рестайлинг',
                    },
                ],
            },
            {
                model: 'F_PACE',
                offers_count: 14,
                human_name: 'F-Pace',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '20690937',
                        offers_count: 14,
                        human_name: 'I',
                    },
                ],
            },
            {
                model: 'XJ',
                offers_count: 2,
                human_name: 'XJ',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '20731773',
                        offers_count: 1,
                        human_name: 'IV (X351) Рестайлинг',
                    },
                    {
                        super_gen_id: '6042954',
                        offers_count: 1,
                        human_name: 'IV (X351)',
                    },
                ],
            },
            {
                model: 'E_PACE',
                offers_count: 2,
                human_name: 'E-Pace',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '21041667',
                        offers_count: 2,
                        human_name: 'I',
                    },
                ],
            },
            {
                model: 'I_PACE',
                offers_count: 1,
                human_name: 'I-Pace',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '21211930',
                        offers_count: 1,
                        human_name: 'I',
                    },
                ],
            },
        ],
        offers_count: 21,
        human_name: 'Jaguar',
    },
    {
        mark: 'JEEP',
        models: [
            {
                model: 'WRANGLER',
                offers_count: 7,
                human_name: 'Wrangler',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '2307119',
                        offers_count: 1,
                        human_name: 'III (JK)',
                    },
                    {
                        super_gen_id: '21172381',
                        offers_count: 6,
                        human_name: 'IV (JL)',
                    },
                ],
            },
            {
                model: 'RENEGADE',
                offers_count: 5,
                human_name: 'Renegade',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '20250476',
                        offers_count: 5,
                        human_name: 'I',
                    },
                ],
            },
            {
                model: 'CHEROKEE',
                offers_count: 3,
                human_name: 'Cherokee',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '20155404',
                        offers_count: 2,
                        human_name: 'V (KL)',
                    },
                    {
                        super_gen_id: '21193678',
                        offers_count: 1,
                        human_name: 'V (KL) Рестайлинг',
                    },
                ],
            },
            {
                model: 'GRAND_CHEROKEE',
                offers_count: 15,
                human_name: 'Grand Cherokee',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '20040921',
                        offers_count: 15,
                        human_name: 'IV (WK2) Рестайлинг',
                    },
                ],
            },
        ],
        offers_count: 30,
        human_name: 'Jeep',
    },
    {
        mark: 'KIA',
        models: [
            {
                model: 'VENGA',
                offers_count: 3,
                human_name: 'Venga',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '7303014',
                        offers_count: 2,
                        human_name: 'I',
                    },
                    {
                        super_gen_id: '20452350',
                        offers_count: 1,
                        human_name: 'I Рестайлинг',
                    },
                ],
            },
            {
                model: 'STINGER',
                offers_count: 8,
                human_name: 'Stinger',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '20895791',
                        offers_count: 8,
                        human_name: 'I',
                    },
                ],
            },
            {
                model: 'SPECTRA',
                offers_count: 3,
                human_name: 'Spectra',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '21129042',
                        offers_count: 1,
                        human_name: 'II',
                    },
                    {
                        super_gen_id: '2307256',
                        offers_count: 2,
                        human_name: 'I Рестайлинг 2',
                    },
                ],
            },
            {
                model: 'CARNIVAL',
                offers_count: 1,
                human_name: 'Carnival',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '21186381',
                        offers_count: 1,
                        human_name: 'III',
                    },
                ],
            },
            {
                model: 'MOHAVES',
                offers_count: 7,
                human_name: 'Mohave',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '22033440',
                        offers_count: 4,
                        human_name: 'I Рестайлинг 2',
                    },
                    {
                        super_gen_id: '5052557',
                        offers_count: 1,
                        human_name: 'I',
                    },
                    {
                        super_gen_id: '20979392',
                        offers_count: 2,
                        human_name: 'I Рестайлинг',
                    },
                ],
            },
            {
                model: 'K5',
                offers_count: 9,
                human_name: 'K5',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '22462291',
                        offers_count: 9,
                        human_name: 'III',
                    },
                ],
            },
            {
                model: 'SORENTO',
                offers_count: 34,
                human_name: 'Sorento',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '21993009',
                        offers_count: 1,
                        human_name: 'IV',
                    },
                    {
                        super_gen_id: '8506299',
                        offers_count: 21,
                        human_name: 'II Рестайлинг',
                    },
                    {
                        super_gen_id: '20268637',
                        offers_count: 5,
                        human_name: 'III Prime',
                    },
                    {
                        super_gen_id: '21180462',
                        offers_count: 7,
                        human_name: 'III Prime Рестайлинг',
                    },
                ],
            },
            {
                model: 'SPORTAGE',
                offers_count: 49,
                human_name: 'Sportage',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '4669204',
                        offers_count: 1,
                        human_name: 'II Рестайлинг',
                    },
                    {
                        super_gen_id: '20101920',
                        offers_count: 8,
                        human_name: 'III Рестайлинг',
                    },
                    {
                        super_gen_id: '21365721',
                        offers_count: 12,
                        human_name: 'IV Рестайлинг',
                    },
                    {
                        super_gen_id: '6407700',
                        offers_count: 3,
                        human_name: 'III',
                    },
                    {
                        super_gen_id: '20683534',
                        offers_count: 25,
                        human_name: 'IV',
                    },
                ],
            },
            {
                model: 'OPTIMA',
                offers_count: 34,
                human_name: 'Optima',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '20526471',
                        offers_count: 8,
                        human_name: 'IV',
                    },
                    {
                        super_gen_id: '21342050',
                        offers_count: 22,
                        human_name: 'IV Рестайлинг',
                    },
                    {
                        super_gen_id: '20050520',
                        offers_count: 4,
                        human_name: 'III Рестайлинг',
                    },
                ],
            },
            {
                model: 'CARENS',
                offers_count: 1,
                human_name: 'Carens',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '2307224',
                        offers_count: 1,
                        human_name: 'II (UN)',
                    },
                ],
            },
            {
                model: 'CEED',
                offers_count: 35,
                human_name: 'Ceed',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '8290723',
                        offers_count: 2,
                        human_name: 'II',
                    },
                    {
                        super_gen_id: '21212472',
                        offers_count: 17,
                        human_name: 'III',
                    },
                    {
                        super_gen_id: '2307183',
                        offers_count: 1,
                        human_name: 'I',
                    },
                    {
                        super_gen_id: '6103302',
                        offers_count: 2,
                        human_name: 'I Рестайлинг',
                    },
                    {
                        super_gen_id: '23101718',
                        offers_count: 1,
                        human_name: 'III Рестайлинг',
                    },
                    {
                        super_gen_id: '20681913',
                        offers_count: 12,
                        human_name: 'II Рестайлинг',
                    },
                ],
            },
            {
                model: 'SELTOS',
                offers_count: 1,
                human_name: 'Seltos',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '21662520',
                        offers_count: 1,
                        human_name: 'I',
                    },
                ],
            },
            {
                model: 'PICANTO',
                offers_count: 4,
                human_name: 'Picanto',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '20933523',
                        offers_count: 2,
                        human_name: 'III',
                    },
                    {
                        super_gen_id: '7315189',
                        offers_count: 2,
                        human_name: 'II',
                    },
                ],
            },
            {
                model: 'MAGENTIS',
                offers_count: 1,
                human_name: 'Magentis',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '2307213',
                        offers_count: 1,
                        human_name: 'II',
                    },
                ],
            },
            {
                model: 'RIO',
                offers_count: 83,
                human_name: 'Rio',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '21028015',
                        offers_count: 58,
                        human_name: 'IV',
                    },
                    {
                        super_gen_id: '7694524',
                        offers_count: 10,
                        human_name: 'III',
                    },
                    {
                        super_gen_id: '20508999',
                        offers_count: 8,
                        human_name: 'III Рестайлинг',
                    },
                    {
                        super_gen_id: '22500704',
                        offers_count: 7,
                        human_name: 'IV Рестайлинг',
                    },
                ],
            },
            {
                model: 'XCEED',
                offers_count: 1,
                human_name: 'XCeed',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '21623160',
                        offers_count: 1,
                        human_name: 'I',
                    },
                ],
            },
            {
                model: 'SOUL',
                offers_count: 45,
                human_name: 'Soul',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '21551393',
                        offers_count: 15,
                        human_name: 'III',
                    },
                    {
                        super_gen_id: '20923366',
                        offers_count: 26,
                        human_name: 'II Рестайлинг',
                    },
                    {
                        super_gen_id: '20110752',
                        offers_count: 3,
                        human_name: 'II',
                    },
                    {
                        super_gen_id: '7772395',
                        offers_count: 1,
                        human_name: 'I Рестайлинг',
                    },
                ],
            },
            {
                model: 'CERATO',
                offers_count: 18,
                human_name: 'Cerato',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '21383450',
                        offers_count: 10,
                        human_name: 'IV',
                    },
                    {
                        super_gen_id: '9343457',
                        offers_count: 5,
                        human_name: 'III',
                    },
                    {
                        super_gen_id: '20888011',
                        offers_count: 1,
                        human_name: 'III Рестайлинг (Classic)',
                    },
                    {
                        super_gen_id: '4645742',
                        offers_count: 2,
                        human_name: 'II',
                    },
                ],
            },
            {
                model: 'K900',
                offers_count: 1,
                human_name: 'K900',
                category: 'CARS',
                super_gen_count: [
                    {
                        super_gen_id: '21481320',
                        offers_count: 1,
                        human_name: 'II',
                    },
                ],
            },
        ],
        offers_count: 338,
        human_name: 'Kia',
    },
];

it('гуманифицирует фильтры', async() => {
    expect(humanify(filters, markModels)).toMatchSnapshot();
});
