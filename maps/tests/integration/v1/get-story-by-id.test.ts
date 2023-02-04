import * as Long from 'long';
import {StoriesIntTypes} from '../../../app/proto-schemas/stories-int/types';

import {BIZ_IDS} from '../../fixtures/story';
import {simpleCreateStory, deleteStoryRequest, getStoryByIdRequest} from '../../utils/v1/request-utils';
import {prepareTests} from '../../utils/test-utils';
import {nockSnippetsOnDelete} from '../../utils/nock-utils';
import {ExpectedError} from '../../types';
import {ErrorCode} from '../../../app/lib/api-error';

describe('post /v1/get_story_by_id', () => {
    const bizId = BIZ_IDS[0];

    prepareTests();

    beforeEach(async () => {
        await simpleCreateStory(bizId);
    });

    it('should fail on non-existent story', async () => {
        await validateFailedRequest(
            {
                id: Long.fromNumber(999)
            },
            {
                code: ErrorCode.NOT_FOUND,
                description: 'Story #999 not found'
            }
        );
    });

    it('should fail with 404 on story marked as deleted', async () => {
        nockSnippetsOnDelete(bizId);
        await deleteStoryRequest({id: Long.fromNumber(1)});
        await validateFailedRequest(
            {
                id: Long.fromNumber(1)
            },
            {
                code: ErrorCode.NOT_FOUND,
                description: 'Story #1 not found'
            }
        );
    });

    it('should successfully get story', async () => {
        const id = Long.fromNumber(1, true);
        const response = await getStoryByIdRequest({id});
        expect(response.status).toEqual(200);
        const decodedResponse = response.data as StoriesIntTypes.StoryByIdResponse;
        const result = decodedResponse.result;
        const expectedStory: StoriesIntTypes.Story = {
            id,
            createdAt: result.createdAt,
            published: true,
            data: {
                title: result.data.title,
                coverUrlTemplate: result.data.coverUrlTemplate,
                screens: result.data.screens,
                tags: []
            },
            bizIds: [bizId],
            moderation: {
                status: StoriesIntTypes.Moderation.Status.APPROVED,
                reasons: []
            }
        };
        expect(result).toEqual(expectedStory);
    });
});

async function validateFailedRequest(
    request: StoriesIntTypes.StoryByIdRequest,
    expectedError: ExpectedError,
    statusCode = 404
): Promise<void> {
    const response = await getStoryByIdRequest(request);
    expect(response.status).toEqual(statusCode);
    expect(response.data).toEqual(expectedError);
}
