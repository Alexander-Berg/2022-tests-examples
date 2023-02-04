import Long from 'long';
import lodash from 'lodash';
import {ErrorCode} from '../../../../app/lib/api-error';
import {StoryDbRecord} from '../../../../app/types/db';
import {RequestV2} from '../../../../app/v2/types';
import {BIZ_IDS, getTestStory} from '../../../fixtures/story.v2';
import {ExpectedError} from '../../../types';
import {getAllBusinesses, getAllStories} from '../../../utils/db-utils';
import {nockModeration, nockSnippetsOnDelete, nockSnippetsOnPut, waitForPendingMocks} from '../../../utils/nock-utils';
import {waitForExpected} from '../../../utils/promise-utils';
import {prepareTests} from '../../../utils/test-utils';
import {simpleChangeStoryStatus, simpleCreateStory, updateStoryRequest} from '../../../utils/v2/request-utils';

const STORY_ID = '1';

describe('PUT /v2/story/:story_id', () => {
    prepareTests();

    beforeEach(async () => {
        await simpleCreateStory(BIZ_IDS[0], BIZ_IDS[1]);
        await simpleCreateStory(BIZ_IDS[1], BIZ_IDS[2]);
    });

    describe('fail cases', () => {
        it('should fail on non-existent id', async () => {
            await validateFailedRequest(
                '3',
                {
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
                STORY_ID,
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
                STORY_ID,
                {
                    bizIds: []
                },
                {
                    code: ErrorCode.VALIDATION_ERROR,
                    description: 'Invalid input data: "bizIds" must contain at least 1 items'
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
                    description: 'Invalid input data: "data.screens" is required'
                }
            );
        });
    });

    describe('success cases', () => {
        it('should successfully update story on biz ids change', async () => {
            const bizIds = [BIZ_IDS[1], BIZ_IDS[2]];
            nockSnippetsOnDelete(Long.fromString(BIZ_IDS[0]));
            await validateSuccessfulRequest({bizIds});
            const businesses = await getAllBusinesses();
            expect(businesses).toEqual({
                [BIZ_IDS[1]]: [
                    {
                        id: '1',
                        order: 0
                    },
                    {
                        id: '2',
                        order: 1
                    }
                ],
                [BIZ_IDS[2]]: [
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
            nockSnippetsOnDelete(Long.fromString(BIZ_IDS[0]));
            await validateSuccessfulRequest({bizIds});
            const businesses = await getAllBusinesses();
            expect(businesses).toEqual({
                [BIZ_IDS[1]]: [
                    {
                        id: '1',
                        order: 0
                    },
                    {
                        id: '2',
                        order: 1
                    }
                ],
                [BIZ_IDS[2]]: [
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
            await updateStoryRequest(STORY_ID, {published: false});
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
                [BIZ_IDS[0]]: [
                    {
                        id: '1',
                        order: 0
                    }
                ],
                [BIZ_IDS[1]]: [
                    {
                        id: '2',
                        order: 1
                    }
                ],
                [BIZ_IDS[2]]: [
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
                (['approved', 'rejected', 'pending'] as const).forEach((type) => {
                    it(`if story is ${type}`, async () => {
                        await simpleChangeStoryStatus(STORY_ID, type);
                        const data = getTestStory().data;
                        nockModeration(data, {isError: isModerationBackendError});
                        await updateStoryRequest(STORY_ID, {data});

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
            nockSnippetsOnDelete(Long.fromString(removedBizId), {
                isBvmError: true
            });
            await updateStoryRequest(
                STORY_ID,
                {
                    bizIds: [BIZ_IDS[1]]
                },
                {expectedPendingMocks: 1}
            );
            expect((await getAllBusinesses())[removedBizId]).toEqual(undefined);
        });

        it('should successfully update story if snippet backend responds with error', async () => {
            const addedBizId = BIZ_IDS[3];
            nockSnippetsOnPut(Long.fromString(addedBizId), {
                isSnippetsError: true
            });
            await updateStoryRequest(STORY_ID, {
                bizIds: [BIZ_IDS[0], BIZ_IDS[1], addedBizId]
            });
            expect((await getAllBusinesses())[addedBizId]).toEqual([
                {
                    id: '1',
                    order: 0
                }
            ]);
        });

        it('should skip snippet update if organization already has stories', async () => {
            const addedBizId = BIZ_IDS[2];
            nockSnippetsOnPut(Long.fromString(addedBizId));
            await updateStoryRequest(
                STORY_ID,
                {
                    bizIds: [BIZ_IDS[0], BIZ_IDS[1], addedBizId]
                },
                {expectedPendingMocks: 2}
            );
        });

        it('should skip snippet update if organization has stories left', async () => {
            const removedBizId = BIZ_IDS[2];
            nockSnippetsOnDelete(Long.fromString(removedBizId));
            await updateStoryRequest(
                STORY_ID,
                {
                    bizIds: [BIZ_IDS[0]]
                },
                {expectedPendingMocks: 2}
            );
        });
    });
});

async function validateSuccessfulRequest(request: RequestV2.UpdateStory): Promise<void> {
    const lookupId = '1';
    if (request.data) {
        nockModeration(request.data);
    }
    const response = await updateStoryRequest(lookupId, request);
    if (request.data) {
        await waitForPendingMocks(0);
    }
    expect(response.status).toEqual(204);
    expect(response.data).toEqual('');
}

function validateFailedRequestWithData(
    extension: Partial<RequestV2.UpdateStory['data']>,
    expectedError: ExpectedError
): Promise<void> {
    return validateFailedRequest(
        STORY_ID,
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
    storyId: string,
    body: RequestV2.UpdateStory,
    expectedError: ExpectedError,
    errorCode?: number
): Promise<void> {
    const response = await updateStoryRequest(storyId, body);
    expect(response.status).toEqual(errorCode || 400);
    expect(response.data).toEqual(expectedError);
}
