const de = require('descript');
const nock = require('nock');
const createContext = require('auto-core/server/descript/createContext');

const block = require('./createChatForDeal');

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
        .get('/1.0/safe-deal/deal/get/1234567890')
        .reply(200, {
            deal: {
                buyer_id: 'af79187ad268bf83',
                seller_id: '3fb65bc8332bb8cb',
                subject: {
                    autoru: {
                        offer: { id: '1115364323-611158ca' },
                    },
                },
            },
        });
    publicApi
        .post('/1.0/chat/room', {
            users: [ 'af79187ad268bf83', '3fb65bc8332bb8cb' ],
            subject: {
                offer: {
                    category: 'cars',
                    id: '1115364323-611158ca',
                },
            },
        })
        .reply(200, createChatFixtures.response200());

    const params = {
        deal_id: '1234567890',
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
