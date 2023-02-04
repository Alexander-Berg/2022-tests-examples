module.exports = {
    app: '../../app/index.js',
    workers: 1,
    server : {
        port: process.env.NODE_PORT   
    },
    extensions: {
        '@vertis/luster-prometheus': {
            port: process.env._DEPLOY_METRICS_PORT
        },
    }
};