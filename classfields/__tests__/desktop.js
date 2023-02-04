/* eslint-disable max-len */

const router = require('../desktop');

describe('index', function() {
    const route = router.getRouteByName('index');

    describe('default params', function() {
        it('should build a semantic url with default params', function() {
            expect(route.build()).toBe('/');
        });

        it('should get default params from root semantic url', function() {
            expect(route.match('/')).toEqual({});
        });
    });

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                rgid: '587795'
            })).toBe('/moskva/');
            expect(route.build({
                rgid: '417899'
            })).toBe('/sankt-peterburg/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/moskva/')).toEqual({
                rgid: 587795
            });
            expect(route.match('/sankt-peterburg/')).toEqual({
                rgid: 417899
            });
        });
    });
});

describe('newbuilding', function() {
    const route = router.getRouteByName('newbuilding');

    describe('default params', function() {
        it('should build a semantic url with default params', function() {
            expect(route.build({
                siteName: 'название-777'
            })).toBe('/moskva/kupit/novostrojka/nazvanie-777/');
        });
        it('should build a semantic url with default params (z- case)', function() {
            expect(route.build({
                siteName: 'z-town-777'
            })).toBe('/moskva/kupit/novostrojka/ztown-777/');
        });
        it('should get default params from root semantic url', function() {
            expect(route.match('/moskva/kupit/novostrojka/nazvanie-777/')).toEqual({
                rgid: 587795,
                typeCode: 'kupit',
                siteName: 'nazvanie-777',
                id: '777'
            });
        });
    });

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                siteName: 'название-777',
                rgid: '587795'
            })).toBe('/moskva/kupit/novostrojka/nazvanie-777/');

            expect(route.build({
                siteName: 'название-777',
                rgid: '417899'
            })).toBe('/sankt-peterburg/kupit/novostrojka/nazvanie-777/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/novostrojka/nazvanie-777/')).toEqual({
                rgid: 587795,
                typeCode: 'kupit',
                siteName: 'nazvanie-777',
                id: '777'
            });
            expect(route.match('/sankt-peterburg/kupit/novostrojka/nazvanie-777/')).toEqual({
                rgid: 417899,
                typeCode: 'kupit',
                siteName: 'nazvanie-777',
                id: '777'
            });
        });
    });
});

describe('newbuilding-mortgage', function() {
    const route = router.getRouteByName('newbuilding-mortgage');

    describe('default params', function() {
        it('should build a semantic url with default params', function() {
            expect(route.build({
                siteName: 'название-777'
            })).toBe('/moskva/kupit/novostrojka/nazvanie-777/ipoteka/');
        });
        it('should get default params from root semantic url', function() {
            expect(route.match('/moskva/kupit/novostrojka/nazvanie-777/ipoteka/')).toEqual({
                rgid: 587795,
                typeCode: 'kupit',
                siteName: 'nazvanie-777',
                id: '777'
            });
        });
    });

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                siteName: 'название-777',
                rgid: '587795'
            })).toBe('/moskva/kupit/novostrojka/nazvanie-777/ipoteka/');

            expect(route.build({
                siteName: 'название-777',
                rgid: '417899'
            })).toBe('/sankt-peterburg/kupit/novostrojka/nazvanie-777/ipoteka/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/novostrojka/nazvanie-777/ipoteka/')).toEqual({
                rgid: 587795,
                typeCode: 'kupit',
                siteName: 'nazvanie-777',
                id: '777'
            });
            expect(route.match('/sankt-peterburg/kupit/novostrojka/nazvanie-777/ipoteka/')).toEqual({
                rgid: 417899,
                typeCode: 'kupit',
                siteName: 'nazvanie-777',
                id: '777'
            });
        });
    });
});

describe('newbuilding-plans', function() {
    const route = router.getRouteByName('newbuilding-plans');

    describe('default params', function() {
        it('should build a semantic url with default params', function() {
            expect(route.build({
                siteName: 'название-777'
            })).toBe('/moskva/kupit/novostrojka/nazvanie-777/planirovki/');
        });
        it('should get default params from root semantic url', function() {
            expect(route.match('/moskva/kupit/novostrojka/nazvanie-777/planirovki/')).toEqual({
                rgid: 587795,
                typeCode: 'kupit',
                siteName: 'nazvanie-777',
                id: '777'
            });
        });
    });

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                siteName: 'название-777',
                rgid: '587795'
            })).toBe('/moskva/kupit/novostrojka/nazvanie-777/planirovki/');

            expect(route.build({
                siteName: 'название-777',
                rgid: '417899'
            })).toBe('/sankt-peterburg/kupit/novostrojka/nazvanie-777/planirovki/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/novostrojka/nazvanie-777/planirovki/')).toEqual({
                rgid: 587795,
                typeCode: 'kupit',
                siteName: 'nazvanie-777',
                id: '777'
            });
            expect(route.match('/sankt-peterburg/kupit/novostrojka/nazvanie-777/planirovki/')).toEqual({
                rgid: 417899,
                typeCode: 'kupit',
                siteName: 'nazvanie-777',
                id: '777'
            });
        });
    });
});

describe('search', function() {
    const route = router.getRouteByName('search');

    describe('default params', function() {
        it('should build a semantic url with default params', function() {
            expect(route.build()).toBe('/moskva/kupit/kvartira/');
        });

        it('should get default params from root semantic url', function() {
            expect(route.match('/moskva/kupit/kvartira/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL'
            });
        });
    });

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                rgid: 587795
            })).toBe('/moskva/kupit/kvartira/');

            expect(route.build({
                rgid: 417899
            })).toBe('/sankt-peterburg/kupit/kvartira/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/kvartira/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL'
            });
            expect(route.match('/sankt-peterburg/kupit/kvartira/')).toEqual({
                category: 'APARTMENT',
                rgid: 417899,
                type: 'SELL'
            });
        });
    });

    describe('type', function() {
        it('should build a semantic url with `type` param', function() {
            expect(route.build({
                type: 'SELL'
            })).toBe('/moskva/kupit/kvartira/');

            expect(route.build({
                type: 'RENT'
            })).toBe('/moskva/snyat/kvartira/');
        });

        it('should get `type` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/kvartira/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL'
            });
            expect(route.match('/moskva/snyat/kvartira/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'RENT'
            });
        });
    });

    describe('category', function() {
        it('should build a semantic url with `category` param', function() {
            expect(route.build({
                category: 'APARTMENT'
            })).toBe('/moskva/kupit/kvartira/');

            expect(route.build({
                category: 'ROOMS'
            })).toBe('/moskva/kupit/komnata/');
        });

        it('should get `category` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/kvartira/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL'
            });
            expect(route.match('/moskva/kupit/komnata/')).toEqual({
                category: 'ROOMS',
                rgid: 587795,
                type: 'SELL'
            });
        });
    });

    describe('commercialFilters', function() {
        it('should build a semantic url with `commercialType` param', function() {
            expect(route.build({
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                renovation: 'COSMETIC_DONE'
            })).toBe('/moskva/kupit/kommercheskaya-nedvizhimost/ofis/s-kosmeticheskym-remontom/');

            expect(route.build({
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                renovation: 'DESIGNER_RENOVATION',
                floorMin: '1',
                floorMax: '1',
            })).toBe('/moskva/kupit/kommercheskaya-nedvizhimost/ofis/na-pervom-etage-i-s-dizainerskym-remontom/');
        });

        it('should get `commercialType` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/kommercheskaya-nedvizhimost/ofis/s-kosmeticheskym-remontom/')).toEqual({
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                renovation: 'COSMETIC_DONE',
                rgid: 587795,
                type: 'SELL'
            });
            // eslint-disable-next-line max-len
            expect(route.match('/moskva/kupit/kommercheskaya-nedvizhimost/ofis/na-pervom-etage-i-s-dizainerskym-remontom/')).toEqual({
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                renovation: 'DESIGNER_RENOVATION',
                floorMin: '1',
                floorMax: '1',
                rgid: 587795,
                type: 'SELL'
            });
        });
    });

    describe('houseType', function() {
        it('should build a semantic url with `houseType` param', function() {
            expect(route.build({
                category: 'HOUSE',
                houseType: 'TOWNHOUSE',
            })).toBe('/moskva/kupit/dom/townhouse/');
        });

        it('should get `houseType` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/dom/townhouse/')).toEqual({
                category: 'HOUSE',
                houseType: 'TOWNHOUSE',
                rgid: 587795,
                type: 'SELL'
            });
        });
    });

    describe('roomsTotal', function() {
        it('should build a semantic url with `roomsTotal` param', function() {
            expect(route.build({
                roomsTotal: '2'
            })).toBe('/moskva/kupit/kvartira/dvuhkomnatnaya/');

            expect(route.build({
                roomsTotal: 'OPEN_PLAN'
            })).toBe('/moskva/kupit/kvartira/svobodnaya-planirovka/');
        });

        it('should get `roomsTotal` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/kvartira/dvuhkomnatnaya/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                roomsTotal: '2'
            });
            expect(route.match('/moskva/kupit/komnata/svobodnaya-planirovka/')).toEqual({
                category: 'ROOMS',
                rgid: 587795,
                type: 'SELL',
                roomsTotal: 'OPEN_PLAN'
            });
        });
    });

    describe('metroGeoId', function() {
        it('should build a semantic url with `metroGeoId` param', function() {
            expect(route.build({
                metroGeoId: '20369'
            })).toBe('/moskva/kupit/kvartira/metro-rechnoy-vokzal/');

            expect(route.build({
                metroGeoId: [ '20369', '20372' ]
            })).toBe('/moskva/kupit/kvartira/?metroGeoId=20369&metroGeoId=20372');
        });

        it('should get `metroGeoId` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/kvartira/metro-rechnoy-vokzal/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                metroGeoId: '20369'
            });
            expect(route.match('/moskva/kupit/kvartira/metro-chkalovskaya-1/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                metroGeoId: '20515'
            });
            expect(route.match('/vsevolozhsk/kupit/kvartira/metro-prospekt-bolshevikov/')).toEqual({
                category: 'APARTMENT',
                rgid: 417867,
                type: 'SELL',
                metroGeoId: '20317'
            });
        });
    });

    describe('subLocality', function() {
        it('should build a semantic url with `subLocality` param', function() {
            expect(route.build({
                subLocality: '193300',
                subLocalityName: 'Дорогомилово',
                subLocalityType: 'CITY_DISTRICT'
            })).toBe('/moskva/kupit/kvartira/dist-dorogomilovo-193300/');
            expect(route.build({
                subLocality: [ '2686', '193310' ]
            })).toBe('/moskva/kupit/kvartira/?subLocality=2686&subLocality=193310');
            expect(route.build({
                subLocality: [ '2686', '193300' ],
                subLocalityName: 'Дорогомилово'
            })).toBe('/moskva/kupit/kvartira/?subLocality=2686&subLocality=193300');
            expect(route.build({
                subLocality: [ '2686', '193300' ],
                subLocalityName: 'Дорогомилово',
                metroGeoId: '20369'
            })).toBe('/moskva/kupit/kvartira/?subLocality=2686&subLocality=193300&metroGeoId=20369');
            expect(route.build({
                subLocality: '193300',
                subLocalityName: 'Дорогомилово',
                metroGeoId: '20369'
            })).toBe('/moskva/kupit/kvartira/?subLocality=193300&metroGeoId=20369');
            expect(route.build({
                subLocality: '193300',
                subLocalityName: 'Дорогомилово',
                subLocalityType: 'CITY_DISTRICT',
                testParam: 'test'
            })).toBe('/moskva/kupit/kvartira/dist-dorogomilovo-193300/?testParam=test');
            expect(route.build({
                subLocality: '17379566'
            })).toBe('/moskva/kupit/kvartira/?subLocality=17379566');
            expect(route.build({
                subLocality: '17379566',
                subLocalityName: 'Округ Любой',
                subLocalityType: 'SUBJECT_FEDERATION_DISTRICT'
            })).toBe('/moskva/kupit/kvartira/?subLocality=17379566');
        });

        it('should get `subLocality` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/kvartira/dist-dorogomilovo-193300/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                subLocality: '193300',
                subLocalityName: 'dorogomilovo-193300'
            });
            expect(route.match('/moskva/kupit/kvartira/?subLocality=2686&subLocality=193310')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                subLocality: [ '2686', '193310' ]
            });
            expect(route.match('/moskva/kupit/kvartira/?subLocality=2686')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                subLocality: '2686'
            });
        });
    });

    describe('street and house', function() {
        it('should build a semantic url with `street` param', function() {
            expect(route.build({
                streetName: 'ulica-arbat',
                streetId: 123123,
            })).toBe('/moskva/kupit/kvartira/st-ulica-arbat-123123/');

            expect(route.build({
                streetName: [ 'ulica-arbat', 'ulica-cyurupy' ],
                streetId: [ 123123, 456456 ],
            })).toBe('/moskva/kupit/kvartira/?streetId=123123&streetId=456456');

            expect(route.build({
                streetName: 'ulica-arbat',
                streetId: 123123,
                buildingIds: 123124908912,
                houseNumber: '24'
            })).toBe('/moskva/kupit/kvartira/st-ulica-arbat-123123/dom-24-123124908912/');
        });

        it('should get `direction` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/kvartira/st-ulica-arbat-123123/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                streetId: '123123',
                streetName: 'ulica-arbat',
                type: 'SELL',
            });
            expect(route.match('/moskva/snyat/kvartira/?streetId=123123&streetId=456456')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'RENT',
                streetId: [ '123123', '456456' ]
            });
            expect(route.match('/moskva/kupit/kvartira/st-ulica-arbat-123123/dom-24-123124908912/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                streetId: '123123',
                streetName: 'ulica-arbat',
                houseNumber: '24',
                buildingIds: '123124908912'
            });
        });
    });

    describe('railwayStation', function() {
        it('should build a semantic url with `railwayStation` param', function() {
            expect(route.build({
                railwayStation: '61517',
                railwayStationName: 'Пролетарская',
            }))
                .toBe('/moskva/kupit/kvartira/railway-proletarskaya-61517/');
            expect(route.build({ railwayStation: [ '61517', '60073' ] }))
                .toBe('/moskva/kupit/kvartira/?railwayStation=61517&railwayStation=60073');
            expect(route.build({
                railwayStation: '61517',
                railwayStationName: 'Пролетарская',
                testParam: 'test'
            })).toBe('/moskva/kupit/kvartira/railway-proletarskaya-61517/?testParam=test');
        });

        it('should get `railwayStation` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/kvartira/railway-proletarskaya-61517/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                railwayStation: '61517',
                railwayStationName: 'proletarskaya-61517'
            });
            expect(route.match('/moskva/kupit/kvartira/?railwayStation=61517&railwayStation=60073')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                railwayStation: [ '61517', '60073' ]
            });
            expect(route.match('/moskva/kupit/kvartira/?railwayStation=61517')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                railwayStation: '61517'
            });
        });
    });

    describe('direction', function() {
        it('should build a semantic url with `direction` param', function() {
            expect(route.build({
                direction: 1
            })).toBe('/moskva/kupit/kvartira/shosse-ostashkovskoe/');

            expect(route.build({
                direction: [ '1', '2' ]
            })).toBe('/moskva/kupit/kvartira/?direction=1&direction=2');

            expect(route.build({
                direction: '1',
                subLocality: '2686'
            })).toBe('/moskva/kupit/kvartira/?direction=1&subLocality=2686');
        });

        it('should get `direction` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/kvartira/shosse-ostashkovskoe/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                direction: '1'
            });
            expect(route.match('/moskva/kupit/kvartira/?direction=1&direction=2')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                direction: [ '1', '2' ]
            });
            expect(route.match('/moskva/kupit/kvartira/?direction=1&subLocality=2686')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                direction: '1',
                subLocality: '2686'
            });
        });
    });

    describe('commercialBuildingType', function() {
        it('should build a semantic url with `commercialBuildingType` param', function() {
            expect(route.build({
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                type: 'RENT',
                commercialBuildingType: 'RESIDENTIAL_BUILDING',
            })).toBe('/moskva/snyat/kommercheskaya-nedvizhimost/ofis/zhiloy-dom/');

            expect(route.build({
                category: 'COMMERCIAL',
                type: 'RENT',
                commercialBuildingType: 'SHOPPING_CENTER'
            })).toBe('/moskva/snyat/kommercheskaya-nedvizhimost/torgoviy-center/');
        });

        it('should get `commercialBuildingType` param from a semantic url', function() {
            expect(route.match('/moskva/snyat/kommercheskaya-nedvizhimost/ofis/zhiloy-dom/')).toEqual({
                rgid: 587795,
                type: 'RENT',
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                commercialBuildingType: 'RESIDENTIAL_BUILDING'
            });
            expect(route.match('/moskva/snyat/kommercheskaya-nedvizhimost/torgoviy-center/')).toEqual({
                rgid: 587795,
                type: 'RENT',
                category: 'COMMERCIAL',
                commercialBuildingType: 'SHOPPING_CENTER'
            });
        });
    });
});

