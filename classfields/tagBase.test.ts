import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import type { TDescriptContext } from 'auto-core/server/descript/createContext';
import getTagFixtures from 'auto-core/server/resources/journal-api/methods/getTag.fixtures';
import getArticlesFixtures from 'auto-core/server/resources/journal-api/methods/getArticles.fixtures';

import tagBase from './tagBase';
import mockJournalApi from './mocks/journalApi.mocks';

const context = { req: createHttpReq(), res: createHttpRes() } as unknown as TDescriptContext;

it('возвращает базовые блоки', () => {
    const params = {
        tag_id: 'bmw',
        page: 1,
    };

    mockJournalApi({
        path: `/tags/${ params.tag_id }/`,
        reply: getTagFixtures.response200WithMark(),
        withDefaultQuery: false,
        query: { withPostsModels: 'true' },
    });
    mockJournalApi({ path: '/posts', reply: getArticlesFixtures.response200(), query: { tags: params.tag_id } });

    return de.run(tagBase, { context, params }).then(
        (result) => {
            expect(result.mag.tagPage).toMatchSnapshot();
            expect(result.listing.pagination).toMatchSnapshot();
            expect(result.listing.posts).toHaveLength(3);
        });
});
