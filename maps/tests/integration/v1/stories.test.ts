import * as http from 'http';
import * as nock from 'nock';
import * as got from 'got';
import * as Long from 'long';
import * as faker from 'faker';
import {StoriesIntTypes, StoriesIntSchemas, bvmSchemas} from '@yandex-int/geosmb-server-proto';
import {app} from 'app/app';
import {startServer, stopServer} from 'tests/integration/test-server';
import {intHostConfigLoader} from 'app/lib/host-loader';
import {
    FetchStoriesRequest,
    FetchStoriesResponse,
    StoryButtonType,
    StoryButtonTypeLink,
    StoryScreenType,
    Story
} from 'app/v1/routers/stories';

const request: FetchStoriesRequest = {
    permalink: '123',
    offset: 0,
    pageSize: 5,
    isOwner: false,
    isOnlineBusiness: false
};

const client = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    responseType: 'buffer'
});

describe('/v1/stories/fetch', () => {
    let server: http.Server;
    let url: string;
    let storiesIntHost: string;
    let bvmIntHost: string;

    let nonMatchedRequests: string[] = [];
    const handleNonMatchRequests = (req: unknown): void => {
        if (req instanceof http.ClientRequest) {
            nonMatchedRequests.push(req.path);
        }
    };

    function validateNoMissedRequests(): void {
        const nonMatched = [...nonMatchedRequests];
        nonMatchedRequests = [];
        expect(nonMatched).toEqual([]);
    }

    function validateNoPendingMocksRequests(): void {
        const pendingMocks = nock.pendingMocks();
        nock.cleanAll();
        expect(pendingMocks).toEqual([]);
    }

    beforeAll(async () => {
        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);
        nock.emitter.on('no match', handleNonMatchRequests);

        const {bvmInt, storiesInt} = await intHostConfigLoader.get();
        storiesIntHost = storiesInt;
        bvmIntHost = bvmInt;
    });

    afterAll(async () => {
        await stopServer(server);
        nock.enableNetConnect();
        nock.emitter.off('no match', handleNonMatchRequests);
    });

    afterEach(() => {
        validateNoPendingMocksRequests();
        validateNoMissedRequests();
    });

    it('should return valid result from storiesInt', async () => {
        nockBvmIntWith(200, bvmSchemas.fetchNoCreateBizIdOutput.encode({
            bizId: Long.fromNumber(123)
        }));
        const [intStory, story] = getTestStories();
        nockStoriesIntWith(200, StoriesIntSchemas.storiesByBizIdResponse.encode({
            totalCount: 1,
            results: [intStory]
        }));

        const response = await fetchStories();
        expect(response.statusCode).toEqual(200);
        const expectedResponse: FetchStoriesResponse = {
            totalCount: 1,
            results: [story]
        };
        expect(response.body).toEqual(expectedResponse);
    });

    it('should return result without unpublished stories', async () => {
        nockBvmIntWith(200, bvmSchemas.fetchNoCreateBizIdOutput.encode({
            bizId: Long.fromNumber(123)
        }));
        const [firstIntStory, firstStory] = getTestStories();
        const [secondIntStory] = getTestStories();
        nockStoriesIntWith(200, StoriesIntSchemas.storiesByBizIdResponse.encode({
            totalCount: 2,
            results: [firstIntStory, {
                ...secondIntStory,
                published: false
            }]
        }));

        const response = await fetchStories();
        const expectedResponse: FetchStoriesResponse = {
            totalCount: 2,
            results: [firstStory]
        };
        expect(response.body).toEqual(expectedResponse);
    });

    it('should proxy valid error from storiesInt', async () => {
        nockBvmIntWith(200, bvmSchemas.fetchNoCreateBizIdOutput.encode({
            bizId: Long.fromNumber(123)
        }));
        nockStoriesIntWith(400, StoriesIntSchemas.error.encode({
            code: StoriesIntTypes.ErrorCode.INCORRECT_DATA,
            description: 'StoriesInt error'
        }));

        const response = await fetchStories();
        expect(response.statusCode).toEqual(400);
        expect(response.body).toEqual({
            error: 'Bad Request',
            message: 'StoriesInt error',
            statusCode: 400
        });
    });

    it('should return error when storiesInt is unavailable', async () => {
        nockBvmIntWith(200, bvmSchemas.fetchNoCreateBizIdOutput.encode({
            bizId: Long.fromNumber(123)
        }));
        nockStoriesIntWith(500);

        const response = await fetchStories();
        expect(response.statusCode).toEqual(500);
        expect(response.body).toEqual({
            error: 'Internal Server Error',
            message: 'An internal server error occurred',
            statusCode: 500
        });
    });

    it('should return empty response when bvmInt responds with no bizId', async () => {
        nockBvmIntWith(200, bvmSchemas.fetchNoCreateBizIdOutput.encode({}));

        const response = await fetchStories();
        expect(response.statusCode).toEqual(200);
        const expectedResponse: FetchStoriesResponse = {
            totalCount: 0,
            results: []
        };
        expect(response.body).toEqual(expectedResponse);
    });

    it('should return error when bvmInt is unavailable', async () => {
        nockBvmIntWith(500);

        const response = await fetchStories();
        expect(response.statusCode).toEqual(500);
        expect(response.body).toEqual({
            error: 'Internal Server Error',
            message: 'An internal server error occurred',
            statusCode: 500
        });
    });

    describe('query params is missing', () => {
        it('should return error if "permalink" param is missing', async () => {
            const response = await fetchStories({permalink: undefined});
            expect(response.statusCode).toEqual(400);
            expect(response.body).toEqual({
                error: 'Bad Request',
                message: 'ValidationError: "permalink" is required',
                statusCode: 400
            });
        });

        it('should return error if "offset" param is missing', async () => {
            const response = await fetchStories({offset: undefined});
            expect(response.statusCode).toEqual(400);
            expect(response.body).toEqual({
                error: 'Bad Request',
                message: 'ValidationError: "offset" is required',
                statusCode: 400
            });
        });

        it('should return error if "pageSize" param is missing', async () => {
            const response = await fetchStories({pageSize: undefined});
            expect(response.statusCode).toEqual(400);
            expect(response.body).toEqual({
                error: 'Bad Request',
                message: 'ValidationError: "pageSize" is required',
                statusCode: 400
            });
        });

        it('should return error if "pageSize" param is zero', async () => {
            const response = await fetchStories({pageSize: 0});
            expect(response.statusCode).toEqual(400);
            expect(response.body).toEqual({
                error: 'Bad Request',
                message: 'ValidationError: "pageSize" must be a positive number',
                statusCode: 400
            });
        });
    });

    function nockBvmIntWith(responseCode: number, body?: Buffer): void {
        nock(bvmIntHost)
            .post('/v1/fetch_no_create_biz_id')
            .reply(responseCode, body);
    }

    function nockStoriesIntWith(responseCode: number, body?: Buffer): void {
        nock(storiesIntHost)
            .post('/v1/get_stories_by_biz_id')
            .reply(responseCode, body);
    }

    function fetchStories(params?: Record<string, unknown>): Promise<{statusCode: number, body: any}> {
        const searchParams = {
            ...request,
            ...params
        };
        for (const [key, value] of Object.entries(searchParams)) {
            if (value === undefined) {
                delete searchParams[key as keyof typeof searchParams];
            }
        }
        return client.get(`${url}/v1/stories/fetch`, {
            searchParams,
            responseType: 'json'
        });
    }
});