describe('search-categories', function() {
    const route = router.getRouteByName('search-categories');

    it('should build a semantic url ', function() {
        expect(route.build({
            rgid: 587795,
            type: 'SELL'
        })).toBe('/moskva/kupit/');

        expect(route.build({
            rgid: 417899,
            type: 'RENT'
        })).toBe('/sankt-peterburg/snyat/');
    });

    it('should get `rgid` and `type` params from a semantic url', function() {
        expect(route.match('/moskva/kupit/')).toEqual({
            rgid: 587795,
            type: 'SELL'
        });
        expect(route.match('/sankt-peterburg/snyat/')).toEqual({
            rgid: 417899,
            type: 'RENT'
        });
    });
});

describe('amp-search', function() {
    const route = router.getRouteByName('amp-search');

    describe('default params', function() {
        it('should build a semantic url with default params', function() {
            expect(route.build()).toBe('/amp/moskva/kupit/kvartira/');
        });

        it('should get default params from root semantic url', function() {
            expect(route.match('/amp/moskva/kupit/kvartira/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL'
            });
        });
    });

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                rgid: 587795
            })).toBe('/amp/moskva/kupit/kvartira/');

            expect(route.build({
                rgid: 417899
            })).toBe('/amp/sankt-peterburg/kupit/kvartira/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/amp/moskva/kupit/kvartira/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL'
            });
            expect(route.match('/amp/sankt-peterburg/kupit/kvartira/')).toEqual({
                category: 'APARTMENT',
                rgid: 417899,
                type: 'SELL'
            });
        });
    });

    describe('type', function() {
        it('should build a semantic url with `type` param', function() {
            expect(route.build({
                type: 'SELL'
            })).toBe('/amp/moskva/kupit/kvartira/');

            expect(route.build({
                type: 'RENT'
            })).toBe('/amp/moskva/snyat/kvartira/');
        });

        it('should get `type` param from a semantic url', function() {
            expect(route.match('/amp/moskva/kupit/kvartira/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL'
            });
            expect(route.match('/amp/moskva/snyat/kvartira/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'RENT'
            });
        });
    });

    describe('category', function() {
        it('should build a semantic url with `category` param', function() {
            expect(route.build({
                category: 'APARTMENT'
            })).toBe('/amp/moskva/kupit/kvartira/');

            expect(route.build({
                category: 'ROOMS'
            })).toBe('/amp/moskva/kupit/komnata/');
        });

        it('should get `category` param from a semantic url', function() {
            expect(route.match('/amp/moskva/kupit/kvartira/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL'
            });
            expect(route.match('/amp/moskva/kupit/komnata/')).toEqual({
                category: 'ROOMS',
                rgid: 587795,
                type: 'SELL'
            });
        });
    });

    describe('commercialFilters', function() {
        it('should build a semantic url with `commercialType` param', function() {
            expect(route.build({
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                renovation: 'COSMETIC_DONE'
            })).toBe('/amp/moskva/kupit/kommercheskaya-nedvizhimost/ofis/s-kosmeticheskym-remontom/');

            expect(route.build({
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                renovation: 'DESIGNER_RENOVATION',
                floorMin: '1',
                floorMax: '1',
            })).toBe('/amp/moskva/kupit/kommercheskaya-nedvizhimost/ofis/na-pervom-etage-i-s-dizainerskym-remontom/');
        });

        it('should get `commercialType` param from a semantic url', function() {
            expect(route.match('/amp/moskva/kupit/kommercheskaya-nedvizhimost/ofis/s-kosmeticheskym-remontom/')).toEqual({
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                renovation: 'COSMETIC_DONE',
                rgid: 587795,
                type: 'SELL'
            });
            // eslint-disable-next-line max-len
            expect(route.match('/amp/moskva/kupit/kommercheskaya-nedvizhimost/ofis/na-pervom-etage-i-s-dizainerskym-remontom/')).toEqual({
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                renovation: 'DESIGNER_RENOVATION',
                floorMin: '1',
                floorMax: '1',
                rgid: 587795,
                type: 'SELL'
            });
        });
    });

    describe('houseType', function() {
        it('should build a semantic url with `houseType` param', function() {
            expect(route.build({
                category: 'HOUSE',
                houseType: 'TOWNHOUSE',
            })).toBe('/amp/moskva/kupit/dom/townhouse/');
        });

        it('should get `houseType` param from a semantic url', function() {
            expect(route.match('/amp/moskva/kupit/dom/townhouse/')).toEqual({
                category: 'HOUSE',
                houseType: 'TOWNHOUSE',
                rgid: 587795,
                type: 'SELL'
            });
        });
    });

    describe('roomsTotal', function() {
        it('should build a semantic url with `roomsTotal` param', function() {
            expect(route.build({
                roomsTotal: '2'
            })).toBe('/amp/moskva/kupit/kvartira/dvuhkomnatnaya/');

            expect(route.build({
                roomsTotal: 'OPEN_PLAN'
            })).toBe('/amp/moskva/kupit/kvartira/svobodnaya-planirovka/');
        });

        it('should get `roomsTotal` param from a semantic url', function() {
            expect(route.match('/amp/moskva/kupit/kvartira/dvuhkomnatnaya/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                roomsTotal: '2'
            });
            expect(route.match('/amp/moskva/kupit/komnata/svobodnaya-planirovka/')).toEqual({
                category: 'ROOMS',
                rgid: 587795,
                type: 'SELL',
                roomsTotal: 'OPEN_PLAN'
            });
        });
    });

    describe('metroGeoId', function() {
        it('should build a semantic url with `metroGeoId` param', function() {
            expect(route.build({
                metroGeoId: '20369'
            })).toBe('/amp/moskva/kupit/kvartira/metro-rechnoy-vokzal/');

            expect(route.build({
                metroGeoId: [ '20369', '20372' ]
            })).toBe('/amp/moskva/kupit/kvartira/?metroGeoId=20369&metroGeoId=20372');
        });

        it('should get `metroGeoId` param from a semantic url', function() {
            expect(route.match('/amp/moskva/kupit/kvartira/metro-rechnoy-vokzal/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                metroGeoId: '20369'
            });
            expect(route.match('/amp/moskva/kupit/kvartira/metro-chkalovskaya-1/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                metroGeoId: '20515'
            });
            expect(route.match('/amp/vsevolozhsk/kupit/kvartira/metro-prospekt-bolshevikov/')).toEqual({
                category: 'APARTMENT',
                rgid: 417867,
                type: 'SELL',
                metroGeoId: '20317'
            });
        });
    });

    describe('subLocality', function() {
        it('should build a semantic url with `subLocality` param', function() {
            expect(route.build({
                subLocality: '193300',
                subLocalityName: 'Дорогомилово',
                subLocalityType: 'CITY_DISTRICT'
            })).toBe('/amp/moskva/kupit/kvartira/dist-dorogomilovo-193300/');
            expect(route.build({
                subLocality: [ '2686', '193310' ]
            })).toBe('/amp/moskva/kupit/kvartira/?subLocality=2686&subLocality=193310');
            expect(route.build({
                subLocality: [ '2686', '193300' ],
                subLocalityName: 'Дорогомилово',
                metroGeoId: '20369'
            })).toBe('/amp/moskva/kupit/kvartira/?subLocality=2686&subLocality=193300&metroGeoId=20369');
            expect(route.build({
                subLocality: '193300',
                subLocalityName: 'Дорогомилово',
                metroGeoId: '20369'
            })).toBe('/amp/moskva/kupit/kvartira/?subLocality=193300&metroGeoId=20369');
            expect(route.build({
                subLocality: '193300',
                subLocalityName: 'Дорогомилово',
                subLocalityType: 'CITY_DISTRICT',
                testParam: 'test'
            })).toBe('/amp/moskva/kupit/kvartira/dist-dorogomilovo-193300/?testParam=test');
            expect(route.build({
                subLocality: '17379566'
            })).toBe('/amp/moskva/kupit/kvartira/?subLocality=17379566');
            expect(route.build({
                subLocality: '17379566',
                subLocalityName: 'Округ Любой',
                subLocalityType: 'SUBJECT_FEDERATION_DISTRICT'
            })).toBe('/amp/moskva/kupit/kvartira/?subLocality=17379566');
        });

        it('should get `subLocality` param from a semantic url', function() {
            expect(route.match('/amp/moskva/kupit/kvartira/dist-dorogomilovo-193300/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                subLocality: '193300',
                subLocalityName: 'dorogomilovo-193300'
            });
            expect(route.match('/amp/moskva/kupit/kvartira/?subLocality=2686&subLocality=193310')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                subLocality: [ '2686', '193310' ]
            });
            expect(route.match('/amp/moskva/kupit/kvartira/?subLocality=2686')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                subLocality: '2686'
            });
        });
    });

    describe('commercialBuildingType', function() {
        it('should build a semantic url with `commercialBuildingType` param', function() {
            expect(route.build({
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                type: 'RENT',
                commercialBuildingType: 'RESIDENTIAL_BUILDING',
            })).toBe('/amp/moskva/snyat/kommercheskaya-nedvizhimost/ofis/zhiloy-dom/');

            expect(route.build({
                category: 'COMMERCIAL',
                type: 'RENT',
                commercialBuildingType: 'SHOPPING_CENTER'
            })).toBe('/amp/moskva/snyat/kommercheskaya-nedvizhimost/torgoviy-center/');
        });

        it('should get `commercialBuildingType` param from a semantic url', function() {
            expect(route.match('/amp/moskva/snyat/kommercheskaya-nedvizhimost/ofis/zhiloy-dom/')).toEqual({
                rgid: 587795,
                type: 'RENT',
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                commercialBuildingType: 'RESIDENTIAL_BUILDING'
            });
            expect(route.match('/amp/moskva/snyat/kommercheskaya-nedvizhimost/torgoviy-center/')).toEqual({
                rgid: 587795,
                type: 'RENT',
                category: 'COMMERCIAL',
                commercialBuildingType: 'SHOPPING_CENTER'
            });
        });
    });
});

