import {Table} from 'app/types/consts';
import {Schema} from 'app/types/db/rubrics';

const rows: Schema[] = [
    {
        id: 20956,
        permalink: '184105320',
        class_name: 'auto parts',
        seoname: 'manufacture_of_license_plates',
        geosearch_key: 'showcase_auto'
    },
    {
        id: 20276,
        permalink: '184105264',
        class_name: 'driving school',
        seoname: 'seoname',
        geosearch_key: 'showcase_auto'
    },
    {
        id: 30056,
        permalink: '184106132',
        class_name: 'dental',
        seoname: 'seoname',
        geosearch_key: 'showcase_medicine'
    },
    {
        id: 30057,
        permalink: '184106130',
        class_name: 'dental',
        seoname: 'seoname',
        geosearch_key: 'showcase_medicine'
    },
    {
        id: 30061,
        permalink: '184106122',
        class_name: 'ambulance',
        seoname: 'seoname',
        geosearch_key: 'showcase_medicine'
    },
    {
        id: 30062,
        permalink: '184106120',
        class_name: 'ambulance',
        seoname: 'seoname',
        geosearch_key: 'showcase_medicine'
    },
    {
        id: 20954,
        permalink: '184105244',
        class_name: 'car wash',
        seoname: 'seoname',
        geosearch_key: 'showcase_auto'
    },
    {
        id: 30064,
        permalink: '184106116',
        class_name: 'medicine',
        seoname: 'seoname',
        geosearch_key: 'showcase_medicine'
    },
    {
        id: 30065,
        permalink: '184106114',
        class_name: 'medicine',
        seoname: 'seoname',
        geosearch_key: 'showcase_medicine'
    },
    {
        id: 20269,
        permalink: '184105278',
        class_name: 'medicine',
        seoname: 'seoname',
        geosearch_key: 'showcase_medicine'
    },
    {
        id: 20273,
        permalink: '184105270',
        class_name: 'car park',
        seoname: 'seoname',
        geosearch_key: 'showcase_auto'
    }
];

const rubrics = {
    table: Table.RUBRICS,
    rows
};

export {rubrics};
