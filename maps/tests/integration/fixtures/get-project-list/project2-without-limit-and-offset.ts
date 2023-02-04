import {Entry} from '../../../../app/typings';

const total = 200;
const offset = 0;
const limit = 100;

export const project2WithoutLimitAndOffset = {
    meta: {
        total,
        offset,
        limit
    },
    entries: generateEntries()
};

function generateEntries(): Entry[] {
    const res = new Array(limit);

    for (let i = 1; i <= limit; ++i) {
        res[i - 1] = {
            key: (i * 10).toString().padStart(8, '0'),
            project: 'project2',
            value: {
                some_number: limit * i
            },
            created_at: `2019-05-01T${12 + Math.floor(i / 60)}:${(i % 60).toString().padStart(2, '0')}:00.000Z`,
            updated_at: `2019-05-01T${12 + Math.floor(i / 60)}:${(i % 60).toString().padStart(2, '0')}:00.000Z`
        };
    }

    return res;
}
