import Long from 'long';
import {ErrorCode} from '../../../../app/lib/api-error';
import {Moderation, ResponseV2} from '../../../../app/v2/types';
import {BIZ_IDS} from '../../../fixtures/story.v2';
import {ExpectedError} from '../../../types';
import {nockSnippetsOnDelete} from '../../../utils/nock-utils';
import {prepareTests} from '../../../utils/test-utils';
import {deleteStoryRequest, getStoryRequest, simpleCreateStory} from '../../../utils/v2/request-utils';

describe('GET /v2/story/:story_id', () => {
    const bizId = BIZ_IDS[0];

    prepareTests();

    beforeEach(async () => {
        await simpleCreateStory(bizId);
    });

    it('should fail on non-existent story', async () => {
        await validateFailedRequest('999', {
            code: ErrorCode.NOT_FOUND,
            description: 'Story #999 not found'
        });
    });

    it('should fail with 404 on story marked as deleted', async () => {
        nockSnippetsOnDelete(Long.fromString(bizId));
        await deleteStoryRequest('1');
        await validateFailedRequest('1', {
            code: ErrorCode.NOT_FOUND,
            description: 'Story #1 not found'
        });
    });

    it('should successfully get story', async () => {
        const id = '1';
        const response = await getStoryRequest(id);
        expect(response.status).toEqual(200);
        const data = response.data as ResponseV2.GetStory;
        const result = data.result;
        const expectedStory: ResponseV2.GetStory['result'] = {
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
                status: Moderation.Status.APPROVED,
                reasons: []
            }
        };
        expect(result).toEqual(expectedStory);
    });
});

async function validateFailedRequest(storyId: string, expectedError: ExpectedError, statusCode = 404): Promise<void> {
    const response = await getStoryRequest(storyId);
    expect(response.status).toEqual(statusCode);
    expect(response.data).toEqual(expectedError);
}
