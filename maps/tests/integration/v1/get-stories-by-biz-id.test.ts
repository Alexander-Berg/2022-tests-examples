import * as Long from 'long';
import {StoriesIntTypes} from '../../../app/proto-schemas/stories-int/types';

import {BIZ_IDS} from '../../fixtures/story';
import {
    deleteStoryRequest,
    getStoriesByBizIdRequest,
    reorderStories,
    simpleChangeStoryStatus,
    simpleCreateStory
} from '../../utils/v1/request-utils';
import {prepareTests} from '../../utils/test-utils';
import {nockSnippetsOnDelete} from '../../utils/nock-utils';
import {ErrorCode} from '../../../app/lib/api-error';

import {ExpectedError} from '../../types';

describe('post /v1/get_stories_by_biz_id', () => {
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

    it('should fail on zero page size', async () => {
        await validateFailedRequest(
            {
                bizId: BIZ_IDS[0],
                offset: 0,
                pageSize: 0
            },
            {
                code: ErrorCode.VALIDATION_ERROR,
                description: 'Expected page size no be non-zero'
            }
        );
    });

    describe('success cases', () => {
        it('should successfully get stories with zero offset', async () => {
            await validateSuccessRequest(
                {
                    bizId: BIZ_IDS[0],
                    offset: 0,
                    pageSize: 2
                },
                4,
                [4, 3]
            );
        });

        it('should successfully get stories with offset in available range', async () => {
            await validateSuccessRequest(
                {
                    bizId: BIZ_IDS[0],
                    offset: 2,
                    pageSize: 2
                },
                4,
                [2, 1]
            );
        });

        it('should successfully get no stories with offset out of available range', async () => {
            await validateSuccessRequest(
                {
                    bizId: BIZ_IDS[0],
                    offset: 4,
                    pageSize: 2
                },
                4,
                []
            );
        });

        it('should successfully get stories with page size in available range', async () => {
            await validateSuccessRequest(
                {
                    bizId: BIZ_IDS[0],
                    offset: 0,
                    pageSize: 3
                },
                4,
                [4, 3, 2]
            );
        });

        it('should successfully get stories with page size out of available range', async () => {
            await validateSuccessRequest(
                {
                    bizId: BIZ_IDS[0],
                    offset: 0,
                    pageSize: 10
                },
                4,
                [4, 3, 2, 1]
            );
        });

        it('should successfully get stories with single business id in story', async () => {
            await validateSuccessRequest(
                {
                    bizId: BIZ_IDS[0],
                    offset: 0,
                    pageSize: 2
                },
                4,
                [4, 3]
            );
        });

        it('should successfully get stories with multiple business ids in stories', async () => {
            await validateSuccessRequest(
                {
                    bizId: BIZ_IDS[1],
                    offset: 0,
                    pageSize: 6
                },
                6,
                [9, 8, 7, 6, 5, 2]
            );
        });

        it('should successfully get stories with index order different from story order', async () => {
            const bizId = BIZ_IDS[0];
            await reorderStories({
                bizId,
                storyIds: [1, 3, 2, 4].map((id) => Long.fromNumber(id))
            });
            await validateSuccessRequest(
                {
                    bizId,
                    offset: 0,
                    pageSize: 5
                },
                4,
                [1, 3, 2, 4]
            );
        });

        it('should successfully get no stories if business id does not exist', async () => {
            await validateSuccessRequest(
                {
                    bizId: BIZ_IDS[3],
                    offset: 0,
                    pageSize: 2
                },
                0,
                []
            );
        });

        it('should successfully get no stories if business id exists, but has zero stories', async () => {
            const bizId = BIZ_IDS[3];
            const {id} = await simpleCreateStory(bizId);
            nockSnippetsOnDelete(bizId);
            await deleteStoryRequest({id});
            await validateSuccessRequest(
                {
                    bizId,
                    offset: 0,
                    pageSize: 2
                },
                0,
                []
            );
        });

        it('should successfully get no stories after story marked as deleted', async () => {
            await deleteStoryRequest({id: Long.fromNumber(1)});
            await validateSuccessRequest(
                {
                    bizId: BIZ_IDS[0],
                    offset: 0,
                    pageSize: 2
                },
                3,
                [4, 3]
            );
        });

        it('should successfully get moderated stories with onlyApproved', async () => {
            await simpleChangeStoryStatus(Long.fromNumber(1), 'approved');
            await simpleChangeStoryStatus(Long.fromNumber(2), 'rejected', false);
            await simpleChangeStoryStatus(Long.fromNumber(3), 'pending', false);
            await simpleChangeStoryStatus(Long.fromNumber(4), 'approved');
            await validateSuccessRequest(
                {
                    bizId: BIZ_IDS[0],
                    offset: 0,
                    pageSize: 2,
                    onlyApproved: true
                },
                2,
                [4, 1]
            );
        });

        it('should successfully get not yet moderated stories with onlyApproved', async () => {
            await validateSuccessRequest(
                {
                    bizId: BIZ_IDS[0],
                    offset: 0,
                    pageSize: 4,
                    onlyApproved: true
                },
                4,
                [4, 3, 2, 1]
            );
        });

        it('should successfully get stories with proper data inside', async () => {
            const response = await getStoriesByBizIdRequest({
                bizId: BIZ_IDS[0],
                offset: 0,
                pageSize: 10
            });
            const decodedResponse = response.data as StoriesIntTypes.StoriesByBizIdResponse;
            const expectedBizIds = [[BIZ_IDS[2], BIZ_IDS[0]], [BIZ_IDS[0]], [BIZ_IDS[0], BIZ_IDS[1]], [BIZ_IDS[0]]];
            const expectedStories: StoriesIntTypes.Story[] = decodedResponse.results.map((result, index, results) => ({
                id: Long.fromNumber(results.length - index, true),
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
            expect(decodedResponse.results).toEqual(expectedStories);
        });

        it('should successfully get stories moderation', async () => {
            await simpleChangeStoryStatus(Long.fromNumber(2), 'approved', true);
            await simpleChangeStoryStatus(Long.fromNumber(5), 'approved', false);
            await simpleChangeStoryStatus(Long.fromNumber(6), 'pending', true);
            await simpleChangeStoryStatus(Long.fromNumber(7), 'pending', false);
            await simpleChangeStoryStatus(Long.fromNumber(8), 'rejected', true);
            await simpleChangeStoryStatus(Long.fromNumber(9), 'rejected', false);

            const response = await getStoriesByBizIdRequest({
                bizId: BIZ_IDS[1],
                offset: 0,
                pageSize: 10
            });
            const decodedResponse = response.data as StoriesIntTypes.StoriesByBizIdResponse;
            const expectedModerations: StoriesIntTypes.Moderation.Data[] = [
                {
                    status: StoriesIntTypes.Moderation.Status.REJECTED,
                    reasons: [
                        {
                            type: StoriesIntTypes.Moderation.RejectedReasonType.POLICY,
                            entity: 'foo'
                        }
                    ]
                },
                {
                    status: StoriesIntTypes.Moderation.Status.APPROVED,
                    reasons: []
                },
                {
                    status: StoriesIntTypes.Moderation.Status.PENDING,
                    reasons: []
                },
                {
                    status: StoriesIntTypes.Moderation.Status.APPROVED,
                    reasons: []
                },
                {
                    status: StoriesIntTypes.Moderation.Status.APPROVED,
                    reasons: []
                },
                {
                    status: StoriesIntTypes.Moderation.Status.APPROVED,
                    reasons: []
                }
            ];
            expect(decodedResponse.results.map((result) => result.moderation)).toEqual(expectedModerations);
        });
    });
});

async function validateSuccessRequest(
    request: StoriesIntTypes.StoriesByBizIdRequest,
    expectedTotalCount: number,
    expectedStoryIds: number[]
): Promise<void> {
    const response = await getStoriesByBizIdRequest(request);
    expect(response.status).toEqual(200);
    const decodedResponse = response.data as StoriesIntTypes.StoriesByBizIdResponse;
    expect(decodedResponse.totalCount).toEqual(expectedTotalCount);
    expect(decodedResponse.results.map((story) => story.id.toNumber())).toEqual(expectedStoryIds);
}

async function validateFailedRequest(
    request: StoriesIntTypes.StoriesByBizIdRequest,
    expectedError: ExpectedError,
    statusCode = 400
): Promise<void> {
    const response = await getStoriesByBizIdRequest(request);
    expect(response.status).toEqual(statusCode);
    expect(response.data).toEqual(expectedError);
}
