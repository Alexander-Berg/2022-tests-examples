import * as Long from 'long';
import lodash from 'lodash';
import {StoriesIntTypes} from '../../../app/proto-schemas/stories-int/types';

import {getAllBusinesses, getAllStories} from '../../utils/db-utils';

import {simpleChangeStoryStatus, simpleCreateStory, updateStoryRequest} from '../../utils/v1/request-utils';
import {getTestStory, BIZ_IDS} from '../../fixtures/story';
import {prepareTests} from '../../utils/test-utils';
import {waitForExpected} from '../../utils/promise-utils';
import {nockSnippetsOnPut, nockSnippetsOnDelete, nockModeration, waitForPendingMocks} from '../../utils/nock-utils';

import {ExpectedError} from '../../types';
import {StoryDbRecord} from '../../../app/types/db';
import {ErrorCode} from '../../../app/lib/api-error';

describe('post /v1/update_story', () => {
    prepareTests();

    beforeEach(async () => {
        await simpleCreateStory(BIZ_IDS[0], BIZ_IDS[1]);
        await simpleCreateStory(BIZ_IDS[1], BIZ_IDS[2]);
    });

    describe('fail cases', () => {
        it('should fail on non-existent id', async () => {
            await validateFailedRequest(
                {
                    id: Long.fromNumber(3),
                    bizIds: [BIZ_IDS[0]]
                },
                {
                    code: ErrorCode.NOT_FOUND,
                    description: 'Record #3 not found'
                },
                404
            );
        });

        it('should fail on no data to update', async () => {
            await validateFailedRequest(
                {},
                {
                    code: ErrorCode.EMPTY_DATA,
                    description: 'No data to update story #1'
                },
                400
            );
        });

        it('should fail on no biz ids to update', async () => {
            await validateFailedRequest(
                {
                    bizIds: []
                },
                {
                    code: ErrorCode.EMPTY_DATA,
                    description: 'No data to update story #1'
                }
            );
        });

        it('should fail on invalid title', async () => {
            await validateFailedRequestWithData(
                {
                    title: 'x'.repeat(16)
                },
                {
                    code: ErrorCode.VALIDATION_ERROR,
                    description:
                        'Invalid input data: "data.title" length must be less than or equal to 15 characters long'
                }
            );
        });

        it('should fail on no screens', async () => {
            await validateFailedRequestWithData(
                {
                    screens: undefined
                },
                {
                    code: ErrorCode.VALIDATION_ERROR,
                    description: 'Invalid input data: "data.screens" must contain at least 1 items'
                }
            );
        });
    });

    describe('success cases', () => {
        it('should successfully update story on biz ids change', async () => {
            const bizIds = [BIZ_IDS[1], BIZ_IDS[2]];
            nockSnippetsOnDelete(BIZ_IDS[0]);
            await validateSuccessfulRequest({bizIds});
            const businesses = await getAllBusinesses();
            expect(businesses).toEqual({
                [BIZ_IDS[1].toString()]: [
                    {
                        id: '1',
                        order: 0
                    },
                    {
                        id: '2',
                        order: 1
                    }
                ],
                [BIZ_IDS[2].toString()]: [
                    {
                        id: '2',
                        order: 0
                    },
                    {
                        id: '1',
                        order: 1
                    }
                ]
            });
        });

        it('should successfully update story on biz ids change with duplicates', async () => {
            const bizIds = [BIZ_IDS[1], BIZ_IDS[1]];
            nockSnippetsOnDelete(BIZ_IDS[0]);
            await validateSuccessfulRequest({bizIds});
            const businesses = await getAllBusinesses();
            expect(businesses).toEqual({
                [BIZ_IDS[1].toString()]: [
                    {
                        id: '1',
                        order: 0
                    },
                    {
                        id: '2',
                        order: 1
                    }
                ],
                [BIZ_IDS[2].toString()]: [
                    {
                        id: '2',
                        order: 0
                    }
                ]
            });
        });

        it('should successfully update story on data change', async () => {
            const data = getTestStory().data;
            await validateSuccessfulRequest({data});
            const stories = await getAllStories();
            expect(stories[0].data).toEqual({...data, version: 1});
        });

        it('should successfully update story with tags on data change', async () => {
            const data = getTestStory().data;
            const dataWithTags = lodash.cloneDeep(data);
            dataWithTags.tags = ['lastMile'];
            await validateSuccessfulRequest({data: dataWithTags});
            const stories = await getAllStories();
            expect(stories[0].data).toEqual({...dataWithTags, version: 1});
        });

        it('should successfully update story on unpublish', async () => {
            await validateSuccessfulRequest({published: false});
            const stories = await getAllStories();
            expect(stories[0].published).toEqual(false);
        });

        it('should successfully update story on publish', async () => {
            await updateStoryRequest({id: Long.fromNumber(1), published: false});
            await validateSuccessfulRequest({published: true});
            const stories = await getAllStories();
            expect(stories[0].published).toEqual(true);
        });

        it('should successfully update story on multiple parameters change', async () => {
            const data = getTestStory().data;
            const bizIds = [BIZ_IDS[0], BIZ_IDS[2]];
            await validateSuccessfulRequest({data, bizIds});
            const stories = await getAllStories();
            expect(stories[0].data).toEqual({...data, version: 1});
            const businesses = await getAllBusinesses();
            expect(businesses).toEqual({
                [BIZ_IDS[0].toString()]: [
                    {
                        id: '1',
                        order: 0
                    }
                ],
                [BIZ_IDS[1].toString()]: [
                    {
                        id: '2',
                        order: 1
                    }
                ],
                [BIZ_IDS[2].toString()]: [
                    {
                        id: '2',
                        order: 0
                    },
                    {
                        id: '1',
                        order: 1
                    }
                ]
            });
        });

        type UpdateTestParams = {
            testName: string;
            waitDescription: string;
            expect: (story: StoryDbRecord) => void;
            isModerationBackendError?: boolean;
        };
        const tests: UpdateTestParams[] = [
            {
                testName: 'should set moderation status as unknown on clean web backend error',
                waitDescription: 'Moderation status set unknown',
                expect: (story) => {
                    expect(story.moderation_status_sent).toEqual(null);
                    expect(story.moderation_status).toEqual('unknown');
                },
                isModerationBackendError: true
            },
            {
                testName: 'should set moderation status as sent on success',
                waitDescription: 'Moderation status set sent',
                expect: (story) => {
                    expect(story.moderation_status_sent).toBeTruthy();
                    expect(story.moderation_status).toEqual('sent');
                }
            }
        ];

        tests.forEach(({testName, waitDescription, isModerationBackendError, expect}) => {
            describe(testName, () => {
                const storyId = Long.fromNumber(1);

                (['approved', 'rejected', 'pending'] as const).forEach((type) => {
                    it(`if story is ${type}`, async () => {
                        await simpleChangeStoryStatus(storyId, type);
                        const data = getTestStory().data;
                        nockModeration(data, {isError: isModerationBackendError});
                        await updateStoryRequest({id: storyId, data});

                        await waitForPendingMocks();
                        await waitForExpected(async () => {
                            expect((await getAllStories())[0]);
                        }, waitDescription);
                    });
                });
            });
        });
    });

    describe('snippet cases', () => {
        it('should successfully update story if bvm int responds with error', async () => {
            const removedBizId = BIZ_IDS[0];
            nockSnippetsOnDelete(removedBizId, {
                isBvmError: true
            });
            await updateStoryRequest(
                {
                    id: Long.fromNumber(1),
                    bizIds: [BIZ_IDS[1]]
                },
                {expectedPendingMocks: 1}
            );
            expect((await getAllBusinesses())[removedBizId.toString()]).toEqual(undefined);
        });

        it('should successfully update story if snippet backend responds with error', async () => {
            const addedBizId = BIZ_IDS[3];
            nockSnippetsOnPut(addedBizId, {
                isSnippetsError: true
            });
            await updateStoryRequest({
                id: Long.fromNumber(1),
                bizIds: [BIZ_IDS[0], BIZ_IDS[1], addedBizId]
            });
            expect((await getAllBusinesses())[addedBizId.toString()]).toEqual([
                {
                    id: '1',
                    order: 0
                }
            ]);
        });

        it('should skip snippet update if organization already has stories', async () => {
            const addedBizId = BIZ_IDS[2];
            nockSnippetsOnPut(addedBizId);
            await updateStoryRequest(
                {
                    id: Long.fromNumber(1),
                    bizIds: [BIZ_IDS[0], BIZ_IDS[1], addedBizId]
                },
                {expectedPendingMocks: 2}
            );
        });

        it('should skip snippet update if organization has stories left', async () => {
            const removedBizId = BIZ_IDS[2];
            nockSnippetsOnDelete(removedBizId);
            await updateStoryRequest(
                {
                    id: Long.fromNumber(1),
                    bizIds: [BIZ_IDS[0]]
                },
                {expectedPendingMocks: 2}
            );
        });
    });
});

async function validateSuccessfulRequest(request: Partial<StoriesIntTypes.StoryUpdateRequest>): Promise<void> {
    const lookupId = Long.fromNumber(1);
    if (request.data) {
        nockModeration(request.data);
    }
    const response = await updateStoryRequest({
        id: lookupId,
        ...request
    });
    if (request.data) {
        await waitForPendingMocks(0);
    }
    expect(response.status).toEqual(200);
    expect(response.data).toEqual({});
}

function validateFailedRequestWithData(
    extension: Partial<StoriesIntTypes.StoryData>,
    expectedError: ExpectedError
): Promise<void> {
    return validateFailedRequest(
        {
            data: {
                ...getTestStory().data,
                ...extension
            }
        },
        expectedError
    );
}

async function validateFailedRequest(
    request: Partial<StoriesIntTypes.StoryUpdateRequest>,
    expectedError: ExpectedError,
    errorCode?: number
): Promise<void> {
    const response = await updateStoryRequest({
        id: Long.fromNumber(1),
        ...request
    });
    expect(response.status).toEqual(errorCode || 400);
    expect(response.data).toEqual(expectedError);
}
