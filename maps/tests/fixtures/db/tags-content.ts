import {Table, Locale} from 'app/types/consts';
import {Schema} from 'app/types/db/tags-content';

const rows: Schema[] = [
    {
        tag_id: 1,
        locale: Locale.RU,
        name: 'Приоритет в Дзене',
        caption: 'Подборки, которые мы хотим показывать чаще на главной Яндекса'
    },
    {
        tag_id: 2,
        locale: Locale.RU,
        name: 'Дзен-покупки'
    },
    {
        tag_id: 3,
        locale: Locale.RU,
        name: 'Бары и пабы',
        caption: 'Развлечения'
    }
];

const tagsContent = {
    table: Table.TAGS_CONTENT,
    rows
};
export {tagsContent};
