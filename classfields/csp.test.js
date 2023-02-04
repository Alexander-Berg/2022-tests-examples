jest.mock('auto-core/lib/csp', () => {
    return {
        NONE: '\'none\'',
        SELF: '\'self\'',
        UNSAFE_INLINE: '\'unsafe-inline\'',
        UNSAFE_EVAL: '\'unsafe-eval\'',
        reportUri: jest.fn(() => {
            return 'https://csp-report.yandex.ru/';
        }),
        nonce: () => `'nonce-bGV0IHRoZSBnYWxheHkgYnVybg=='`,
    };
});

jest.mock('auto-core/appConfig', () => {
    return {
        envProd: true,
    };
});

const {
    preparePolicy,
    stringifyHeader,
} = require('./csp');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
let reqMock;
beforeEach(() => {
    reqMock = createHttpReq();
});

describe('stringifyHeader', () => {
    const TEST = [
        {
            'in': {
                'default-src': [ 'self' ],
                'connect-src': [ '*.yandex.net', '*.yandex.ru' ],
            },
            out: 'default-src self;connect-src *.yandex.net *.yandex.ru',
        },
    ];

    TEST.forEach((test, index) => {
        it(`stringifyHeader должен превращать объект в строку #${ index }`, () => {
            expect(stringifyHeader(test.in)).toEqual(test.out);
        });
    });
});

describe('preparePolicy', () => {
    beforeEach(() => {
        reqMock.router.route.getData.mockReturnValue({
            controller: 'listing',
        });
    });

    afterEach(() => {
        reqMock.router.route.getData.mockReset();
        reqMock.router.route.getName.mockReset();
    });

    it('стандартный preparePolicy', () => {
        expect(preparePolicy(reqMock)).toMatchSnapshot();
    });

    it('report-only preparePolicy', () => {
        expect(preparePolicy(reqMock, true)).toMatchSnapshot();
    });

    it('стандартный preparePolicy для billing', () => {
        reqMock.router.route.getData.mockReturnValue({
            controller: 'billing',
        });
        reqMock.router.route.getName.mockReturnValue('billing');

        expect(preparePolicy(reqMock)).toMatchSnapshot();
    });

    it('стандартный preparePolicy для статей журнала', () => {
        reqMock.router.route.getName.mockReturnValue('mag-article');

        expect(preparePolicy(reqMock)).toMatchSnapshot();
    });

    it('стандартный preparePolicy для страниц журнала', () => {
        reqMock.router.route.getName.mockReturnValue('mag-index');
        reqMock.router.route.getData.mockImplementation(jest.fn(() => ({
            controller: 'fake-controller',
        })));

        expect(preparePolicy(reqMock)).toMatchSnapshot();
    });

    it('стандартный preparePolicy для страниц гаража', () => {
        reqMock.router.route.getName.mockReturnValue('garage');
        reqMock.router.route.getData.mockImplementation(jest.fn(() => ({
            controller: 'fake-controller',
        })));

        expect(preparePolicy(reqMock)).toMatchSnapshot();
    });

    it('стандартный preparePolicy для промо ОСАГО', () => {
        reqMock.router.route.getName.mockReturnValue('osago-promo');
        reqMock.router.route.getData.mockImplementation(jest.fn(() => ({
            controller: 'fake-controller',
        })));

        expect(preparePolicy(reqMock)).toMatchSnapshot();
    });

    it('стандартный preparePolicy для моих кредитов', () => {
        reqMock.router.route.getName.mockReturnValue('my-credits');
        reqMock.router.route.getData.mockImplementation(jest.fn(() => ({
            controller: 'fake-controller',
        })));

        expect(preparePolicy(reqMock)).toMatchSnapshot();
    });
});
