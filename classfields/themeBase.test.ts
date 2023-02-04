import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import type { TDescriptContext } from 'auto-core/server/descript/createContext';
import getTagFixtures from 'auto-core/server/resources/journal-api/methods/getTag.fixtures';
import getArticlesFixtures from 'auto-core/server/resources/journal-api/methods/getArticles.fixtures';

import themeBase from './themeBase';
import mockJournalApi from './mocks/journalApi.mocks';

const context = { req: createHttpReq(), res: createHttpRes() } as unknown as TDescriptContext;

it('возвращает базовые блоки', () => {
    const params = {
        theme_id: 'news',
        page: 1,
    };

    mockJournalApi({
        path: `/categories/${ params.theme_id }/`,
        reply: getTagFixtures.response200WithMark(),
        withDefaultQuery: false,
        query: { withPostsModels: 'true' },
    });
    mockJournalApi({ path: '/posts', reply: getArticlesFixtures.response200(), query: { categories: params.theme_id } });

    return de.run(themeBase, { context, params }).then(
        (result) => {
            expect(result.mag.themePage).toMatchSnapshot();
            expect(result.listing.pagination).toMatchSnapshot();
            expect(result.listing.posts).toHaveLength(3);
        },
    );
});
