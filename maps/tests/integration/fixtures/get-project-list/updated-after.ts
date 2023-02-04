import {Entry} from '../../../../app/typings';

const total = 80;
const offset = 0;
const limit = 100;

export const updatedAfter = {
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
            key: (i * 10).toString().padStart(8, '0'),
            project: 'project1',
            value: {
                some_number: 200 + 1 - i
            },
            // tslint:disable-next-line: ter-max-len
            created_at: `2019-05-01T${14 + Math.floor((total + 1 - i) / 60)}:${((total + 1 - i) % 60).toString().padStart(2, '0')}:00.000Z`,
            // tslint:disable-next-line: ter-max-len
            updated_at: `2019-05-01T${13 + Math.floor((total + 1 - i) / 60)}:${((total + 1 - i) % 60).toString().padStart(2, '0')}:00.000Z`
        };
    }

    return res;
}
