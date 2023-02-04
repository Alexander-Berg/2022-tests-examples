import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import type { TDescriptContext } from 'auto-core/server/descript/createContext';
import getTagFixtures from 'auto-core/server/resources/journal-api/methods/getTag.fixtures';
import getCategoryFixtures from 'auto-core/server/resources/journal-api/methods/getCategory.fixtures';
import getArticlesFixtures from 'auto-core/server/resources/journal-api/methods/getArticles.fixtures';

import mockJournalApi from './mocks/journalApi.mocks';
import tagFilters from './tagFilters';

const context = { req: createHttpReq(), res: createHttpRes() } as unknown as TDescriptContext;

it('возвращает базовые блоки', () => {
    const params = {
        tag_id: 'bmw',
        page: 1,
        type: 'theme',
        slug: 'news',
    };

    mockJournalApi({ path: `/tags/${ params.tag_id }/`, reply: getTagFixtures.response200WithMark(), withDefaultQuery: false });
    mockJournalApi({ path: `/categories/${ params.slug }/`, reply: getCategoryFixtures.response200WithoutBlocks(), withDefaultQuery: false });
    mockJournalApi({ path: '/posts', reply: getArticlesFixtures.response200(), query: { categories: params.slug, tags: params.tag_id } });

    return de.run(tagFilters, { context, params }).then(
        (result) => {
            expect(result.mag.tagPage).toMatchSnapshot();
            expect(result.listing.pagination).toMatchSnapshot();
            expect(result.listing.posts).toHaveLength(3);
        },
    );
});
