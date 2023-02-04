jest.mock('auto-core/lib/handledErrors/RedirectError', () => ({
    createErrorFromReq: (req, code, params) => params,
    CODES: {},
}));

const redirect = require('./redirect-from-old-compare');

const nextMock = jest.fn();
const resMock = {};
let reqMock;

beforeEach(() => {
    reqMock = {
        browser: { isMobile: true },
        router: {
            route: {
                getData: () => ({ controller: 'compare' }),
                getName: () => 'compare',
            },
            params: {
                content: 'offers',
                category: 'cars',
            },
        },
    };
});

it('не должен средиректить, если урл не относится к сравнению', () => {
    reqMock.router.route.getData = () => ({ controller: 'other' });
    reqMock.router.route.getName = () => 'other-page';
    redirect(reqMock, resMock, nextMock);

    expect(nextMock).toHaveBeenCalledWith();
    expect(nextMock).toHaveBeenCalledTimes(1);
});

describe('актуальный урл сравнения', () => {
    // https://autoru_frontend.base_domain/compare-offers/
    it('не должен средиректить, если это действующий урл сравнения без старых флагов', () => {
        redirect(reqMock, resMock, nextMock);

        expect(nextMock).toHaveBeenCalledWith();
        expect(nextMock).toHaveBeenCalledTimes(1);
    });

    // https://autoru_frontend.base_domain/compare-offers/?diffOnly=false&by=param&type=cars
    it('должен средиректить, если это действующий урл сравнения офферов, но со старыми флагом моделей', () => {
        reqMock.router.params.type = 'cars';
        redirect(reqMock, resMock, nextMock);

        expect(nextMock).toHaveBeenCalledWith({
            location: 'https://autoru_frontend.base_domain/compare-models/',
            status: 302,
        });
        expect(nextMock).toHaveBeenCalledTimes(1);
    });
});

describe('старое стравнение', () => {
    // https://auto.ru/compare/?diffOnly=false&by=param&type=ads
    it('должен средиректить со старого сравнения офферов на новый урл', () => {
        reqMock.router.route.getName = () => 'old-compare';
        reqMock.router.params = { type: 'ads' };
        redirect(reqMock, resMock, nextMock);

        expect(nextMock).toHaveBeenCalledWith({
            location: 'https://autoru_frontend.base_domain/compare-offers/',
            status: 301,
        });
        expect(nextMock).toHaveBeenCalledTimes(1);
    });

    // https://auto.ru/compare/?diffOnly=false&by=param&type=cars
    it('должен средиректить со старого сравнения моделей на новый урл', () => {
        reqMock.router.route.getName = () => 'old-compare';
        reqMock.router.params = { type: 'cars' };
        redirect(reqMock, resMock, nextMock);

        expect(nextMock).toHaveBeenCalledWith({
            location: 'https://autoru_frontend.base_domain/compare-models/',
            status: 301,
        });
        expect(nextMock).toHaveBeenCalledTimes(1);
    });

    // https://auto.ru/compare/
    it('должен средиректить со старого урла сравнения без флагов на новый урл сравнения офферов', () => {
        reqMock.router.route.getName = () => 'old-compare';
        reqMock.router.params = {};
        redirect(reqMock, resMock, nextMock);

        expect(nextMock).toHaveBeenCalledWith({
            location: 'https://autoru_frontend.base_domain/compare-offers/',
            status: 301,
        });
        expect(nextMock).toHaveBeenCalledTimes(1);
    });
});
