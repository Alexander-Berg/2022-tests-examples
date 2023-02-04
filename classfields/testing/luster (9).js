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

        'luster-log-stat': {
            instances: [
                {
                    name: 'tskv-log',
                    path: process.env.FRONT_LOG_PATH
                }
            ]
        },

        'luster-prometheus': {
            port: process.env._DEPLOY_METRICS_PORT
        },

        'luster-bunker': {
            refresh: 60 * 60 * 1000,
            connection: {
                protocol: process.env.BUNKER_PROTOCOL + ':',
                host: process.env.BUNKER_HOST,
                port: process.env.BUNKER_PORT,
                timeout: 500,
                maxRetries: 10,
                requestId: 'luster-bunker',
                family: 6
            },
            nodes: [
                {
                    node: 'vertis-moderation/realty',
                    version: 'latest'
                },
                {
                    node: 'realty-www',
                    version: 'latest'
                }
            ]
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
                addrs: process.env.ADDRS_TVMID,
                panorama: process.env.CORE_STVDESCR_TVMID,
                blackbox: process.env.BLACKBOX_CLIENT_ID,
                realtyApi: process.env.REALTY_API_TVMID
            }
        }
    }
};
