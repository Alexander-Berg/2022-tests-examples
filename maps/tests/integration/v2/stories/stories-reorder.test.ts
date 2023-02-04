import {ErrorCode} from '../../../../app/lib/api-error';
import {BIZ_IDS} from '../../../fixtures/story.v2';
import {getAllBusinesses} from '../../../utils/db-utils';
import {prepareTests} from '../../../utils/test-utils';
import {deleteStoryRequest, simpleCreateStory, storiesReorder} from '../../../utils/v2/request-utils';

describe('POST /stories/:biz_id/reorder', () => {
    const bizId = BIZ_IDS[0];

    prepareTests();

    beforeEach(async () => {
        await simpleCreateStory(bizId);
        await simpleCreateStory(bizId);
        await simpleCreateStory(bizId);
    });

    describe('fail cases', () => {
        it('should fail if business id does not exist', async () => {
            const response = await storiesReorder(BIZ_IDS[1], {
                storyIds: [3, 2, 1].map((id) => String(id))
            });
            expect(response.status).toEqual(404);
            expect(response.data).toEqual({
                code: ErrorCode.NOT_FOUND,
                description: `Business #${BIZ_IDS[1]} not found`
            });
        });

        it('should fail if story ids mismatch by amount', async () => {
            const response = await storiesReorder(bizId, {
                storyIds: [2, 1].map((id) => String(id))
            });
            expect(response.status).toEqual(400);
            expect(response.data).toEqual({
                code: ErrorCode.INCORRECT_DATA,
                description: 'Cannot reorder, story ids in database and request do not match'
            });
        });

        it('should fail if story ids mismatch by values', async () => {
            const response = await storiesReorder(bizId, {
                storyIds: [4, 2, 1].map((id) => String(id))
            });
            expect(response.status).toEqual(400);
            expect(response.data).toEqual({
                code: ErrorCode.INCORRECT_DATA,
                description: 'Cannot reorder, story ids in database and request do not match'
            });
        });
    });

    it('should successfully reorder stories if new order is same as old after story been marked as deleted', async () => {
        await deleteStoryRequest('1');
        const response = await storiesReorder(bizId, {
            storyIds: [3, 2].map((id) => String(id))
        });

        expect(response.status).toEqual(204);
        expect(response.data).toEqual('');

        const businessRecords = await getAllBusinesses();
        expect(businessRecords).toEqual({
            [bizId.toString()]: [
                {
                    id: '2',
                    order: 1
                },
                {
                    id: '3',
                    order: 2
                }
            ]
        });
    });

    it('should successfully reorder stories if new order is same as old', async () => {
        const response = await storiesReorder(bizId, {
            storyIds: [3, 2, 1].map((id) => String(id))
        });

        expect(response.status).toEqual(204);
        expect(response.data).toEqual('');
    });

    it('should successfully reorder stories', async () => {
        const response = await storiesReorder(bizId, {
            storyIds: [1, 2, 3].map((id) => String(id))
        });
        expect(response.status).toEqual(204);
        expect(response.data).toEqual('');

        const businessRecords = await getAllBusinesses();
        expect(businessRecords).toEqual({
            [bizId]: [
                {
                    id: '3',
                    order: 1
                },
                {
                    id: '2',
                    order: 2
                },
                {
                    id: '1',
                    order: 3
                }
            ]
        });
    });
});
