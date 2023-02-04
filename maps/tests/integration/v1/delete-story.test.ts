import * as Long from 'long';

import {getAllBusinesses, getAllStories} from '../../utils/db-utils';

import {BIZ_IDS} from '../../fixtures/story';
import {deleteStoryRequest, simpleCreateStory} from '../../utils/v1/request-utils';
import {prepareTests} from '../../utils/test-utils';
import {nockSnippetsOnDelete} from '../../utils/nock-utils';
import {ErrorCode} from '../../../app/lib/api-error';

describe('post /v1/delete_story', () => {
    const bizId = BIZ_IDS[0];

    prepareTests();

    beforeEach(async () => {
        await simpleCreateStory(bizId);
    });

    it('should fail on non-existent id', async () => {
        const response = await deleteStoryRequest({id: Long.fromNumber(9)});
        expect(response.status).toEqual(404);
        expect(response.data).toEqual({
            code: ErrorCode.NOT_FOUND,
            description: 'Record #9 not found'
        });
    });

    it('should successfully delete story', async () => {
        nockSnippetsOnDelete(bizId);
        const response = await deleteStoryRequest({id: Long.fromNumber(1)});
        expect(response.status).toEqual(200);
        expect(response.data).toEqual({});

        const storyRecords = await getAllStories();
        expect(storyRecords.length).toEqual(1);
        expect(storyRecords[0].deleted).toEqual(true);
        const businessRecords = await getAllBusinesses();
        expect(businessRecords).toEqual({});
    });

    describe('snippets cases', () => {
        it('should successfully delete story if bvm int responds with error', async () => {
            nockSnippetsOnDelete(bizId, {
                isBvmError: true
            });
            await deleteStoryRequest({id: Long.fromNumber(1)}, {expectedPendingMocks: 1});
            expect((await getAllStories())[0].deleted).toEqual(true);
        });

        it('should successfully delete story if snippets backend responds with error', async () => {
            nockSnippetsOnDelete(bizId, {
                isSnippetsError: true
            });
            await deleteStoryRequest({id: Long.fromNumber(1)});
            expect((await getAllStories())[0].deleted).toEqual(true);
        });

        it('should skip snippets update if organization has stories left', async () => {
            await simpleCreateStory(bizId);
            nockSnippetsOnDelete(bizId);
            await deleteStoryRequest({id: Long.fromNumber(1)}, {expectedPendingMocks: 2});
        });
    });
});
