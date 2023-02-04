/* eslint-disable jest/no-commented-out-tests */
const router = require('../touch-phone');

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
                siteName: 'название'
            })).toBe('/moskva/kupit/novostrojka/nazvanie/');
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
                siteName: 'название',
                typeCode: 'kupit',
                rgid: '587795'
            })).toBe('/moskva/kupit/novostrojka/nazvanie/');

            expect(route.build({
                siteName: 'название',
                typeCode: 'kupit',
                rgid: '417899'
            })).toBe('/sankt-peterburg/kupit/novostrojka/nazvanie/');
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

            expect(route.build({
                category: 'HOUSE'
            })).toBe('/moskva/kupit/dom/');

            expect(route.build({
                category: 'LOT'
            })).toBe('/moskva/kupit/uchastok/');

            expect(route.build({
                category: 'COMMERCIAL'
            })).toBe('/moskva/kupit/kommercheskaya-nedvizhimost/');
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

            expect(route.match('/moskva/kupit/dom/')).toEqual({
                category: 'HOUSE',
                rgid: 587795,
                type: 'SELL'
            });

            expect(route.match('/moskva/kupit/uchastok/')).toEqual({
                category: 'LOT',
                rgid: 587795,
                type: 'SELL'
            });

            expect(route.match('/moskva/kupit/kommercheskaya-nedvizhimost/')).toEqual({
                category: 'COMMERCIAL',
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
                subLocality: '17379566'
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

    describe('commercialType', function() {
        it('should build a semantic url with `commercialType` param', function() {
            expect(route.build({
                category: 'COMMERCIAL',
                commercialType: 'OFFICE'
            })).toBe('/moskva/kupit/kommercheskaya-nedvizhimost/ofis/');

            expect(route.build({
                category: 'COMMERCIAL',
                commercialType: 'FREE_PURPOSE',
                type: 'RENT'
            })).toBe(
                '/moskva/snyat/kommercheskaya-nedvizhimost/pomeshchenie-svobodnogo-naznacheniya/'
            );
        });

        it('should get `commercialType` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/kommercheskaya-nedvizhimost/ofis/')).toEqual({
                category: 'COMMERCIAL',
                commercialType: 'OFFICE',
                rgid: 587795,
                type: 'SELL'
            });

            expect(route
                .match('/moskva/snyat/kommercheskaya-nedvizhimost/pomeshchenie-svobodnogo-naznacheniya/')).toEqual({
                category: 'COMMERCIAL',
                commercialType: 'FREE_PURPOSE',
                rgid: 587795,
                type: 'RENT'
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

describe('subscriptions', function() {
    const route = router.getRouteByName('subscriptions');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/subscriptions/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/subscriptions/')).toEqual({});
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

describe('search-history', function() {
    const route = router.getRouteByName('search-history');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/search-history/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/search-history/')).toEqual({});
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

describe('add-offer', function() {
    const route = router.getRouteByName('add-offer');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/management-new/add/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/management-new/add/')).toEqual({});
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

describe('app-prelanding', function() {
    const route = router.getRouteByName('app-prelanding');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/app/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/app/')).toEqual({});
    });
});

describe('offer-egrn-report', function() {
    const route = router.getRouteByName('offer-egrn-report');

    it('should build a semantic url', function() {
        expect(route.build({
            id: '123'
        })).toBe('/offer-egrn-report/123/');
    });

    it('should get params from a semantic url', function() {
        expect(route.match('/offer-egrn-report/123/')).toEqual({
            id: '123'
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

describe('widget-site-offers', function() {
    const route = router.getRouteByName('widget-site-offers');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/widget-site-offers/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/widget-site-offers/')).toEqual({});
    });
});

describe('widget-site-reviews', function() {
    const route = router.getRouteByName('widget-site-reviews');

    it('should build a semantic url with `siteId` param', function() {
        expect(route.build({
            siteId: 12345,
        })).toBe('/widget-site-reviews/12345/');
    });

    it('should get `siteId` param from a semantic url', function() {
        expect(route.match('/widget-site-reviews/12345/')).toEqual({
            siteId: '12345',
        });
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

describe('404', function() {
    const route = router.getRouteByName('404');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/404/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/404/')).toEqual({});
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

describe('filters', function() {
    const route = router.getRouteByName('filters');

    it('should build a semantic url', function() {
        expect(route.build()).toBe('/filters/');
    });

    it('should get empty params from a semantic url', function() {
        expect(route.match('/filters/')).toEqual({});
    });
});

describe('offers-search-map', function() {
    const route = router.getRouteByName('offers-search-map');

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

describe('sites-search', function() {
    const route = router.getRouteByName('sites-search');

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
                subLocality: [ '2686', '193310' ]
            })).toBe('/moskva/kupit/novostrojka/?subLocality=2686&subLocality=193310');
            expect(route.build({
                subLocality: '17379566'
            })).toBe('/moskva/kupit/novostrojka/?subLocality=17379566');
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
});

describe('sites-search-map', function() {
    const route = router.getRouteByName('sites-search-map');

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
                subLocality: [ '2686', '193310' ]
            })).toBe('/moskva/kupit/novostrojka/karta/?subLocality=2686&subLocality=193310');
            expect(route.build({
                subLocality: '17379566'
            })).toBe('/moskva/kupit/novostrojka/karta/?subLocality=17379566');
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

describe('villages-search-map', function() {
    const route = router.getRouteByName('villages-search-map');

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
});

describe('village-offers-search', function() {
    const route = router.getRouteByName('village-offers-search');

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                rgid: 587795,
                villageName: 'деревня-курчатово'
            })).toBe('/moskva/kupit/kottedzhnye-poselki/derevnya-kurchatovo/objekty/');

            expect(route.build({
                rgid: 417899,
                villageName: 'деревня-курчатово'
            })).toBe('/sankt-peterburg/kupit/kottedzhnye-poselki/derevnya-kurchatovo/objekty/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/moskva/kupit/kottedzhnye-poselki/derevnya-kurchatovo/objekty/')).toEqual({
                rgid: 587795,
                type: 'SELL',
                villageName: 'derevnya-kurchatovo'
            });
            expect(route.match('/sankt-peterburg/kupit/kottedzhnye-poselki/derevnya-kurchatovo/objekty/')).toEqual({
                rgid: 417899,
                type: 'SELL',
                villageName: 'derevnya-kurchatovo'
            });
        });
    });
});

describe('villages-search', function() {
    const route = router.getRouteByName('villages-search');

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

describe('village', function() {
    const route = router.getRouteByName('village');

    describe('default params', function() {
        it('should build a semantic url with default params', function() {
            expect(route.build({
                villageName: 'поселок-123',
            })).toBe('/moskva/kupit/kottedzhnye-poselki/poselok-123/');
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

describe('districts', function() {
    const route = router.getRouteByName('districts');

    describe('default params', function() {
        it('should build a semantic url with default params', function() {
            expect(route.build()).toBe('/moskva/districts/');
        });

        it('should get default params from root semantic url', function() {
            expect(route.match('/moskva/districts/')).toEqual({
                rgid: 587795,
            });
        });
    });

    describe('rgid', function() {
        it('should build a semantic url with `rgid` param', function() {
            expect(route.build({
                rgid: 587795,
            })).toBe('/moskva/districts/');

            expect(route.build({
                rgid: 417899,
            })).toBe('/sankt-peterburg/districts/');
        });

        it('should get `rgid` param from a semantic url', function() {
            expect(route.match('/moskva/districts/')).toEqual({
                rgid: 587795,
            });
            expect(route.match('/sankt-peterburg/districts/')).toEqual({
                rgid: 417899,
            });
        });
    });
});

describe('district', function() {
    const route = router.getRouteByName('district');

    it('should build a semantic url with `rgid` and `metroId` params', function() {
        expect(route.build({
            rgid: 587795,
            districtId: 'some-district-id'
        })).toBe('/moskva/district/some-district-id/');

        expect(route.build({
            rgid: 417899,
            districtId: 'some-district-id'
        })).toBe('/sankt-peterburg/district/some-district-id/');
    });

    it('should get `rgid` and `districtId` params from a semantic url', function() {
        expect(route.match('/moskva/district/some-district-id/')).toEqual({
            rgid: 587795,
            districtId: 'some-district-id'
        });
        expect(route.match('/sankt-peterburg/district/some-district-id/')).toEqual({
            rgid: 417899,
            districtId: 'some-district-id'
        });
    });
});
