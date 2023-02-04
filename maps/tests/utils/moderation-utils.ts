import {ModerationSenderData, RejectedReasonType, TOLOKA_PREFIX} from '../../app/types/moderation';
import {isKnownTolokaReason, isKnownYangReason} from '../../app/utils/moderation-utils';
import {StoryDbRecord} from '../../app/types/db';

type ModerationVerdict = ModerationSenderData['verdicts'][number];

export function getModerationEndVerdict(storyOrStoryKey: StoryDbRecord | string): ModerationVerdict {
    const storyKey = typeof storyOrStoryKey === 'string' ? storyOrStoryKey : getStoryKey(storyOrStoryKey);
    return {
        key: storyKey,
        name: 'clean_web_moderation_end',
        value: true,
        entity: 'story',
        source: 'clean-web',
        subsource: 'clean-web'
    };
}

export function getVerdict(
    storyOrStoryKey: StoryDbRecord | string,
    reason: RejectedReasonType | 'unknown'
): ModerationVerdict {
    const storyKey = typeof storyOrStoryKey === 'string' ? storyOrStoryKey : getStoryKey(storyOrStoryKey);
    if (isKnownTolokaReason(reason)) {
        return {
            key: storyKey,
            name: TOLOKA_PREFIX + reason,
            value: true,
            entity: 'story',
            source: 'clean-web',
            subsource: 'any'
        };
    } else if (isKnownYangReason(reason)) {
        return {
            key: storyKey,
            name: reason,
            value: true,
            entity: 'story',
            source: 'clean-web',
            subsource: 'any'
        };
    }
    return {
        key: storyKey,
        name: reason,
        value: true,
        entity: 'story',
        source: 'clean-web',
        subsource: 'unknown'
    };
}

function getStoryKey(story: StoryDbRecord): string {
    return `${story.id.toString()}:${story.moderation_status_sent?.valueOf() ?? 0}`;
}
