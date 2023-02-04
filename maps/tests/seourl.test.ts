import url from 'url';
import {startCase} from 'lodash-es';
import transformSeoUrl from 'server/seourl/transform-seo-url';
import {BasicRequest} from 'types/request';
import {SearchResponse} from 'search/types/search-response';
import {MasstransitLine} from 'masstransit/types/line';

interface TestDescription {
    requestUrl: string;
    responseUrl: string;
    fakeSearchResponse?: unknown;
    fakeMasstransitLine?: unknown;
}

const TESTS: Record<string, TestDescription> = {
    shortcutMoscowTraffic: {
        requestUrl: 'https://yandex.ru/maps/moscow_traffic',
        responseUrl: 'https://yandex.ru/maps/?ll=37.620393%2C55.753960&z=10&l=trf%2Ctrfe'
    },
    shortcutMoscow: {
        requestUrl: 'https://yandex.ru/maps/moscow',
        responseUrl: 'https://yandex.ru/maps/?ll=37.620393%2C55.753960&z=10'
    },
    shortcutTraffic: {
        requestUrl: 'https://yandex.ru/maps/traffic',
        responseUrl: 'https://yandex.ru/maps/?l=trf%2Ctrfe'
    },
    layerTraffic: {
        requestUrl: 'https://yandex.ru/maps/213/moscow/probki/',
        responseUrl: 'https://yandex.ru/maps/?ll=37.620393%2C55.753960&z=10&l=trf%2Ctrfe'
    },
    layerRoutes: {
        requestUrl: 'https://yandex.ru/maps/213/moscow/routes/',
        responseUrl: 'https://yandex.ru/maps/?ll=37.620393%2C55.753960&z=10&rtt=auto&rtext='
    },
    layerPanorama: {
        requestUrl: 'https://yandex.ru/maps/213/moscow/panorama/',
        responseUrl: 'https://yandex.ru/maps/?ll=37.620393%2C55.753960&z=10&l=stv%2Csta'
    },
    layerTransport: {
        requestUrl: 'https://yandex.ru/maps/213/moscow/transport/',
        responseUrl: 'https://yandex.ru/maps/?ll=37.620393%2C55.753960&z=10&l=masstransit'
    },
    layerPhoto: {
        requestUrl: 'https://yandex.ru/maps/213/moscow/photo/',
        responseUrl: 'https://yandex.ru/maps/?ll=37.620393%2C55.753960&z=10&l=pht'
    },
    layerSputnik: {
        requestUrl: 'https://yandex.ru/maps/213/moscow/sputnik/',
        responseUrl: 'https://yandex.ru/maps/?ll=37.620393%2C55.753960&z=10&l=sat'
    },
    layerHybrid: {
        requestUrl: 'https://yandex.ru/maps/213/moscow/hybrid/',
        responseUrl: 'https://yandex.ru/maps/?ll=37.620393%2C55.753960&z=10&l=sat%2Cskl'
    },
    rubric: {
        requestUrl: 'https://yandex.ru/maps/213/moscow/category/cafe/',
        responseUrl: 'https://yandex.ru/maps/?ll=37.620393%2C55.753960&z=10&text=rubric%3A%28cafe%29&display-text=cafe',
        fakeSearchResponse: {}
    },
    rubricWithId: {
        requestUrl: 'https://yandex.ru/maps/213/moscow/category/pub_bar/184106384/',
        responseUrl:
            'https://yandex.ru/maps/?ll=37.620393%2C55.753960&z=10&text=category_id%3A%28184106384%29&display-text=Bar',
        fakeSearchResponse: {requestRubric: {name: 'Bar'}}
    },
    rubricWithoutResponse: {
        requestUrl: 'https://yandex.ru/maps/213/moscow/category/pub_bar/184106384/',
        responseUrl:
            'https://yandex.ru/maps/?ll=37.620393%2C55.753960&z=10&text=category_id%3A%28184106384%29&display-text=pub_bar',
        fakeSearchResponse: {}
    },
    house: {
        requestUrl: 'https://yandex.ru/maps/213/moscow/house/house_seoname/34.42323,45.23234/',
        responseUrl: 'https://yandex.ru/maps/?ll=37.620393%2C55.753960&z=10&text=45.232340%2C34.423230'
    },
    toponym: {
        requestUrl: 'https://yandex.ru/maps/geo/reutov/53063504/',
        responseUrl: 'https://yandex.ru/maps/?ol=geo&ouri=ymapsbm1%3A%2F%2Fgeo%3Foid%3D53063504',
        fakeSearchResponse: {items: [{uri: 'ymapsbm1://geo?oid=53063504'}]}
    },
    toponymWithRegion: {
        requestUrl: 'https://yandex.ru/maps/213/moscow/geo/rayon_khamovniki/53211698/',
        responseUrl: 'https://yandex.ru/maps/?ol=geo&ouri=ymapsbm1%3A%2F%2Fgeo%3Foid%3D53211698',
        fakeSearchResponse: {items: [{uri: 'ymapsbm1://geo?oid=53211698'}]}
    },
    orgpage: {
        requestUrl: 'https://yandex.ru/maps/org/seoname_org/23423434/',
        responseUrl: 'https://yandex.ru/maps/?ol=biz&oid=23423434',
        fakeSearchResponse: {items: [{}]}
    },
    orgpageGallery: {
        requestUrl: 'https://yandex.ru/maps/org/seoname_org/23423434/gallery/',
        responseUrl: 'https://yandex.ru/maps/?ol=biz&oid=23423434&photos%5Bbusiness%5D=23423434',
        fakeSearchResponse: {items: [{}]}
    },
    orgpageOnline: {
        requestUrl: 'https://yandex.ru/maps/org/seoname_org/777/',
        responseUrl: 'yandexmaps://open_url?url=https%3A%2F%2Fyandex.ru%2Fweb-maps%2Forg%2Fseoname_org%2F777%2F',
        fakeSearchResponse: {items: [{onlineProperties: {}}]}
    },
    feedback: {
        requestUrl: 'https://yandex.ru/maps/feedback/',
        responseUrl: 'https://yandex.ru/maps/?feedback='
    },
    routes: {
        requestUrl:
            'https://yandex.ru/maps/213/moscow/routes/bus_B/796d617073626d313a2f2f7472616e7369742f6c696e653f69643d32303336393235373136266c6c3d33372e36323130393225324335352e373531343737266e616d653d25443025393126723d3235383026747970653d627573/',
        responseUrl: 'https://yandex.ru/maps/?ll=37.620393%2C55.753960&z=10&masstransit%5BrouteId%5D=2036925716',
        fakeMasstransitLine: {activeThread: {properties: {ThreadMetaData: {lineId: 2036925716}}}}
    },
    stops: {
        requestUrl: 'https://yandex.ru/maps/213/moscow/stops/12345678/',
        responseUrl: 'https://yandex.ru/maps/?ll=37.620393%2C55.753960&z=10&masstransit%5BstopId%5D=12345678'
    },
    transports: {
        requestUrl: 'https://yandex.ru/maps/213/moscow/transport/buses/',
        responseUrl:
            'https://yandex.ru/maps/?ll=37.620393%2C55.753960&z=10&l=masstransit&masstransit%5BvehicleType%5D=buses'
    },
    chain: {
        requestUrl: 'https://yandex.ru/maps/213/moscow/chain/sberbank/6003612/',
        responseUrl:
            'https://yandex.ru/maps/?ll=37.620393%2C55.753960&z=10&text=chain_id%3A%286003612%29&display-text=Sberbank',
        fakeSearchResponse: {chain: {name: 'Sberbank'}}
    },
    chainWithoutResponse: {
        requestUrl: 'https://yandex.ru/maps/213/moscow/chain/sberbank/6003612/',
        responseUrl:
            'https://yandex.ru/maps/?ll=37.620393%2C55.753960&z=10&text=chain_id%3A%286003612%29&display-text=sberbank',
        fakeSearchResponse: {}
    },
    search: {
        requestUrl: 'https://yandex.ru/maps/213/moscow/search/Еда/',
        responseUrl: 'https://yandex.ru/maps/?ll=37.620393%2C55.753960&z=10&text=%D0%95%D0%B4%D0%B0'
    },
    searchEncoded: {
        requestUrl: 'https://yandex.ru/maps/213/moscow/search/%D0%95%D0%B4%D0%B0/',
        responseUrl: 'https://yandex.ru/maps/?ll=37.620393%2C55.753960&z=10&text=%D0%95%D0%B4%D0%B0'
    },
    parking: {
        requestUrl: 'https://yandex.ru/maps/parking',
        responseUrl: 'https://yandex.ru/maps/?l=carparks&z=15'
    },
    appmetrica: {
        requestUrl:
            'https://4.redirect.appmetrica.yandex.tld/serve/1033597222417082058?rtt=mt&rtext=55.734044,37.589508~55.763699,37.604991',
        responseUrl: 'https://yandex.tld/maps/?rtt=mt&rtext=55.734044%2C37.589508~55.763699%2C37.604991'
    },
    appmetrica2: {
        requestUrl:
            'https://4.redirect.appmetrica.yandex.com/maps.yandex.ru/?l=map,trf&appmetrica_tracking_id=1033819187316812386',
        responseUrl: 'https://yandex.com/maps/?l=map%2Ctrf&appmetrica_tracking_id=1033819187316812386'
    },
    unsupported: {
        requestUrl:
            'https://maps.yandex.ru/?text=cafe&source=wizbiz_new_map_single&z=14&ll=37.604991,55.763699&oid=1018907821&ol=biz',
        responseUrl:
            'https://yandex.ru/maps/?text=cafe&source=wizbiz_new_map_single&z=14&ll=37.604991%2C55.763699&oid=1018907821&ol=biz'
    },
    oldDomain: {
        requestUrl: 'https://maps.yandex.ru/213/moscow/',
        responseUrl: 'https://yandex.ru/maps/?ll=37.620393%2C55.753960&z=10'
    },
    haritaOldDomain: {
        requestUrl: 'https://harita.yandex.com.tr/213/moscow/',
        responseUrl: 'https://yandex.com.tr/harita/?ll=37.620393%2C55.753960&z=10'
    },
    covidMap: {
        requestUrl: 'https://yandex.ru/maps/covid19',
        responseUrl: 'yandexmaps://open_url?url=https%3A%2F%2Fyandex.ru%2Fweb-maps%2Fcovid19'
    },
    actual: {
        requestUrl: 'https://yandex.ru/maps/actual',
        responseUrl: 'yandexmaps://open_url?url=https%3A%2F%2Fyandex.ru%2Fweb-maps%2Factual'
    },
    tinyUrlOnlyQuery: {
        requestUrl: 'https://yandex.ru/maps/-/onlyQuery?mode=search&z=15',
        responseUrl: 'https://yandex.ru/maps/?ll=37.597937%2C55.764190&z=15&text=rubric%3A%28cafe%29&mode=search'
    },
    tinyUrlWithPath: {
        requestUrl: 'https://yandex.ru/maps/-/withPath',
        responseUrl: 'https://yandex.ru/maps/?ol=biz&oid=123123',
        fakeSearchResponse: {items: [{}]}
    },
    mrc: {
        requestUrl: 'https://yandex.ru/maps/213/moscow/?ll=37.6,55.7&z=17&mrc[id]=218004061',
        responseUrl:
            'yandexmaps://open_url?url=https%3A%2F%2Fyandex.ru%2Fweb-maps%2F213%2Fmoscow%2F%3Fll%3D37.6%2C55.7%26z%3D17%26mrc%5Bid%5D%3D218004061'
    },
    usermaps: {
        requestUrl: 'https://yandex.ru/maps/213/moscow/?um=constructor:123',
        responseUrl:
            'yandexmaps://open_url?url=https%3A%2F%2Fyandex.ru%2Fweb-maps%2F213%2Fmoscow%2F%3Fum%3Dconstructor%3A123'
    },
    externalLanding: {
        requestUrl: 'https://yandex.ru/maps/navi/promo',
        responseUrl: 'yandexmaps://open_url?url=https%3A%2F%2Fyandex.ru%2Fweb-maps%2Fnavi%2Fpromo'
    }
};

