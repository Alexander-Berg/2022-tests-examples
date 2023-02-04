import {Entry} from '../../../../app/typings';

const total = 200;
const offset = 50;
const limit = 100;

export const project1OrderedByCreatedAtAscAndOffset = {
    meta: {
        total,
        offset,
        limit
    },
    entries: generateEntries()
};

function generateEntries(): Entry[] {
    const res = new Array(limit);

    for (let i = offset; i <= limit + offset; ++i) {
        res[i - offset - 1] = {
            key: (2010 - i * 10).toString().padStart(8, '0'),
            project: 'project1',
            value: {
                some_number: i
            },
            created_at: `2019-05-01T${12 + Math.floor(i / 60)}:${(i % 60).toString().padStart(2, '0')}:00.000Z`,
            updated_at: `2019-05-01T${11 + Math.floor(i / 60)}:${(i % 60).toString().padStart(2, '0')}:00.000Z`
        };
    }

    return res;
}
