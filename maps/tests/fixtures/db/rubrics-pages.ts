import {Table, Branch} from 'app/types/consts';
import {Schema} from 'app/types/db/rubrics-pages';

const rows: Schema[] = [
    {
        rubric_id: 20956,
        page_id: 1,
        branch: Branch.DRAFT
    },
    {
        rubric_id: 30056,
        page_id: 1,
        branch: Branch.PUBLIC
    }
];

const rubricsPages = {
    table: Table.RUBRICS_PAGES,
    rows
};

export {rubricsPages};
