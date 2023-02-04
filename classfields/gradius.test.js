jest.mock('auto-core/lib/geobase-binding.js', () => {
    return {
        getRegionById: (rid) => {
            switch (rid) {
                case 66: return { id: 66, type: 6 };
                case 213: return { id: 213, type: 6 };
                case 225: return { id: 225, type: 3 };
                case 11318: return { id: 11318, type: 5 };
                default: return {};
            }
        },
        getParentsIds: (rid) => {
            switch (rid) {
                case 66: return [ 66, 121084, 11318, 59, 225, 10001, 10000 ];
                case 225: return [ 225, 10001, 10000 ];
                case 11318: return [ 11318, 59, 225, 10001, 10000 ];
                default: return [];
            }
        },
    };
});

const Graduis = require('auto-core/lib/cookie/gradius');

let geoIds;
let gradius;
let reqData;

beforeEach(() => {
    geoIds = [];

    reqData = {
        geoIds: geoIds,
        gradiusCookieValue: undefined,
        routerParams: { category: 'cars' },
        cookies: {},
    };

    gradius = new Graduis(reqData);
});

it('should return null if no regions', () => {
    expect(gradius.get()).toBeNull();
});

it('should return null if more than one region', () => {
    geoIds.push(1, 2);
    expect(gradius.get()).toBeNull();
});

it('should return null for country', () => {
    geoIds.push(225);
    expect(gradius.get()).toBeNull();
});

it('should return null for area', () => {
    geoIds.push(11318);
    expect(gradius.get()).toBeNull();
});

it('should return 100 if cookie gradius=100 exists', () => {
    reqData.routerParams.category = 'trucks';
    reqData.gradiusCookieValue = '100';
    geoIds.push(213);

    expect(gradius.get()).toEqual(100);
});

it('should return 100 if cookie gradius=100 exists (special regions)', () => {
    reqData.routerParams.category = 'trucks';
    reqData.gradiusCookieValue = '100';
    geoIds.push(66);

    expect(gradius.get()).toEqual(100);
});

it('should return value 0 from if cookie gradius=0 exists', () => {
    reqData.routerParams.category = 'trucks';
    reqData.gradiusCookieValue = '0';
    geoIds.push(213);

    expect(gradius.get()).toEqual(0);
});

it('should return 200 as default value for cars', () => {
    reqData.routerParams.category = 'cars';
    geoIds.push(213);

    expect(gradius.get()).toEqual(200);
});

it('should return 500 as default value for cars in Omsk', () => {
    reqData.routerParams.category = 'cars';
    geoIds.push(66);

    expect(gradius.get()).toEqual(500);
});

it('should return 500 as default radius for trucks', () => {
    reqData.routerParams.category = 'trucks';
    geoIds.push(213);
    expect(gradius.get()).toEqual(500);
});

it('should return 0 as default radius for dealers', () => {
    reqData.routerParams.category = 'cars';
    geoIds.push(213);

    expect(gradius.get({ type: 'dealers' })).toEqual(0);
});

it('should return 200 as default radius for light commercial vehicles', () => {
    reqData.routerParams.category = 'trucks';
    reqData.routerParams.trucks_category = 'lcv';
    geoIds.push(213);

    expect(gradius.get()).toEqual(200);
});

describe('cache', () => {
    it('should cache respect options.type', () => {
        gradius.get({ rid: [ 213 ] });
        const result = gradius.get({ rid: [ 213 ], type: 'dealers' });
        expect(result).toEqual(0);
    });
});
