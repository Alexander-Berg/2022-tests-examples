const runTests = require('./helpers/runTests');
const handler = require('./handler');

jest.mock('./helpers/runTests', () => jest.fn());

const callback = jest.fn();
const logger = {
    info: jest.fn(),
};

const runAllPromises = () => {
    return new Promise((resolve) => {
        setImmediate(() => {
            resolve(undefined);
        });
    });
};


afterEach(() => {
    jest.clearAllMocks();
});

describe('если нет хука в метаданных', () => {
    const callMock = {
        request: {
            deployment: {
                userMetadata: '',
            },
        },
    };
    it('должен вызвать колбэк и залогировать отсутствие хука', () => {
        handler(callMock, callback, logger);
        expect(callback).toHaveBeenCalledWith(null, {});
        expect(logger.info).toHaveBeenCalledWith({
            _context: 'response',
            message: 'no hook',
        });
        expect(runTests).toHaveBeenCalledTimes(0);
    });
});

describe('если есть хук в метаданных', () => {
    const callMock = {
        request: {
            deployment: {
                userMetadata: 'DEPLOY_HOOK autotest',
            },
        },
    };

    it('должен вызвать runTests, колбэк и залогировать успешное срабатывание', async() => {
        runTests.mockImplementation(() => Promise.resolve());
        handler(callMock, callback, logger);
        expect(callback).toHaveBeenCalledTimes(0);
        expect(logger.info).toHaveBeenCalledTimes(0);

        expect(runTests).toHaveBeenCalledTimes(1);

        await(runAllPromises());
        expect(callback).toHaveBeenCalledWith(null, {});
        expect(logger.info).toHaveBeenCalledWith({
            _context: 'response',
            message: 'hook handled',
        });
    });

    it('должен вызвать runTests, колбэк и залогировать неуспешное срабатывание', async() => {
        runTests.mockImplementation(() => Promise.reject('some error'));
        handler(callMock, callback, logger);
        expect(callback).toHaveBeenCalledTimes(0);
        expect(logger.info).toHaveBeenCalledTimes(0);

        expect(runTests).toHaveBeenCalledTimes(1);

        await(runAllPromises());
        expect(callback).toHaveBeenCalledWith('some error');
        expect(logger.info).toHaveBeenCalledWith({
            _context: 'response',
            message: 'hook failed "some error"',
        });
    });

});

describe(('для нескольких хуков'), () => {
    const callMock = {
        request: {
            deployment: {
                userMetadata: 'DEPLOY_HOOK autotest; DEPLOY_HOOK perfomance-test param=value;',
                service_name: 'gf-desktop',
            },
        },
    };
    it('должен вызвать runTests с правильными параметрами для каждого из них', async() => {
        runTests.mockImplementation(() => Promise.resolve());
        handler(callMock, callback, logger);

        expect(runTests).toHaveBeenCalledTimes(2);
        expect(runTests.mock.calls).toEqual([
            [ new Map([
                [ 'gf-desktop', [
                    'https://hub.vertis.yandex.net/v0/teamcity/build?buildTypeId=Verticals_Testing_General_Desktop_RunAllDesktopTests&branch=' ] ],
                [ 'gf-mobile', [
                    'https://hub.vertis.yandex.net/v0/teamcity/build?buildTypeId=Verticals_Testing_General_Mobile_RunAllMobileTests&branch=' ] ],
            ]), 'gf-desktop', [] ],
            [ new Map([
                [ 'gf-desktop', [
                    'https://hub.vertis.yandex.net/v0/teamcity/build?buildTypeId=Verticals_Testing_Speed_OYandex_TestRelease_Desktop_Run&branch=' ] ],
                [ 'gf-mobile', [
                    'https://hub.vertis.yandex.net/v0/teamcity/build?buildTypeId=Verticals_Testing_Speed_OYandex_TestRelease_Mobile_Run&branch=' ] ],
            ]), 'gf-desktop', [ 'param=value' ] ],
        ]);
    });

    it('должен вызвать успешный коллбэк и логгер один раз', async() => {
        runTests.mockImplementation(() => Promise.resolve());
        handler(callMock, callback, logger);
        expect(callback).toHaveBeenCalledTimes(0);
        expect(logger.info).toHaveBeenCalledTimes(0);

        await(runAllPromises());
        expect(callback).toHaveBeenCalledTimes(1);
        expect(callback).toHaveBeenCalledWith(null, {});

        expect(logger.info).toHaveBeenCalledTimes(1);
        expect(logger.info).toHaveBeenCalledWith({
            _context: 'response',
            message: 'hook handled',
        });
    });

    it('должен вызвать коллбэк и логгер с ошибкой один раз', async() => {
        runTests.mockImplementation(() => Promise.reject('some error'));
        handler(callMock, callback, logger);
        expect(callback).toHaveBeenCalledTimes(0);
        expect(logger.info).toHaveBeenCalledTimes(0);

        await(runAllPromises());

        expect(callback).toHaveBeenCalledTimes(1);
        expect(callback).toHaveBeenCalledWith('some error');

        expect(logger.info).toHaveBeenCalledTimes(1);
        expect(logger.info).toHaveBeenCalledWith({
            _context: 'response',
            message: 'hook failed "some error"',
        });
    });
});
