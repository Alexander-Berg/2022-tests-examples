const autoRuSusanin = require('./auto.ru/susanin');
const mAutoRuSusanin = require('./auto.ru/susanin');
const magAutoRuSusanin = require('./mag.auto.ru/susanin');

const desktopTestCases = require('./auto.ru/susanin.testcases');
const mobileTestCases = require('./auto.ru/susanin.testcases');

const defaultOptions = {
    baseDomain: 'auto.ru',
    geoAlias: '',
    geoIds: [],
    geoOverride: false,
};

const defaultRouteParams = {};

mobileTestCases.forEach((testCase) => {
    it(`auto.ru должен построить ${ testCase.url }`, () => {
        const options = {
            ...defaultOptions,
        };

        jest.doMock('auto-core/appConfig', () => {
            return { appId: 'af-mobile' };
        });
        const linkModule = require('./link');
        const link = linkModule(mAutoRuSusanin, testCase.route.routeName, testCase.route.routeParams, options);

        expect(link).toBe(`https://auto.ru${ testCase.url }`);
    });
});

desktopTestCases.forEach((testCase) => {
    it(`auto.ru должен построить ${ testCase.url }`, () => {
        const options = {
            ...defaultOptions,
        };

        const linkModule = require('./link');
        const link = linkModule(autoRuSusanin, testCase.route.routeName, testCase.route.routeParams, options);

        expect(link).toBe('https://auto.ru' + testCase.url);
    });
});

describe('Проверяем генерацию адресов для auto.ru', () => {
    it('для мобильного', () => {
        const routeName = 'index';
        const options = {
            ...defaultOptions,
        };

        const routeParams = { ...defaultRouteParams };

        jest.doMock('auto-core/appConfig', () => {
            return { appId: 'af-mobile' };
        });
        const linkModule = require('./link');
        const link = linkModule(mAutoRuSusanin, routeName, routeParams, options);

        expect(link).toBe('https://auto.ru/');
    });

    it('для десктопа', () => {
        const routeName = 'index';
        const susanin = autoRuSusanin;
        const options = { ...defaultOptions };
        const routeParams = { ...defaultRouteParams };

        const linkModule = require('./link');
        const link = linkModule(susanin, routeName, routeParams, options);

        expect(link).toBe('https://auto.ru/');
    });
});

describe('Проверяем генерацию адресов для mag.auto.ru', () => {
    it('mag-index', () => {
        const options = { ...defaultOptions };

        const linkModule = require('./link');
        const link = linkModule(magAutoRuSusanin, 'mag-index', {}, options);
        expect(link).toBe('https://mag.auto.ru/');
    });

    it('mag.auto.ru/tag/research/', () => {
        const options = { ...defaultOptions };

        const linkModule = require('./link');
        const link = linkModule(magAutoRuSusanin, 'mag-tag', { tag_id: 'research' }, options);
        expect(link).toBe('https://mag.auto.ru/tag/research/');
    });
});

describe('Проверяем генерацию хостов для мобильных', () => {
    it('для мобильного', () => {
        const routeName = 'reviews-index';
        const options = {
            ...defaultOptions,
        };

        const routeParams = { ...defaultRouteParams };

        jest.doMock('auto-core/appConfig', () => {
            return { appId: 'af-mobile' };
        });
        const linkModule = require('./link');
        const link = linkModule(mAutoRuSusanin, routeName, routeParams, options);

        expect(link).toBe('https://auto.ru/reviews/');
    });

    it('для десктопа', () => {
        const routeName = 'reviews-index';
        const susanin = autoRuSusanin;
        const options = { ...defaultOptions };
        const routeParams = { ...defaultRouteParams };

        const linkModule = require('./link');
        const link = linkModule(susanin, routeName, routeParams, options);

        expect(link).toBe('https://auto.ru/reviews/');
    });
});