describe('map', function() {
    const route = router.getRouteByName('map');

    describe('default params', function() {
        it('should build a semantic url with default params', function() {
            expect(route.build()).toBe('/moskva/kupit/kvartira/karta/');
        });

        it('should get default params from root semantic url', function() {
            expect(route.match('/moskva/kupit/kvartira/karta/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL'
            });
        });
    });

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                rgid: 587795
            })).toBe('/moskva/kupit/kvartira/karta/');

            expect(route.build({
                rgid: 417899
            })).toBe('/sankt-peterburg/kupit/kvartira/karta/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/kvartira/karta/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL'
            });
            expect(route.match('/sankt-peterburg/kupit/kvartira/karta/')).toEqual({
                category: 'APARTMENT',
                rgid: 417899,
                type: 'SELL'
            });
        });
    });

    describe('type', function() {
        it('should build a semantic url with `type` param', function() {
            expect(route.build({
                type: 'SELL'
            })).toBe('/moskva/kupit/kvartira/karta/');

            expect(route.build({
                type: 'RENT'
            })).toBe('/moskva/snyat/kvartira/karta/');
        });

        it('should get `type` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/kvartira/karta/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL'
            });
            expect(route.match('/moskva/snyat/kvartira/karta/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'RENT'
            });
        });
    });

    describe('category', function() {
        it('should build a semantic url with `category` param', function() {
            expect(route.build({
                category: 'APARTMENT'
            })).toBe('/moskva/kupit/kvartira/karta/');

            expect(route.build({
                category: 'ROOMS'
            })).toBe('/moskva/kupit/komnata/karta/');
        });

        it('should get `category` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/kvartira/karta/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL'
            });
            expect(route.match('/moskva/kupit/komnata/karta/')).toEqual({
                category: 'ROOMS',
                rgid: 587795,
                type: 'SELL'
            });
        });
    });

    describe('roomsTotal', function() {
        it('should build a semantic url with `roomsTotal` param', function() {
            expect(route.build({
                roomsTotal: '2'
            })).toBe('/moskva/kupit/kvartira/dvuhkomnatnaya/karta/');

            expect(route.build({
                roomsTotal: [ '6', 'PLUS_7' ]
            })).toBe('/moskva/kupit/kvartira/karta/?roomsTotal=6&roomsTotal=PLUS_7');

            expect(route.build({
                roomsTotal: 'OPEN_PLAN'
            })).toBe('/moskva/kupit/kvartira/svobodnaya-planirovka/karta/');

            expect(route.build({
                roomsTotal: [ '1', '2' ]
            })).toBe('/moskva/kupit/kvartira/1,2-komnatnie/karta/');

            expect(route.build({
                roomsTotal: [ '1', '3' ]
            })).toBe('/moskva/kupit/kvartira/karta/?roomsTotal=1&roomsTotal=3');

            expect(route.build({
                roomsTotal: [ 'STUDIO' ]
            })).toBe('/moskva/kupit/kvartira/studiya/karta/');

            expect(route.build({
                roomsTotal: [ 'STUDIO', '1' ]
            })).toBe('/moskva/kupit/kvartira/studiya,1-komnatnie/karta/');

            expect(route.build({
                roomsTotal: [ 'STUDIO', '2' ]
            })).toBe('/moskva/kupit/kvartira/karta/?roomsTotal=STUDIO&roomsTotal=2');

            expect(route.build({
                roomsTotal: [ 'STUDIO', 'PLUS_4' ]
            })).toBe('/moskva/kupit/kvartira/karta/?roomsTotal=STUDIO&roomsTotal=PLUS_4');

            expect(route.build({
                roomsTotal: [ 'STUDIO', '2', '3' ]
            })).toBe('/moskva/kupit/kvartira/karta/?roomsTotal=STUDIO&roomsTotal=2&roomsTotal=3');

            expect(route.build({
                roomsTotal: [ 'STUDIO', '2', 'PLUS_4' ]
            })).toBe('/moskva/kupit/kvartira/karta/?roomsTotal=STUDIO&roomsTotal=2&roomsTotal=PLUS_4');
        });

        it('should get `roomsTotal` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/kvartira/dvuhkomnatnaya/karta/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                roomsTotal: '2'
            });
            expect(route.match('/moskva/kupit/komnata/svobodnaya-planirovka/karta/')).toEqual({
                category: 'ROOMS',
                rgid: 587795,
                type: 'SELL',
                roomsTotal: 'OPEN_PLAN'
            });
            expect(route.match('/moskva/kupit/komnata/2,4,7-i-bolee-komnatnie/karta/')).toEqual({
                category: 'ROOMS',
                rgid: 587795,
                type: 'SELL',
                roomsTotal: [ '2', '4', 'PLUS_7' ]
            });
        });
    });

    describe('metroGeoId', function() {
        it('should build a semantic url with `metroGeoId` param', function() {
            expect(route.build({
                metroGeoId: '20369'
            })).toBe('/moskva/kupit/kvartira/metro-rechnoy-vokzal/karta/');

            expect(route.build({
                metroGeoId: [ '20369', '20372' ]
            })).toBe('/moskva/kupit/kvartira/karta/?metroGeoId=20369&metroGeoId=20372');
        });

        it('should get `metroGeoId` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/kvartira/metro-rechnoy-vokzal/karta/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                metroGeoId: '20369'
            });
            expect(route.match('/moskva/kupit/kvartira/metro-chkalovskaya-1/karta/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                metroGeoId: '20515'
            });
            expect(
                route.match('/vsevolozhsk/kupit/kvartira/metro-prospekt-bolshevikov/karta/')
            ).toEqual({
                category: 'APARTMENT',
                rgid: 417867,
                type: 'SELL',
                metroGeoId: '20317'
            });
        });
    });

    describe('subLocality', function() {
        it('should build a semantic url with `subLocality` param', function() {
            expect(route.build({
                subLocality: '193300',
                subLocalityName: 'Дорогомилово',
                subLocalityType: 'CITY_DISTRICT'
            })).toBe('/moskva/kupit/kvartira/dist-dorogomilovo-193300/karta/');
            expect(route.build({
                subLocality: [ '2686', '193310' ]
            })).toBe('/moskva/kupit/kvartira/karta/?subLocality=2686&subLocality=193310');
            expect(route.build({
                subLocality: '17379566'
            })).toBe('/moskva/kupit/kvartira/karta/?subLocality=17379566');
        });

        it('should get `subLocality` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/kvartira/dist-dorogomilovo-193300/karta/')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                subLocality: '193300',
                subLocalityName: 'dorogomilovo-193300'
            });
            expect(route.match('/moskva/kupit/kvartira/karta/?subLocality=2686&subLocality=193310')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                subLocality: [ '2686', '193310' ]
            });
            expect(route.match('/moskva/kupit/kvartira/karta/?subLocality=2686')).toEqual({
                category: 'APARTMENT',
                rgid: 587795,
                type: 'SELL',
                subLocality: '2686'
            });
        });
    });
});

describe('newbuilding-search', function() {
    const route = router.getRouteByName('newbuilding-search');

    describe('default params', function() {
        it('should build a semantic url with default params', function() {
            expect(route.build()).toBe('/moskva/kupit/novostrojka/');
        });

        it('should get default params from root semantic url', function() {
            expect(route.match('/moskva/kupit/novostrojka/')).toEqual({
                rgid: 587795,
                type: 'SELL'
            });
        });
    });

    describe('developerId', function() {
        it('should build a semantic url with `developerId` params', function() {
            expect(route.build({
                type: 'SELL',
                developerId: '52308',
                developerName: 'pik'
            })).toBe('/moskva/kupit/novostrojka/z-pik-52308/');
        });

        it('should build a semantic url with `developerId` and `roomsTotal` params', function() {
            expect(route.build({
                rgid: 587795,
                type: 'SELL',
                developerId: '52308',
                developerName: 'pik',
                roomsTotal: '2'
            })).toBe('/moskva/kupit/novostrojka/dvuhkomnatnaya/z-pik-52308/');
        });

        it('should get `developerId` params from root semantic url', function() {
            expect(route.match('/moskva/kupit/novostrojka/z-pik-52308/')).toEqual({
                rgid: 587795,
                type: 'SELL',
                developerId: '52308',
                developerName: 'pik',
            });
        });

        it('should get `developerName`, `developerId`, `roomsTotal` params from root semantic url', function() {
            expect(route.match('/moskva/kupit/novostrojka/dvuhkomnatnaya/z-pik-52308/')).toEqual({
                rgid: 587795,
                type: 'SELL',
                developerId: '52308',
                developerName: 'pik',
                roomsTotal: '2'
            });
        });
    });

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                rgid: 587795
            })).toBe('/moskva/kupit/novostrojka/');

            expect(route.build({
                rgid: 417899
            })).toBe('/sankt-peterburg/kupit/novostrojka/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/novostrojka/')).toEqual({
                rgid: 587795,
                type: 'SELL'
            });
            expect(route.match('/sankt-peterburg/kupit/novostrojka/')).toEqual({
                rgid: 417899,
                type: 'SELL'
            });
        });
    });

    describe('roomsTotal', function() {
        it('should build a semantic url with `roomsTotal` param', function() {
            expect(route.build({
                roomsTotal: '2'
            })).toBe('/moskva/kupit/novostrojka/dvuhkomnatnaya/');

            expect(route.build({
                roomsTotal: 'OPEN_PLAN'
            })).toBe('/moskva/kupit/novostrojka/svobodnaya-planirovka/');
        });

        it('should get `roomsTotal` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/novostrojka/dvuhkomnatnaya/')).toEqual({
                rgid: 587795,
                type: 'SELL',
                roomsTotal: '2'
            });
            expect(route.match('/moskva/kupit/novostrojka/svobodnaya-planirovka/')).toEqual({
                rgid: 587795,
                type: 'SELL',
                roomsTotal: 'OPEN_PLAN'
            });
        });
    });

    describe('metroGeoId', function() {
        it('should build a semantic url with `metroGeoId` param', function() {
            expect(route.build({
                metroGeoId: '20369'
            })).toBe('/moskva/kupit/novostrojka/metro-rechnoy-vokzal/');

            expect(route.build({
                metroGeoId: [ '20369', '20372' ]
            })).toBe('/moskva/kupit/novostrojka/?metroGeoId=20369&metroGeoId=20372');
        });

        it('should get `metroGeoId` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/novostrojka/metro-rechnoy-vokzal/')).toEqual({
                rgid: 587795,
                type: 'SELL',
                metroGeoId: '20369'
            });
            expect(route.match('/moskva/kupit/novostrojka/metro-chkalovskaya-1/')).toEqual({
                rgid: 587795,
                type: 'SELL',
                metroGeoId: '20515'
            });
            expect(route.match('/vsevolozhsk/kupit/novostrojka/metro-prospekt-bolshevikov/')).toEqual({
                rgid: 417867,
                type: 'SELL',
                metroGeoId: '20317'
            });
        });

        it('should not match for specific newbuilding name with metro prefix', function() {
            expect(route.match('/sankt-peterburg/kupit/novostrojka/metro-777/')).toEqual(null);
        });
    });

    describe('subLocality', function() {
        it('should build a semantic url with `subLocality` param', function() {
            expect(route.build({
                subLocality: '193300',
                subLocalityName: 'Дорогомилово',
                subLocalityType: 'CITY_DISTRICT'
            })).toBe('/moskva/kupit/novostrojka/dist-dorogomilovo-193300/');
            expect(route.build({
                subLocality: [ '193310', '193317' ]
            })).toBe('/moskva/kupit/novostrojka/?subLocality=193310&subLocality=193317');
            expect(route.build({
                subLocality: '193310'
            })).toBe('/moskva/kupit/novostrojka/?subLocality=193310');
        });

        it('should get `subLocality` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/novostrojka/dist-dorogomilovo-193300/')).toEqual({
                rgid: 587795,
                type: 'SELL',
                subLocality: '193300',
                subLocalityName: 'dorogomilovo-193300'
            });
            expect(route.match('/moskva/kupit/novostrojka/?subLocality=2686&subLocality=193310')).toEqual({
                rgid: 587795,
                type: 'SELL',
                subLocality: [ '2686', '193310' ]
            });
            expect(route.match('/moskva/kupit/novostrojka/?subLocality=2686')).toEqual({
                rgid: 587795,
                type: 'SELL',
                subLocality: '2686'
            });
        });
    });

    describe('railwayStation', function() {
        it('should build a semantic url with `railwayStation` param', function() {
            expect(route.build({
                railwayStation: '61517',
                railwayStationName: 'Пролетарская',
            }))
                .toBe('/moskva/kupit/novostrojka/railway-proletarskaya-61517/');
            expect(route.build({ railwayStation: [ '61517', '60073' ] }))
                .toBe('/moskva/kupit/novostrojka/?railwayStation=61517&railwayStation=60073');
        });

        it('should get `railwayStation` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/novostrojka/railway-proletarskaya-61517/')).toEqual({
                rgid: 587795,
                type: 'SELL',
                railwayStation: '61517',
                railwayStationName: 'proletarskaya-61517'
            });
            expect(route.match('/moskva/kupit/novostrojka/?railwayStation=61517&railwayStation=60073')).toEqual({
                rgid: 587795,
                type: 'SELL',
                railwayStation: [ '61517', '60073' ]
            });
            expect(route.match('/moskva/kupit/novostrojka/?railwayStation=61517')).toEqual({
                rgid: 587795,
                type: 'SELL',
                railwayStation: '61517'
            });
        });
        describe('deliveryDate', function() {
            it('should build a semantic url with `deliveryDate` params', function() {
                expect(route.build({
                    type: 'SELL',
                    deliveryDate: '4_2023',
                })).toBe('/moskva/kupit/novostrojka/sdacha-2023/');
            });

            it('should get `deliveryDate` params from root semantic url', function() {
                expect(route.match('/moskva/kupit/novostrojka/sdacha-2023/')).toEqual({
                    rgid: 587795,
                    type: 'SELL',
                    deliveryDate: '4_2023',
                });
            });
        });
    });
});

