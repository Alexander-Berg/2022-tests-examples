export default {
    entries: [{
        project: 'project1',
        key: '1000',
        data: {
            info: 'test',
            number_val: 1000000
        },
        created_at: '2019-06-04T12:00:00Z',
        updated_at: '2019-06-04T13:00:00Z',
        deleted_at: null
    }, {
        project: 'a'.repeat(256),
        key: '1000',
        data: {
            info: 'test',
            number_val: 123312
        },
        created_at: '2019-06-04T12:00:00Z',
        updated_at: '2019-06-04T13:00:00Z',
        deleted_at: null
    }, {
        project: 'project1',
        key: 'a'.repeat(256),
        data: {
            info: 'test',
            number_val: 123312
        },
        created_at: '2019-06-04T12:00:00Z',
        updated_at: '2019-06-04T13:00:00Z',
        deleted_at: null
    }, {
        project: 'project1',
        key: 'deleted',
        data: {
            info: 'test',
            number_val: 123312
        },
        created_at: '2019-06-04T12:00:00Z',
        updated_at: '2019-06-04T13:00:00Z',
        deleted_at: '2019-06-04T13:00:00Z'
    }]
};
