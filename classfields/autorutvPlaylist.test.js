const de = require('descript');
// const _ = require('lodash');

const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');
const mockBunker = getBunkerMock([ 'common/yt_playlists' ])['common/yt_playlists'];

jest.mock('auto-core/server/resources/bunker/methods/getYtPlaylists', () => {
    const de = require('descript');
    return de.func({
        block: () => {
            return mockBunker;
        },
    });
});

const createContext = require('auto-core/server/descript/createContext');

const autorutvPlaylist = require('./autorutvPlaylist');

const youtubeNock = require('auto-core/server/resources/youtube/youtube.nock');
const getAutorutvPlaylistItems = require('auto-core/server/resources/youtube/methods/getAutorutvPlaylistItems.nock.fixture');

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

// eslint-disable-next-line jest/no-commented-out-tests
// it('достанет тумбы из youtube api а названия из бункера, если ручка ответила', () => {
//     bunkerMock.forEach(({ id }) => {
//         youtubeNock
//             .get('/youtube/v3/playlistItems')
//             .query({
//                 playlistId: id,
//                 maxResults: 1,
//                 part: 'snippet',
//                 key: 'autoru_frontend.youtube_api.key',
//             })
//             .reply(200, getAutorutvPlaylistItems.success_response(id));
//     });

//     return de.run(autorutvPlaylist, { context })
//         .then((result) => {
//             expect(_.map(result, 'thumbnail')).toMatchSnapshot();
//             expect(_.map(result, 'label')).toMatchSnapshot();
//         });
// });

// из-за de.Cache мы теперь можем протестить только что-то одно
// и пока только кейс про бункер
it('достанет данные из бункера если ручка вернула ошибку', () => {
    mockBunker.forEach(({ id }) => {
        youtubeNock
            .get('/youtube/v3/playlistItems')
            .query({
                playlistId: id,
                maxResults: 1,
                part: 'snippet',
                key: 'autoru_frontend.youtube_api.key',
            })
            .reply(200, getAutorutvPlaylistItems.error_response());
    });

    return de.run(autorutvPlaylist, { context })
        .then((result) => {
            expect(result).toMatchSnapshot();
        });
});
