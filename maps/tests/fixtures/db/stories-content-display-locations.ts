import {Table, StoryDisplayLocation} from 'app/types/consts';
import {DisplayLocationSchema} from 'app/types/db/stories-content';

const rows: DisplayLocationSchema[] = [
    {
        story_content_id: 1,
        display_location: StoryDisplayLocation.CONTENT
    },
    {
        story_content_id: 1,
        display_location: StoryDisplayLocation.HEADER
    },
    {
        story_content_id: 2,
        display_location: StoryDisplayLocation.HEADER
    },
    {
        story_content_id: 3,
        display_location: StoryDisplayLocation.CONTENT
    },
    {
        story_content_id: 3,
        display_location: StoryDisplayLocation.HEADER
    },
    {
        story_content_id: 4,
        display_location: StoryDisplayLocation.HEADER
    },
    {
        story_content_id: 5,
        display_location: StoryDisplayLocation.EMPTY
    },
    {
        story_content_id: 5,
        display_location: StoryDisplayLocation.HEADER
    },
    {
        story_content_id: 6,
        display_location: StoryDisplayLocation.EMPTY
    }
];

const storiesContentDisplayLocations = {
    table: Table.STORIES_CONTENT_DISPLAY_LOCATIONS,
    rows
};

export {storiesContentDisplayLocations};