describe('amp-newbuilding-search', function() {
    const route = router.getRouteByName('amp-newbuilding-search');

    describe('default params', function() {
        it('should build a semantic url with default params', function() {
            expect(route.build()).toBe('/amp/moskva/kupit/novostrojka/');
        });

        it('should get default params from root semantic url', function() {
            expect(route.match('/amp/moskva/kupit/novostrojka/')).toEqual({
                rgid: 587795,
                type: 'SELL'
            });
        });
    });

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                rgid: 587795
            })).toBe('/amp/moskva/kupit/novostrojka/');

            expect(route.build({
                rgid: 417899
            })).toBe('/amp/sankt-peterburg/kupit/novostrojka/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/amp/moskva/kupit/novostrojka/')).toEqual({
                rgid: 587795,
                type: 'SELL'
            });
            expect(route.match('/amp/sankt-peterburg/kupit/novostrojka/')).toEqual({
                rgid: 417899,
                type: 'SELL'
            });
        });
    });

    describe('roomsTotal', function() {
        it('should build a semantic url with `roomsTotal` param', function() {
            expect(route.build({
                roomsTotal: '2'
            })).toBe('/amp/moskva/kupit/novostrojka/dvuhkomnatnaya/');

            expect(route.build({
                roomsTotal: 'OPEN_PLAN'
            })).toBe('/amp/moskva/kupit/novostrojka/svobodnaya-planirovka/');
        });

        it('should get `roomsTotal` param from a semantic url', function() {
            expect(route.match('/amp/moskva/kupit/novostrojka/dvuhkomnatnaya/')).toEqual({
                rgid: 587795,
                type: 'SELL',
                roomsTotal: '2'
            });
            expect(route.match('/amp/moskva/kupit/novostrojka/svobodnaya-planirovka/')).toEqual({
                rgid: 587795,
                type: 'SELL',
                roomsTotal: 'OPEN_PLAN'
            });
        });
    });

    describe('metroGeoId', function() {
        it('should build a semantic url with `metroGeoId` param', function() {
            expect(route.build({
                metroGeoId: '20369'
            })).toBe('/amp/moskva/kupit/novostrojka/metro-rechnoy-vokzal/');

            expect(route.build({
                metroGeoId: [ '20369', '20372' ]
            })).toBe('/amp/moskva/kupit/novostrojka/?metroGeoId=20369&metroGeoId=20372');
        });

        it('should get `metroGeoId` param from a semantic url', function() {
            expect(route.match('/amp/moskva/kupit/novostrojka/metro-rechnoy-vokzal/')).toEqual({
                rgid: 587795,
                type: 'SELL',
                metroGeoId: '20369'
            });
            expect(route.match('/amp/moskva/kupit/novostrojka/metro-chkalovskaya-1/')).toEqual({
                rgid: 587795,
                type: 'SELL',
                metroGeoId: '20515'
            });
            expect(route.match('/amp/vsevolozhsk/kupit/novostrojka/metro-prospekt-bolshevikov/')).toEqual({
                rgid: 417867,
                type: 'SELL',
                metroGeoId: '20317'
            });
        });

        it('should not match for specific newbuilding name with metro prefix', function() {
            expect(route.match('/sankt-peterburg/kupit/novostrojka/metro-777/')).toEqual(null);
        });
    });

    describe('subLocality', function() {
        it('should build a semantic url with `subLocality` param', function() {
            expect(route.build({
                subLocality: '193300',
                subLocalityName: 'Дорогомилово',
                subLocalityType: 'CITY_DISTRICT'
            })).toBe('/amp/moskva/kupit/novostrojka/dist-dorogomilovo-193300/');
            expect(route.build({
                subLocality: [ '2686', '193310' ]
            })).toBe('/amp/moskva/kupit/novostrojka/?subLocality=2686&subLocality=193310');
            expect(route.build({
                subLocality: [ '2686', '193300' ],
                subLocalityName: 'Дорогомилово',
                metroGeoId: '20369'
            })).toBe('/amp/moskva/kupit/novostrojka/?subLocality=2686&subLocality=193300&metroGeoId=20369');
            expect(route.build({
                subLocality: '193300',
                subLocalityName: 'Дорогомилово',
                metroGeoId: '20369'
            })).toBe('/amp/moskva/kupit/novostrojka/?subLocality=193300&metroGeoId=20369');
            expect(route.build({
                subLocality: '193300',
                subLocalityName: 'Дорогомилово',
                subLocalityType: 'CITY_DISTRICT',
                testParam: 'test'
            })).toBe('/amp/moskva/kupit/novostrojka/dist-dorogomilovo-193300/?testParam=test');
            expect(route.build({
                subLocality: '17379566'
            })).toBe('/amp/moskva/kupit/novostrojka/?subLocality=17379566');
            expect(route.build({
                subLocality: '17379566',
                subLocalityName: 'Округ Любой',
                subLocalityType: 'SUBJECT_FEDERATION_DISTRICT'
            })).toBe('/amp/moskva/kupit/novostrojka/?subLocality=17379566');
        });

        it('should get `subLocality` param from a semantic url', function() {
            expect(route.match('/amp/moskva/kupit/novostrojka/dist-dorogomilovo-193300/')).toEqual({
                rgid: 587795,
                type: 'SELL',
                subLocality: '193300',
                subLocalityName: 'dorogomilovo-193300'
            });
            expect(route.match('/amp/moskva/kupit/novostrojka/?subLocality=2686&subLocality=193310')).toEqual({
                rgid: 587795,
                type: 'SELL',
                subLocality: [ '2686', '193310' ]
            });
            expect(route.match('/amp/moskva/kupit/novostrojka/?subLocality=2686')).toEqual({
                rgid: 587795,
                type: 'SELL',
                subLocality: '2686'
            });
        });
    });
});

describe('newbuilding-map', function() {
    const route = router.getRouteByName('newbuilding-map');

    describe('default params', function() {
        it('should build a semantic url with default params', function() {
            expect(route.build()).toBe('/moskva/kupit/novostrojka/karta/');
        });

        it('should get default params from root semantic url', function() {
            expect(route.match('/moskva/kupit/novostrojka/karta/')).toEqual({
                rgid: 587795,
                type: 'SELL'
            });
        });
    });

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                rgid: 587795
            })).toBe('/moskva/kupit/novostrojka/karta/');

            expect(route.build({
                rgid: 417899
            })).toBe('/sankt-peterburg/kupit/novostrojka/karta/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/novostrojka/karta/')).toEqual({
                rgid: 587795,
                type: 'SELL'
            });
            expect(route.match('/sankt-peterburg/kupit/novostrojka/karta/')).toEqual({
                rgid: 417899,
                type: 'SELL'
            });
        });
    });

    describe('roomsTotal', function() {
        it('should build a semantic url with `roomsTotal` param', function() {
            expect(route.build({
                roomsTotal: '2'
            })).toBe('/moskva/kupit/novostrojka/dvuhkomnatnaya/karta/');

            expect(route.build({
                roomsTotal: 'OPEN_PLAN'
            })).toBe('/moskva/kupit/novostrojka/svobodnaya-planirovka/karta/');
        });

        it('should get `roomsTotal` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/novostrojka/dvuhkomnatnaya/karta/')).toEqual({
                rgid: 587795,
                type: 'SELL',
                roomsTotal: '2'
            });
            expect(route.match('/moskva/kupit/novostrojka/svobodnaya-planirovka/karta/')).toEqual({
                rgid: 587795,
                type: 'SELL',
                roomsTotal: 'OPEN_PLAN'
            });
        });
    });

    describe('metroGeoId', function() {
        it('should build a semantic url with `metroGeoId` param', function() {
            expect(route.build({
                metroGeoId: '20369'
            })).toBe('/moskva/kupit/novostrojka/metro-rechnoy-vokzal/karta/');

            expect(route.build({
                metroGeoId: [ '20369', '20372' ]
            })).toBe('/moskva/kupit/novostrojka/karta/?metroGeoId=20369&metroGeoId=20372');
        });

        it('should get `metroGeoId` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/novostrojka/metro-rechnoy-vokzal/karta/')).toEqual({
                rgid: 587795,
                type: 'SELL',
                metroGeoId: '20369'
            });
            expect(route.match('/moskva/kupit/novostrojka/metro-chkalovskaya-1/karta/')).toEqual({
                rgid: 587795,
                type: 'SELL',
                metroGeoId: '20515'
            });
            expect(
                route.match('/vsevolozhsk/kupit/novostrojka/metro-prospekt-bolshevikov/karta/')
            ).toEqual({
                rgid: 417867,
                type: 'SELL',
                metroGeoId: '20317'
            });
        });
    });

    describe('subLocality', function() {
        it('should build a semantic url with `subLocality` param', function() {
            expect(route.build({
                subLocality: '193300',
                subLocalityName: 'Дорогомилово',
                subLocalityType: 'CITY_DISTRICT'
            })).toBe('/moskva/kupit/novostrojka/dist-dorogomilovo-193300/karta/');
            expect(route.build({
                subLocality: [ '193310', '193317' ]
            })).toBe('/moskva/kupit/novostrojka/karta/?subLocality=193310&subLocality=193317');
            expect(route.build({
                subLocality: '193310'
            })).toBe('/moskva/kupit/novostrojka/karta/?subLocality=193310');
        });

        it('should get `subLocality` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/novostrojka/dist-dorogomilovo-193300/karta/')).toEqual({
                rgid: 587795,
                type: 'SELL',
                subLocality: '193300',
                subLocalityName: 'dorogomilovo-193300'
            });
            expect(route.match('/moskva/kupit/novostrojka/karta/?subLocality=2686&subLocality=193310')).toEqual({
                rgid: 587795,
                type: 'SELL',
                subLocality: [ '2686', '193310' ]
            });
            expect(route.match('/moskva/kupit/novostrojka/karta/?subLocality=2686')).toEqual({
                rgid: 587795,
                type: 'SELL',
                subLocality: '2686'
            });
        });
    });
});

