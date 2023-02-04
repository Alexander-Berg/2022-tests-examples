const de = require('descript');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const createContext = require('auto-core/server/descript/createContext');

const article = require('./article');

const journalApi = require('auto-core/server/resources/journal-api/getResource.nock.fixtures');

let context;
let req;
let res;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();

    context = createContext({ req, res });
});

describe('редиректы c uppercase символами в url', () => {
    it('301 редирект с урла с символами в верхнем регистре на урл с нижним регистром', () => {
        const params = {
            article_id: 'Facetnewreal',
        };

        journalApi
            .get(`/posts/${ params.article_id }/?withPostsModels=true`)
            .reply(200, {
                blocks: [
                    { text: '', type: 'text' },
                ],
            });

        return de.run(article, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        code: 'MAG_REDIRECT_TO_LOWERCASE_ID',
                        id: 'REDIRECTED',
                        location: 'https://mag.autoru_frontend.base_domain/article/facetnewreal/',
                        status_code: 301,
                    },
                });
            });
    });
});
