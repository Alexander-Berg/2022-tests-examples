const de = require('descript');

const cookiesForProject = require('./cookiesForProject');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });

    req.cookies.autoru_sid = 'foo';
    req.cookies.gids = '1,2,3';
});

it('должен вернуть куки для проекта', () => {
    return de.run(cookiesForProject, {
        context,
    }).then((result) => {
        expect(result).toEqual({ gids: '1,2,3' });
    });
});
