/* tslint:disable:ter-max-len */
/* tslint:disable:no-irregular-whitespace */

import {Table, Branch, Locale, FeedType} from 'app/types/consts';
import {Schema as RegionInfoSchema} from 'app/types/db/regions-info';

type Schema = Omit<RegionInfoSchema, 'id'>;

const draftedRows: Schema[] = [
    {
        // id: is auto incremented
        region_id: 1,
        branch: Branch.DRAFT,
        locale: Locale.RU,
        geo_region_ids: [213],
        meta: {
            title: 'Москва',
            geoRegionId: 213
        },
        content: {
            feed: [
                {
                    ids: ['6f37895b-ecc9-4347-aa27-d08a7b6c9c8d'],
                    date: '2020-03-31T21:00:00.000Z',
                    type: FeedType.STORIES
                },
                {
                    ids: ['671c0cdf-1283-4e15-9f68-bad87c174070'],
                    date: '2020-03-31T21:00:00.000Z',
                    type: FeedType.STORIES
                },
                {
                    ids: [1],
                    date: '2020-03-31T21:00:00.000Z',
                    type: FeedType.PAGES
                }
            ],
            feedFilters: [
                {
                    tagId: 3,
                    title: 'Сервисы'
                }
            ],
            selectedStories: ['6f37895b-ecc9-4347-aa27-d08a7b6c9c8d', '671c0cdf-1283-4e15-9f68-bad87c174070'],
            feedFiltersTitle: 'Все материалы'
        }
    },
    {
        region_id: 2,
        branch: Branch.DRAFT,
        locale: Locale.RU,
        geo_region_ids: [2],
        meta: {
            title: 'Санкт-Петербург',
            geoRegionId: 2
        },
        content: {
            feed: [
                {
                    ids: ['6f37895b-ecc9-4347-aa27-d08a7b6c9c8d'],
                    date: '2020-03-31T21:00:00.000Z',
                    type: FeedType.STORIES
                },
                {
                    ids: ['671c0cdf-1283-4e15-9f68-bad87c174070'],
                    date: '2020-03-29T21:00:00.000Z',
                    type: FeedType.STORIES
                }
            ],
            feedFilters: [],
            selectedStories: ['6f37895b-ecc9-4347-aa27-d08a7b6c9c8d', '671c0cdf-1283-4e15-9f68-bad87c174070'],
            feedFiltersTitle: 'Все материалы'
        }
    }
];

// Create published region info by draft
const rows: Schema[] = [...draftedRows];
draftedRows.forEach((row) =>
    rows.push({
        ...row,
        branch: Branch.PUBLIC
    })
);

const regionsInfo = {
    table: Table.REGIONS_INFO,
    rows: rows.map((row) => ({
        ...row,
        meta: JSON.stringify(row.meta),
        content: JSON.stringify(row.content)
    }))
};

export {regionsInfo};
