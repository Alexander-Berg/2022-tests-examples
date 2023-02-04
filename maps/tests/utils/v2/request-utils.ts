import Long from 'long';
import * as Got from 'got/dist/source/types';
import {getTestStory} from '../../fixtures/story.v2';
import {ErrorBody} from '../../../app/lib/api-error';
import {RequestV2, ResponseV2} from '../../../app/v2/types';
import {nockModeration, nockSnippetsOnPut} from '../nock-utils';
import {jsonClient, makeRequestGenerator, RequestGenerator} from '../request-utils';
import {getAllBusinesses, getAllStories, runWriteQuery} from '../db-utils';
import {ModerationSenderData} from '../../../app/types/moderation';

type SuccessResponse<T = unknown> = {
    status: 200;
    data: T;
};

type FailResponse = {
    status: Exclude<number, 200>;
    data: ErrorBody;
};

type V2Response<T = unknown> = SuccessResponse<T> | FailResponse;

type V2RequestGenerator<ResProto> = RequestGenerator<Got.OptionsOfJSONResponseBody, V2Response<ResProto | ErrorBody>>;

function makeV2RequestGenerator<ResProto>(path: string, httpMethod: Got.HTTPAlias): V2RequestGenerator<ResProto> {
    return makeRequestGenerator<Got.OptionsOfJSONResponseBody, V2Response<ResProto | ErrorBody>, ResProto | ErrorBody>(
        (data) => {
            return jsonClient[httpMethod](`v2/${path}`, data);
        },
        (result) => {
            const {body, statusCode} = result;
            if (statusCode !== 200) {
                return {
                    status: statusCode,
                    data: body as ErrorBody
                };
            }
            return {
                status: statusCode,
                data: body
            };
        }
    );
}

const createStoryRequest = (json: RequestV2.CreateStory, options = {}) =>
    makeV2RequestGenerator<ResponseV2.CreateStory>('story', 'post')({json}, options);

const updateStoryRequest = (storyId: string, json: RequestV2.UpdateStory, options = {}) =>
    makeV2RequestGenerator<void>(`story/${storyId}`, 'put')({json}, options);

const deleteStoryRequest = (storyId: string, options = {}) =>
    makeV2RequestGenerator<void>(`story/${storyId}`, 'delete')({}, options);

const getStoryRequest = (storyId: string, options = {}) =>
    makeV2RequestGenerator<ResponseV2.GetStory>(`story/${storyId}`, 'get')({}, options);

const getStories = (bizId: string, searchParams: RequestV2.GetStories, options = {}) =>
    makeV2RequestGenerator<ResponseV2.GetStories>(`stories/${bizId}`, 'get')({searchParams}, options);

const storiesSetModerationStatus = (json: ModerationSenderData, options = {}) =>
    makeV2RequestGenerator<void>('stories/set_moderation_status', 'post')({json}, options);

const storiesReorder = (bizId: string, json: RequestV2.ReorderStories, options = {}) =>
    makeV2RequestGenerator<void>(`/stories/${bizId}/reorder`, 'post')({json}, options);

type SimpleCreateStoryResult = {
    id: string;
    request: RequestV2.CreateStory;
};

async function simpleChangeStoryStatus(
    storyId: string,
    type: 'approved' | 'rejected' | 'pending',
    trusted?: boolean
): Promise<void> {
    const stories = await getAllStories();
    const story = stories.find((story) => story.id === storyId);
    if (!story) {
        throw new Error(`Story ${storyId} does not exist!`);
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
                    WHERE id = ${storyId}
                `);
            break;
        case 'pending':
            await runWriteQuery(`
                    UPDATE stories
                    SET moderation_status = 'sent',
                        moderation_status_changed = NOW(),
                        ${trustedClause}
                        rejected_reasons = '{}'::jsonb[]
                    WHERE id = ${storyId}
                `);
            break;
        case 'rejected':
            await runWriteQuery(`
                    UPDATE stories
                    SET moderation_status = 'received',
                        moderation_status_changed = NOW(),
                        ${trustedClause}
                        rejected_reasons = ARRAY['{"entity":"foo","type":"policy"}'::jsonb]
                    WHERE id = ${storyId}
                `);
            break;
    }
}

async function simpleCreateStory(...bizIds: string[]): Promise<SimpleCreateStoryResult> {
    const businesses = await getAllBusinesses();
    const nonExistentBusinesses = bizIds.filter((bizId) => !businesses[bizId]);
    if (nonExistentBusinesses.length !== 0) {
        nockSnippetsOnPut(nonExistentBusinesses.map((bizId) => Long.fromString(bizId)));
    }
    const request = getTestStory(...bizIds);
    nockModeration(request.data);
    const result = await createStoryRequest(request);
    const id = (result.data as ResponseV2.CreateStory).id;
    return {
        id,
        request
    };
}

export {
    simpleCreateStory,
    createStoryRequest,
    updateStoryRequest,
    getStoryRequest,
    deleteStoryRequest,
    getStories,
    storiesReorder,
    simpleChangeStoryStatus,
    storiesSetModerationStatus
};
