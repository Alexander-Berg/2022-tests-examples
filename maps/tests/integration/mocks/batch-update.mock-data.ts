export default {
    entries: [{
        project: 'project1',
        key: '1',
        data: {
            old_entry: true
        }
    }, {
        project: 'project1',
        key: '2',
        data: {
            old_entry: true
        }
    }, {
        project: 'project1',
        key: '3',
        data: {
            old_entry: true
        }
    }, {
        project: 'project1',
        key: '4',
        data: {
            old_entry: {
                first: false,
                second: false
            }
        }
    }, {
        project: 'a'.repeat(256),
        key: '4',
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