describe('commercial-search', function() {
    const route = router.getRouteByName('commercial-search');

    describe('default params', function() {
        it('should build a semantic url with default params', function() {
            expect(route.build()).toBe('/moskva/snyat/kommercheskaya-nedvizhimost/');
        });

        it('should get default params from root semantic url', function() {
            expect(route.match('/moskva/snyat/kommercheskaya-nedvizhimost/')).toEqual({
                rgid: 587795,
                category: 'COMMERCIAL',
                type: 'RENT'
            });
        });

        it('should build a semantic url with `category` param', function() {
            expect(route.build({
                category: 'COMMERCIAL'
            })).toBe('/moskva/snyat/kommercheskaya-nedvizhimost/');
        });
    });

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                rgid: 587795
            })).toBe('/moskva/snyat/kommercheskaya-nedvizhimost/');

            expect(route.build({
                rgid: 417899
            })).toBe('/sankt-peterburg/snyat/kommercheskaya-nedvizhimost/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/moskva/snyat/kommercheskaya-nedvizhimost/')).toEqual({
                rgid: 587795,
                category: 'COMMERCIAL',
                type: 'RENT'
            });
            expect(route.match('/sankt-peterburg/snyat/kommercheskaya-nedvizhimost/')).toEqual({
                rgid: 417899,
                category: 'COMMERCIAL',
                type: 'RENT'
            });
        });
    });

    describe('commercialType', function() {
        it('should build a semantic url with `type` param', function() {
            expect(route.build({
                commercialType: 'OFFICE'
            })).toBe('/moskva/snyat/kommercheskaya-nedvizhimost/ofis/');

            expect(route.build({
                commercialType: 'HOTEL'
            })).toBe('/moskva/snyat/kommercheskaya-nedvizhimost/gostinica/');
        });

        it('should get `commercialType` param from a semantic url', function() {
            expect(route.match('/moskva/snyat/kommercheskaya-nedvizhimost/ofis/')).toEqual({
                rgid: 587795,
                type: 'RENT',
                category: 'COMMERCIAL',
                commercialType: 'OFFICE'
            });
            expect(route.match('/moskva/snyat/kommercheskaya-nedvizhimost/gostinica/')).toEqual({
                rgid: 587795,
                type: 'RENT',
                category: 'COMMERCIAL',
                commercialType: 'HOTEL'
            });
        });
    });

    describe('metroGeoId', function() {
        it('should build a semantic url with `metroGeoId` param', function() {
            expect(route.build({
                metroGeoId: '20369'
            })).toBe('/moskva/snyat/kommercheskaya-nedvizhimost/metro-rechnoy-vokzal/');

            expect(route.build({
                metroGeoId: [ '20369', '20372' ]
            })).toBe(
                '/moskva/snyat/kommercheskaya-nedvizhimost/?metroGeoId=20369&metroGeoId=20372'
            );
        });

        it('should get `metroGeoId` param from a semantic url', function() {
            expect(
                route.match('/moskva/snyat/kommercheskaya-nedvizhimost/metro-rechnoy-vokzal/')
            ).toEqual({
                rgid: 587795,
                metroGeoId: '20369',
                category: 'COMMERCIAL',
                type: 'RENT'
            });
            expect(
                route.match('/moskva/snyat/kommercheskaya-nedvizhimost/metro-chkalovskaya-1/')
            ).toEqual({
                rgid: 587795,
                metroGeoId: '20515',
                category: 'COMMERCIAL',
                type: 'RENT'
            });
            expect(
                route.match('/vsevolozhsk/snyat/kommercheskaya-nedvizhimost/metro-prospekt-bolshevikov/')
            ).toEqual({
                rgid: 417867,
                metroGeoId: '20317',
                category: 'COMMERCIAL',
                type: 'RENT'
            });
        });
    });

    describe('subLocality', function() {
        it('should build a semantic url with `subLocality` param', function() {
            expect(route.build({
                subLocality: '193300',
                subLocalityName: 'Дорогомилово',
                subLocalityType: 'CITY_DISTRICT'
            })).toBe('/moskva/snyat/kommercheskaya-nedvizhimost/dist-dorogomilovo-193300/');
            expect(route.build({
                subLocality: [ '193310', '193317' ]
            })).toBe('/moskva/snyat/kommercheskaya-nedvizhimost/?subLocality=193310&subLocality=193317');
            expect(route.build({
                subLocality: '193310'
            })).toBe('/moskva/snyat/kommercheskaya-nedvizhimost/?subLocality=193310');
        });

        it('should get `subLocality` param from a semantic url', function() {
            expect(
                route.match('/moskva/snyat/kommercheskaya-nedvizhimost/dist-dorogomilovo-193300/')
            ).toEqual({
                rgid: 587795,
                subLocality: '193300',
                subLocalityName: 'dorogomilovo-193300',
                category: 'COMMERCIAL',
                type: 'RENT'
            });
            // eslint-disable-next-line max-len
            expect(route.match('/moskva/snyat/kommercheskaya-nedvizhimost/?subLocality=2686&subLocality=193310')).toEqual({
                rgid: 587795,
                category: 'COMMERCIAL',
                type: 'RENT',
                subLocality: [ '2686', '193310' ]
            });
            expect(route.match('/moskva/snyat/kommercheskaya-nedvizhimost/?subLocality=2686')).toEqual({
                rgid: 587795,
                category: 'COMMERCIAL',
                type: 'RENT',
                subLocality: '2686'
            });
        });
    });
});

describe('commercial-map', function() {
    const route = router.getRouteByName('commercial-map');

    describe('default params', function() {
        it('should build a semantic url with default params', function() {
            expect(route.build()).toBe('/moskva/snyat/kommercheskaya-nedvizhimost/karta/');
        });

        it('should get default params from root semantic url', function() {
            expect(route.match('/moskva/snyat/kommercheskaya-nedvizhimost/karta/')).toEqual({
                rgid: 587795,
                category: 'COMMERCIAL',
                type: 'RENT'
            });
        });

        it('should build a semantic url with `category` param', function() {
            expect(route.build({
                category: 'COMMERCIAL'
            })).toBe('/moskva/snyat/kommercheskaya-nedvizhimost/karta/');
        });
    });

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                rgid: 587795
            })).toBe('/moskva/snyat/kommercheskaya-nedvizhimost/karta/');

            expect(route.build({
                rgid: 417899
            })).toBe('/sankt-peterburg/snyat/kommercheskaya-nedvizhimost/karta/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/moskva/snyat/kommercheskaya-nedvizhimost/karta/')).toEqual({
                rgid: 587795,
                category: 'COMMERCIAL',
                type: 'RENT'
            });
            expect(route.match('/sankt-peterburg/snyat/kommercheskaya-nedvizhimost/karta/')).toEqual({
                rgid: 417899,
                category: 'COMMERCIAL',
                type: 'RENT'
            });
        });
    });

    describe('commercialType', function() {
        it('should build a semantic url with `type` param', function() {
            expect(route.build({
                commercialType: 'OFFICE'
            })).toBe('/moskva/snyat/kommercheskaya-nedvizhimost/ofis/karta/');

            expect(route.build({
                commercialType: 'HOTEL'
            })).toBe('/moskva/snyat/kommercheskaya-nedvizhimost/gostinica/karta/');
        });

        it('should get `commercialType` param from a semantic url', function() {
            expect(route.match('/moskva/snyat/kommercheskaya-nedvizhimost/ofis/karta/')).toEqual({
                rgid: 587795,
                type: 'RENT',
                category: 'COMMERCIAL',
                commercialType: 'OFFICE'
            });
            expect(route.match('/moskva/snyat/kommercheskaya-nedvizhimost/gostinica/karta/')).toEqual({
                rgid: 587795,
                type: 'RENT',
                category: 'COMMERCIAL',
                commercialType: 'HOTEL'
            });
        });
    });

    describe('metroGeoId', function() {
        it('should build a semantic url with `metroGeoId` param', function() {
            expect(route.build({
                metroGeoId: '20369'
            })).toBe('/moskva/snyat/kommercheskaya-nedvizhimost/metro-rechnoy-vokzal/karta/');

            expect(route.build({
                metroGeoId: [ '20369', '20372' ]
            })).toBe(
                '/moskva/snyat/kommercheskaya-nedvizhimost/karta/?metroGeoId=20369&metroGeoId=20372'
            );
        });

        it('should get `metroGeoId` param from a semantic url', function() {
            expect(
                route.match('/moskva/snyat/kommercheskaya-nedvizhimost/metro-rechnoy-vokzal/karta/')
            ).toEqual({
                rgid: 587795,
                metroGeoId: '20369',
                category: 'COMMERCIAL',
                type: 'RENT'
            });
            expect(
                route.match('/moskva/snyat/kommercheskaya-nedvizhimost/metro-chkalovskaya-1/karta/')
            ).toEqual({
                rgid: 587795,
                metroGeoId: '20515',
                category: 'COMMERCIAL',
                type: 'RENT'
            });
            expect(
                route.match('/vsevolozhsk/snyat/kommercheskaya-nedvizhimost/metro-prospekt-bolshevikov/karta/')
            ).toEqual({
                rgid: 417867,
                metroGeoId: '20317',
                category: 'COMMERCIAL',
                type: 'RENT'
            });
        });
    });

    describe('subLocality', function() {
        it('should build a semantic url with `subLocality` param', function() {
            expect(route.build({
                subLocality: '193300',
                subLocalityName: 'Дорогомилово',
                subLocalityType: 'CITY_DISTRICT'
            })).toBe('/moskva/snyat/kommercheskaya-nedvizhimost/dist-dorogomilovo-193300/karta/');
            expect(route.build({
                subLocality: [ '193310', '193317' ]
            })).toBe('/moskva/snyat/kommercheskaya-nedvizhimost/karta/?subLocality=193310&subLocality=193317');
            expect(route.build({
                subLocality: '193310'
            })).toBe('/moskva/snyat/kommercheskaya-nedvizhimost/karta/?subLocality=193310');
        });

        it('should get `subLocality` param from a semantic url', function() {
            expect(
                route.match('/moskva/snyat/kommercheskaya-nedvizhimost/dist-dorogomilovo-193300/karta/')
            ).toEqual({
                rgid: 587795,
                subLocality: '193300',
                subLocalityName: 'dorogomilovo-193300',
                category: 'COMMERCIAL',
                type: 'RENT'
            });
            // eslint-disable-next-line max-len
            expect(route.match('/moskva/snyat/kommercheskaya-nedvizhimost/karta/?subLocality=2686&subLocality=193310')).toEqual({
                rgid: 587795,
                category: 'COMMERCIAL',
                type: 'RENT',
                subLocality: [ '2686', '193310' ]
            });
            expect(route.match('/moskva/snyat/kommercheskaya-nedvizhimost/karta/?subLocality=2686')).toEqual({
                rgid: 587795,
                category: 'COMMERCIAL',
                type: 'RENT',
                subLocality: '2686'
            });
        });
    });
});

describe('streets', function() {
    const route = router.getRouteByName('streets');

    describe('default params', function() {
        it('should build a semantic url with default params', function() {
            expect(route.build()).toBe('/moskva/streets/');
        });

        it('should get default params from root semantic url', function() {
            expect(route.match('/moskva/streets/')).toEqual({
                rgid: 587795,
            });
        });
    });

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                rgid: 587795
            })).toBe('/moskva/streets/');

            expect(route.build({
                rgid: 417899
            })).toBe('/sankt-peterburg/streets/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/moskva/streets/')).toEqual({
                rgid: 587795
            });
            expect(route.match('/sankt-peterburg/streets/')).toEqual({
                rgid: 417899
            });
        });
    });
});

