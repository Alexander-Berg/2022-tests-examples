import {Table, Locale} from 'app/types/consts';
import {Schema} from 'app/types/db/rubrics-content';

const rows: Schema[] = [
    {
        rubric_id: 20956,
        locale: Locale.RU,
        name: 'Магазин автозапчастей и автотоваров'
    },
    {
        rubric_id: 20276,
        locale: Locale.RU,
        name: 'Автошкола'
    },
    {
        rubric_id: 30056,
        locale: Locale.RU,
        name: 'Стоматологическая клиника'
    },
    {
        rubric_id: 30057,
        locale: Locale.RU,
        name: 'Стоматологическая поликлиника'
    },
    {
        rubric_id: 30061,
        locale: Locale.RU,
        name: 'Скорая медицинская помощь'
    },
    {
        rubric_id: 30062,
        locale: Locale.RU,
        name: 'Детская скорая помощь'
    },
    {
        rubric_id: 20954,
        locale: Locale.RU,
        name: 'Автомойка'
    },
    {
        rubric_id: 30064,
        locale: Locale.RU,
        name: 'Урологический центр'
    },
    {
        rubric_id: 30065,
        locale: Locale.RU,
        name: 'Медицинская реабилитация'
    },
    {
        rubric_id: 20269,
        locale: Locale.RU,
        name: 'Медицинская комиссия'
    },
    {
        rubric_id: 20273,
        locale: Locale.RU,
        name: 'Автомобильная парковка'
    }
];

const rubricsContent = {
    table: Table.RUBRICS_CONTENT,
    rows
};

export {rubricsContent};
