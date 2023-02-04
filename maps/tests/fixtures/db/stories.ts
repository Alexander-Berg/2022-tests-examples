import {Table} from 'app/types/consts';
import {Schema} from 'app/types/db/stories';

const rows: Schema[] = [
    {
        id: '671c0cdf-1283-4e15-9f68-bad87c174070'
    },
    {
        id: '6f37895b-ecc9-4347-aa27-d08a7b6c9c8d'
    },
    {
        id: 'ba6a1ef9-9454-45c4-aa58-96488edc3599'
    }
];

const stories = {
    table: Table.STORIES,
    rows
};
export {stories};
