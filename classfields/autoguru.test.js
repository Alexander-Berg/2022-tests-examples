jest.mock('auto-core/lib/util/getBunkerDict', () => {
    const fs = require('fs');
    const path = require('path');
    return jest.fn((node) => {
        if (node === 'autoguru/questions') {
            return fs.readFileSync(
                path.resolve('mockData/bunker/autoguru/questions.txt'),
                { encoding: 'utf-8' },
            );
        }
    });
});

const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const autoguru = require('./autoguru');
const getBunkerDict = require('auto-core/lib/util/getBunkerDict');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен вернуть ответы, если они есть в бункере', () => {
    const params = {};

    return de.run(autoguru, { context, params })
        .then((result) => {
            expect(result).toMatchObject({
                answerValues: [],
                questionIndex: 0,
            });
            expect(result.questions.length > 0).toBe(true);
        });
});

it('должен вернуть undefined, если нет бункера', () => {
    getBunkerDict.mockImplementation(() => undefined);
    const params = {};

    return de.run(autoguru, { context, params })
        .then((result) => {
            expect(result).toBeUndefined();
        });
});