describe('street', function() {
    const route = router.getRouteByName('street');

    it('should build a semantic url with `rgid` and `streetId` params', function() {
        expect(route.build({
            rgid: 587795,
            streetId: 'street-123'
        })).toBe('/moskva/street/street-123/');

        expect(route.build({
            rgid: 417899,
            streetId: 'street-123'
        })).toBe('/sankt-peterburg/street/street-123/');
    });

    it('should get `rgid` and `streetId` params from a semantic url', function() {
        expect(route.match('/moskva/street/street-123/')).toEqual({
            rgid: 587795,
            streetId: 'street-123'
        });
        expect(route.match('/sankt-peterburg/street/street-123/')).toEqual({
            rgid: 417899,
            streetId: 'street-123'
        });
    });
});

describe('metro-stations', function() {
    const route = router.getRouteByName('metro-stations');

    describe('default params', function() {
        it('should build a semantic url with default params', function() {
            expect(route.build()).toBe('/moskva/metro-stations/');
        });

        it('should get default params from root semantic url', function() {
            expect(route.match('/moskva/metro-stations/')).toEqual({
                rgid: 587795,
            });
        });
    });

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                rgid: 587795
            })).toBe('/moskva/metro-stations/');

            expect(route.build({
                rgid: 417899
            })).toBe('/sankt-peterburg/metro-stations/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/moskva/metro-stations/')).toEqual({
                rgid: 587795
            });
            expect(route.match('/sankt-peterburg/metro-stations/')).toEqual({
                rgid: 417899
            });
        });
    });
});

describe('metro-station', function() {
    const route = router.getRouteByName('metro-station');

    it('should build a semantic url with `rgid` and `metroId` params', function() {
        expect(route.build({
            rgid: 587795,
            metroId: 'some-metro-id'
        })).toBe('/moskva/metro-station/some-metro-id/');

        expect(route.build({
            rgid: 417899,
            metroId: 'some-metro-id'
        })).toBe('/sankt-peterburg/metro-station/some-metro-id/');
    });

    it('should get `rgid` and `metroId` params from a semantic url', function() {
        expect(route.match('/moskva/metro-station/some-metro-id/')).toEqual({
            rgid: 587795,
            metroId: 'some-metro-id'
        });
        expect(route.match('/sankt-peterburg/metro-station/some-metro-id/')).toEqual({
            rgid: 417899,
            metroId: 'some-metro-id'
        });
    });
});

describe('offers-archive', function() {
    const route = router.getRouteByName('offers-archive');

    describe('default params', function() {
        it('should build a semantic url with default params', function() {
            expect(route.build()).toBe('/otsenka-kvartiry-po-adresu-onlayn/');
        });

        it('should get empty params from root semantic url', function() {
            expect(route.match('/otsenka-kvartiry-po-adresu-onlayn/')).toEqual({});
        });
    });

    describe('address', function() {
        it('should build a semantic url with `address` param', function() {
            expect(route.build({
                address: 'some-address'
            })).toBe('/otsenka-kvartiry-po-adresu-onlayn/some-address/');
        });

        it('should get `address` param from a semantic url', function() {
            expect(route.match('/otsenka-kvartiry-po-adresu-onlayn/some-address/')).toEqual({
                address: 'some-address'
            });
        });
    });

    describe('type and category', function() {
        it('should build a semantic url with `type` and `category` params', function() {
            expect(route.build({
                address: 'some-address',
                offerType: 'RENT',
                offerCategory: 'APARTMENT',
            })).toBe('/otsenka-kvartiry-po-adresu-onlayn/some-address/snyat/kvartira/');
        });

        it('should get `type` and `category` params from a semantic url', function() {
            expect(route.match('/otsenka-kvartiry-po-adresu-onlayn/some-address/kupit/kvartira/')).toEqual({
                address: 'some-address',
                offerType: 'SELL',
                offerCategory: 'APARTMENT',
            });
        });
    });

    describe('page', function() {
        it('should build a semantic url with `page` param', function() {
            expect(route.build({
                page: 2,
                address: 'some-address',
                offerType: 'RENT',
                offerCategory: 'APARTMENT',
            })).toBe('/otsenka-kvartiry-po-adresu-onlayn/some-address/snyat/kvartira/2/');
        });

        it('should get `page` param from a semantic url', function() {
            expect(route.match('/otsenka-kvartiry-po-adresu-onlayn/some-address/kupit/kvartira/8/')).toEqual({
                address: 'some-address',
                offerType: 'SELL',
                offerCategory: 'APARTMENT',
                page: '8'
            });
        });
    });
});

describe('documents', function() {
    const route = router.getRouteByName('documents');

    describe('default params', function() {
        it('should build a semantic url with default params', function() {
            expect(route.build()).toBe('/dokumenty/');
        });

        it('should get empty params from root semantic url', function() {
            expect(route.match('/dokumenty/')).toEqual({ type: 'RENT' });
        });
    });

    describe('type', function() {
        it('should build a semantic url with `type` param', function() {
            expect(route.build({
                type: 'RENT',
            })).toBe('/dokumenty/arenda/');
        });

        it('should get `type` param from a semantic url', function() {
            expect(route.match('/dokumenty/arenda/')).toEqual({
                type: 'RENT',
            });
        });
    });

    describe('documentType', function() {
        it('should build a semantic url with `documentType` param', function() {
            expect(route.build({
                type: 'RENT',
                documentType: 'RENT_CONTRACT',
            })).toBe('/dokumenty/arenda/dogovor-nayma-zhilogo-pomeshcheniya/');
        });

        it('should get `documentType` param from a semantic url', function() {
            expect(route.match('/dokumenty/arenda/dogovor-nayma-zhilogo-pomeshcheniya/')).toEqual({
                documentType: 'RENT_CONTRACT',
                type: 'RENT'
            });
        });
    });
});

describe('village-search', function() {
    const route = router.getRouteByName('village-search');

    describe('default params', function() {
        it('should build a semantic url with default params', function() {
            expect(route.build()).toBe('/moskva/kupit/kottedzhnye-poselki/');
        });

        it('should get default params from root semantic url', function() {
            expect(route.match('/moskva/kupit/kottedzhnye-poselki/')).toEqual({
                rgid: 587795,
                type: 'SELL'
            });
        });
    });

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                rgid: 587795
            })).toBe('/moskva/kupit/kottedzhnye-poselki/');

            expect(route.build({
                rgid: 417899
            })).toBe('/sankt-peterburg/kupit/kottedzhnye-poselki/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/kottedzhnye-poselki/')).toEqual({
                rgid: 587795,
                type: 'SELL'
            });
            expect(route.match('/sankt-peterburg/kupit/kottedzhnye-poselki/')).toEqual({
                rgid: 417899,
                type: 'SELL'
            });
        });
    });

    describe('type', function() {
        it('should build a semantic url with `type` param', function() {
            expect(route.build({
                type: 'SELL'
            })).toBe('/moskva/kupit/kottedzhnye-poselki/');
        });

        it('should get `type` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/kottedzhnye-poselki/')).toEqual({
                rgid: 587795,
                type: 'SELL'
            });
        });
    });

    describe('direction', function() {
        it('should build a semantic url with `direction` param', function() {
            expect(route.build({
                type: 'SELL',
                direction: 35
            })).toBe('/moskva/kupit/kottedzhnye-poselki/shosse-rublyovskoe/');
        });

        it('should get `type` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/kottedzhnye-poselki/shosse-rublyovskoe/')).toEqual({
                rgid: 587795,
                type: 'SELL',
                direction: '35'
            });
        });
    });
});

describe('village-map', function() {
    const route = router.getRouteByName('village-map');

    describe('default params', function() {
        it('should build a semantic url with default params', function() {
            expect(route.build()).toBe('/moskva/kupit/kottedzhnye-poselki/karta/');
        });

        it('should get default params from root semantic url', function() {
            expect(route.match('/moskva/kupit/kottedzhnye-poselki/karta/')).toEqual({
                rgid: 587795,
                type: 'SELL'
            });
        });
    });

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                rgid: 587795
            })).toBe('/moskva/kupit/kottedzhnye-poselki/karta/');

            expect(route.build({
                rgid: 417899
            })).toBe('/sankt-peterburg/kupit/kottedzhnye-poselki/karta/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/kottedzhnye-poselki/karta/')).toEqual({
                rgid: 587795,
                type: 'SELL'
            });
            expect(route.match('/sankt-peterburg/kupit/kottedzhnye-poselki/karta/')).toEqual({
                rgid: 417899,
                type: 'SELL'
            });
        });
    });
    describe('subLocality', function() {
        it('should build a semantic url with `subLocality` param', function() {
            expect(route.build({
                subLocality: '193300',
                subLocalityName: 'Дорогомилово',
                subLocalityType: 'CITY_DISTRICT'
            })).toBe('/moskva/kupit/kottedzhnye-poselki/dist-dorogomilovo-193300/karta/');
            expect(route.build({
                subLocality: [ '2686', '193310' ]
            })).toBe('/moskva/kupit/kottedzhnye-poselki/karta/?subLocality=2686&subLocality=193310');
            expect(route.build({
                subLocality: '17379566'
            })).toBe('/moskva/kupit/kottedzhnye-poselki/karta/?subLocality=17379566');
        });

        it('should get `subLocality` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/kottedzhnye-poselki/dist-dorogomilovo-193300/karta/')).toEqual({
                rgid: 587795,
                type: 'SELL',
                subLocality: '193300',
                subLocalityName: 'dorogomilovo-193300'
            });
            expect(route.match('/moskva/kupit/kottedzhnye-poselki/karta/?subLocality=2686&subLocality=193310')).toEqual({
                rgid: 587795,
                type: 'SELL',
                subLocality: [ '2686', '193310' ]
            });
            expect(route.match('/moskva/kupit/kottedzhnye-poselki/karta/?subLocality=2686')).toEqual({
                rgid: 587795,
                type: 'SELL',
                subLocality: '2686'
            });
        });
    });
});

describe('village', function() {
    const route = router.getRouteByName('village');

    describe('default params', function() {
        it('should build a semantic url with default params', function() {
            expect(route.build({
                villageName: 'поселок-123',
            })).toBe('/moskva/kupit/kottedzhnye-poselki/poselok-123/');
        });

        it('should build a semantic url with name wich inlcudes id and get param id', function() {
            expect(route.build({
                villageName: 'poselok-777',
                id: '777'
            })).toBe('/moskva/kupit/kottedzhnye-poselki/poselok-777/');
        });

        it('should build a semantic url with default params and get param id', function() {
            expect(route.build({
                villageName: 'poselok',
                id: '777'
            })).toBe('/moskva/kupit/kottedzhnye-poselki/poselok-777/');
        });

        it('should get default params from root semantic url', function() {
            expect(route.match('/moskva/kupit/kottedzhnye-poselki/poselok-123/')).toEqual({
                rgid: 587795,
                type: 'SELL',
                villageName: 'poselok-123',
                id: '123'
            });
        });
    });

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                villageName: 'поселок-123',
                rgid: '587795'
            })).toBe('/moskva/kupit/kottedzhnye-poselki/poselok-123/');

            expect(route.build({
                villageName: 'поселок-123',
                rgid: '417899'
            })).toBe('/sankt-peterburg/kupit/kottedzhnye-poselki/poselok-123/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/kottedzhnye-poselki/poselok-123/')).toEqual({
                rgid: 587795,
                type: 'SELL',
                villageName: 'poselok-123',
                id: '123'
            });
            expect(route.match('/sankt-peterburg/kupit/kottedzhnye-poselki/poselok-123/')).toEqual({
                rgid: 417899,
                type: 'SELL',
                villageName: 'poselok-123',
                id: '123'
            });
        });
    });
});

describe('developer', function() {
    const route = router.getRouteByName('developer');

    describe('default params', function() {
        it('should build a semantic url with default params', function() {
            expect(route.build({
                developerName: 'строитель-123',
            })).toBe('/moskva/zastroyschik/stroitel-123/');
        });

        it('should get default params from root semantic url', function() {
            expect(route.match('/moskva/zastroyschik/stroitel-123/')).toEqual({
                rgid: 587795,
                developerName: 'stroitel-123',
                developerId: '123'
            });
        });
    });

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                developerName: 'строитель-123',
                rgid: '587795'
            })).toBe('/moskva/zastroyschik/stroitel-123/');

            expect(route.build({
                developerName: 'строитель-123',
                rgid: '417899'
            })).toBe('/sankt-peterburg/zastroyschik/stroitel-123/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/moskva/zastroyschik/stroitel-123/')).toEqual({
                rgid: 587795,
                developerName: 'stroitel-123',
                developerId: '123'
            });
            expect(route.match('/sankt-peterburg/zastroyschik/stroitel-123/')).toEqual({
                rgid: 417899,
                developerName: 'stroitel-123',
                developerId: '123'
            });
        });
    });
});

