import lodash from 'lodash';
import * as Long from 'long';
import * as got from 'got';
import {StoriesIntSchemas, StoriesIntTypes} from '../../../app/proto-schemas/stories-int/types';
import {ProtoMessage} from '../../../app/lib/proto';
import {protoClient, makeRequestGenerator, RequestGenerator} from '../request-utils';
import {getTestStory} from '../../fixtures/story';
import {nockModeration, nockSnippetsOnPut} from '../nock-utils';
import {getAllBusinesses, getAllStories, runWriteQuery} from '../db-utils';
import {ModerationSenderData} from '../../../app/types/moderation';
import {ErrorBody} from '../../../app/lib/api-error';

type SuccessResponse<T = unknown> = {
    status: 200;
    data: T;
};

type FailResponse = {
    status: Exclude<number, 200>;
    data: ErrorBody;
};

type V1Response<T = unknown> = SuccessResponse<T> | FailResponse;

type V1RequestGenerator<ReqProto, ResProto> = RequestGenerator<ReqProto, V1Response<ResProto | ErrorBody>>;

function makeV1RequestGenerator<ReqProto, ResProto>(
    path: string,
    protoRequest: ProtoMessage<ReqProto>,
    protoResponse: ProtoMessage<ResProto>,
    description: string
): V1RequestGenerator<ReqProto, ResProto> {
    return makeRequestGenerator(
        (data) =>
            protoClient.post('v1/' + path, {
                body: protoRequest.encode(data)
            }),
        (result) => {
            const buffer = Buffer.from(result.body, 'binary');
            if (result.statusCode !== 200) {
                return {
                    status: result.statusCode,
                    data: decode(StoriesIntSchemas.error, buffer, 'error')
                };
            }
            return {
                status: result.statusCode,
                data: decode(protoResponse, buffer, description)
            };
        }
    );
}

function decode<T>(proto: ProtoMessage<T>, obj: Buffer, description: string): T {
    const decoded = proto.decode(obj);
    if (decoded.error) {
        throw new Error(`Error while decoding ${description}: ${decoded.error.message}`);
    }
    return decoded.data;
}

export const createStoryRequest = makeV1RequestGenerator(
    'create_story',
    StoriesIntSchemas.storyCreateRequest,
    StoriesIntSchemas.storyCreateResponse,
    'storyCreateResponse'
);

export const updateStoryRequest = makeV1RequestGenerator(
    'update_story',
    StoriesIntSchemas.storyUpdateRequest,
    StoriesIntSchemas.storyUpdateResponse,
    'storyUpdateResponse'
);

export const deleteStoryRequest = makeV1RequestGenerator(
    'delete_story',
    StoriesIntSchemas.storyDeleteRequest,
    StoriesIntSchemas.storyDeleteResponse,
    'storyDeleteResponse'
);

export const getStoriesByBizIdRequest = makeV1RequestGenerator(
    'get_stories_by_biz_id',
    StoriesIntSchemas.storiesByBizIdRequest,
    StoriesIntSchemas.storiesByBizIdResponse,
    'storiesByBizIdResponse'
);

export const getStoryByIdRequest = makeV1RequestGenerator(
    'get_story_by_id',
    StoriesIntSchemas.storyByIdRequest,
    StoriesIntSchemas.storyByIdResponse,
    'storyByIdResponse'
);

export const reorderStories = makeV1RequestGenerator(
    'reorder_stories',
    StoriesIntSchemas.reorderStoriesRequest,
    StoriesIntSchemas.reorderStoriesResponse,
    'reorderStoriesResponse'
);

export const receiveModerationVerdict = makeRequestGenerator<ModerationSenderData, got.Response<string>>(
    (data) =>
        protoClient.post('v1/receive_moderation_verdict', {
            body: JSON.stringify(data),
            headers: {
                'Content-Type': 'application/json'
            }
        }),
    lodash.identity
);

export async function simpleChangeStoryStatus(
    storyId: Long,
    type: 'approved' | 'rejected' | 'pending',
    trusted?: boolean
): Promise<void> {
    const stories = await getAllStories();
    const story = stories.find((story) => Long.fromString(story.id).eq(storyId));
    if (!story) {
        throw new Error(`Story ${storyId.toString()} does not exist!`);
    }
    const trustedClause = typeof trusted === 'boolean' ? `trusted = ${trusted},` : '';
    switch (type) {
        case 'approved':
            await runWriteQuery(`
                    UPDATE stories
                    SET moderation_status = 'received',
                        moderation_status_changed = NOW(),
                        ${trustedClause}
                        rejected_reasons = '{}'::jsonb[]
                    WHERE id = ${storyId.toString()}
                `);
            break;
        case 'pending':
            await runWriteQuery(`
                    UPDATE stories
                    SET moderation_status = 'sent',
                        moderation_status_changed = NOW(),
                        ${trustedClause}
                        rejected_reasons = '{}'::jsonb[]
                    WHERE id = ${storyId.toString()}
                `);
            break;
        case 'rejected':
            await runWriteQuery(`
                    UPDATE stories
                    SET moderation_status = 'received',
                        moderation_status_changed = NOW(),
                        ${trustedClause}
                        rejected_reasons = ARRAY['{"entity":"foo","type":"policy"}'::jsonb]
                    WHERE id = ${storyId.toString()}
                `);
            break;
    }
}

type SimpleCreateStoryResult = {
    id: Long;
    request: StoriesIntTypes.StoryCreateRequest;
};

export async function simpleCreateStory(...bizIds: Long[]): Promise<SimpleCreateStoryResult> {
    const businesses = await getAllBusinesses();
    const nonExistentBusinesses = bizIds.filter((bizId) => !businesses[bizId.toString()]);
    if (nonExistentBusinesses.length !== 0) {
        nockSnippetsOnPut(nonExistentBusinesses);
    }
    const request = getTestStory(...bizIds);
    nockModeration(request.data);
    const result = await createStoryRequest(request);
    const id = (result.data as StoriesIntTypes.StoryCreateResponse).id;
    return {
        id,
        request
    };
}
