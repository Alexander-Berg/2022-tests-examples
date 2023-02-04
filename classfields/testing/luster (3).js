const { PRJ_DIR } = process.env;

const path = require('path');

module.exports = {
    app: path.join(PRJ_DIR, 'app'),

    workers: 4,

    control: {
        forkTimeout: 5000,
        stopTimeout: 15000
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
            nodes: []
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
                blackbox: process.env.BLACKBOX_CLIENT_ID,
                realtyApi: process.env.REALTY_API_TVMID
            }
        }
    }
};
