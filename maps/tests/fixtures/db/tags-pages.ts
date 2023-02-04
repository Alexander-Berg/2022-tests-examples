import {Table, Branch, Tag} from 'app/types/consts';
import {Schema} from 'app/types/db/tags-pages';

const rows: Schema[] = [
    {
        tag_id: 1,
        page_id: 1,
        branch: Branch.DRAFT,
        assignment: Tag.DEFAULT
    },
    {
        tag_id: 2,
        page_id: 1,
        branch: Branch.DRAFT,
        assignment: Tag.DEFAULT
    },
    {
        tag_id: 3,
        page_id: 1,
        branch: Branch.PUBLIC,
        assignment: Tag.DEFAULT
    },
    {
        tag_id: 4,
        page_id: 1,
        branch: Branch.PUBLIC,
        assignment: Tag.LINKS
    },
    {
        tag_id: 4,
        page_id: 2,
        branch: Branch.PUBLIC,
        assignment: Tag.LINKS
    },
    {
        tag_id: 4,
        page_id: 3,
        branch: Branch.PUBLIC,
        assignment: Tag.LINKS
    },
    {
        tag_id: 4,
        page_id: 4,
        branch: Branch.PUBLIC,
        assignment: Tag.LINKS
    },
    {
        tag_id: 5,
        page_id: 4,
        branch: Branch.PUBLIC,
        assignment: Tag.LINKS
    },
    {
        tag_id: 5,
        page_id: 1,
        branch: Branch.DRAFT,
        assignment: Tag.LINKS
    },
    {
        tag_id: 5,
        page_id: 2,
        branch: Branch.DRAFT,
        assignment: Tag.LINKS
    },
    {
        tag_id: 5,
        page_id: 3,
        branch: Branch.DRAFT,
        assignment: Tag.LINKS
    },
    {
        tag_id: 5,
        page_id: 3,
        branch: Branch.PUBLIC,
        assignment: Tag.LINKS
    },
    {
        tag_id: 5,
        page_id: 4,
        branch: Branch.DRAFT,
        assignment: Tag.LINKS
    },
    {
        tag_id: 6,
        page_id: 4,
        branch: Branch.PUBLIC,
        assignment: Tag.DEFAULT
    },
    {
        tag_id: 7,
        page_id: 3,
        branch: Branch.PUBLIC,
        assignment: Tag.DEFAULT
    },
    {
        tag_id: 4,
        page_id: 2,
        branch: Branch.PUBLIC,
        assignment: Tag.DEFAULT
    },
    {
        tag_id: 4,
        page_id: 3,
        branch: Branch.PUBLIC,
        assignment: Tag.DEFAULT
    },
    {
        tag_id: 4,
        page_id: 5,
        branch: Branch.PUBLIC,
        assignment: Tag.DEFAULT
    },
    {
        tag_id: 4,
        page_id: 5,
        branch: Branch.DRAFT,
        assignment: Tag.DEFAULT
    },
    {
        tag_id: 4,
        page_id: 6,
        branch: Branch.DRAFT,
        assignment: Tag.DEFAULT
    }
];

const tagsPages = {
    table: Table.TAGS_PAGES,
    rows
};
export {tagsPages};