describe('review-card', () => {
    let desktopOptions;
    let mobileOptions;
    beforeEach(() => {
        desktopOptions = {
            ...defaultOptions,
        };

        mobileOptions = {
            ...defaultOptions,
        };
    });

    describe('полный набор параметров', () => {
        let params;
        beforeEach(() => {
            params = {
                category: undefined,
                mark: 'GAZ',
                model: '31105',
                parent_category: 'cars',
                reviewId: '9285',
                super_gen: '6419084',
            };
        });

        it('должен сгенерировать урл для десктопа', () => {
            const linkModule = require('./link');
            const url = linkModule(autoRuSusanin, 'review-card', params, desktopOptions);
            expect(url).toEqual('https://auto.ru/review/cars/gaz/31105/6419084/9285/');
        });

        it('должен сгенерировать урл для мобилки', () => {
            jest.doMock('auto-core/appConfig', () => {
                return { appId: 'af-mobile' };
            });
            const linkModule = require('./link');
            const url = linkModule(mAutoRuSusanin, 'review-card', params, mobileOptions);
            expect(url).toEqual('https://auto.ru/review/cars/gaz/31105/6419084/9285/');
        });
    });

    describe('неполный набор параметров', () => {
        // это кейс для старых урлов, где может не быть модели и поколения

        let params;
        beforeEach(() => {
            params = {
                category: undefined,
                mark: 'GAZ',
                model: undefined,
                parent_category: 'cars',
                reviewId: '9715',
                super_gen: undefined,
            };
        });

        it('должен сгенерировать урл для десктопа', () => {
            const linkModule = require('./link');
            const url = linkModule(autoRuSusanin, 'review-card', params, desktopOptions);
            expect(url).toEqual('https://auto.ru/review/cars/gaz/9715/');
        });

        it('должен сгенерировать урл для мобилки', () => {
            jest.doMock('auto-core/appConfig', () => {
                return { appId: 'af-mobile' };
            });
            const linkModule = require('./link');
            const url = linkModule(mAutoRuSusanin, 'review-card', params, mobileOptions);
            expect(url).toEqual('https://auto.ru/review/cars/gaz/9715/');
        });
    });

    describe('вообще нет ммм', () => {
        // это кейс для старых урлов, где может не быть марки, модели и поколения

        let params;
        beforeEach(() => {
            params = {
                category: undefined,
                mark: undefined,
                model: undefined,
                parent_category: 'cars',
                reviewId: '9715',
                super_gen: undefined,
            };
        });

        it('должен сгенерировать урл для десктопа', () => {
            const linkModule = require('./link');
            const url = linkModule(autoRuSusanin, 'review-card', params, desktopOptions);
            expect(url).toEqual('https://auto.ru/review/cars/9715/');
        });

        it('должен сгенерировать урл для мобилки', () => {
            jest.doMock('auto-core/appConfig', () => {
                return { appId: 'af-mobile' };
            });
            const linkModule = require('./link');
            const url = linkModule(mAutoRuSusanin, 'review-card', params, mobileOptions);
            expect(url).toEqual('https://auto.ru/review/cars/9715/');
        });
    });
});

describe('генерация геоурлов', () => {
    let desktopOptions;
    let mobileOptions;
    beforeEach(() => {
        desktopOptions = {
            ...defaultOptions,
        };

        mobileOptions = {
            ...defaultOptions,

        };
    });

    describe('есть гео', () => {
        let params;
        beforeEach(() => {
            desktopOptions.geoAlias = 'moskva';
            desktopOptions.geoIds = [ 213 ];

            mobileOptions.geoAlias = 'moskva';
            mobileOptions.geoIds = [ 213 ];

            params = {
                category: 'cars',
                mark: 'audi',
                model: 'a4',
                section: 'used',
            };
        });

        it('должен сгенерировать урл для десктопа', () => {
            const linkModule = require('./link');
            const url = linkModule(autoRuSusanin, 'listing', params, desktopOptions);
            expect(url).toEqual('https://auto.ru/moskva/cars/audi/a4/used/');
        });

        it('должен сгенерировать урл для десктопа с ЧПУ по цвету', () => {
            const linkModule = require('./link');
            const url = linkModule(
                autoRuSusanin,
                'listing',
                { ...params, color: [ '040001' ] },
                desktopOptions,
            );
            expect(url).toEqual('https://auto.ru/moskva/cars/audi/a4/used/color-chernyj/');
        });

        it('должен сгенерировать урл для мобилки', () => {
            jest.doMock('auto-core/appConfig', () => {
                return { appId: 'af-mobile' };
            });
            const linkModule = require('./link');
            const url = linkModule(mAutoRuSusanin, 'listing', params, mobileOptions);
            expect(url).toEqual('https://auto.ru/moskva/cars/audi/a4/used/');
        });
    });

    describe('нет гео', () => {
        let params;
        beforeEach(() => {
            params = {
                category: 'cars',
                mark: 'audi',
                model: 'a4',
                section: 'used',
            };
        });

        it('должен сгенерировать урл для десктопа', () => {
            const linkModule = require('./link');
            const url = linkModule(autoRuSusanin, 'listing', params, desktopOptions);
            expect(url).toEqual('https://auto.ru/cars/audi/a4/used/');
        });

        it('должен сгенерировать урл для мобилки', () => {
            jest.doMock('auto-core/appConfig', () => {
                return { appId: 'af-mobile' };
            });
            const linkModule = require('./link');
            const url = linkModule(mAutoRuSusanin, 'listing', params, mobileOptions);
            expect(url).toEqual('https://auto.ru/cars/audi/a4/used/');
        });
    });
});