function getTestStories(): [StoriesIntTypes.Story, Story] {
    const title = faker.commerce.productName();
    const coverImageUrl = faker.image.imageUrl();
    const screens = [{
        url: faker.image.imageUrl(),
        button: {
            title: faker.commerce.productName(),
            url: faker.image.imageUrl()
        }
    }];

    const intStory: StoriesIntTypes.Story = {
        id: Long.fromNumber(1),
        bizIds: [],
        published: true,
        createdAt: {
            seconds: Long.fromNumber(1),
            nanos: 0
        },
        moderation: {
            status: StoriesIntTypes.Moderation.Status.APPROVED,
            reasons: []
        },
        data: {
            title,
            coverUrlTemplate: coverImageUrl,
            screens: screens.map((screen) => ({
                photo: {
                    urlTemplate: screen.url
                },
                buttons: screen.button ?
                [{
                    openUrlButton: {
                        linkType: StoriesIntTypes.StoryScreenButtonLinkType.EXTERNAL_LINK,
                        title: screen.button.title,
                        url: screen.button.url
                    }
                }] :
                []
            }))
        }
    };
    const story: Story = {
        id: '1',
        title,
        coverImageUrlTemplate: coverImageUrl,
        createdAt: 1000,
        screens: screens.map((screen, index) => ({
            id: index.toString(),
            type: StoryScreenType.PHOTO,
            buttons: screen.button ?
            [{
                type: StoryButtonType.DEFAULT,
                name: screen.button.title,
                url: screen.button.url
            }] :
            [],
            buttonsV2: screen.button ?
            [{
                type: StoryButtonType.OPEN_URL,
                typeLink: StoryButtonTypeLink.EXTERNAL_LINK,
                title: screen.button.title,
                tags: [],
                backgroundColor: '#3cb300',
                titleColor: '#fff',
                url: screen.button.url
            }] :
            [],
            content: [{
                width: 1080,
                height: 1920,
                urlTemplate: screen.url
            }]
        }))
    };

    return [intStory, story];
}
