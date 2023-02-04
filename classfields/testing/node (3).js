const { COOKIES_DOMAIN } = process.env;

module.exports = {
    cookiesDomain: COOKIES_DOMAIN,
    bunker: {
        refresh: 60 * 60 * 1000,
        cachePath: null,
        connection: {
            family: 6,
            protocol: process.env.BUNKER_PROTOCOL + ':',
            host: process.env.BUNKER_HOST,
            port: process.env.BUNKER_PORT,
            timeout: 500,
            maxRetries: 10,
            requestId: 'luster-bunker'
        },
        nodes: []
    },
    jaeger: {
        hostname: process.env._DEPLOY_TRACING_ADDR,
        port: process.env._DEPLOY_TRACING_BINARY_PORT
    },
    realtyApi: {
        protocol: process.env.REALTY_API_PROTOCOL + ':',
        hostname: process.env.REALTY_API_HOST,
        port: process.env.REALTY_API_PORT,
        connection: {
            timeout: 1000
        }
    },
    blackbox: {
        protocol: process.env.BLACKBOX_PROTOCOL + ':',
        hostname: process.env.BLACKBOX_HOST,
        port: process.env.BLACKBOX_PORT,
        connection: {
            timeout: 1000
        }
    },
    tvm: {
        refresh: 30 * 60 * 1000,
        connection: {
            protocol: process.env.TVM_PROTOCOL + ':',
            hostname: process.env.TVM_HOST,
            port: process.env.TVM_PORT,
            family: 6
        },
        src: process.env.TVM_ID,
        secret: process.env.TVM_SECRET,
        dst: {
            blackbox: process.env.BLACKBOX_CLIENT_ID
        }
    },
    metrika: {
        counterId: '2119876'
    }
};
