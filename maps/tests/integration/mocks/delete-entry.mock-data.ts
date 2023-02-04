export default {
    entries: [{
        project: 'project1',
        key: '1',
        data: {
            info: 'test1'
        },
        created_at: '2019-06-04T12:00:00Z',
        updated_at: '2019-06-04T13:00:00Z',
        deleted_at: null
    }, {
        project: 'project2',
        key: '2',
        data: {
            info: 'test2'
        },
        created_at: '2019-06-04T12:00:00Z',
        updated_at: '2019-06-04T13:00:00Z',
        deleted_at: null
    }, {
        project: 'p'.repeat(256),
        key: '1',
        data: {
            info: 'test1'
        },
        created_at: '2019-06-04T12:00:00Z',
        updated_at: '2019-06-04T13:00:00Z',
        deleted_at: null
    }, {
        project: 'project2',
        key: '1'.repeat(256),
        data: {
            info: 'test2'
        },
        created_at: '2019-06-04T12:00:00Z',
        updated_at: '2019-06-04T13:00:00Z',
        deleted_at: null
    }]

};
