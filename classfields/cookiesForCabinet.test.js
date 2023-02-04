const de = require('descript');

const cookiesForCabinet = require('./cookiesForCabinet');

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

    req.cookies.is_sidebar_collapsed = 'bar';
});

it('должен вернуть куки для кабинета', () => {
    return de.run(cookiesForCabinet, {
        context,
    }).then((result) => {
        expect(result).toEqual({ is_sidebar_collapsed: 'bar' });
    });
});