describe('Transform SEO url', () => {
    Object.entries(TESTS).forEach(([testName, test]) => {
        it(startCase(testName), async () => {
            const tld = test.requestUrl.includes('yandex.com.tr') ? 'tr' : 'ru';
            const transformedUrl = await transformSeoUrl({tld} as BasicRequest, encodeURIComponent(test.requestUrl), {
                getLongUrlFromToken,
                fetchSearchResults: () => getFakeSearchResponse(test.fakeSearchResponse),
                fetchMasstransitLine: () => getFakeMasstransitLine(test.fakeMasstransitLine)
            });
            expect(transformedUrl).toEqual(test.responseUrl);
        });
    });
});

async function getLongUrlFromToken(token: string): Promise<url.URL> {
    let pathname = '/maps';
    switch (token) {
        case 'onlyQuery':
            pathname = '/maps/?ll=37.597937,55.764190&z=16&text=rubric:(cafe)';
            break;
        case 'withPath':
            pathname = '/maps/org/123123';
    }
    return new url.URL('http://yandex.ru' + pathname);
}

async function getFakeSearchResponse(response: unknown): Promise<SearchResponse> {
    return response as SearchResponse;
}

async function getFakeMasstransitLine(line?: unknown): Promise<MasstransitLine | null> {
    if (!line) {
        return null;
    }

    return line as MasstransitLine;
}
