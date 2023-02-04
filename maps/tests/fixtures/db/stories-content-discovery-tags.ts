import {Table} from 'app/types/consts';
import {TagsSchema} from 'app/types/db/stories-content';

const rows: TagsSchema[] = [
    {
        story_content_id: 1,
        tag_id: 1
    },
    {
        story_content_id: 1,
        tag_id: 2
    }
];

const storiesContentDiscoveryTags = {
    table: Table.STORIES_CONTENT_DISCOVERY_TAGS,
    rows
};

export {storiesContentDiscoveryTags};
