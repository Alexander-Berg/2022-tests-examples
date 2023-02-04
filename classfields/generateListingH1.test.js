'use strict';

const deepFreeze = require('deep-freeze');
const MockDate = require('mockdate');

const ENGINE_TYPES = require('auto-core/data/catalog/engine_types.json');

const generateListingH1 = require('./generateListingH1');

// фризим на всякий случай
const mmmInfoWithMark = deepFreeze({
    mark: { id: 'AUDI', cyrillic_name: 'Ауди', name: 'Audi', itemFilterParams: { mark: 'AUDI' } },
});

const mmmInfoWithModel = deepFreeze({
    mark: { id: 'AUDI', cyrillic_name: 'Ауди', name: 'Audi', itemFilterParams: { mark: 'AUDI' } },
    model: { cyrillic_name: 'А1', id: 'A1', itemFilterParams: { model: 'A1' }, name: 'A1' },
});

beforeEach(() => {
    MockDate.set('2020-04-17');
});

afterEach(() => {
    MockDate.reset();
});

const renderLink = ({ name }) => ' ' + name;

[ 'all', 'new', 'used' ].forEach((section) => {
    createSuite({ category: 'cars', section });
    createSuite({ category: 'cars', section, searchParams: { 'do': 300000 } });
    createSuite({ category: 'cars', section, year_from: 2014, year_to: 2014 });
    createSuite({ category: 'cars', section, year_from: 2014, year_to: 2014, searchParams: { 'do': 300000 } });
    createSuite({ category: 'cars', section, year_from: 2014, year_to: 2014, searchParams: { 'do': 300000 },
        catalog_equipment: [ 'seats-5' ], displacement_to: 3000, displacement_from: 3000 });
});

[ 'all', 'new', 'used' ].forEach((section) => {
    [ 'snowmobile', 'scooters', 'motorcycle', 'atv' ].forEach((motoCategory) => {
        createSuite({ category: 'moto', section, moto_category: motoCategory });
        createSuite({ category: 'moto', section, moto_category: motoCategory, price_to: 100000 });
    });
});

[ 'all', 'new', 'used' ].forEach((section) => {
    [
        'agricultural',
        'artic',
        'autoloader',
        'bulldozers',
        'bus',
        'construction',
        'crane',
        'dredge',
        'lcv',
        'municipal',
        'trailer',
        'truck',
    ]
        .forEach((trucksCategory) => {
            createSuite({ category: 'trucks', section, trucks_category: trucksCategory });
            createSuite({ category: 'trucks', section, trucks_category: trucksCategory, price_to: 100000 });
        });
});

Object.keys(ENGINE_TYPES).forEach(engineType => {
    createSuite({ engine_group: [ engineType ], category: 'cars' });
});

