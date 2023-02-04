import {Table, Branch} from 'app/types/consts';
import {Schema} from 'app/types/db/tags';

interface Row {
    options: Schema['options'];
    branch: Schema['branch'];
}

const rows: Row[] = [
    {
        // id: is serial and auto incremented
        options: {},
        branch: Branch.PUBLIC
    },
    {
        options: {},
        branch: Branch.PUBLIC
    },
    {
        options: {},
        branch: Branch.PUBLIC
    },
    {
        options: {},
        branch: Branch.PUBLIC
    },
    {
        options: {},
        branch: Branch.PUBLIC
    },
    {
        options: {},
        branch: Branch.PUBLIC
    },
    {
        options: {},
        branch: Branch.PUBLIC
    }
];

const tags = {
    table: Table.TAGS,
    rows: rows.map((row) => ({
        ...row,
        options: JSON.stringify(row.options)
    }))
};
export {tags};
