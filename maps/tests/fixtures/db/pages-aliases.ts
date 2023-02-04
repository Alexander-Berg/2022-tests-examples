import {Table} from 'app/types/consts';
import {draftedRows as pages} from 'tests/fixtures/db/pages-content';
import {Schema} from 'app/types/db/pages-aliases';

const rows: Schema[] = pages.map((page) => ({
    page_id: page.page_id,
    alias: `alias-for-page-with-id-${page.page_id}`
}));

const pagesAliases = {
    table: Table.PAGES_ALIASES,
    rows
};

export {pagesAliases};