function createSuite(searchParams) {
    describe(`searchParams=${ JSON.stringify(searchParams) }`, () => {
        it('нет ммм, нет гео', () => {
            const options = {
                searchParameters: searchParams,
                renderLink,
            };

            expect(generateH1(options)).toMatchSnapshot();
        });

        it('нет ммм, есть гео', () => {
            const options = {
                geoName: 'в Москве',
                searchParameters: searchParams,
                renderLink,
            };

            expect(generateH1(options)).toMatchSnapshot();
        });

        it('нет ммм, есть кузов', () => {
            const options = {
                searchParameters: searchParams,
                bodyTypeGroupSeoName: 'SEDAN',
                renderLink,
            };

            expect(generateH1(options)).toMatchSnapshot();
        });

        it('нет ммм, есть автоматическая трансмиссия', () => {
            const options = {
                searchParameters: {
                    transmission: [ 'AUTO', 'VARIATOR', 'AUTOMATIC', 'ROBOT' ],
                    ...searchParams,
                },
                renderLink,
            };

            expect(generateH1(options)).toMatchSnapshot();
        });

        it('нет ммм, нет трансмиссии', () => {
            const options = {
                searchParameters: {
                    transmission: [ 'AUTOMATIC' ],
                    ...searchParams,
                },
                renderLink,
            };

            expect(generateH1(options)).toMatchSnapshot();
        });

        it('нет ммм, есть привод', () => {
            const options = {
                searchParameters: {
                    gear_type: [ 'FORWARD_CONTROL' ],
                    ...searchParams,
                },
                renderLink,
            };

            expect(generateH1(options)).toMatchSnapshot();
        });

        it('есть марка, нет гео', () => {
            const options = {
                mmmInfo: mmmInfoWithMark,
                searchParameters: {
                    catalog_filter: [ { mark: 'AUDI' } ],
                    ...searchParams,
                },
                renderLink,
            };

            expect(generateH1(options)).toMatchSnapshot();
        });

        it('есть марка, есть гео', () => {
            const options = {
                mmmInfo: mmmInfoWithMark,
                geoName: 'в Москве',
                searchParameters: {
                    catalog_filter: [ { mark: 'AUDI' } ],
                    ...searchParams,
                },
                renderLink,
            };

            expect(generateH1(options)).toMatchSnapshot();
        });

        it('есть марка и модель, нет гео', () => {
            const options = {
                mmmInfo: mmmInfoWithModel,
                searchParameters: {
                    catalog_filter: [ { mark: 'AUDI', model: 'A1' } ],
                    ...searchParams,
                },
                renderLink,
            };

            expect(generateH1(options)).toMatchSnapshot();
        });

        it('есть марка и модель, есть гео', () => {
            const options = {
                mmmInfo: mmmInfoWithModel,
                geoName: 'в Москве',
                searchParameters: {
                    catalog_filter: [ { mark: 'AUDI', model: 'A1' } ],
                    ...searchParams,
                },
                renderLink,
            };

            expect(generateH1(options)).toMatchSnapshot();
        });

        // тесты с кузовом и приводом имеют смысл только в легковых
        if (searchParams.category === 'cars') {
            it('есть марка и кузов', () => {
                const options = {
                    mmmInfo: mmmInfoWithMark,
                    geoName: 'в Москве',
                    searchParameters: {
                        catalog_filter: [ { mark: 'AUDI' } ],
                        ...searchParams,
                    },
                    bodyTypeGroupSeoName: 'SEDAN',
                    renderLink,
                };

                expect(generateH1(options)).toMatchSnapshot();
            });
        }

        if (searchParams.category === 'cars') {
            it('есть марка и привод', () => {
                const options = {
                    mmmInfo: mmmInfoWithMark,
                    geoName: 'в Москве',
                    searchParameters: {
                        catalog_filter: [ { mark: 'AUDI' } ],
                        gear_type: [ 'FORWARD_CONTROL' ],
                        ...searchParams,
                    },
                    renderLink,
                };

                expect(generateH1(options)).toMatchSnapshot();
            });
        }

        if (searchParams.category === 'cars') {
            it('есть кузов и привод', () => {
                const options = {
                    mmmInfo: mmmInfoWithMark,
                    searchParameters: {
                        gear_type: [ 'FORWARD_CONTROL' ],
                        ...searchParams,
                    },
                    bodyTypeGroupSeoName: 'SEDAN',
                    renderLink,
                };

                expect(generateH1(options)).toMatchSnapshot();
            });
        }
    });
}

it('должен вывести правильный текст для страницы автогуру', () => {
    const options = {
        geoName: 'в Москве',
        searchParameters: { category: 'cars' },
        isAutoGuruPage: true,
        renderLink,
    };

    expect(generateH1(options)).toEqual('Сервис по подбору автомобиля по параметрам');
});

it('должен вывести правильный текст для листинга по тегу', () => {
    const options = {
        isTagListingPage: true,
        searchParameters: {
            category: 'cars',
            search_tag: [ 'compact' ],
        },
        searchTagSeoName: 'Компактные автомобили',
        renderLink,
    };

    expect(generateH1(options)).toEqual('Компактные автомобили');
});

it('должен показать цену в рублях', () => {
    const options = {
        geoName: 'в Москве',
        searchParameters: {
            price_to: 10000,
            category: 'cars',
            section: 'all',
        },
        renderLink,
    };

    expect(generateH1(options)).toEqual('Купить автомобиль До 10 000 рублей, в Москве');
});

it('должен показать цену в валюте', () => {
    const options = {
        geoName: 'в Москве',
        searchParameters: {
            price_to: 10000,
            category: 'cars',
            section: 'all',
            currency: 'USD',
        },
        renderLink,
    };

    expect(generateH1(options)).toEqual('Купить автомобиль До 10 000 долларов, в Москве');
});

it('не упадет если не найдет такой привод в словаре', () => {
    const options = {
        geoName: 'в Москве',
        searchParameters: {
            category: 'trucks',
            section: 'all',
            trucks_category: 'lcv',
            gear_type: [ 'FULL' ],
        },
        renderLink,
    };

    expect(generateH1(options)).toEqual('Купить лёгкий коммерческий транспорт в Москве');
});

// Для того, чтобы снепшот был на 4к строк меньше
function generateH1(options) {
    const result = generateListingH1(options);

    if (typeof result === 'object') {
        return result.join('');
    }

    return result;
}
