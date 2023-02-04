import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import journalApi from 'auto-core/server/resources/journal-api/getResource.nock.fixtures';
import type { TDescriptContext } from 'auto-core/server/descript/createContext';
import getTagFixtures from 'auto-core/server/resources/journal-api/methods/getTag.fixtures';

import tag from './listing/tag';

jest.mock('auto-core/server/blocks/ads', () => null);

const context = { req: createHttpReq(), res: createHttpRes() } as unknown as TDescriptContext;

describe('редиректы', () => {
    it('должен ответить 301, если архивный тег с поколением', () => {
        const params = {
            tag_id: 'volkswagen-golfgti-20270377',
        };

        journalApi
            .get(`/tags/${ params.tag_id }/?withPostsModels=true`)
            .reply(200, getTagFixtures.response200WithArchivedAndGenerationMMM());

        return de.run(tag, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        code: 'MAG_CAR_GENERATION_TAG_TO_CAR_MODEL_TAG',
                        id: 'REDIRECTED',
                        location: 'https://mag.autoru_frontend.base_domain/tag/volkswagen-golfgti/',
                        status_code: 301,
                    },
                });
            });
    });
});