describe('developers-list', function() {
    const route = router.getRouteByName('developers-list');

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                rgid: '587795'
            })).toBe('/moskva/zastroyschiki/');

            expect(route.build({
                rgid: '417899'
            })).toBe('/sankt-peterburg/zastroyschiki/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/moskva/zastroyschiki/')).toEqual({
                rgid: 587795,
            });
            expect(route.match('/sankt-peterburg/zastroyschiki/')).toEqual({
                rgid: 417899,
            });
        });
    });

    describe('buildingClass', function() {
        it('should build a semantic url with `buildingClass` param', function() {
            expect(route.build({
                rgid: '587795',
                buildingClass: 'ECONOM'
            })).toBe('/moskva/zastroyschiki/klass-econom/');
            expect(route.build({
                rgid: '587795',
                buildingClass: 'COMFORT'
            })).toBe('/moskva/zastroyschiki/klass-comfort/');
            expect(route.build({
                rgid: '587795',
                buildingClass: 'COMFORT_PLUS'
            })).toBe('/moskva/zastroyschiki/klass-comfort-plus/');
            expect(route.build({
                rgid: '587795',
                buildingClass: 'BUSINESS'
            })).toBe('/moskva/zastroyschiki/klass-business/');
            expect(route.build({
                rgid: '587795',
                buildingClass: 'ELITE'
            })).toBe('/moskva/zastroyschiki/klass-elite/');
        });

        it('should get `buidingClass` param from a semantic url', function() {
            expect(route.match('/moskva/zastroyschiki/klass-econom/')).toEqual({
                rgid: 587795,
                buildingClass: 'ECONOM'
            });
            expect(route.match('/moskva/zastroyschiki/klass-comfort/')).toEqual({
                rgid: 587795,
                buildingClass: 'COMFORT'
            });
            expect(route.match('/moskva/zastroyschiki/klass-comfort-plus/')).toEqual({
                rgid: 587795,
                buildingClass: 'COMFORT_PLUS'
            });
            expect(route.match('/moskva/zastroyschiki/klass-business/')).toEqual({
                rgid: 587795,
                buildingClass: 'BUSINESS'
            });
            expect(route.match('/moskva/zastroyschiki/klass-elite/')).toEqual({
                rgid: 587795,
                buildingClass: 'ELITE'
            });
        });
    });
});

describe('profile', function() {
    const route = router.getRouteByName('profile');

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                profileUserType: 'AGENT',
                profileName: 'Простой агент',
                profileUid: '123',
                rgid: '417899'
            })).toBe('/sankt-peterburg/agenti/123/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/sankt-peterburg/agenti/123/')).toEqual({
                rgid: 417899,
                profileUserType: 'AGENT',
                profileName: '123',
                profileUid: '123',
            });
        });
    });

    describe('profileUserType', function() {
        it('should build a semantic url with `profileUserType` param', function() {
            expect(route.build({
                profileUserType: 'AGENT',
                profileName: 'Простой агент',
                profileUid: '123',
                rgid: '417899'
            })).toBe('/sankt-peterburg/agenti/123/');

            expect(route.build({
                profileUserType: 'AGENCY',
                profileName: 'агенство',
                profileUid: '123',
                rgid: '417899'
            })).toBe('/sankt-peterburg/agentstva/agenstvo-123/');
        });

        it('should get `profileUserType` param from a semantic url', function() {
            expect(route.match('/sankt-peterburg/agenti/123/')).toEqual({
                rgid: 417899,
                profileUserType: 'AGENT',
                profileName: '123',
                profileUid: '123',
            });

            expect(route.match('/sankt-peterburg/agentstva/agenstvo-123/')).toEqual({
                profileName: 'agenstvo-123',
                profileUid: '123',
                profileUserType: 'AGENCY',
                rgid: 417899,
                transliteratedAgencyName: 'agenstvo',
            });
        });
    });
});

describe('profile-search', function() {
    const route = router.getRouteByName('profile-search');

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                profileUserType: 'AGENT',
                rgid: '417899'
            })).toBe('/sankt-peterburg/agenti/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/sankt-peterburg/agenti/')).toEqual({
                rgid: 417899,
                profileUserType: 'AGENT',
            });
        });
    });

    describe('profileUserType', function() {
        it('should build a semantic url with `profileUserType` param', function() {
            expect(route.build({
                profileUserType: 'AGENT',
                rgid: '417899'
            })).toBe('/sankt-peterburg/agenti/');

            expect(route.build({
                profileUserType: 'AGENCY',
                rgid: '417899'
            })).toBe('/sankt-peterburg/agentstva/');
        });

        it('should get `profileUserType` param from a semantic url', function() {
            expect(route.match('/sankt-peterburg/agenti/')).toEqual({
                rgid: 417899,
                profileUserType: 'AGENT',
            });

            expect(route.match('/sankt-peterburg/agentstva/')).toEqual({
                profileUserType: 'AGENCY',
                rgid: 417899,
            });
        });
    });
});

describe('mortgage-calculator', function() {
    const route = router.getRouteByName('mortgage-calculator');

    describe('default params', function() {
        it('should build a semantic url with default params', function() {
            expect(route.build()).toBe('/moskva/ipoteka/calculator/');
        });

        it('should get empty params from root semantic url', function() {
            expect(route.match('/moskva/ipoteka/calculator/')).toEqual({
                rgid: 587795
            });
        });
    });

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                rgid: '417899'
            })).toBe('/sankt-peterburg/ipoteka/calculator/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/sankt-peterburg/ipoteka/calculator/')).toEqual({
                rgid: 417899,
            });
        });
    });
});

describe('mortgage-search', function() {
    const route = router.getRouteByName('mortgage-search');

    describe('default params', function() {
        it('should build a semantic url with default params', function() {
            expect(route.build()).toBe('/moskva/ipoteka/');
        });

        it('should get empty params from root semantic url', function() {
            expect(route.match('/moskva/ipoteka/')).toEqual({
                rgid: 587795
            });
        });
    });

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                rgid: '417899'
            })).toBe('/sankt-peterburg/ipoteka/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/sankt-peterburg/ipoteka/')).toEqual({
                rgid: 417899,
            });
        });
    });
});

describe('mortgage-bank', function() {
    const route = router.getRouteByName('mortgage-bank');

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                rgid: 587795,
                mortgageBankId: 666,
                mortgageBankName: 'bank',
            })).toBe('/moskva/ipoteka/bank-666/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/moskva/ipoteka/bank-666/')).toEqual({
                rgid: 587795,
                mortgageBankId: '666',
                mortgageBankName: 'bank-666',
            });
        });
    });
});

describe('mortgage-program', function() {
    const route = router.getRouteByName('mortgage-program');

    describe('mortgageBankName and mortgageProgramName', function() {
        it('should build a semantic url with `mortgageBankName` and `mortgageProgramName` params', function() {
            expect(route.build({
                mortgageBankId: 666,
                mortgageBankName: 'bank',
                mortgageProgramId: 12345,
                mortgageProgramName: 'возьми ипотеку'
            })).toBe('/ipoteka/bank-666/vozmi-ipoteku-12345/');
        });

        it('should get `mortgageBankName` and `mortgageProgramName` params from a semantic url', function() {
            expect(route.match('/ipoteka/bank-666/vozmi-ipoteku-12345/')).toEqual({
                mortgageBankId: '666',
                mortgageBankName: 'bank-666',
                mortgageProgramId: '12345',
                mortgageProgramName: 'vozmi-ipoteku-12345',
            });
        });
    });
});

describe('404', function() {
    const route = router.getRouteByName('404');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/404/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/404/')).toEqual({});
    });
});

describe('newbuilding-awards', function() {
    const route = router.getRouteByName('newbuilding-awards');

    it('should build a semantic url', function() {
        expect(route.build({
            awardsName: 'movemsk'
        })).toBe('/movemsk/');
    });

    it('should get awardsName param from a semantic url', function() {
        expect(route.match('/movemsk/')).toEqual({
            awardsName: 'movemsk',
        });
    });
});

describe('favorites', function() {
    const route = router.getRouteByName('favorites');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/favorites/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/favorites/')).toEqual({});
    });
});

describe('favorites-map', function() {
    const route = router.getRouteByName('favorites-map');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/favorites/karta/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/favorites/karta/')).toEqual({});
    });
});

describe('shared-favorites', function() {
    const route = router.getRouteByName('shared-favorites');

    it('should build a semantic url', function() {
        expect(route.build({
            sharedLink: 'some-link'
        })).toBe('/shared-favorites/some-link/');
    });

    it('should get sharedLink param from a semantic url', function() {
        expect(route.match('/shared-favorites/some-link/')).toEqual({
            sharedLink: 'some-link'
        });
    });
});

describe('comparison', function() {
    const route = router.getRouteByName('comparison');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/comparison/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/comparison/')).toEqual({});
    });
});

describe('widget-site-offers', function() {
    const route = router.getRouteByName('widget-site-offers');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/widget-site-offers/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/widget-site-offers/')).toEqual({});
    });
});

describe('promocodes', function() {
    const route = router.getRouteByName('promocodes');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/promocodes/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/promocodes/')).toEqual({});
    });
});

describe('wallet', function() {
    const route = router.getRouteByName('wallet');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/wallet/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/wallet/')).toEqual({});
    });
});

describe('offer', function() {
    const route = router.getRouteByName('offer');

    it('should build a semantic url', function() {
        expect(route.build({
            id: 12345789
        })).toBe('/offer/12345789/');
    });

    it('should get params from a semantic url', function() {
        expect(route.match('/offer/12345789/')).toEqual({
            id: '12345789'
        });
    });
});

describe('alfabank', function() {
    const route = router.getRouteByName('alfabank');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/alfabank/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/alfabank/')).toEqual({});
    });
});

describe('gate-geoselector', function() {
    const route = router.getRouteByName('gate-geoselector');

    it('should build a semantic url', function() {
        expect(route.build({
            action: 'suggest'
        })).toBe('/gate/geoselector/suggest/');
    });

    it('should get action param from a semantic url', function() {
        expect(route.match('/gate/geoselector/suggest/')).toEqual({
            action: 'suggest'
        });
    });
});

describe('gate', function() {
    const route = router.getRouteByName('gate');

    it('should build a semantic url', function() {
        expect(route.build({
            controller: 'some-controller',
            action: 'some-action'
        })).toBe('/gate/some-controller/some-action/');
    });

    it('should get params from a semantic url', function() {
        expect(route.match('/gate/some-controller/some-action/')).toEqual({
            controller: 'some-controller',
            action: 'some-action'
        });
    });
});

describe('dashboard', function() {
    const route = router.getRouteByName('dashboard');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/management-new/dashboard/');
    });

    it('should build a semantic url with popup', function() {
        expect(route.build({
            popup: 'clients'
        })).toBe('/management-new/dashboard/popup/clients/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/management-new/dashboard/')).toEqual({});
    });

    it('should get popup param from a semantic url', function() {
        expect(route.match('/management-new/dashboard/popup/clients/')).toEqual({
            popup: 'clients'
        });
    });

    it('should not match with wrong popup', function() {
        expect(route.match('/management-new/dashboard/popup/some-popup/')).toEqual(null);
    });
});

describe('finances', function() {
    const route = router.getRouteByName('finances');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/management-new/finances/');
    });

    it('should build a semantic url with popup', function() {
        expect(route.build({
            popup: 'wallet'
        })).toBe('/management-new/finances/popup/wallet/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/management-new/finances/')).toEqual({});
    });

    it('should get popup param from a semantic url', function() {
        expect(route.match('/management-new/finances/popup/wallet/')).toEqual({
            popup: 'wallet'
        });
    });

    it('should not match with wrong popup', function() {
        expect(route.match('/management-new/finances/popup/some-popup/')).toEqual(null);
    });
});