describe('review-listing', () => {
    let desktopOptions;
    let mobileOptions;
    beforeEach(() => {
        desktopOptions = {
            ...defaultOptions,
        };

        mobileOptions = {
            ...defaultOptions,
        };
    });

    const TESTS = [
        {
            routeName: 'reviews-listing-all',
            routeParams: {
                catalog_filter: [],
                parent_category: 'cars',
            },
            result: 'https://auto.ru/reviews/cars/all/',
            resultMobile: 'https://auto.ru/reviews/cars/all/',
        },
        {
            routeName: 'reviews-listing-cars',
            routeParams: {
                mark: 'AUDI',
                catalog_filter: [ { mark: 'AUDI' } ],
                model: undefined,
                parent_category: 'cars',
                super_gen: [],
            },
            result: 'https://auto.ru/reviews/cars/audi/',
            resultMobile: 'https://auto.ru/reviews/cars/audi/',
        },
        {
            routeName: 'reviews-listing-cars',
            routeParams: {
                mark: 'BMW',
                catalog_filter: [ { mark: 'BMW', model: '3ER', generation: '7744658' } ],
                model: '3ER',
                parent_category: 'cars',
                super_gen: [ '7744658' ],
            },
            result: 'https://auto.ru/reviews/cars/bmw/3er/7744658/',
            resultMobile: 'https://auto.ru/reviews/cars/bmw/3er/7744658/',
        },
        {
            routeName: 'reviews-listing-cars',
            routeParams: {
                mark: 'BMW',
                catalog_filter: [
                    { mark: 'BMW', model: '3ER', generation: '7744658' },
                    { mark: 'BMW', model: '3ER', generation: '20548423' },
                ],
                model: '3ER',
                parent_category: 'cars',
                super_gen: [ '7744658', '20548423' ],
            },
            result: 'https://auto.ru/reviews/cars/all/?' +
                'catalog_filter=mark%3DBMW%2Cmodel%3D3ER%2Cgeneration%3D7744658&' +
                'catalog_filter=mark%3DBMW%2Cmodel%3D3ER%2Cgeneration%3D20548423',
            resultMobile: 'https://auto.ru/reviews/cars/all/?' +
                'catalog_filter=mark%3DBMW%2Cmodel%3D3ER%2Cgeneration%3D7744658&' +
                'catalog_filter=mark%3DBMW%2Cmodel%3D3ER%2Cgeneration%3D20548423',
        },
        {
            routeName: 'reviews-listing',
            routeParams: {
                category: 'motorcycle',
                catalog_filter: [],
                parent_category: 'moto',
            },
            result: 'https://auto.ru/reviews/moto/motorcycle/',
            resultMobile: 'https://auto.ru/reviews/moto/motorcycle/',
        },
        {
            routeName: 'reviews-listing',
            routeParams: {
                category: 'motorcycle',
                mark: 'BMW',
                catalog_filter: [ { mark: 'BMW' } ],
                parent_category: 'moto',
            },
            result: 'https://auto.ru/reviews/moto/motorcycle/bmw/',
            resultMobile: 'https://auto.ru/reviews/moto/motorcycle/bmw/',
        },
    ];

    TESTS.forEach((testCase) => {
        describe(`${ testCase.routeName } ${ JSON.stringify(testCase.routeParams) } -> ${ testCase.result }`, () => {
            it('должен сгенерировать урл для десктопа', () => {
                const linkModule = require('./link');
                const url = linkModule(autoRuSusanin, testCase.routeName, testCase.routeParams, desktopOptions);
                expect(url).toEqual(testCase.result);
            });

            it('должен сгенерировать урл для мобилки', () => {
                jest.doMock('auto-core/appConfig', () => {
                    return { appId: 'af-mobile' };
                });
                const linkModule = require('./link');
                const url = linkModule(mAutoRuSusanin, testCase.routeName, testCase.routeParams, mobileOptions);
                expect(url).toEqual(testCase.resultMobile);
            });
        });
    });
});

