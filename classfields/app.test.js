jest.mock('@vertis/pino', () => {
    return {
        info: jest.fn(),
    }
});
jest.mock('@yandex-int/uatraits', () => {
    return {
        Detector: function() {
            return {
                detectByHeaders: jest.fn(() => ({ isBrowser: 'true', isRobot: 'false' })),
            };
        },
    }
});
jest.mock('./lib/isRealUserBrowser', () => jest.fn());
jest.mock('./lib/createRequestHandler', () => () => {});

const isRealUserBrowser = require('./lib/isRealUserBrowser');

let req;
let res;

beforeEach(() => {
    req = {
        headers: {},
    };
    res = {
        end: jest.fn(),
        setHeader: jest.fn(),
    };
});

const app = require('./index');

// бот по юзерагенту
// не бот по юзерагенту
// запрос за статикой

const URLS_FOR_TEST = [
    {
        url: '/.well-known/bla-bla',
        result: '1',
    },
    {
        url: '/favicon.ico',
        result: '1',
    },
    {
        url: '/ads.txt',
        result: '1',
    },
    {
        url: '/robots.txt',
        result: '1',
    },
    {
        url: '/apple-site-bla-bla',
        result: '1',
    },
    {
        url: '/google.bla-bla.html',
        result: '1',
    },
    {
        url: '/yandex.bla-bla.html',
        result: '1',
    },
    {
        url: '/sitemap-bla-bla',
        result: '1',
    },
    {
        url: '/some-bla-bla',
        result: '0',
    },
    {
        url: '/some-bla-bla/?everybodybecoolthisis=molly',
        result: '1',
    },
    {
        url: '/some-bla-bla/?smth=13&everybodybecoolthisis=crasher',
        result: '1',
    },
    {
        url: '/download-report/',
        result: '1',
    },
];

it('возвращает 1 для бота по UA', () => {
    res.end.mockImplementationOnce((result) => {
        expect(result).toEqual('1');
    });

    app(req, res);

    expect(res.end).toHaveBeenCalled();
});

it('возвращает 0 для НЕ бота по UA', () => {
    res.end.mockImplementationOnce((result) => {
        expect(result).toEqual('0');
    });

    isRealUserBrowser.mockImplementationOnce(() => true);

    app(req, res);

    expect(res.end).toHaveBeenCalled();
});

URLS_FOR_TEST.forEach((testConfig) => {
    const {
        url,
        result: expectedResult,
    } = testConfig;

    it(`для не бота по UA на запросе к ${ url } возвращает ${ expectedResult }`, () => {
        res.end.mockImplementationOnce((result) => {
            expect(result).toEqual(expectedResult);
        });

        isRealUserBrowser.mockImplementationOnce(() => true);
        req.headers['x-real-uri'] = url;

        app(req, res);

        expect(res.end).toHaveBeenCalled();
    });
});
