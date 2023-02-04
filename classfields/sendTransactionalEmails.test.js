jest.mock('auto-core/lib/util/getBunkerDict');

const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const sender = require('auto-core/server/resources/sender/sender.nock.fixtures');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const getBunkerDict = require('auto-core/lib/util/getBunkerDict');

const sendTransactionalEmails = require('./sendTransactionalEmails');

let req;
let res;
let context;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

beforeEach(() => {
    sender
        .post('/api/0/autoru/transactional/test-campaign/send')
        .query((query) => query.to_email === 'test@example.com')
        .reply(200, {
            result: {
                status: 'OK',
            },
        });

    getBunkerDict.mockReturnValue({
        'test-campaign': {
            test: [ 'test@example.com' ],
        },
    });
});

it('должен взять email из бункера по переданному ИД кампании и отправить письмо', () => {
    return de.run(sendTransactionalEmails, { context, params: { 'campaign-slug': 'test-campaign' } })
        .then((result) => {
            expect(result).toStrictEqual([ { status: 'OK' } ]);
        });
});

it('должен вернуть ошибку, если не найден campaign-slug', async() => {
    await expect(
        de.run(sendTransactionalEmails, { context, params: { 'campaign-slug': 'unknown' } }),
    ).rejects.toMatchSnapshot();
});