describe('listing', () => {
    let desktopOptions;
    let mobileOptions;
    beforeEach(() => {
        desktopOptions = {
            ...defaultOptions,
        };

        mobileOptions = {
            ...defaultOptions,
        };
    });

    it('должен сгенерировать урл из парметров catalog_filter', () => {
        const params = {
            category: 'cars',
            section: 'all',
            catalog_filter: [ {
                mark: 'AUDI',
                model: 'A4',
                generation: '7754683',
                configuration: '7754685',
                tech_param: '7765816',
            } ],
        };
        const linkModule = require('./link');
        const url = linkModule(autoRuSusanin, 'listing', params, desktopOptions);
        expect(url).toBe('https://auto.ru/cars/audi/a4/7754683/7754685/7765816/all/');
    });

    it('должен сгенерировать урл из парметров exclude_catalog_filter', () => {
        const params = {
            category: 'cars',
            section: 'all',
            exclude_catalog_filter: [ {
                mark: 'AUDI',
                model: 'A4',
                generation: '7754683',
            } ],
        };
        const linkModule = require('./link');
        const url = linkModule(autoRuSusanin, 'listing', params, desktopOptions);
        expect(url).toBe('https://auto.ru/cars/all/?exclude_catalog_filter=mark%3DAUDI%2Cmodel%3DA4%2Cgeneration%3D7754683');
    });

    it('должен сгенерировать урл из парметров catalog_filter, mobile', () => {
        const params = {
            category: 'cars',
            section: 'all',
            catalog_filter: [ {
                mark: 'AUDI',
                model: 'A4',
                generation: '7754683',
                configuration: '7754685',
                tech_param: '7765816',
            } ],
        };
        jest.doMock('auto-core/appConfig', () => {
            return { appId: 'af-mobile' };
        });
        const linkModule = require('./link');
        const url = linkModule(mAutoRuSusanin, 'listing', params, mobileOptions);
        expect(url).toBe('https://auto.ru/cars/audi/a4/7754683/7754685/7765816/all/');
    });

    it('должен сгенерировать ссылку с фильтрами - цена', () => {
        const params = {
            category: 'cars',
            section: 'new',
            catalog_filter: [
                { mark: 'RENAULT', model: 'DUSTER' },
            ],
            'do': 1000000,
        };
        const linkModule = require('./link');
        const url = linkModule(autoRuSusanin, 'listing', params, { ...defaultOptions });
        expect(url).toBe('https://auto.ru/cars/renault/duster/new/do-1000000/');
    });
});

// такой фильтр может приходить в сохраненных поисках например
it('должен сгенерить урл листинга с удаленным вендором', () => {
    const params = {
        category: 'cars',
        section: 'all',
        catalog_filter: [ { vendor: 'VENDOR15' } ],
    };
    const linkModule = require('./link');
    const url = linkModule(autoRuSusanin, 'listing', params, { ...defaultOptions });
    expect(url).toBe('https://auto.ru/cars/all/?exclude_catalog_filter=vendor%3DVENDOR10');
});

it('должен бросить ошибку, если неправильный routeName', () => {
    const params = {
        category: 'cars',
        section: 'all',
    };
    const routeName = 'incorrectRoute';
    const fn = () => {
        const linkModule = require('./link');
        linkModule(autoRuSusanin, routeName, params, { ...defaultOptions });
    };

    expect(fn).toThrow(`Unknown routeName '${ routeName }'`);
});

