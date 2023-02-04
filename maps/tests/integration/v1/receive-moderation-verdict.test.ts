import * as Long from 'long';

import {getAllStories} from '../../utils/db-utils';

import {BIZ_IDS} from '../../fixtures/story';
import {receiveModerationVerdict, simpleCreateStory} from '../../utils/v1/request-utils';
import {prepareTests} from '../../utils/test-utils';

import {
    KNOWN_TOLOKA_REASONS,
    KNOWN_YANG_REASONS,
    ModerationSenderData,
    ModerationSentStatus,
    RejectedReason,
    RejectedReasonType
} from '../../../app/types/moderation';
import {getModerationEndVerdict, getVerdict} from '../../utils/moderation-utils';
import {StoryDbRecord} from '../../../app/types/db';

describe('post /v1/receive_moderation_verdict', () => {
    const bizId = BIZ_IDS[0];

    prepareTests();

    beforeEach(async () => {
        await simpleCreateStory(bizId);
    });

    it('should fail on invalid input', async () => {
        const response = await receiveModerationVerdict({
            verdicts: [{}]
        } as unknown as ModerationSenderData);
        expect(response.statusCode).toEqual(400);
    });

    it('should fail on invalid key', async () => {
        const response = await receiveModerationVerdict({
            verdicts: [getModerationEndVerdict('invalid')]
        });
        expect(response.statusCode).toEqual(400);
    });

    it('should successfully change moderation status to approved', async () => {
        const story = (await getAllStories())[0];
        const response = await receiveModerationVerdict({
            verdicts: [getModerationEndVerdict(story)]
        });
        expect(response.statusCode).toEqual(200);
        await validateModerationStatus(Long.fromNumber(1), 'received', []);
        await validateTrusted(Long.fromNumber(1), true);
    });

    it('should successfully change moderation status to rejected', async () => {
        const story = (await getAllStories())[0];
        const response = await receiveModerationVerdict({
            verdicts: [getVerdict(story, 'policy')]
        });
        expect(response.statusCode).toEqual(200);
        await validateModerationStatus(Long.fromNumber(1), 'received', [
            {
                type: 'policy',
                entity: 'story'
            }
        ]);
        await validateTrusted(Long.fromNumber(1), false);
    });

    it('should not make story trusted after single rejection and later approval', async () => {
        const story = (await getAllStories())[0];

        await receiveModerationVerdict({
            verdicts: [getModerationEndVerdict(story)]
        });
        await validateModerationStatus(Long.fromNumber(1), 'received', []);
        await validateTrusted(Long.fromNumber(1), true);

        await receiveModerationVerdict({
            verdicts: [getVerdict(story, 'policy')]
        });
        await validateModerationStatus(Long.fromNumber(1), 'received', [
            {
                type: 'policy',
                entity: 'story'
            }
        ]);
        await validateTrusted(Long.fromNumber(1), false);

        await receiveModerationVerdict({
            verdicts: [getModerationEndVerdict(story)]
        });
        await validateModerationStatus(Long.fromNumber(1), 'received', []);
        await validateTrusted(Long.fromNumber(1), false);
    });

    it('should treat unknown key as valid rejected reason', async () => {
        const story = (await getAllStories())[0];
        const response = await receiveModerationVerdict({
            verdicts: [getVerdict(story, 'unknown'), getModerationEndVerdict(story)]
        });
        expect(response.statusCode).toEqual(200);
        await validateModerationStatus(Long.fromNumber(1), 'received', [
            {
                type: 'unknown',
                entity: 'story'
            }
        ]);
    });

    it('should treat advert key as approved verdict', async () => {
        const story = (await getAllStories())[0];
        const response = await receiveModerationVerdict({
            verdicts: [getVerdict(story, 'advert'), getModerationEndVerdict(story)]
        });
        expect(response.statusCode).toEqual(200);
        await validateModerationStatus(Long.fromNumber(1), 'received', []);
    });

    it('should successfully store all rejected reasons', async () => {
        const story = (await getAllStories())[0];
        const allReasons: RejectedReasonType[] = [...KNOWN_TOLOKA_REASONS, ...KNOWN_YANG_REASONS, 'unknown'];
        const response = await receiveModerationVerdict({
            verdicts: allReasons.map((reason) => getVerdict(story, reason))
        });
        expect(response.statusCode).toEqual(200);
        const ignoredReasons: RejectedReasonType[] = ['advert', 'no_object', 'meaningless_text'];
        await validateModerationStatus(
            Long.fromNumber(1),
            'received',
            allReasons
                .filter((reason) => !ignoredReasons.includes(reason))
                .map((reason) => ({
                    type: reason,
                    entity: 'story'
                }))
        );
    });
});

async function validateStory(storyId: Long, expect: (story: StoryDbRecord) => void): Promise<void> {
    const stories = await getAllStories();
    const story = stories.find((story) => Long.fromString(story.id).eq(storyId));
    if (!story) {
        throw new Error(`Story ${storyId.toString()} does not exist!`);
    }
    expect(story);
}

async function validateModerationStatus(
    storyId: Long,
    moderationStatus: ModerationSentStatus,
    reasons: RejectedReason[]
): Promise<void> {
    await validateStory(storyId, (story) => {
        expect(story.moderation_status).toEqual(moderationStatus);
        expect(story.rejected_reasons).toEqual(reasons);
    });
}

async function validateTrusted(storyId: Long, trusted: boolean): Promise<void> {
    await validateStory(storyId, (story) => {
        expect(story.trusted).toEqual(trusted);
    });
}
