/* eslint-disable jest/expect-expect */
/* eslint-disable max-len */
const { Request, Response, nextFunction, metroAPIMock } = require('./mocks');

jest.mock('vertis-metro');

const metro = require('vertis-metro');

metro.mockImplementation(metroAPIMock);

const RedirectError = require('realty-core/app/lib/redirect-error');

const seoMiddleware = require('../');

const router = require('realty-router').desktop;
const searchRoute = router.getRouteByName('search');
const streetRoute = router.getRouteByName('street');
const streetsRoute = router.getRouteByName('streets');

const checkRedirect = url => {
    expect(nextFunction).toHaveBeenCalled();

    const error = nextFunction.mock.calls[0][0];

    expect(RedirectError.isRedirectError(error)).toBeTruthy();
    expect(error.data.location).toBe(url);
    expect(error.data.status).toBe(301);
};

const checkSameUrl = () => {
    expect(nextFunction).toHaveBeenCalled();

    const error = nextFunction.mock.calls[0][0];

    expect(error).toEqual(undefined);
    expect(RedirectError.isRedirectError(error)).toBeFalsy();
};

describe('SEO-редиректы', () => {
    beforeEach(() => {
        nextFunction.mockClear();
    });

    describe('метро', () => {
        it('Гео не соответствует метро, корректировка rgid', async() => {
            const originalParams = {
                rgid: '552970',
                type: 'SELL',
                category: 'APARTMENT',
                metroGeoId: '20490'
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe('/omsk/kupit/kvartira/metro-park-kultury/');

            const request = new Request(currentUrl);
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/kvartira/metro-park-kultury/');
        });

        it('Гео соответствует метро', async() => {
            const originalParams = {
                rgid: '579098',
                type: 'SELL',
                category: 'APARTMENT',
                metroGeoId: '102068'
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe('/novosibirsk/kupit/kvartira/metro-gagarinskaya-1/');

            const request = new Request(currentUrl);
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkSameUrl();
        });
    });

    describe('улица', () => {
        it('Гео не соответствует улице, корректировка rgid', async() => {
            const originalParams = {
                rgid: '579098',
                type: 'SELL',
                category: 'APARTMENT',
                streetId: '57945',
                streetName: 'Корабельная улица'
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe('/novosibirsk/kupit/kvartira/st-korabelnaya-ulica-57945/');

            const request = new Request(currentUrl);
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva/kupit/kvartira/st-korabelnaya-ulica-57945/');
        });

        it('Гео соответствует улице', async() => {
            const originalParams = {
                rgid: '579098',
                type: 'SELL',
                category: 'APARTMENT',
                streetId: '15390',
                streetName: 'улица Галущака'
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe('/novosibirsk/kupit/kvartira/st-ulica-galushchaka-15390/');

            const request = new Request(currentUrl);
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkSameUrl();
        });

        it('Некорректное название улицы, корректировка streetName', async() => {
            const originalParams = {
                rgid: '579098',
                type: 'SELL',
                category: 'APARTMENT',
                streetId: '57945',
                streetName: 'Неправильная улица'
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe('/novosibirsk/kupit/kvartira/st-nepravilnaya-ulica-57945/');

            const request = new Request(currentUrl);
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva/kupit/kvartira/st-korabelnaya-ulica-57945/');
        });

        it('Отсутствуе название улицы, корректировка streetName', async() => {
            const originalParams = {
                rgid: '579098',
                type: 'SELL',
                category: 'APARTMENT',
                streetId: '57945',
                streetName: ''
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe('/novosibirsk/kupit/kvartira/?streetIdCode=57945');

            const request = new Request(currentUrl);
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva/kupit/kvartira/st-korabelnaya-ulica-57945/');
        });

        it('Не редиректит на урл с параметром streetName, если pageName = street ', async() => {
            const originalParams = {
                rgid: '587795',
                streetId: '57945'
            };

            const currentUrl = streetRoute.build(originalParams);

            expect(currentUrl).toBe('/moskva/street/57945/');

            const request = new Request(currentUrl);
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkSameUrl();
        });
    });

    describe('район', () => {
        it('Гео не соответствует району, корректировка rgid', async() => {
            const originalParams = {
                rgid: '587795',
                type: 'SELL',
                category: 'APARTMENT',
                subLocality: '264684'
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe('/moskva/kupit/kvartira/?subLocality=264684');

            const request = new Request(currentUrl);
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/novosibirsk/kupit/kvartira/?subLocality=264684');
        });

        describe('Гео в районе внутри Питера + ЛО или Москвы + МО должно редиретить на subjectFederation', () => {
            it('для района в Питере', async() => {
                const originalParams = {
                    rgid: '587795',
                    type: 'SELL',
                    category: 'APARTMENT',
                    subLocalityName: 'Василеостровский',
                    subLocalityType: 'CITY_DISTRICT',
                    subLocality: '417970'
                };

                const currentUrl = searchRoute.build(originalParams);

                expect(currentUrl).toBe('/moskva/kupit/kvartira/dist-vasileostrovskij-417970/');

                const request = new Request(currentUrl);
                const response = new Response();

                await seoMiddleware()(request, response, nextFunction);

                checkRedirect('/sankt-peterburg_i_leningradskaya_oblast/kupit/kvartira/dist-vasileostrovskij-417970/');
            });

            it('для района в Москве', async() => {
                const originalParams = {
                    rgid: '741965',
                    type: 'SELL',
                    category: 'APARTMENT',
                    subLocalityName: 'Перово',
                    subLocalityType: 'CITY_DISTRICT',
                    subLocality: '193363'
                };

                const currentUrl = searchRoute.build(originalParams);

                expect(currentUrl).toBe('/sankt-peterburg_i_leningradskaya_oblast/kupit/kvartira/dist-perovo-193363/');

                const request = new Request(currentUrl);
                const response = new Response();

                await seoMiddleware()(request, response, nextFunction);

                checkRedirect('/moskva_i_moskovskaya_oblast/kupit/kvartira/dist-perovo-193363/');
            });
        });

        describe('редиректы на city при выбранном subLocality', () => {
            it('SubLocality не выбран', async() => {
                const request = new Request('/moskva/kupit/kvartira/');
                const response = new Response();

                await seoMiddleware()(request, response, nextFunction);

                checkSameUrl();
            });

            it('SubLocality выбран - CITY_DISTRICT', async() => {
                const request = new Request('/omskaya_oblast/kupit/kvartira/dist-armejskij-179001/');
                const response = new Response();

                await seoMiddleware()(request, response, nextFunction);

                checkRedirect('/omsk/kupit/kvartira/dist-armejskij-179001/');
            });

            it('SubLocality выбран - NOT_ADMINISTRATIVE_DISTRICT', async() => {
                const request = new Request('/novosibirskay_oblast/kupit/kvartira/?subLocality=264684');
                const response = new Response();

                await seoMiddleware()(request, response, nextFunction);

                checkRedirect('/novosibirsk/kupit/kvartira/?subLocality=264684');
            });

            it('Rgid уже subjectFederation для выбранного subLocality', async() => {
                const request = new Request('/moskva_i_moskovskaya_oblast/kupit/kvartira/?subLocality=183320');
                const response = new Response();

                await seoMiddleware()(request, response, nextFunction);

                checkSameUrl();
            });
        });
    });

    describe('ЖД станция', () => {
        it('редирект на корректный гео по Ж/Д', async() => {
            const request = new Request('/moskva/railway/61517/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/railway/61517/');
        });
    });

    describe('административный округ', () => {
        it('Гео не соответствует административному округу, корректировка rgid', async() => {
            const originalParams = {
                rgid: '552970',
                type: 'SELL',
                category: 'APARTMENT',
                subLocality: [ '17367991', '17367987', '17367990', '17367989', '17367992' ]
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe('/omsk/kupit/kvartira/ao-zelenogradskij/');

            const request = new Request(currentUrl);
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/kvartira/ao-zelenogradskij/');
        });

        it('Гео соответствует административному округу', async() => {
            const originalParams = {
                rgid: '741964',
                type: 'SELL',
                category: 'APARTMENT',
                subLocality: [ '17367991', '17367987', '17367990', '17367989', '17367992' ]
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe('/moskva_i_moskovskaya_oblast/kupit/kvartira/ao-zelenogradskij/');

            const request = new Request(currentUrl);
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkSameUrl();
        });
    });

    describe('малонаселенное гео', () => {
        it('Малонаселенное гео, корректировка rgid на столицу региона', async() => {
            const originalParams = {
                rgid: '108285',
                type: 'SELL',
                category: 'APARTMENT'
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe('/oktyabrskiy_2-y/kupit/kvartira/');

            const request = new Request(currentUrl);
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/irkutskaya_oblast/kupit/kvartira/?rgid=108285');
        });

        it('Населенное гео', async() => {
            const originalParams = {
                rgid: '5',
                type: 'SELL',
                category: 'APARTMENT'
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe('/abakan/kupit/kvartira/');

            const request = new Request(currentUrl);
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkSameUrl();
        });
    });

    describe('один потомок у округа/муниципального образования', () => {
        it('Один потомок у округа/мун.образования, корректировка rgid на потомка', async() => {
            const originalParams = {
                rgid: '1943',
                type: 'SELL',
                category: 'APARTMENT'
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe('/okrug_habarovsk/kupit/kvartira/');

            const request = new Request(currentUrl);
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/habarovsk/kupit/kvartira/');
        });

        it('Несколько потомков у округа/мун.образования', async() => {
            const originalParams = {
                rgid: '1931',
                type: 'SELL',
                category: 'APARTMENT'
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe('/okrug_sayanogorsk/kupit/kvartira/');

            const request = new Request(currentUrl);
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkSameUrl();
        });
    });

    describe('гео без алиаса и дефолтная Москва в алиасе', () => {
        it('Гео без алиаса и дефолтный алиас moskva, корректировка rgid на столицу региона', async() => {
            const originalParams = {
                rgid: '341803',
                type: 'SELL',
                category: 'APARTMENT'
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe('/moskva/kupit/kvartira/?rgid=341803');

            const request = new Request(currentUrl);
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/kareliya/kupit/kvartira/?rgid=341803');
        });

        it('Гео без алиаса и алиас столицы региона', async() => {
            const originalParams = {
                rgid: '25558',
                rgidCode: 'ryazan',
                type: 'SELL',
                category: 'APARTMENT'
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe('/ryazan/kupit/kvartira/?rgid=25558');

            const request = new Request(currentUrl);
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkSameUrl();
        });

        it('Гео без алиаса, но потомок Москвы и дефолтный алиас moskva', async() => {
            const originalParams = {
                rgid: '17398782',
                type: 'SELL',
                category: 'APARTMENT'
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe('/moskva/kupit/kvartira/?rgid=17398782');

            const request = new Request(currentUrl);
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkSameUrl();
        });
    });

    describe('приоритет гео уточнений', () => {
        it('Приоритет гео-уточнений, район важнее метро', async() => {
            const originalParams = {
                rgid: '549570',
                type: 'SELL',
                category: 'APARTMENT',
                metroGeoId: '102068',
                subLocality: 179001
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe('/ryazan/kupit/kvartira/?metroGeoId=102068&subLocality=179001');

            const request = new Request('/ryazan/kupit/kvartira/metro-gagarinskaya-1/?subLocality=179001');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/omsk/kupit/kvartira/dist-armejskij-179001/');
        });

        it('Приоритет гео-уточнений, административный округ важнее метро', async() => {
            const originalParams = {
                rgid: '741964',
                type: 'SELL',
                category: 'APARTMENT',
                metroGeoId: '102068',
                subLocality: [ '17367991', '17367987', '17367990', '17367989', '17367992' ]
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe('/moskva_i_moskovskaya_oblast/kupit/kvartira/?metroGeoId=102068&' +
            'subLocality=17367991&subLocality=17367987&subLocality=17367990&subLocality=17367989&subLocality=17367992');

            const request = new Request('/moskva_i_moskovskaya_oblast/kupit/kvartira/metro-gagarinskaya-1/' +
            '?subLocality=17367991&subLocality=17367987&subLocality=17367990&subLocality=17367989&subLocality=17367992');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/kvartira/ao-zelenogradskij/');
        });

        it('Метро пересекаем с улицей. Если одна улица, строим ЧПУ для метро, streetId оставляем квери-параметром.', async() => {
            const originalParams = {
                rgid: '5',
                type: 'SELL',
                category: 'APARTMENT',
                metroGeoId: '102068',
                streetId: '15390',
                streetName: 'улица Галущака'
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe('/abakan/kupit/kvartira/metro-gagarinskaya-1/?streetId=15390');

            const request = new Request('/abakan/kupit/kvartira/metro-gagarinskaya-1/?streetId=15390');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/novosibirsk/kupit/kvartira/metro-gagarinskaya-1/?streetId=15390');
        });

        it('Метро пересекаем с улицей. Если несколько улиц, metroGeoId и streetId оставляем квери-параметрами', async() => {
            const originalParams = {
                rgid: '5',
                type: 'SELL',
                category: 'APARTMENT',
                metroGeoId: '102068',
                streetId: [ '15390', '74986' ]
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe('/abakan/kupit/kvartira/?metroGeoId=102068&streetId=15390&streetId=74986');

            const request = new Request('/abakan/kupit/kvartira/?metroGeoId=102068&streetId=15390&streetId=74986');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/novosibirsk/kupit/kvartira/?metroGeoId=102068&streetId=15390&streetId=74986');
        });

        it('Приоритет гео-уточнений, район важнее улицы', async() => {
            const originalParams = {
                rgid: '552970',
                type: 'SELL',
                category: 'APARTMENT',
                subLocality: '264684',
                streetId: '57945'
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe('/omsk/kupit/kvartira/?subLocality=264684&streetId=57945');

            const request = new Request('/omsk/kupit/kvartira/?subLocality=264684&streetId=57945');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/novosibirsk/kupit/kvartira/?subLocality=264684');
        });

        it('Приоритет гео-уточнений, административный округ важнее улицы', async() => {
            const originalParams = {
                rgid: '741964',
                type: 'SELL',
                category: 'APARTMENT',
                subLocality: [ '17367991', '17367987', '17367990', '17367989', '17367992' ],
                streetId: '57945'
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe('/moskva_i_moskovskaya_oblast/kupit/kvartira/?subLocality=17367991&' +
            'subLocality=17367987&subLocality=17367990&subLocality=17367989&subLocality=17367992&streetId=57945');

            const request = new Request('/moskva_i_moskovskaya_oblast/kupit/kvartira/?subLocality=17367991&' +
            'subLocality=17367987&subLocality=17367990&subLocality=17367989&subLocality=17367992&streetId=57945');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/kvartira/ao-zelenogradskij/');
        });

        it('Приоритет гео-уточнений, район важнее метро и важнее улицы', async() => {
            const originalParams = {
                rgid: '549570',
                type: 'SELL',
                category: 'APARTMENT',
                metroGeoId: '102068',
                subLocality: '179001',
                streetId: '57945'
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe('/ryazan/kupit/kvartira/?metroGeoId=102068&subLocality=179001&streetId=57945');

            const request = new Request('/ryazan/kupit/kvartira/metro-gagarinskaya-1/?subLocality=179001&streetId=57945');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/omsk/kupit/kvartira/dist-armejskij-179001/');
        });

        it('Приоритет гео-уточнений, административный округ важнее метро и важнее улицы', async() => {
            const originalParams = {
                rgid: '741964',
                type: 'SELL',
                category: 'APARTMENT',
                subLocality: [ '17367991', '17367987', '17367990', '17367989', '17367992' ],
                metroGeoId: '102068',
                streetId: '57945'
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe(
                '/moskva_i_moskovskaya_oblast/kupit/kvartira/?subLocality=17367991&subLocality=17367987&' +
            'subLocality=17367990&subLocality=17367989&subLocality=17367992&metroGeoId=102068&streetId=57945');

            const request = new Request(
                '/moskva_i_moskovskaya_oblast/kupit/kvartira/?subLocality=17367991&subLocality=17367987&' +
            'subLocality=17367990&subLocality=17367989&subLocality=17367992&metroGeoId=102068&streetId=57945');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/kvartira/ao-zelenogradskij/');
        });

        it('Приоритет гео-уточнений, метро важнее шоссе', async() => {
            const request = new Request('/ryazan/kupit/kvartira/metro-gagarinskaya-1/?direction=11');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/novosibirsk/kupit/kvartira/metro-gagarinskaya-1/');
        });
    });

    describe('с гет-параметров на чпу', () => {
        it('Фильтр с ЧПУ', async() => {
            const request = new Request('/moskva/kupit/kvartira/?balcony=BALCONY');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva/kupit/kvartira/s-balkonom/');
        });

        it('Метро с ЧПУ', async() => {
            const request = new Request('/moskva/kupit/kvartira/?metroGeoId=20490');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/kvartira/metro-park-kultury/');
        });

        it('Административный округ с ЧПУ', async() => {
            const request = new Request(
                '/moskva/kupit/kvartira/?subLocality=17367991&subLocality=17367987' +
            '&subLocality=17367990&subLocality=17367989&subLocality=17367992');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/kvartira/ao-zelenogradskij/');
        });
    });

    describe('жк с другими гео-уточнениями в чпу', () => {
        it('ЖК и МЕТРО в ЧПУ', async() => {
            const request = new Request(
                '/moskva_i_moskovskaya_oblast/kupit/kvartira' +
            '/zhk-salarevo-park-375274/metro-novoslobodskaya/'
            );
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/kvartira/metro-novoslobodskaya/');
        });

        it('Листинг только ЖК в ЧПУ', async() => {
            const request = new Request('/moskva_i_moskovskaya_oblast/kupit/kvartira/zhk-salarevo-park-375274/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkSameUrl();
        });

        it('Листинг ЖК без названия в ЧПУ', async() => {
            const request = new Request('/moskva_i_moskovskaya_oblast/kupit/kvartira/zhk--375274/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/kvartira/zhk-salarevo-park-375274/');
        });

        it('Листинг ЖК с неверным названием в ЧПУ', async() => {
            const request = new Request('/moskva_i_moskovskaya_oblast/kupit/kvartira/zhk-keka-375274/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/kvartira/zhk-salarevo-park-375274/');
        });

        it('Листинг ЖК с неверным ГЕО в ЧПУ', async() => {
            const request = new Request('/omsk/kupit/kvartira/zhk-salarevo-park-375274/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/kvartira/zhk-salarevo-park-375274/');
        });

        it('Карточка ЖК с неверным названием в ЧПУ', async() => {
            const request = new Request('/moskva/kupit/novostrojka/keka-375274/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/novostrojka/salarevo-park-375274/');
        });

        it('Карточка ЖК с неверным ГЕО в ЧПУ', async() => {
            const request = new Request('/omsk/kupit/novostrojka/salarevo-park-375274/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/novostrojka/salarevo-park-375274/');
        });

        it('Карточка ЖК с populatedRgid внутри МО или ЛО', async() => {
            const request = new Request('/lyubercy/kupit/novostrojka/lyubercy-1839196/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/novostrojka/lyubercy-1839196/');
        });

        it('rgid в листинге с типом CITY_DISTRICT ', async() => {
            const originalParams = {
                rgid: 17367991,
                rgidCode: 'moskva_i_moskovskaya_oblast',
                type: 'RENT',
                category: 'APARTMENT'
            };

            const currentUrl = searchRoute.build(originalParams);

            expect(currentUrl).toBe('/moskva_i_moskovskaya_oblast/snyat/kvartira/?rgid=17367991');

            const request = new Request(currentUrl);
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/snyat/kvartira/dist-staroe-kryukovo-17367991/?rgid=741964');
        });
    });

    describe('шоссе только для МСК + МО', () => {
        it('Шоссе выбрано для МО', async() => {
            const request = new Request('/moskovskaya_oblast/kupit/kvartira/shosse-kievskoe/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/kvartira/shosse-kievskoe/');
        });

        it('Шоссе выбрано для МСК+МО', async() => {
            const request = new Request('/moskva_i_moskovskaya_oblast/kupit/kvartira/shosse-kievskoe/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkSameUrl();
        });

        it('Невалидное имя застройщика', async() => {
            const request = new Request('/moskva_i_moskovskaya_oblast/kupit/novostrojka/z-keka-268784/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/novostrojka/z-mr-group-268784/');
        });
    });

    describe('редирект на suggestRgid при его наличии', () => {
        it('урл с suggestRgid', async() => {
            const request = new Request('/chukotskiy_ao/agentstva/?suggestRgid=741964');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/agentstva/');
        });
    });

    describe('редирект на валидное гео для застройщика на листинге новостроек', () => {
        it('гео с типом SUBJECT_FEDERATION в котором есть застройщик', async() => {
            const request = new Request('/moskva_i_moskovskaya_oblast/kupit/novostrojka/z-mr-group-268784/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkSameUrl();
        });

        it('гео внутри SUBJECT_FEDERATION в котором есть застройщик', async() => {
            const request = new Request('/moskva/kupit/novostrojka/z-mr-group-268784/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/novostrojka/z-mr-group-268784/');
        });

        it('гео в котором нет застройщика', async() => {
            const request = new Request('/omsk/kupit/novostrojka/z-mr-group-268784/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/omsk/kupit/novostrojka/');
        });
    });

    describe('редирект на гео на странице застройщика', () => {
        it('редирект на SUBJECT_FEDERATON на странице застройщикав том же гео', async() => {
            const request = new Request('/moskva/zastroyschik/mr-group-268784/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/zastroyschik/mr-group-268784/');
        });

        it('редирект на SUBJECT_FEDERATON на странице застройщикав в другом гео', async() => {
            const request = new Request('/omsk/zastroyschik/mr-group-268784/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/zastroyschik/mr-group-268784/');
        });
    });

    describe('редирект в поиске агенств', () => {
        it('редирект на subjectFederationRgid в ProfileSearch', async() => {
            const request = new Request('/moskva/agentstva/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/agentstva/?rgid=587795');
        });

        it('редирект на subjectFederationRgid в ProfileSearch отсутствует если нет subjectFederationRgid', async() => {
            const request = new Request('/moskva_i_moskovskaya_oblast/agentstva/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkSameUrl();
        });
    });

    describe('редиректы в чпу подомам (UnifiedAddress)', () => {
        it('редирект на ЧПУ с unifiedAddress', async() => {
            const url = '/moskva/kupit/kvartira/?unifiedAddress=Россия%2C%20Москва%2C%20улица%20Арбат%2C%2024';
            const request = new Request(url);
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva/kupit/kvartira/st-ulica-arbat-62613/dom-24-8017417998175267261/');
        });

        it('не редиректит на ЧПУ с unifiedAddress, если их несколько', async() => {
            const url = '/moskva/kupit/kvartira/?unifiedAddress=Россия%2C%20Москва%2C%20улица%20Арбат%2C%2024&' +
            'unifiedAddress=Россия%2C%20Москва%2C%20улица%20Цюрупы%2C%2020к1';
            const request = new Request(url);
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkSameUrl();
        });
    });

    describe('редиректы в чпу по домам (houseNumber)', () => {
        it('редирект на ЧПУ улица + дом', async() => {
            const request = new Request('/moskva/kupit/kvartira/?streetId=62613&buildingIds=8017417998175267261');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva/kupit/kvartira/st-ulica-arbat-62613/dom-24-8017417998175267261/');
        });

        it('если несколько домов, редиректит на ЧПУ с улицей, но не строит ЧПУ с домом', async() => {
        // eslint-disable-next-line max-len
            const url = '/moskva/kupit/kvartira/?streetId=62613&buildingIds=8017417998175267261&buildingIds=8017417998175267262';
            const request = new Request(url);
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva/kupit/kvartira/st-ulica-arbat-62613/' +
            '?buildingIds=8017417998175267261&buildingIds=8017417998175267262');
        });

        it('редирект на ЧПУ улица если невалидный айди дома', async() => {
            const request = new Request('/moskva/kupit/kvartira/st-ulica-arbat-62613/dom-24-0000/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva/kupit/kvartira/st-ulica-arbat-62613/');
        });
    });

    describe('невалидные айди', () => {
        it('редирект на корректный URL по Ж/Д', async() => {
            const request = new Request('/moskva/kupit/kvartira/railway-61517/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/kvartira/railway-proletarskaya-61517/');
        });

        it('Несуществующий айди района', async() => {
            const request = new Request('/moskva/snyat/kvartira/dist-tverskoj-1234567890/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva/snyat/kvartira/');
        });

        it('Невалидный айди района', async() => {
            const request = new Request('/moskva/snyat/kvartira/dist-tverskoj-qwerty/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva/snyat/kvartira/');
        });

        it('Несуществующий айди Ж/Д', async() => {
            const request = new Request('/moskva_i_moskovskaya_oblast/kupit/kvartira/railway-malinovka-1234567890/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/kvartira/');
        });

        it('Невалидный айди Ж/Д', async() => {
            const request = new Request('/moskva_i_moskovskaya_oblast/kupit/kvartira/railway-malinovka-qwerty/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/kvartira/');
        });

        it('Несуществующий айди улицы', async() => {
            const request = new Request('/moskva_i_moskovskaya_oblast/kupit/kvartira/st-kostromskaya-ulica-1234567890/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/kvartira/');
        });

        it('Невалидный айди улицы', async() => {
            const request = new Request('/moskva_i_moskovskaya_oblast/kupit/kvartira/st-kostromskaya-ulica-qwerty/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/kvartira/');
        });

        it('Несуществующий айди ЖК', async() => {
            const request = new Request('/moskva_i_moskovskaya_oblast/kupit/kvartira/zhk-salarevo-park-1234567890/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/kvartira/');
        });

        it('Невалидный айди ЖК', async() => {
            const request = new Request('/moskva_i_moskovskaya_oblast/kupit/kvartira/zhk-salarevo-park-tutu/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/kvartira/');
        });

        it('Несуществующий административный округ', async() => {
            const request = new Request('/moskva_i_moskovskaya_oblast/kupit/kvartira/ao-tutu/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/kupit/kvartira/');
        });

        it('Eсли pageName = metro-stations и невалидный айди метро - редирект на metro-stations', async() => {
            const request = new Request('/moskva_i_moskovskaya_oblast/metro-station/0000/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/metro-stations/');
        });

        it('Eсли pageName = street и невалидный айди улицы - редирект на streets', async() => {
            const request = new Request('/moskva/street/12345678/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva/streets/');
        });

        it('Eсли pageName = railway и невалидный айди ЖД - редирект на railways', async() => {
            const request = new Request('/moskva/railway/12345678/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva/railways/');
        });
    });

    describe('редирет на деталки агентсва', () => {
        it('редирект на subjectFederationRgid если по ргиду есть выдача', async() => {
            const request = new Request('/moskva/agentstva/tortik-4077389352/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/agentstva/tortik-4077389352/');
        });

        it('редирект на место регистрации если по ргиду нет выдачи', async() => {
            const request = new Request('/ryazan/agentstva/sol-4045447503/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/novosibirskaya_oblast/agentstva/sol-4045447503/');
        });
    });

    describe('редирект со старого урла', () => {
        it('редирект #1 с /zhk-premium/ на /zhk-premium-elitnye/', async() => {
            const request = new Request('/moskva/kupit/novostrojka/zhk-premium/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva/kupit/novostrojka/zhk-premium-elitnye/');
        });

        it('редирект #2 с /zhk-premium/ на /zhk-premium-elitnye/', async() => {
            const request = new Request('/moskva/kupit/novostrojka/zhk-premium-i-s-parkom/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva/kupit/novostrojka/zhk-premium-elitnye-i-s-parkom/');
        });

        it('редирект #3 с /zhk-premium/ на /zhk-premium-elitnye/', async() => {
            const request = new Request('/moskva/kupit/novostrojka/predchistovya-otdelka-i-zhk-premium/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva/kupit/novostrojka/predchistovya-otdelka-i-zhk-premium-elitnye/');
        });
    });

    describe('редирект с протухшего оффера', () => {
        it('Офер существует', async() => {
            const request = new Request('/offer/76667772818213/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkSameUrl();
        });

        it('Офер протух', async() => {
            const request = new Request('/offer/23238478923742/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/sankt-peterburg/kupit/kvartira/tryohkomnatnaya/');
        });
    });

    describe('Редиректы на листинге улиц', () => {
        it('Редирект с чпу малонаселлоного гео', async() => {
            const originalParams = {
                rgid: 328386
            };

            const currentUrl = streetsRoute.build(originalParams);

            expect(currentUrl).toBe('/sverdlovskaya_oblast_zarechnyy/streets/');

            const request = new Request(currentUrl);
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/sverdlovskaya_oblast/streets/?rgid=328386');
        });

        it('Не должно быть редиректа с населенного гео', async() => {
            const originalParams = {
                rgid: 579098
            };

            const currentUrl = streetsRoute.build(originalParams);

            expect(currentUrl).toBe('/novosibirsk/streets/');

            const request = new Request(currentUrl);
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkSameUrl();
        });
    });

    describe('редиректы с неиспользуемых чпу', () => {
        beforeEach(() => {
            nextFunction.mockClear();
        });

        it('редирект с /s-evroplanirovkoy/ на /evroplanirovka/', async() => {
            const request = new Request('/moskva/kupit/s-evroplanirovkoy/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva/kupit/evroplanirovka/');
        });

        it('редирект с s-evroplanirovkoy на evroplanirovka с двойным фильтром', async() => {
            const request = new Request('/moskva/kupit/s-balkonom-i-s-evroplanirovkoy/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva/kupit/s-balkonom-i-evroplanirovka/');
        });

        it('редирект с s-evroplanirovkoy на evroplanirovka с двойным фильтром (второй фильтр)', async() => {
            const request = new Request('/moskva/kupit/s-evroplanirovkoy-i-s-balkonom/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva/kupit/evroplanirovka-i-s-balkonom/');
        });
    });

    describe('редиректы с разводящих по станциям метро', () => {
        beforeEach(() => {
            nextFunction.mockClear();
        });

        it('редирект с Москвы на МО', async() => {
            const request = new Request('/moskva/metro-stations/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/metro-stations/');
        });

        it('редирект с СПБ на ЛО', async() => {
            const request = new Request('/sankt-peterburg/metro-stations/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/sankt-peterburg_i_leningradskaya_oblast/metro-stations/');
        });

        it('редирект с гео без метро на Главную', async() => {
            const request = new Request('/omsk/metro-stations/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/omsk/');
        });

        it('без редирект с гео где есть метро метро', async() => {
            const request = new Request('/novosibirsk/metro-stations/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkSameUrl();
        });
    });

    describe('редиректы для district', () => {
        it('редирект ргида на parent', async() => {
            const request = new Request('/moskva/district/2318/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/district/2318/');
        });

        it('без редиректа', async() => {
            const request = new Request('/moskva_i_moskovskaya_oblast/district/2318/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkSameUrl();
        });

        it('невалдный districtId', async() => {
            const request = new Request('/moskva_i_moskovskaya_oblast/district/100500/');
            const response = new Response();

            await seoMiddleware()(request, response, nextFunction);

            checkRedirect('/moskva_i_moskovskaya_oblast/districts/');
        });
    });
});

describe('SEO-редиректы для городских округов', () => {
    beforeEach(() => {
        nextFunction.mockClear();
    });

    it('редирект c одним малонаселенным потомком', async() => {
        const request = new Request('/abaza/');
        const response = new Response();

        await seoMiddleware()(request, response, nextFunction);

        checkRedirect('/hakasiya/?rgid=17376132');
    });
});

describe('SEO-редиректы с кривыми RGID', () => {
    beforeEach(() => {
        nextFunction.mockClear();
    });

    it('редирект c несуществующего RGID', async() => {
        const request = new Request('/omsk/snyat/kvartira/?areaMin=10&balcony=BALCONY&rgid=100500');
        const response = new Response();

        await seoMiddleware()(request, response, nextFunction);

        checkRedirect('/omsk/snyat/kvartira/?areaMin=10&balcony=BALCONY');
    });

    it('редирект с RGID от района города', async() => {
        const request = new Request('/sankt-peterburg_i_leningradskaya_oblast/?rgid=417972');
        const response = new Response();

        await seoMiddleware()(request, response, nextFunction);

        checkRedirect('/sankt-peterburg_i_leningradskaya_oblast/');
    });
});
