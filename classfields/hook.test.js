const autoruHandler = require('./autoru/handler');
const generalHandler = require('./general/handler');
const backendApiHandler = require('./backend-api/handler');

jest.mock('./autoru/handler', () => jest.fn());
jest.mock('./general/handler', () => jest.fn());
jest.mock('./backend-api/handler', () => jest.fn());

const callbackMock = jest.fn();

const hook = require('./hook');

afterEach(() => {
    jest.clearAllMocks();
});

it('должен вызвать callback и не вызывать хэндлеры, если не тот сервис', () => {
    const callMock = {
        request: {
            deployment: {
                state: 'SUCCESS',
                service_name: 'unknown',
            },
        },
        metadata: {
            getMap: () => ({
                headers: {},
            }),
        },
    };
    hook(callMock, callbackMock);
    expect(callbackMock).toHaveBeenCalledWith(null, {});
    expect(autoruHandler).toHaveBeenCalledTimes(0);
    expect(generalHandler).toHaveBeenCalledTimes(0);
});

it('должен вызвать callback и не вызывать хэндлеры, если не статус не success', () => {
    const callMock = {
        request: {
            deployment: {
                state: 'IN_PROGRESS',
                service_name: 'af-desktop',
            },
        },
        metadata: {
            getMap: () => ({
                headers: {},
            }),
        },
    };
    hook(callMock, callbackMock);
    expect(callbackMock).toHaveBeenCalledWith(null, {});
    expect(autoruHandler).toHaveBeenCalledTimes(0);
    expect(generalHandler).toHaveBeenCalledTimes(0);
});

it('должен вызвать хэндлер автору для сервисов автору', () => {
    const callMock = {
        request: {
            deployment: {
                state: 'SUCCESS',
                service_name: 'af-desktop',
            },
        },
        metadata: {
            getMap: () => ({
                headers: {},
            }),
        },
    };
    hook(callMock, callbackMock);
    expect(callbackMock).toHaveBeenCalledTimes(0);
    expect(autoruHandler).toHaveBeenCalled();
});

it('должен вызвать хэндлер объявлений для сервисов объявлений', () => {
    const callMock = {
        request: {
            deployment: {
                state: 'SUCCESS',
                service_name: 'gf-desktop',
            },
        },
        metadata: {
            getMap: () => ({
                headers: {},
            }),
        },
    };
    hook(callMock, callbackMock);
    expect(callbackMock).toHaveBeenCalledTimes(0);
    expect(generalHandler).toHaveBeenCalled();
});

it('должен вызвать хэндлер бекенда для сервиса гараж апи', () => {
    const callMock = {
        request: {
            deployment: {
                state: 'SUCCESS',
                service_name: 'garage-api',
            },
        },
        metadata: {
            getMap: () => ({
                headers: {},
            }),
        },
    };
    hook(callMock, callbackMock);
    expect(callbackMock).toHaveBeenCalledTimes(0);
    expect(backendApiHandler).toHaveBeenCalled();
});

it('должен вызвать хэндлер бекенда для сервиса апи отзывных', () => {
    const callMock = {
        request: {
            deployment: {
                state: 'SUCCESS',
                service_name: 'recalls-api',
            },
        },
        metadata: {
            getMap: () => ({
                headers: {},
            }),
        },
    };
    hook(callMock, callbackMock);
    expect(callbackMock).toHaveBeenCalledTimes(0);
    expect(backendApiHandler).toHaveBeenCalled();
});
