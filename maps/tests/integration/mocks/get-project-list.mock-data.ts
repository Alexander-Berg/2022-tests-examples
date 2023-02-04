import {EntryWithTimestamps} from '../../../app/typings';

export default {
    entries: generateEntries()
};

function generateEntries(): EntryWithTimestamps[] {
    const res = new Array(400);

    for (let i = 1; i <= 200; ++i) {
        res[2 * i - 2] = {
            project: 'project1',
            key: (2010 - i * 10).toString().padStart(8, '0'),
            data: {
                some_number: i
            },
            created_at: `2019-05-01T${12 + Math.floor(i / 60)}:${i % 60}:00Z`,
            updated_at: `2019-05-01T${11 + Math.floor(i / 60)}:${i % 60}:00Z`
        };

        res[2 * i - 1] = {
            project: 'project2',
            key: (i * 10).toString().padStart(8, '0'),
            data: {
                some_number: i * 100
            },
            created_at: `2019-05-01T${12 + Math.floor(i / 60)}:${i % 60}:00Z`,
            updated_at: `2019-05-01T${12 + Math.floor(i / 60)}:${i % 60}:00Z`
        };
    }

    res.push({
        project: 'a'.repeat(256),
        key: '123456',
        data: {
            some_data: 'some_value'
        },
        created_at: '2019-05-01T12:00:00Z',
        updated_at: '2019-05-01T12:00:00Z',
        deleted_at: null
    });

    return res;
}
