const doRequest = require('../../../lib/doRequest');
jest.mock('../../../lib/doRequest', () => jest.fn());

const runTests = require('./runTests');

const INCORRECT_SERVICE_NAME = 'gf-amp';
const CORRECT_SERVICE_NAME = 'gf-desktop';

const SAMPLE_PARAMS = [ 'foo=bar' ];

const URLS_MAP = new Map([
    [ 'gf-desktop', [
        'https://hub.vertis.yandex.net/v0/teamcity/build?buildTypeId=Verticals_Testing_General_WebTests&branch=master',
    ] ],
]);

it('не должен вызвать doRequest, если нет урлов для этого сервиса', () => {
    runTests(URLS_MAP, INCORRECT_SERVICE_NAME, SAMPLE_PARAMS);
    expect(doRequest).toHaveBeenCalledTimes(0);
});

it('должен вызвать doRequest и передать в урл параметры', () => {
    runTests(URLS_MAP, CORRECT_SERVICE_NAME, SAMPLE_PARAMS);
    // eslint-disable-next-line max-len
    expect(doRequest.mock.calls[0][0]).toBe('https://hub.vertis.yandex.net/v0/teamcity/build?buildTypeId=Verticals_Testing_General_WebTests&branch=master&foo=bar');
});
