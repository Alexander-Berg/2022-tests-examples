import { prepareHeadersForLog } from './prepareHeadersForLog';

it('не должен ничего делать, если нет заголовков', () => {
    expect(prepareHeadersForLog()).toEqual({});
});

it('не должен трогать оригинальные хедеры', () => {
    const headers = {
        cookie: 'Session_id=3:1594730293.5.1.1590746937316:EgABAAAAA',
        host: 'auto.ru',
    };
    const result = prepareHeadersForLog(headers);

    expect(headers.cookie).toBe('Session_id=3:1594730293.5.1.1590746937316:EgABAAAAA');
    // защита от дурака: обфускация и правда произошла
    expect(result.cookie).toBe('Session_id=3%3A1594730293.5.1.XXXXXXXXXXXXXXXXXXXXXXX');
});

describe('common', () => {
    it.each([
        [
            { cookie: 'foo=bar', host: 'auto.ru' },
            { cookie: 'foo=bar', host: 'auto.ru' },
        ],
        [
            { host: 'auto.ru' },
            { host: 'auto.ru' },
        ],
        // тест на OAuth
        [
            { authorization: 'OAuth AgAAAAAhdgllAAB7k5lqImAR0m0LJD01KRdN3z0' },
            { authorization: 'OAuth AgAAAAAhdgllAAB7XXXXXXXXXXXXXXXXXXXXXXX' },
        ],
        // тест на OAuth
        [
            { 'x-authorization': 'Vertis AgAAAAAhdgllAAB7k5lqImAR0m0LJD01KRdN3z0' },
            { 'x-authorization': 'Vertis AgAAAAAhdgllAAB7XXXXXXXXXXXXXXXXXXXXXXX' },
        ],
        // тест на Session_id
        [
            { cookie: 'Session_id=3%3A1594730293.5.1.1590746937316:EgABAAAAA', host: 'auto.ru' },
            { cookie: 'Session_id=3%3A1594730293.5.1.XXXXXXXXXXXXXXXXXXXXXXX', host: 'auto.ru' },
        ],
        // тест на sessionid2
        [
            { cookie: 'sessionid2=3%3A1594730293.5.1.1590746937316:EgABAAAAA', host: 'auto.ru' },
            { cookie: 'sessionid2=3%3A1594730293.5.1.XXXXXXXXXXXXXXXXXXXXXXX', host: 'auto.ru' },
        ],
        // тест на ya-client-cookie
        [
            { 'ya-client-cookie': 'sessionid2=3%3A1594730293.5.1.1590746937316:EgABAAAAA', host: 'o.yandex.ru' },
            { 'ya-client-cookie': 'sessionid2=3%3A1594730293.5.1.XXXXXXXXXXXXXXXXXXXXXXX', host: 'o.yandex.ru' },
        ],
    ])(`должен преобразовать %j в %j`, (headers, result) => {
        expect(prepareHeadersForLog(headers)).toEqual(result);
    });
});

describe('auto.ru', () => {
    it.each([
        [
            { cookie: 'autoru_sid=36201940%7C1563384608735.7776000' },
            { cookie: 'autoru_sid=36201940%7C1563384608735.XXXXXXX' },
        ],
        [
            { 'x-cookie': 'sessionid2=3:1594730293.5.1.1590746937316:EgABAAAAA' },
            { 'x-cookie': 'sessionid2=3%3A1594730293.5.1.XXXXXXXXXXXXXXXXXXXXXXX' },
        ],
        [
            { 'x-session-id': '36201940%7C1563384608735.7776000' },
            { 'x-session-id': '36201940%7C15633XXXXXXXXXXXXXXXX' },
        ],
        [
            { 'x-ya-user-ticket-vertis': 'eyJ0eXBlIjoiSldUIiwiYWxnIjoiUlM1MTIiLCJraWQiOiJhMmIyMjcwZS1jZTZhLTNjNjYtM2ViNi05OTBjMjMzMTVkMGMif' },
            { 'x-ya-user-ticket-vertis': 'eyJ0eXBlIjoiSldUIiwiYWxnIjoiUlM1MTIiLCJraWQiOiJhXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX' },
        ],
    ])(`должен преобразовать %j в %j`, (headers, result) => {
        expect(prepareHeadersForLog(headers)).toEqual(result);
    });
});
