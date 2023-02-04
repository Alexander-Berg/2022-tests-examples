module.exports = {
    // eslint-disable-next-line node/no-missing-require
    app: require.resolve('../../build/server/app.js'),

    workers: 4,

    control: {
        forkTimeout: 5000,
        stopTimeout: 10000
    },

    server: {
        port: process.env.NODE_PORT
    },

    extensions: {
        '@vertis/luster-vertislogs': {
            extendConsole: true,
            stdout: true,
            stderr: 'stdout'
        },

        'luster-prometheus': {
            port: process.env._DEPLOY_METRICS_PORT
        },

        'luster-tvm': {
            refresh: 30 * 60 * 1000,
            connection: {
                protocol: process.env.TVM_PROTOCOL + ':',
                host: process.env.TVM_HOST,
                port: process.env.TVM_PORT,
                family: 6
            },
            src: process.env.TVM_ID,
            secret: process.env.TVM_SECRET,
            dst: {
                ydb: process.env.YDB_CLIENT_ID,
                blackbox: process.env.BLACKBOX_CLIENT_ID
            }
        }
    }
};
