import {Table, Platform} from 'app/types/consts';
import {PlatformsSchema} from 'app/types/db/stories-content';

const rows: PlatformsSchema[] = [
    {
        story_content_id: 1,
        platform: Platform.ANDROID
    },
    {
        story_content_id: 1,
        platform: Platform.IOS
    },
    {
        story_content_id: 1,
        platform: Platform.MORDA
    },
    {
        story_content_id: 2,
        platform: Platform.IOS
    },
    {
        story_content_id: 2,
        platform: Platform.MORDA
    },
    {
        story_content_id: 3,
        platform: Platform.ANDROID
    },
    {
        story_content_id: 3,
        platform: Platform.IOS
    },
    {
        story_content_id: 3,
        platform: Platform.MORDA
    },
    {
        story_content_id: 4,
        platform: Platform.IOS
    },
    {
        story_content_id: 4,
        platform: Platform.MORDA
    },
    {
        story_content_id: 5,
        platform: Platform.IOS
    },
    {
        story_content_id: 5,
        platform: Platform.MORDA
    },
    {
        story_content_id: 6,
        platform: Platform.IOS
    }
];

const storiesContentPlatforms = {
    table: Table.STORIES_CONTENT_PLATFORMS,
    rows
};

export {storiesContentPlatforms};
