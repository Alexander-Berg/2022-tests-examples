import {Table} from 'app/types/consts';
import {Schema} from 'app/types/db/pages-tracker';
import {pagesContent} from 'tests/fixtures/db/pages-content';

const rows: Schema[] = (
    pagesContent.rows.map((pageContent) => {
        // Ignore for tests: refresh-tracker-info
        if (pageContent.page_id === 1) {
            return;
        }

        return {
            page_id: pageContent.page_id,
            branch: pageContent.branch,
            info: {
                id: `tracker_${pageContent.page_id}_${pageContent.branch}`
            }
        };
    }) as Schema[]
).filter(Boolean);

const pagesTracker = {
    table: Table.PAGES_TRACKER,
    rows: rows.map((row) => ({
        ...row,
        info: JSON.stringify(row.info)
    }))
};

export {pagesTracker};
