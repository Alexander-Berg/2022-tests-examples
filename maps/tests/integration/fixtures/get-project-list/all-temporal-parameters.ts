import {Entry} from '../../../../app/typings';

const total = 14;
const offset = 0;
const limit = 100;

export const allTemporalParameters = {
    meta: {
        total,
        offset,
        limit
    },
    entries: generateEntries()
};

function generateEntries(): Entry[] {
    const res = new Array(total);

    for (let i = 1; i <= total; ++i) {
        res[i - 1] = {
            key: ((111 + i) * 10).toString().padStart(8, '0'),
            project: 'project1',
            value: {
                some_number: 90 - i
            },
            created_at: `2019-05-01T13:${(30 - i).toString().padStart(2, '0')}:00.000Z`,
            updated_at: `2019-05-01T12:${(30 - i).toString().padStart(2, '0')}:00.000Z`
        };
    }

    return res;
}
