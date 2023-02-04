module.exports = {
    // eslint-disable-next-line node/no-missing-require
    app: require.resolve('../../build/server/app.js'),

    workers: Number(process.env.NODE_LUSTER_WORKERS) || 1,

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
        }
    }
};