it('должен построить ссылку на группу', () => {
    const params = {
        section: 'new',
        category: 'cars',
        mark: 'KIA',
        model: 'CERATO',
        configuration_id: '21383533',
        super_gen: '21383450',
    };
    const routeName = 'card-group';
    const linkModule = require('./link');
    const url = linkModule(autoRuSusanin, routeName, params, { ...defaultOptions });

    expect(url).toBe('https://auto.ru/cars/new/group/kia/cerato/21383450-21383533/');
});

it('должен построить ссылку на группу mobile', () => {
    const params = {
        section: 'new',
        category: 'cars',
        mark: 'KIA',
        model: 'CERATO',
        configuration_id: '21383533',
        super_gen: '21383450',
    };
    const routeName = 'card-group';
    const linkModule = require('./link');
    const url = linkModule(mAutoRuSusanin, routeName, params, { ...defaultOptions });

    expect(url).toBe('https://auto.ru/cars/new/group/kia/cerato/21383450-21383533/');
});

it('должен сгенерировать ссылку на amp', () => {
    const params = {
        category: 'trucks',
        catalog_filter: null,
        moto_category: null,
        section: 'all',
        trucks_category: 'LCV',
        year_from: null,
        year_to: null,
    };
    const linkModule = require('./link');
    const url = linkModule(autoRuSusanin, 'listing-amp', params, { ...defaultOptions });
    expect(url).toBe('https://auto.ru/amp/lcv/all/');
});

it('должен сгенерировать ссылку на amp с фильтрами - цвет', () => {
    const params = {
        category: 'cars',
        section: 'used',
        catalog_filter: [
            { mark: 'AUDI', model: 'A5' },
        ],
        color_sef: 'color-chernyj',
    };
    const linkModule = require('./link');
    const url = linkModule(autoRuSusanin, 'listing-amp', params, { ...defaultOptions });
    expect(url).toBe('https://auto.ru/amp/cars/audi/a5/used/color-chernyj/');
});

it('должен сгенерировать ссылку на amp с фильтрами - цена', () => {
    const params = {
        category: 'cars',
        section: 'used',
        catalog_filter: [
            { mark: 'AUDI', model: 'A5' },
        ],
        'do': 1000000,
    };
    const linkModule = require('./link');
    const url = linkModule(autoRuSusanin, 'listing-amp', params, { ...defaultOptions });
    expect(url).toBe('https://auto.ru/amp/cars/audi/a5/used/do-1000000/');
});

it('должен сгенерировать ссылку на amp с фильтрами - привод', () => {
    const params = {
        category: 'cars',
        section: 'used',
        catalog_filter: [
            { mark: 'AUDI' },
        ],
        drive_sef: 'drive-forward_wheel',
    };
    const linkModule = require('./link');
    const url = linkModule(autoRuSusanin, 'listing-amp', params, { ...defaultOptions });
    expect(url).toBe('https://auto.ru/amp/cars/audi/used/drive-forward_wheel/');
});

it('должен сгенерировать ссылку на amp с фильтрами - двигатель', () => {
    const params = {
        category: 'cars',
        section: 'used',
        catalog_filter: [
            { mark: 'AUDI' },
        ],
        engine_type_sef: 'engine-benzin',
    };
    const linkModule = require('./link');
    const url = linkModule(autoRuSusanin, 'listing-amp', params, { ...defaultOptions });
    expect(url).toBe('https://auto.ru/amp/cars/audi/used/engine-benzin/');
});

it('должен правильно построить ссылку с маркой в мото', () => {
    const params = {
        moto_category: 'SCOOTERS',
        section: 'all',
        category: 'moto',
        catalog_filter: [
            { mark: 'BMW' },
        ],
    };
    const routeName = 'moto-listing';
    const linkModule = require('./link');
    const url = linkModule(autoRuSusanin, routeName, params, { ...defaultOptions });

    expect(url).toBe('https://auto.ru/scooters/bmw/all/');
});

it('должен правильно построить ссылку на страницу новинки года (статья в журнале)', () => {
    const params = {
        article_id: 'new-car-of-the-year',
    };
    const routeName = 'mag-article';
    const linkModule = require('./link');
    const url = linkModule(magAutoRuSusanin, routeName, params, { ...defaultOptions });

    expect(url).toBe('https://auto.ru/new-car-of-the-year/');
});
