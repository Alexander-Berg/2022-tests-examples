import {StoryDbRecord} from '../../../../app/types/db';
import {
    KNOWN_TOLOKA_REASONS,
    KNOWN_YANG_REASONS,
    ModerationSenderData,
    ModerationSentStatus,
    RejectedReason,
    RejectedReasonType
} from '../../../../app/types/moderation';
import {BIZ_IDS} from '../../../fixtures/story.v2';
import {getAllStories} from '../../../utils/db-utils';
import {getModerationEndVerdict, getVerdict} from '../../../utils/moderation-utils';
import {prepareTests} from '../../../utils/test-utils';
import {simpleCreateStory, storiesSetModerationStatus} from '../../../utils/v2/request-utils';

describe('POST /v2/stories/set_moderation_status', () => {
    const bizId = BIZ_IDS[0];

    prepareTests();

    beforeEach(async () => {
        await simpleCreateStory(bizId);
    });

    it('should fail on invalid input', async () => {
        const response = await storiesSetModerationStatus({
            verdicts: [{}]
        } as unknown as ModerationSenderData);
        expect(response.status).toEqual(400);
    });

    it('should fail on invalid key', async () => {
        const response = await storiesSetModerationStatus({
            verdicts: [getModerationEndVerdict('invalid')]
        });
        expect(response.status).toEqual(400);
    });

    it('should successfully change moderation status to approved', async () => {
        const story = (await getAllStories())[0];
        const response = await storiesSetModerationStatus({
            verdicts: [getModerationEndVerdict(story)]
        });
        expect(response.status).toEqual(204);
        await validateModerationStatus('1', 'received', []);
        await validateTrusted('1', true);
    });

    it('should successfully change moderation status to rejected', async () => {
        const story = (await getAllStories())[0];
        const response = await storiesSetModerationStatus({
            verdicts: [getVerdict(story, 'policy')]
        });
        expect(response.status).toEqual(204);
        await validateModerationStatus('1', 'received', [
            {
                type: 'policy',
                entity: 'story'
            }
        ]);
        await validateTrusted('1', false);
    });

    it('should not make story trusted after single rejection and later approval', async () => {
        const story = (await getAllStories())[0];

        await storiesSetModerationStatus({
            verdicts: [getModerationEndVerdict(story)]
        });
        await validateModerationStatus('1', 'received', []);
        await validateTrusted('1', true);

        await storiesSetModerationStatus({
            verdicts: [getVerdict(story, 'policy')]
        });
        await validateModerationStatus('1', 'received', [
            {
                type: 'policy',
                entity: 'story'
            }
        ]);
        await validateTrusted('1', false);

        await storiesSetModerationStatus({
            verdicts: [getModerationEndVerdict(story)]
        });
        await validateModerationStatus('1', 'received', []);
        await validateTrusted('1', false);
    });

    it('should treat unknown key as valid rejected reason', async () => {
        const story = (await getAllStories())[0];
        const response = await storiesSetModerationStatus({
            verdicts: [getVerdict(story, 'unknown'), getModerationEndVerdict(story)]
        });
        expect(response.status).toEqual(204);
        await validateModerationStatus('1', 'received', [
            {
                type: 'unknown',
                entity: 'story'
            }
        ]);
    });

    it('should treat advert key as approved verdict', async () => {
        const story = (await getAllStories())[0];
        const response = await storiesSetModerationStatus({
            verdicts: [getVerdict(story, 'advert'), getModerationEndVerdict(story)]
        });
        expect(response.status).toEqual(204);
        await validateModerationStatus('1', 'received', []);
    });

    it('should successfully store all rejected reasons', async () => {
        const story = (await getAllStories())[0];
        const allReasons: RejectedReasonType[] = [...KNOWN_TOLOKA_REASONS, ...KNOWN_YANG_REASONS, 'unknown'];
        const response = await storiesSetModerationStatus({
            verdicts: allReasons.map((reason) => getVerdict(story, reason))
        });
        expect(response.status).toEqual(204);
        const ignoredReasons: RejectedReasonType[] = ['advert', 'no_object', 'meaningless_text'];
        await validateModerationStatus(
            '1',
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

async function validateStory(storyId: string, expect: (story: StoryDbRecord) => void): Promise<void> {
    const stories = await getAllStories();
    const story = stories.find((story) => story.id === storyId);
    if (!story) {
        throw new Error(`Story ${storyId} does not exist!`);
    }
    expect(story);
}

async function validateModerationStatus(
    storyId: string,
    moderationStatus: ModerationSentStatus,
    reasons: RejectedReason[]
): Promise<void> {
    await validateStory(storyId, (story) => {
        expect(story.moderation_status).toEqual(moderationStatus);
        expect(story.rejected_reasons).toEqual(reasons);
    });
}

async function validateTrusted(storyId: string, trusted: boolean): Promise<void> {
    await validateStory(storyId, (story) => {
        expect(story.trusted).toEqual(trusted);
    });
}
