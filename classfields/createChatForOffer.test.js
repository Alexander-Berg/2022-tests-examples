const de = require('descript');
const nock = require('nock');
const createContext = require('auto-core/server/descript/createContext');

const block = require('./createChatForOffer');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const createChatFixtures = require('auto-core/server/resources/publicApiChat/methods/createChat.fixtures');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('сделает правильный запрос и создаст чат', async() => {
    publicApi
        .post('/1.0/chat/room', {
            users: [],
            subject: {
                offer: {
                    category: 'cars',
                    id: '1115364323-611158ca',
                },
            },
        })
        .reply(200, createChatFixtures.response200());

    const params = {
        offer_category: 'cars',
        offer_id: '1115364323-611158ca',
    };

    await expect(
        de.run(block, { context, params }),
    ).resolves.toMatchObject({
        origin: 'autoru',
        id: '64d522336e92c2ba20faa275a75cba9c',
        type: 'ROOM_TYPE_OFFER',
    });

    expect(nock.isDone()).toEqual(true);
});
