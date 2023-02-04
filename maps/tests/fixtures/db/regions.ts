import {Table, RegionVersion} from 'app/types/consts';
import {Schema as RegionSchema} from 'app/types/db/regions';

type Schema = Omit<RegionSchema, 'id'>;

const rows: Schema[] = [
    {
        // id: is auto incremented
        meta: null,
        version: RegionVersion.V3
    },
    {
        meta: null,
        version: RegionVersion.V3
    }
];

const regions = {
    table: Table.REGIONS,
    rows: rows.map((row) => ({
        ...row,
        meta: JSON.stringify(row.meta)
    }))
};

export {regions};
