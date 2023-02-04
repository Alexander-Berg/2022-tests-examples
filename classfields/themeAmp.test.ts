import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import type { TDescriptContext } from 'auto-core/server/descript/createContext';
import getCategoryFixtures from 'auto-core/server/resources/journal-api/methods/getCategory.fixtures';
import getArticlesFixtures from 'auto-core/server/resources/journal-api/methods/getArticles.fixtures';

import themeAmp from './themeAmp';
import mockJournalApi from './mocks/journalApi.mocks';

jest.mock('auto-core/server/blocks/ads', () => null);

const context = { req: createHttpReq(), res: createHttpRes() } as unknown as TDescriptContext;

it('возвращает базовые блоки', () => {
    const params = {
        theme_id: 'news',
        page: 1,
    };

    mockJournalApi({ path: `/categories/${ params.theme_id }/`, reply: getCategoryFixtures.response200WithoutBlocks(), withDefaultQuery: false });
    mockJournalApi({ path: '/posts', reply: getArticlesFixtures.response200(), query: { categories: params.theme_id } });

    return de.run(themeAmp, { context, params }).then(
        (result) => {
            expect(result.mag.themePage).toMatchSnapshot();
            expect(result.listing.posts).toHaveLength(3);
            expect(result.listing.pagination).toMatchSnapshot();
        },
    );
});
