import Long from 'long';
import {ErrorCode} from '../../../../app/lib/api-error';
import {Moderation, RequestV2, ResponseV2, Story} from '../../../../app/v2/types';
import {BIZ_IDS} from '../../../fixtures/story.v2';
import {ExpectedError} from '../../../types';
import {nockSnippetsOnDelete} from '../../../utils/nock-utils';
import {prepareTests} from '../../../utils/test-utils';
import {
    deleteStoryRequest,
    getStories,
    simpleChangeStoryStatus,
    simpleCreateStory,
    storiesReorder
} from '../../../utils/v2/request-utils';

describe('GET /v2/stories', () => {
    prepareTests();

    beforeEach(async () => {
        await simpleCreateStory(BIZ_IDS[0]);
        await simpleCreateStory(BIZ_IDS[0], BIZ_IDS[1]);
        await simpleCreateStory(BIZ_IDS[0]);
        await simpleCreateStory(BIZ_IDS[0], BIZ_IDS[2]);
        await simpleCreateStory(BIZ_IDS[1]);
        await simpleCreateStory(BIZ_IDS[1]);
        await simpleCreateStory(BIZ_IDS[1], BIZ_IDS[2]);
        await simpleCreateStory(BIZ_IDS[1], BIZ_IDS[2]);
        await simpleCreateStory(BIZ_IDS[1], BIZ_IDS[2]);
    });

    it('should fail on zero limit size', async () => {
        await validateFailedRequest(
            BIZ_IDS[0],
            {
                offset: 0,
                limit: 0
            },
            {
                code: ErrorCode.VALIDATION_ERROR,
                description: 'Invalid input data: "limit" must be greater than 0'
            }
        );
    });

    describe('success cases', () => {
        it('should successfully get stories with zero offset', async () => {
            await validateSuccessRequest(
                BIZ_IDS[0],
                {
                    offset: 0,
                    limit: 2
                },
                4,
                ['4', '3']
            );
        });

        it('should successfully get stories with offset in available range', async () => {
            await validateSuccessRequest(
                BIZ_IDS[0],
                {
                    offset: 2,
                    limit: 2
                },
                4,
                ['2', '1']
            );
        });

        it('should successfully get no stories with offset out of available range', async () => {
            await validateSuccessRequest(
                BIZ_IDS[0],
                {
                    offset: 4,
                    limit: 2
                },
                4,
                []
            );
        });

        it('should successfully get stories with page size in available range', async () => {
            await validateSuccessRequest(
                BIZ_IDS[0],
                {
                    offset: 0,
                    limit: 3
                },
                4,
                ['4', '3', '2']
            );
        });

        it('should successfully get stories with page size out of available range', async () => {
            await validateSuccessRequest(
                BIZ_IDS[0],
                {
                    offset: 0,
                    limit: 10
                },
                4,
                ['4', '3', '2', '1']
            );
        });

        it('should successfully get stories with single business id in story', async () => {
            await validateSuccessRequest(
                BIZ_IDS[0],
                {
                    offset: 0,
                    limit: 2
                },
                4,
                ['4', '3']
            );
        });

        it('should successfully get stories with multiple business ids in stories', async () => {
            await validateSuccessRequest(
                BIZ_IDS[1],
                {
                    offset: 0,
                    limit: 6
                },
                6,
                ['9', '8', '7', '6', '5', '2']
            );
        });

        it('should successfully get stories with index order different from story order', async () => {
            const bizId = BIZ_IDS[0];
            await storiesReorder(bizId, {
                storyIds: [1, 3, 2, 4].map((id) => String(id))
            });
            await validateSuccessRequest(
                bizId,
                {
                    offset: 0,
                    limit: 5
                },
                4,
                ['1', '3', '2', '4']
            );
        });

        it('should successfully get no stories if business id does not exist', async () => {
            await validateSuccessRequest(
                BIZ_IDS[3],
                {
                    offset: 0,
                    limit: 2
                },
                0,
                []
            );
        });

        it('should successfully get no stories if business id exists, but has zero stories', async () => {
            const bizId = BIZ_IDS[3];
            const {id} = await simpleCreateStory(bizId);
            nockSnippetsOnDelete(Long.fromString(bizId));
            await deleteStoryRequest(id);
            await validateSuccessRequest(
                bizId,
                {
                    offset: 0,
                    limit: 2
                },
                0,
                []
            );
        });

        it('should successfully get no stories after story marked as deleted', async () => {
            await deleteStoryRequest('1');
            await validateSuccessRequest(
                BIZ_IDS[0],
                {
                    offset: 0,
                    limit: 2
                },
                3,
                ['4', '3']
            );
        });

        it('should successfully get moderated stories with onlyApproved', async () => {
            await simpleChangeStoryStatus('1', 'approved');
            await simpleChangeStoryStatus('2', 'rejected', false);
            await simpleChangeStoryStatus('3', 'pending', false);
            await simpleChangeStoryStatus('4', 'approved');
            await validateSuccessRequest(
                BIZ_IDS[0],
                {
                    offset: 0,
                    limit: 2,
                    onlyApproved: true
                },
                2,
                ['4', '1']
            );
        });

        it('should successfully get not yet moderated stories with onlyApproved', async () => {
            await validateSuccessRequest(
                BIZ_IDS[0],
                {
                    offset: 0,
                    limit: 4,
                    onlyApproved: true
                },
                4,
                ['4', '3', '2', '1']
            );
        });

        it('should successfully get stories with proper data inside', async () => {
            const response = await getStories(BIZ_IDS[0], {
                offset: 0,
                limit: 10
            });
            const data = response.data as ResponseV2.GetStories;
            const expectedBizIds = [[BIZ_IDS[2], BIZ_IDS[0]], [BIZ_IDS[0]], [BIZ_IDS[0], BIZ_IDS[1]], [BIZ_IDS[0]]];
            const expectedStories: Story[] = data.results.map((result, index, results) => ({
                id: String(results.length - index),
                createdAt: result.createdAt,
                published: true,
                data: {
                    title: result.data.title,
                    coverUrlTemplate: result.data.coverUrlTemplate,
                    screens: result.data.screens,
                    tags: []
                },
                bizIds: expectedBizIds[index],
                moderation: result.moderation
            }));
            expect(data.results).toEqual(expectedStories);
        });

        it('should successfully get stories moderation', async () => {
            await simpleChangeStoryStatus('2', 'approved', true);
            await simpleChangeStoryStatus('5', 'approved', false);
            await simpleChangeStoryStatus('6', 'pending', true);
            await simpleChangeStoryStatus('7', 'pending', false);
            await simpleChangeStoryStatus('8', 'rejected', true);
            await simpleChangeStoryStatus('9', 'rejected', false);

            const response = await getStories(BIZ_IDS[1], {
                offset: 0,
                limit: 10
            });
            const data = response.data as ResponseV2.GetStories;
            const expectedModerations: Moderation.Data[] = [
                {
                    status: Moderation.Status.REJECTED,
                    reasons: [
                        {
                            type: Moderation.RejectedReasonType.POLICY,
                            entity: 'foo'
                        }
                    ]
                },
                {
                    status: Moderation.Status.APPROVED,
                    reasons: []
                },
                {
                    status: Moderation.Status.PENDING,
                    reasons: []
                },
                {
                    status: Moderation.Status.APPROVED,
                    reasons: []
                },
                {
                    status: Moderation.Status.APPROVED,
                    reasons: []
                },
                {
                    status: Moderation.Status.APPROVED,
                    reasons: []
                }
            ];
            expect(data.results.map((result) => result.moderation)).toEqual(expectedModerations);
        });
    });
});

async function validateSuccessRequest(
    bizId: string,
    searchParams: RequestV2.GetStories,
    expectedTotalCount: number,
    expectedStoriesIds: string[]
): Promise<void> {
    const response = await getStories(bizId, searchParams);
    expect(response.status).toEqual(200);
    const result = response.data as ResponseV2.GetStories;
    expect(result.totalCount).toEqual(expectedTotalCount);
    expect(result.results.map((story) => story.id)).toEqual(expectedStoriesIds);
}

async function validateFailedRequest(
    bizId: string,
    searchParams: RequestV2.GetStories,
    expectedError: ExpectedError,
    statusCode = 400
): Promise<void> {
    const response = await getStories(bizId, searchParams);
    expect(response.status).toEqual(statusCode);
    expect(response.data).toEqual(expectedError);
}