describe('settings', function() {
    const route = router.getRouteByName('settings');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/management-new/settings/');
    });

    it('should build a semantic url with settingsTab', function() {
        expect(route.build({
            settingsTab: 'billing'
        })).toBe('/management-new/settings/billing/');
    });

    it('should build a semantic url with settingsTab and popup', function() {
        expect(route.build({
            settingsTab: 'billing',
            popup: 'clients'
        })).toBe('/management-new/settings/billing/popup/clients/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/management-new/settings/')).toEqual({});
    });

    it('should get settingsTab and popup params from a semantic url', function() {
        expect(route.match('/management-new/settings/billing/popup/clients/')).toEqual({
            settingsTab: 'billing',
            popup: 'clients'
        });
    });

    it('should not match with wrong settingsTab', function() {
        expect(route.match('/management-new/settings/some-settings-tab/popup/clients/')).toEqual(null);
    });
});

describe('add-feed', function() {
    const route = router.getRouteByName('add-feed');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/management-new/feeds/add/');
    });

    it('should build a semantic url with popup', function() {
        expect(route.build({
            popup: 'wallet'
        })).toBe('/management-new/feeds/add/popup/wallet/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/management-new/feeds/add/')).toEqual({});
    });

    it('should get popup params from a semantic url', function() {
        expect(route.match('/management-new/feeds/add/popup/wallet/')).toEqual({
            popup: 'wallet'
        });
    });

    it('should not match with wrong popup', function() {
        expect(route.match('/management-new/feeds/add/popup/some-popup/')).toEqual(null);
    });
});

describe('edit-feed', function() {
    const route = router.getRouteByName('edit-feed');

    it('should build a semantic url', function() {
        expect(route.build({
            feedId: 1234,
            popup: 'clients'
        })).toBe('/management-new/feeds/edit/1234/popup/clients/');
    });

    it('should get feedId and  param from a semantic url', function() {
        expect(route.match('/management-new/feeds/edit/1234/')).toEqual({
            feedId: '1234'
        });
    });

    it('should get feedId and popup params from a semantic url', function() {
        expect(route.match('/management-new/feeds/edit/1234/popup/wallet/')).toEqual({
            feedId: '1234',
            popup: 'wallet'
        });
    });

    it('should not match with wrong feedId', function() {
        expect(route.match('/management-new/feeds/edit/incorrect-feedId/popup/wallet/')).toEqual(null);
    });
});

describe('feeds', function() {
    const route = router.getRouteByName('feeds');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/management-new/feeds/');
    });

    it('should build a semantic url with feedId', function() {
        expect(route.build({
            feedId: 1234,
        })).toBe('/management-new/feeds/1234/errors/');
    });

    it('should build a semantic url with feedId and popup', function() {
        expect(route.build({
            feedId: 1234,
            popup: 'wallet'
        })).toBe('/management-new/feeds/1234/errors/popup/wallet/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/management-new/feeds/')).toEqual({});
    });

    it('should get feedId and popup params from a semantic url', function() {
        expect(route.match('/management-new/feeds/1234/errors/popup/wallet/')).toEqual({
            feedId: '1234',
            popup: 'wallet'
        });
    });
});

describe('management-new', function() {
    const route = router.getRouteByName('management-new');

    it('should build a semantic url', function() {
        expect(route.build({
            popup: 'promotion-raising'
        })).toBe('/management-new/popup/promotion-raising/');
    });

    it('should get params from a semantic url', function() {
        expect(route.match('/management-new/popup/promotion-raising/')).toEqual({
            popup: 'promotion-raising'
        });
    });
});

describe('management-new-add', function() {
    const route = router.getRouteByName('management-new-add');

    it('should build a semantic url', function() {
        expect(route.build({
            mode: 'edit',
            id: 12345,
            popup: 'juridical-wallet-refill'
        })).toBe('/management-new/edit/12345/popup/juridical-wallet-refill/');
    });

    it('should get params from a semantic url', function() {
        expect(route.match('/management-new/add/12345/popup/juridical-wallet-refill/')).toEqual({
            mode: 'add',
            id: '12345',
            popup: 'juridical-wallet-refill'
        });
    });
});

describe('partner', function() {
    const route = router.getRouteByName('partner');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/management-new/partner/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/management-new/partner/')).toEqual({});
    });
});

describe('profsearch', function() {
    const route = router.getRouteByName('profsearch');

    it('should build a semantic url', function() {
        expect(route.build({
            popup: 'clients'
        })).toBe('/management-new/search/popup/clients/');
    });

    it('should get params from a semantic url', function() {
        expect(route.match('/management-new/search/popup/clients/')).toEqual({
            popup: 'clients'
        });
    });
});

describe('clients', function() {
    const route = router.getRouteByName('clients');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/management-new/clients/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/management-new/clients/')).toEqual({});
    });
});

describe('promocodes-new', function() {
    const route = router.getRouteByName('promocodes-new');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/management-new/promocodes/');
    });

    it('should build a semantic url with popup', function() {
        expect(route.build({
            popup: 'clients'
        })).toBe('/management-new/promocodes/popup/clients/');
    });

    it('should get params from a semantic url', function() {
        expect(route.match('/management-new/promocodes/popup/clients/')).toEqual({
            popup: 'clients'
        });
    });
});

describe('management-new-gate', function() {
    const route = router.getRouteByName('management-new-gate');

    it('should build a semantic url', function() {
        expect(route.build({
            controller: 'some-controller',
            action: 'some-action',
        })).toBe('/management-new/gate/some-controller/some-action/');
    });

    it('should get params from a semantic url', function() {
        expect(route.match('/management-new/gate/some-controller/some-action/')).toEqual({
            controller: 'some-controller',
            action: 'some-action',
        });
    });
});

describe('callcenter-management', function() {
    const route = router.getRouteByName('callcenter-management');

    it('should build a semantic url', function() {
        expect(route.build({
            userCode: 'user-123',
            popup: 'wallet'
        })).toBe('/management-new/cc/user-123/popup/wallet/');
    });

    it('should get params from a semantic url', function() {
        expect(route.match('/management-new/cc/user-123/popup/wallet/')).toEqual({
            userCode: 'user-123',
            popup: 'wallet'
        });
    });
});

describe('tariffs', function() {
    const route = router.getRouteByName('tariffs');

    it('should build a semantic url', function() {
        expect(route.build({
            popup: 'wallet'
        })).toBe('/management-new/tariffs/popup/wallet/');
    });

    it('should get params from a semantic url', function() {
        expect(route.match('/management-new/tariffs/popup/wallet/')).toEqual({
            popup: 'wallet'
        });
    });
});

describe('egrn-reports', function() {
    const route = router.getRouteByName('egrn-reports');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/management-new/egrn-reports/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/management-new/egrn-reports/')).toEqual({});
    });
});

describe('callcenter-redirect', function() {
    const route = router.getRouteByName('callcenter-redirect');

    it('should build a semantic url', function() {
        expect(route.build({
            userCode: 'user-123'
        })).toBe('/lk/user-123/');
    });

    it('should get userCode param from a semantic url', function() {
        expect(route.match('/lk/user-123/')).toEqual({
            userCode: 'user-123'
        });
    });
});

describe('promotion', function() {
    const route = router.getRouteByName('promotion');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/promotion/');
    });

    it('should build a semantic url with type', function() {
        expect(route.build({
            type: 'newbuilding'
        })).toBe('/promotion/newbuilding/');
    });

    it('should get params from a semantic url', function() {
        expect(route.match('/promotion/')).toEqual({});
    });

    it('should get type param from a semantic url', function() {
        expect(route.match('/promotion/secondaryAndCommercial/')).toEqual({
            type: 'secondaryAndCommercial'
        });
    });

    it('should not match with wrong type', function() {
        expect(route.match('/promotion/wrong-type/')).toEqual(null);
    });
});

describe('ya-deal-valuation', function() {
    const route = router.getRouteByName('ya-deal-valuation');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/calculator-stoimosti/');
    });

    it('should build a semantic url with flatType', function() {
        expect(route.build({
            flatType: 'trekhkomnatnaya'
        })).toBe('/calculator-stoimosti/kvartira/trekhkomnatnaya/');
    });

    it('should get params from a semantic url', function() {
        expect(route.match('/calculator-stoimosti/')).toEqual({});
    });

    it('should get flatType param from a semantic url', function() {
        expect(route.match('/calculator-stoimosti/kvartira/trekhkomnatnaya/')).toEqual({
            flatType: 'trekhkomnatnaya'
        });
    });
});

describe('blog', function() {
    const route = router.getRouteByName('blog');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/spravochnik');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/spravochnik')).toEqual({});
    });
});

describe('blog-post', function() {
    const route = router.getRouteByName('blog-post');

    it('should build a semantic url', function() {
        expect(route.build({
            postId: 'post-123'
        })).toBe('/spravochnik/post-123');
    });

    it('should get params from a semantic url', function() {
        expect(route.match('/spravochnik/post-123')).toEqual({
            postId: 'post-123'
        });
    });
});

describe('journal', function() {
    const route = router.getRouteByName('journal');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/journal/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/journal/')).toEqual({});
    });
});

describe('journal-post', function() {
    const route = router.getRouteByName('journal-post');

    it('should build a semantic url', function() {
        expect(route.build({
            postId: 'post-123'
        })).toBe('/journal/post/post-123/');
    });

    it('should get params from a semantic url', function() {
        expect(route.match('/journal/post/post-123/')).toEqual({
            postId: 'post-123'
        });
    });
});

describe('journal-tag', function() {
    const route = router.getRouteByName('journal-tag');

    it('should build a semantic url', function() {
        expect(route.build({
            tagId: 'tag-123'
        })).toBe('/journal/tag/tag-123/');
    });

    it('should get params from a semantic url', function() {
        expect(route.match('/journal/tag/tag-123/')).toEqual({
            tagId: 'tag-123'
        });
    });
});

describe('journal-category', function() {
    const route = router.getRouteByName('journal-category');

    it('should build a semantic url', function() {
        expect(route.build({
            categoryId: 'some-category'
        })).toBe('/journal/category/some-category/');
    });

    it('should get params from a semantic url', function() {
        expect(route.match('/journal/category/some-category/')).toEqual({
            categoryId: 'some-category'
        });
    });
});

describe('journal-gate', function() {
    const route = router.getRouteByName('journal-gate');

    it('should build a semantic url', function() {
        expect(route.build({
            controller: 'some-controller',
            action: 'some-action'
        })).toBe('/journal/gate/some-controller/some-action/');
    });

    it('should get params from a semantic url', function() {
        expect(route.match('/journal/gate/some-controller/some-action/')).toEqual({
            controller: 'some-controller',
            action: 'some-action'
        });
    });
});

describe('facebook', function() {
    const route = router.getRouteByName('facebook');

    it('should build a semantic url', function() {
        expect(route.build({
            action: 'app'
        })).toBe('/extra/facebook/app/');
    });

    it('should get params from a semantic url', function() {
        expect(route.match('/extra/facebook/app/')).toEqual({
            action: 'app'
        });
    });
});

describe('app-prelanding', function() {
    const route = router.getRouteByName('app-prelanding');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/app/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/app/')).toEqual({});
    });
});

describe('egrn-paid-report', function() {
    const route = router.getRouteByName('egrn-paid-report');

    it('should build a semantic url', function() {
        expect(route.build({
            paidReportId: 'report-111'
        })).toBe('/egrn-report/report-111/');
    });

    it('should get params from a semantic url', function() {
        expect(route.match('/egrn-report/report-111/')).toEqual({
            paidReportId: 'report-111'
        });
    });
});

describe('egrn-address-purchase', function() {
    const route = router.getRouteByName('egrn-address-purchase');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/proverka-kvartiry/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/proverka-kvartiry/')).toEqual({});
    });
});

describe('subscriptions', function() {
    const route = router.getRouteByName('subscriptions');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/subscriptions/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/subscriptions/')).toEqual({});
    });
});

describe('payment', function() {
    const route = router.getRouteByName('payment');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/payment/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/payment/')).toEqual({});
    });
});

describe('partner-devchats', function() {
    const route = router.getRouteByName('partner-devchats');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/partner/chats/');
    });

    it('should build a semantic url with clientId', function() {
        expect(route.build({
            clientId: 1234
        })).toBe('/partner/chats/1234/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/partner/chats/')).toEqual({});
    });

    it('should get params from a semantic url', function() {
        expect(route.match('/partner/chats/1234/')).toEqual({
            clientId: '1234'
        });
    });
});

describe('partner-gate', function() {
    const route = router.getRouteByName('partner-gate');

    it('should build a semantic url', function() {
        expect(route.build({
            controller: 'some-controller',
            action: 'some-action'
        })).toBe('/partner/gate/some-controller/some-action/');
    });

    it('should get params from a semantic url', function() {
        expect(route.match('/partner/gate/some-controller/some-action/')).toEqual({
            controller: 'some-controller',
            action: 'some-action'
        });
    });
});
