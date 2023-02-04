const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createContext = require('auto-core/server/descript/createContext');

const getExpFlagsString = require('./getExpFlagsString');

let context;
beforeEach(() => {
    context = createContext({ req: createHttpReq() });
});

it('должен вернуть пустую строку, если нет экспериментов', () => {
    delete context.req.experimentsData;

    expect(getExpFlagsString({ context })).toBe('');
});

it('должен вернуть флаги через запятую', () => {
    context.req.experimentsData.experiments = {
        'AUTORUFRONT-111_one': true,
        'AUTORUFRONT-222_two': true,
    };

    expect(getExpFlagsString({ context })).toBe('AUTORUFRONT-111_one,AUTORUFRONT-222_two');
});

it('должен правильно заэнкодить флаги с кириллицей', () => {
    // @see AUTORUFRONT-20445
    context.req.experimentsData.experiments = {
        'AUTORUFRONT-111_один': true,
        'AUTORUFRONT-222_два': true,
    };

    expect(getExpFlagsString({ context })).toBe('AUTORUFRONT-111_%D0%BE%D0%B4%D0%B8%D0%BD,AUTORUFRONT-222_%D0%B4%D0%B2%D0%B0');
});
