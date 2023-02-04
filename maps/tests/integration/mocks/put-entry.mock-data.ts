export default {
    entries: [{
        project: 'project1',
        key: '1000',
        data: {
            old_entry: true
        }
    }, {
        project: 'project2',
        key: '2000',
        data: {
            old_entry: true
        },
        deleted_at: '2019-06-04T13:00:00Z'
    }, {
        project: 'project1',
        key: '1001',
        data: {
            old_entry: true
        }
    }, {
        project: 'a'.repeat(256),
        key: '1000',
        data: {
            old_entry: true
        }
    }, {
        project: 'project1',
        key: 'a'.repeat(256),
        data: {
            old_entry: true
        }
    }]
};
