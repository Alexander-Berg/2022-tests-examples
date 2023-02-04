import Long from 'long';
import {ErrorCode} from '../../../../app/lib/api-error';
import {BIZ_IDS} from '../../../fixtures/story.v2';
import {getAllBusinesses, getAllStories} from '../../../utils/db-utils';
import {nockSnippetsOnDelete} from '../../../utils/nock-utils';
import {prepareTests} from '../../../utils/test-utils';
import {deleteStoryRequest, simpleCreateStory} from '../../../utils/v2/request-utils';

describe('DELETE /v2/story/:story_id', () => {
    const bizId = BIZ_IDS[0];

    prepareTests();

    beforeEach(async () => {
        await simpleCreateStory(bizId);
    });

    it('should fail on non-existent id', async () => {
        const response = await deleteStoryRequest('9');
        expect(response.status).toEqual(404);
        expect(response.data).toEqual({
            code: ErrorCode.NOT_FOUND,
            description: 'Record #9 not found'
        });
    });

    it('should successfully delete story', async () => {
        nockSnippetsOnDelete(Long.fromString(bizId));
        const response = await deleteStoryRequest('1');
        expect(response.status).toEqual(204);
        expect(response.data).toEqual('');

        const storyRecords = await getAllStories();
        expect(storyRecords.length).toEqual(1);
        expect(storyRecords[0].deleted).toEqual(true);
        const businessRecords = await getAllBusinesses();
        expect(businessRecords).toEqual({});
    });

    describe('snippets cases', () => {
        it('should successfully delete story if bvm int responds with error', async () => {
            nockSnippetsOnDelete(Long.fromString(bizId), {
                isBvmError: true
            });
            await deleteStoryRequest('1', {expectedPendingMocks: 1});
            expect((await getAllStories())[0].deleted).toEqual(true);
        });

        it('should successfully delete story if snippets backend responds with error', async () => {
            nockSnippetsOnDelete(Long.fromString(bizId), {
                isSnippetsError: true
            });
            await deleteStoryRequest('1');
            expect((await getAllStories())[0].deleted).toEqual(true);
        });

        it('should skip snippets update if organization has stories left', async () => {
            await simpleCreateStory(bizId);
            nockSnippetsOnDelete(Long.fromString(bizId));
            await deleteStoryRequest('1', {expectedPendingMocks: 2});
        });
    });
});
