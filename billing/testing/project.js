module.exports = require('extend')(true, require('./../common/project'), {
    env: 'testing',
    auth: 'team',
    luster: {
        server: {
            port: '/usr/local/www5/yandex-balance-apikeys-frontend-admin/server/run/index.sock'
        },
        extensions: {
            'luster-log-file': {
                stdout: '/var/log/yandex/yandex-balance-apikeys-frontend-admin/out.log',
                stderr: '/var/log/yandex/yandex-balance-apikeys-frontend-admin/error.log'
            },
            pids: '/var/run/yandex/yandex-balance-apikeys-frontend-admin'
        }
    },
    compress: true,
    servant: {
        host: 'https://apikeys-test.paysys.yandex.net:8668',
        authBlackbox: 'blackbox.yandex-team.ru/blackbox',
        authPassport: 'https://passport.yandex-team.ru/passport',
        authHost: 'yandex-team.ru'
    }
});
