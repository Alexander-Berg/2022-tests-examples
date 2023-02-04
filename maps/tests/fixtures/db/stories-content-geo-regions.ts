import {Table} from 'app/types/consts';
import {GeoRegionsSchema} from 'app/types/db/stories-content';

const rows: GeoRegionsSchema[] = [
    {
        story_content_id: 1,
        geo_region_id: 2
    },
    {
        story_content_id: 1,
        geo_region_id: 213
    },
    {
        story_content_id: 2,
        geo_region_id: 213
    },
    {
        story_content_id: 3,
        geo_region_id: 2
    },
    {
        story_content_id: 3,
        geo_region_id: 213
    },
    {
        story_content_id: 4,
        geo_region_id: 213
    },
    {
        story_content_id: 4,
        geo_region_id: 2
    },
    {
        story_content_id: 5,
        geo_region_id: 2
    },
    {
        story_content_id: 6,
        geo_region_id: 2
    }
];

const storiesContentGeoRegions = {
    table: Table.STORIES_CONTENT_GEO_REGIONS,
    rows
};

export {storiesContentGeoRegions};
