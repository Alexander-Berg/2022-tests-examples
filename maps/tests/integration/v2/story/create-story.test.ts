import Long from 'long';
import lodash from 'lodash';
import {ErrorCode} from '../../../../app/lib/api-error';
import {DbData, StoryDbRecord} from '../../../../app/types/db';
import {RequestV2, StoryScreen, StoryScreenOpenUrlButton} from '../../../../app/v2/types';
import {
    BIZ_IDS,
    getOpenUrlButton,
    getTestStory,
    getTestStoryContentItem,
    getTestStoryScreen
} from '../../../fixtures/story.v2';
import {ExpectedError} from '../../../types';
import {AllBusinesses, getAllBusinesses, getAllStories} from '../../../utils/db-utils';
import {nockModeration, nockSnippetsOnPut, waitForPendingMocks} from '../../../utils/nock-utils';
import {prepareTests} from '../../../utils/test-utils';
import {createStoryRequest, simpleCreateStory} from '../../../utils/v2/request-utils';
import {waitForExpected} from '../../../utils/promise-utils';

describe('POST /v2/story', () => {
    prepareTests();

    describe('fail cases', () => {
        it('should fail on biz ids empty array', async () => {
            await validateFailedRequest(
                {
                    bizIds: []
                },
                {
                    code: ErrorCode.VALIDATION_ERROR,
                    description: 'Invalid input data: "bizIds" must contain at least 1 items'
                }
            );
        });

        it('should fail if title is too short', async () => {
            await validateFailedRequestWithData(
                {
                    title: ''
                },
                {
                    code: ErrorCode.VALIDATION_ERROR,
                    description: 'Invalid input data: "data.title" is not allowed to be empty'
                }
            );
        });

        it('should fail if title is too long', async () => {
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

        it('should fail if there are too few screens', async () => {
            await validateFailedRequestWithData(
                {
                    screens: []
                },
                {
                    code: ErrorCode.VALIDATION_ERROR,
                    description: 'Invalid input data: "data.screens" must contain at least 1 items'
                }
            );
        });

        it('should fail if there are too many screens', async () => {
            await validateFailedRequestWithData(
                {
                    screens: Array(11).fill(getTestStoryScreen())
                },
                {
                    code: ErrorCode.VALIDATION_ERROR,
                    description: 'Invalid input data: "data.screens" must contain less than or equal to 10 items'
                }
            );
        });

        it('should fail is there are too few photos in screen', async () => {
            await validateFailedRequestWithScreen(
                {
                    photo: undefined
                },
                {
                    code: ErrorCode.VALIDATION_ERROR,
                    description: 'Invalid input data: "data.screens[0].photo" is required'
                }
            );
        });

        it('should fail if url template uri schema is invalid', async () => {
            await validateFailedRequestWithScreen(
                {
                    photo: {
                        ...getTestStoryContentItem(),
                        urlTemplate: 'foo://foo.bar/img.png'
                    }
                },
                {
                    code: ErrorCode.VALIDATION_ERROR,
                    description:
                        'Invalid input data: "data.screens[0].photo.urlTemplate" must be a valid uri with a scheme matching the http|https pattern'
                }
            );
        });

        it('should fail if there are too many buttons', async () => {
            await validateFailedRequestWithScreen(
                {
                    buttons: Array(2).fill(getOpenUrlButton())
                },
                {
                    code: ErrorCode.VALIDATION_ERROR,
                    description:
                        'Invalid input data: "data.screens[0].buttons" must contain less than or equal to 1 items'
                }
            );
        });

        it('should fail if button title is too short', async () => {
            await validateFailedRequestWithOpenUrlButton(
                {
                    title: ''
                },
                {
                    code: ErrorCode.VALIDATION_ERROR,
                    description:
                        'Invalid input data: "data.screens[0].buttons[0].openUrlButton.title" is not allowed to be empty'
                }
            );
        });

        it('should fail if button title is too long', async () => {
            await validateFailedRequestWithOpenUrlButton(
                {
                    title: 'x'.repeat(36)
                },
                {
                    code: ErrorCode.VALIDATION_ERROR,
                    description:
                        'Invalid input data: "data.screens[0].buttons[0].openUrlButton.title" length must be less than or equal to 35 characters long'
                }
            );
        });

        it('should fail if button url contains invalid characters', async () => {
            await validateFailedRequestWithOpenUrlButton(
                {
                    url: 'foo.bar/ba z'
                },
                {
                    code: ErrorCode.VALIDATION_ERROR,
                    description:
                        'Invalid input data: "data.screens[0].buttons[0].openUrlButton.url" must be a valid uri with a scheme matching the http|https|yandexmaps pattern'
                }
            );
        });

        it('should fail if button url schema is invalid', async () => {
            await validateFailedRequestWithOpenUrlButton(
                {
                    url: 'foo://bar.baz/'
                },
                {
                    code: ErrorCode.VALIDATION_ERROR,
                    description:
                        'Invalid input data: "data.screens[0].buttons[0].openUrlButton.url" must be a valid uri with a scheme matching the http|https|yandexmaps pattern'
                }
            );
        });
    });

    describe('success cases', () => {
        it('should successfully create one story', async () => {
            const bizId = BIZ_IDS[0];
            nockSnippetsOnPut(Long.fromString(bizId));
            const story = getTestStory(bizId);
            const now = Date.now();
            nockModeration(story.data);
            const response = await createStoryRequest(story);
            expect(response.status).toEqual(200);
            expect(response.data).toEqual({id: '1'});

            const storyRecords = await getAllStories();
            const storyRecord = storyRecords[0];
            const createdAt = storyRecord.created_at.valueOf();
            expect(createdAt).toBeGreaterThan(now);
            expect(createdAt).toBeLessThan(now + 2000);

            const moderationStatusSentTimestamp = storyRecord.moderation_status_sent?.valueOf();
            expect(moderationStatusSentTimestamp).toBeGreaterThan(now);
            expect(moderationStatusSentTimestamp).toBeLessThan(now + 2000);

            const expectedStoryRecord: StoryDbRecord = {
                id: '1',
                published: true,
                deleted: false,
                created_at: storyRecord.created_at,
                moderation_status: 'sent',
                moderation_status_sent: storyRecord.moderation_status_sent,
                moderation_status_changed: null,
                trusted: true,
                rejected_reasons: [],
                data: {
                    ...story.data,
                    version: 1
                }
            };
            expect(storyRecord).toEqual(expectedStoryRecord);
            const businessRecords = await getAllBusinesses();
            const expectedBusinessRecords: AllBusinesses = {
                [bizId]: [
                    {
                        id: '1',
                        order: 0
                    }
                ]
            };
            expect(businessRecords).toEqual(expectedBusinessRecords);
        });

        it('should successfully create a story for several business ids', async () => {
            const story = getTestStory(BIZ_IDS[0], BIZ_IDS[1]);

            nockSnippetsOnPut([BIZ_IDS[0], BIZ_IDS[1]].map((id) => Long.fromString(id)));
            nockModeration(story.data);

            const response = await createStoryRequest(story);

            expect(response.status).toEqual(200);
            expect(response.data).toEqual({id: '1'});

            const businessRecords = await getAllBusinesses();
            const expectedBusinessRecords: AllBusinesses = {
                [BIZ_IDS[0]]: [
                    {
                        id: '1',
                        order: 0
                    }
                ],
                [BIZ_IDS[1]]: [
                    {
                        id: '1',
                        order: 0
                    }
                ]
            };
            expect(businessRecords).toEqual(expectedBusinessRecords);
        });

        it('should successfully create a story for duplicating business ids', async () => {
            const story = getTestStory(BIZ_IDS[0], BIZ_IDS[0]);

            nockSnippetsOnPut(Long.fromString(BIZ_IDS[0]));
            nockModeration(story.data);

            const response = await createStoryRequest(story);

            expect(response.status).toEqual(200);
            expect(response.data).toEqual({id: '1'});

            const businessRecords = await getAllBusinesses();
            const expectedBusinessRecords: AllBusinesses = {
                [BIZ_IDS[0]]: [
                    {
                        id: '1',
                        order: 0
                    }
                ]
            };
            expect(businessRecords).toEqual(expectedBusinessRecords);
        });

        it('should successfully create one story with tags', async () => {
            const bizId = BIZ_IDS[0];
            nockSnippetsOnPut(Long.fromString(bizId));
            const story = getTestStory(bizId);
            const storyWithTags = lodash.cloneDeep(story);
            storyWithTags.data.tags = ['lastMile'];
            nockModeration(storyWithTags.data);
            const response = await createStoryRequest(storyWithTags);
            expect(response.status).toEqual(200);
            expect(response.data).toEqual({id: '1'});

            const storyRecords = await getAllStories();
            const storyRecord = storyRecords[0];

            const expectedStoryData: DbData = {
                ...storyWithTags.data,
                version: 1
            };
            expect(storyRecord.data).toEqual(expectedStoryData);
        });

        it('should successfully trim story data', async () => {
            const bizId = BIZ_IDS[0];
            const story = getTestStory(bizId);
            const storyClone = lodash.cloneDeep(story);
            storyClone.data.screens[0].buttons[0].openUrlButton.url += '  ';
            nockSnippetsOnPut(Long.fromString(bizId));
            nockModeration(story.data);
            await createStoryRequest(storyClone);

            const storyRecords = await getAllStories();
            expect(storyRecords[0].data.screens[0].buttons[0].openUrlButton.url).toEqual(
                story.data.screens[0].buttons[0].openUrlButton.url
            );
        });

        it('should successfully create two stories with different businesses', async () => {
            const {request: firstRequest} = await simpleCreateStory(BIZ_IDS[0]);
            const {request: secondRequest} = await simpleCreateStory(BIZ_IDS[0], BIZ_IDS[1]);

            const storyRecords = await getAllStories();
            expect(storyRecords.length).toEqual(2);
            const expectedStoryRecords: StoryDbRecord[] = [firstRequest, secondRequest].map((story, index) => ({
                id: String(index + 1),
                published: true,
                deleted: false,
                created_at: storyRecords[index].created_at,
                moderation_status: 'sent',
                moderation_status_sent: storyRecords[index].moderation_status_sent,
                moderation_status_changed: null,
                trusted: true,
                rejected_reasons: [],
                data: {
                    ...story.data,
                    version: 1
                }
            }));
            expect(storyRecords).toEqual(expectedStoryRecords);
            const businessRecords = await getAllBusinesses();
            const expectedBusinessRecords: AllBusinesses = {
                [BIZ_IDS[0]]: [
                    {
                        id: '1',
                        order: 0
                    },
                    {
                        id: '2',
                        order: 1
                    }
                ],
                [BIZ_IDS[1]]: [
                    {
                        id: '2',
                        order: 0
                    }
                ]
            };
            expect(businessRecords).toEqual(expectedBusinessRecords);
        });

        it('should set moderation status as unknown on clean web backend error', async () => {
            nockSnippetsOnPut(Long.fromString(BIZ_IDS[0]));
            const story = getTestStory(BIZ_IDS[0]);
            nockModeration(story.data, {isError: true});
            await createStoryRequest(story);

            await waitForPendingMocks();
            await waitForExpected(async () => {
                const stories = await getAllStories();
                expect(stories[0].moderation_status_sent).toEqual(null);
                expect(stories[0].moderation_status).toEqual('unknown');
                expect(stories[0].rejected_reasons).toEqual([]);
            }, 'Moderation status sent timestamp being falsy');
        });

        it('should properly handle story without buttons', async () => {
            const bizId = BIZ_IDS[0];
            nockSnippetsOnPut(Long.fromString(bizId));
            const story = getTestStory(bizId);
            story.data.screens[0].buttons = [];
            nockModeration(story.data);
            await createStoryRequest(story);
        });
    });

    describe('snippets cases', () => {
        it('should successfully create story if bvm int responds with error', async () => {
            const bizId = BIZ_IDS[0];
            nockSnippetsOnPut(Long.fromString(bizId), {
                isBvmError: true
            });
            const story = getTestStory(bizId);
            nockModeration(story.data);
            await createStoryRequest(story, {expectedPendingMocks: 1});
            expect((await getAllStories()).length).toEqual(1);
        });

        it('should successfully create story if snippet backend responds with error', async () => {
            const bizId = BIZ_IDS[0];
            nockSnippetsOnPut(Long.fromString(bizId), {
                isSnippetsError: true
            });
            const story = getTestStory(bizId);
            nockModeration(story.data);
            await createStoryRequest(story);
            expect((await getAllStories()).length).toEqual(1);
        });

        it('should skip snippet update if organization already has stories', async () => {
            const bizId = BIZ_IDS[0];
            await simpleCreateStory(bizId);

            nockSnippetsOnPut(Long.fromString(bizId));
            const secondStory = getTestStory(bizId);
            nockModeration(secondStory.data);
            await createStoryRequest(secondStory, {expectedPendingMocks: 2});
        });
    });
});

function validateFailedRequestWithOpenUrlButton(
    extension: Partial<StoryScreenOpenUrlButton['openUrlButton']>,
    expectedError: ExpectedError
): Promise<void> {
    return validateFailedRequestWithScreen(
        {
            buttons: [
                {
                    openUrlButton: {
                        ...getOpenUrlButton().openUrlButton,
                        ...extension
                    }
                }
            ]
        },
        expectedError
    );
}

function validateFailedRequestWithScreen(extension: Partial<StoryScreen>, expectedError: ExpectedError): Promise<void> {
    return validateFailedRequestWithData(
        {
            screens: [
                {
                    ...getTestStoryScreen(),
                    ...extension
                }
            ]
        },
        expectedError
    );
}

function validateFailedRequestWithData(
    extension: Partial<RequestV2.CreateStory['data']>,
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
    extension: Partial<RequestV2.CreateStory>,
    expectedError: ExpectedError
): Promise<void> {
    const story = getTestStory(BIZ_IDS[0]);
    const response = await createStoryRequest({
        ...story,
        ...extension
    });
    expect(response.status).toEqual(400);
    expect(response.data).toEqual(expectedError);
}
