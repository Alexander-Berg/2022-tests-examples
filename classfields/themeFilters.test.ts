import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import type { TDescriptContext } from 'auto-core/server/descript/createContext';
import getTagFixtures from 'auto-core/server/resources/journal-api/methods/getTag.fixtures';
import getCategoryFixtures from 'auto-core/server/resources/journal-api/methods/getCategory.fixtures';
import getArticlesFixtures from 'auto-core/server/resources/journal-api/methods/getArticles.fixtures';

import themeFilters from './themeFilters';
import mockJournalApi from './mocks/journalApi.mocks';

const context = { req: createHttpReq(), res: createHttpRes() } as unknown as TDescriptContext;

it('возвращает базовые блоки', () => {
    const params = {
        theme_id: 'bmw',
        page: 1,
        type: 'tag',
        slug: 'bmw',
    };

    mockJournalApi({
        path: `/categories/${ params.theme_id }/`,
        reply: getTagFixtures.response200WithMark(),
        withDefaultQuery: false,
    });
    mockJournalApi({
        path: `/tags/${ params.slug }/`,
        reply: getCategoryFixtures.response200WithoutBlocks(),
        withDefaultQuery: false,
    });
    mockJournalApi({ path: '/posts', reply: getArticlesFixtures.response200(), query: { tags: params.theme_id, categories: params.slug } });

    return de.run(themeFilters, { context, params }).then(
        (result) => {
            expect(result.mag.themePage).toMatchSnapshot();
            expect(result.listing.pagination).toMatchSnapshot();
            expect(result.listing.posts).toHaveLength(3);
        },
    );
});
