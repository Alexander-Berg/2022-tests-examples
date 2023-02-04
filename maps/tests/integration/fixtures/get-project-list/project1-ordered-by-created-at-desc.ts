import {Entry} from '../../../../app/typings';

const total = 200;
const offset = 0;
const limit = 100;

export const project1OrderedByCreatedAtDesc = {
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
            project: 'project1',
            value: {
                some_number: total + 1 - i
            },
            // tslint:disable-next-line: ter-max-len
            created_at: `2019-05-01T${12 + Math.floor((total + 1 - i) / 60)}:${((total + 1 - i) % 60).toString().padStart(2, '0')}:00.000Z`,
            // tslint:disable-next-line: ter-max-len
            updated_at: `2019-05-01T${11 + Math.floor((total + 1 - i) / 60)}:${((total + 1 - i) % 60).toString().padStart(2, '0')}:00.000Z`
        };
    }

    return res;
}
