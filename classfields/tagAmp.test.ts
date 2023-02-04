import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import type { TDescriptContext } from 'auto-core/server/descript/createContext';
import getTagFixtures from 'auto-core/server/resources/journal-api/methods/getTag.fixtures';
import getArticlesFixtures from 'auto-core/server/resources/journal-api/methods/getArticles.fixtures';

import mockJournalApi from './mocks/journalApi.mocks';
import tagAmp from './tagAmp';

jest.mock('auto-core/server/blocks/ads', () => null);

const context = { req: createHttpReq(), res: createHttpRes() } as unknown as TDescriptContext;

it('возвращает базовые блоки', () => {
    const params = {
        tag_id: 'bmw',
        page: 1,
    };

    mockJournalApi({ path: `/tags/${ params.tag_id }/`, reply: getTagFixtures.response200WithMark(), withDefaultQuery: false });
    mockJournalApi({ path: '/posts', reply: getArticlesFixtures.response200(), query: { tags: params.tag_id } });

    return de.run(tagAmp, { context, params }).then(
        (result) => {
            expect(result.mag.tagPage).toMatchSnapshot();
            expect(result.listing.posts).toHaveLength(3);
            expect(result.listing.pagination).toMatchSnapshot();
        });
});

describe('редиректы', () => {
    it('должен ответить 301, если архивный тег с поколением', () => {
        const params = {
            tag_id: 'volkswagen-golfgti-20270377',
        };

        mockJournalApi({ path: `/tags/${ params.tag_id }/`, reply: getTagFixtures.response200WithArchivedAndGenerationMMM(), withDefaultQuery: false });
        mockJournalApi({ path: '/posts', reply: getArticlesFixtures.response200(), query: { tags: params.tag_id } });

        return de.run(tagAmp, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        code: 'MAG_CAR_GENERATION_TAG_TO_CAR_MODEL_TAG',
                        id: 'REDIRECTED',
                        location: 'https://mag.autoru_frontend.base_domain/amp/tag/volkswagen-golfgti/',
                        status_code: 301,
                    },
                });
            });
    });
});
