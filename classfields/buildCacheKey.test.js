const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createContext = require('auto-core/server/descript/createContext');

const buildCacheKey = require('./buildCacheKey');

let cacheKeyBuilder;
let context;
beforeEach(() => {
    cacheKeyBuilder = buildCacheKey('backend', 'method');
    context = createContext({ req: createHttpReq() });
});

it('должен вернуть null, если есть кука с импостером мокрицы', () => {
    context.req.cookies['mockritsa_imposter'] = '1';
    const params = {
        foo: 'bar',
    };
    expect(
        cacheKeyBuilder({ context, params }),
    ).toBeNull();
});

it('должен построить ключ и отсортировать ключи', () => {
    const params = {
        zzz: 1,
        bbb: 0,
        aaa: 2,
        only_nds1: true,
        only_nds2: false,
    };
    expect(
        cacheKeyBuilder({ context, params }),
    ).toEqual('descript3-backend://method?aaa=2&bbb=0&only_nds1=true&only_nds2=false&zzz=1');
});

it('должен построить ключ и заэнкодить ключи и значения', () => {
    const params = {
        яблоко: 'красное',
        арбуз: 'зелёный',
    };
    expect(
        cacheKeyBuilder({ context, params }),
        // eslint-disable-next-line max-len
    ).toEqual('descript3-backend://method?%D0%B0%D1%80%D0%B1%D1%83%D0%B7=%D0%B7%D0%B5%D0%BB%D1%91%D0%BD%D1%8B%D0%B9&%D1%8F%D0%B1%D0%BB%D0%BE%D0%BA%D0%BE=%D0%BA%D1%80%D0%B0%D1%81%D0%BD%D0%BE%D0%B5');
});

it('должен построить ключ и удалить пустые значения', () => {
    const params = {
        string_e: '',
        null_e: null,
        array_e: [],
        prop: 'value',
    };
    expect(
        cacheKeyBuilder({ context, params }),
    ).toEqual('descript3-backend://method?prop=value');
});

describe('должен построить ключ и удалить приватные параметры', () => {
    const INVALID_CACHE_PARAMS = [ 'access_key', 'autoruuid', 'session_id', 'sid', 'remote_addr', 'remote_ip' ];

    it.each(INVALID_CACHE_PARAMS)('%s', (key) => {
        const params = {
            [key]: 'value',
            prop: 'value',
        };
        expect(
            cacheKeyBuilder({ context, params }),
        ).toEqual('descript3-backend://method?prop=value');
    });
});
