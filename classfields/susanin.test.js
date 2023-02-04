const autoRuSusanin = require('./auto.ru/susanin');
const mAutoRuSusanin = require('./m.auto.ru/susanin');

const desktopTestCases = require('./auto.ru/susanin.testcases');
const mobileTestCases = require('./m.auto.ru/susanin.testcases');
const ampTestCases = require('./auto.ru/susanin.amp.testcases');

const TESTS_404 = [
    {
        url: '/cars/used/sale/audi/a4/123-ac4_123/',
        route: undefined,
    },
    {
        url: '/cars/used/sale/123-ac4_123/',
        route: undefined,
    },
    {
        url: '/lcv/used/sale/audi/a4/123-ac4_123/',
        route: undefined,
    },
    {
        url: '/catalog/cars/datsun/mi_do/USED/',
        route: undefined,
    },
    {
        url: '/catalog/cars/toyota/rav_4/2309591/2309592/specifications/2309592__2309594a/',
        route: undefined,
    },
    {
        url: '/stats/cars/audi/a3/20785010/20785079/20785079__20794251a/',
        route: undefined,
    },
];

createProjectSuite('auto.ru', autoRuSusanin, desktopTestCases);
createProjectSuite('m.auto.ru', mAutoRuSusanin, mobileTestCases);
createProjectSuite('auto.ru/amp/', autoRuSusanin, ampTestCases);

function createProjectSuite(suiteName, susanin, tests) {
    describe(suiteName, () => {
        tests.forEach((testCase) => {
            it(`должен распарсить "${ testCase.url }" в ${ testCase.route.routeName } ${ JSON.stringify(testCase.route.routeParams) }`, () => {
                const result = susanin.findFirst(testCase.url);
                if (!result) {
                    throw new Error('Роут не найден!');
                }

                expect({
                    routeName: result[0].getName(),
                    routeParams: result[1],
                }).toEqual(testCase.route);
            });
        });

        TESTS_404.forEach((testCase) => {
            it(`не должен распарсить "${ testCase.url }"`, () => {
                const result = susanin.findFirst(testCase.url);
                expect(result).toBeNull();
            });
        });

        describe('mark/model regexp', () => {
            it('может содержать a-z', () => {
                const result = susanin.findFirst('/cars/audi/used/');

                expect(result[1]).toEqual({
                    category: 'cars',
                    mark: 'audi',
                    section: 'used',
                });
            });

            it('может содержать A-Z', () => {
                const result = susanin.findFirst('/cars/AUDI/used/');

                expect(result[1]).toEqual({
                    category: 'cars',
                    mark: 'AUDI',
                    section: 'used',
                });
            });

            it('может содержать 0-9', () => {
                const result = susanin.findFirst('/cars/audi1/used/');

                expect(result[1]).toEqual({
                    category: 'cars',
                    mark: 'audi1',
                    section: 'used',
                });
            });

            it('может содержать _', () => {
                const result = susanin.findFirst('/cars/mi_do/used/');

                expect(result[1]).toEqual({
                    category: 'cars',
                    mark: 'mi_do',
                    section: 'used',
                });
            });

            it('марка не может содержать -', () => {
                const result = susanin.findFirst('/cars/audi-bmw/used/');

                expect(result).toBeNull();
            });

            it('вендор определяется правильно', () => {
                const result = susanin.findFirst('/cars/vendor-domestic/used/');

                expect(result[1]).toEqual({
                    category: 'cars',
                    mark: 'vendor-domestic',
                    section: 'used',
                });
            });

            it('определяет nameplate, если после модели есть тире', () => {
                const result = susanin.findFirst('/cars/audi/bmw-test/used/');

                expect(result[1]).toEqual({
                    category: 'cars',
                    mark: 'audi',
                    model: 'bmw',
                    nameplate_name: 'test',
                    section: 'used',
                });
            });

            it('nameplate не может содержать тире', () => {
                const result = susanin.findFirst('/cars/audi/bmw-test-test/used/');

                expect(result).toBeNull();
            });

            it('не может других символов', () => {
                const result = susanin.findFirst('/cars/mi:do/used/');

                expect(result).toBeNull();
            });

            if (suiteName === 'auto.ru') {
                it('Должен распарсить auto.ru/lz5XeGt8f/one/two/three/four/', () => {
                    const result = susanin.findFirst('/lz5XeGt8f/one/two/three/four/');
                    expect(result[1]).toEqual({ everything: '/one/two/three/four/' });
                });
                it('Должен распарсить auto.ru/lz5XeGt8fthisiszendebug', () => {
                    const result = susanin.findFirst('/lz5XeGt8fthisiszendebug');
                    expect(result[1]).toEqual({ everything: 'thisiszendebug' });
                });
            }
        });
    });
}
