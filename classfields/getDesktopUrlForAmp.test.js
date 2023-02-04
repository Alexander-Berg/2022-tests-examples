jest.mock('auto-core/lib/core/isAmpApp', () => ({
    'default': () => true,
}));

const getDesktopUrl = require('./getDesktopUrl');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');

let req;
beforeEach(() => {
    req = createHttpReq();
});

describe('AMP', () => {
    it('должен преобразовать amp урл "/moskva/amp/cars/bmw/all/" в десктопный', () => {
        req.geoAlias = 'moskva';
        req.geoIds = [ 213 ];
        req.router.params = {
            category: 'cars',
            mark: 'bmw',
            section: 'all',
            geo_id: 213,
        };
        req.urlWithoutRegion = '/amp/cars/bmw/all/';

        expect(
            getDesktopUrl(req, { nomobile: true }),
        ).toEqual('https://autoru_frontend.base_domain/moskva/cars/bmw/all/?nomobile=true');
    });
});
