module.exports = {
    no_oauth: {
        request: {},
        response: {}
    },
    oauth_user: {
        request: {
            headers: {
                authorization: 'oauth fake04uthd1d2d3d4d5d6d7d8d9dfake'
            }
        },
        response: {
            connection_id: 't:54668594',
            have_password: true,
            uid: {
                value: '11229033',
                hosted: false,
                lite: false
            },
            status: {
                value: 'VALID',
                id: 0
            },
            have_hint: true,
            karma: {
                value: 0
            },
            login: 'usernamed1d2',
            error: 'OK',
            karma_status: {
                value: 6000
            },
            oauth: {
                ctime: '2015-03-02 17:00:23',
                uid: '41569062',
                client_homepage: '',
                client_id: 'fakefake208b4c14beff407342c676a3',
                token_id: '54668594',
                meta: '',
                client_ctime: '2015-03-01 02:18:16',
                device_id: '',
                device_name: '',
                client_icon: '',
                issue_time: '2015-03-02 17:00:23',
                client_name: 'ZTestApp',
                expire_time: '2016-03-01 17:00:23',
                is_ttl_refreshable: true,
                scope: 'login:birthday login:avatar'
            },
            dbfields: {
                'subscription.login.0': 'usernamed1d2'
            }
        }
    }
};
