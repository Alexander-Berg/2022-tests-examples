module.exports = {
    testServerPort: process.env.FIXIK_PORT || 5050,
    testRunType: process.env.TEST_RUN_TYPE || 'PLAY',
    idmApiTvmId: 2016033,
    idmApi: {
        host: 'api-idm.test.vertis.yandex.net',
    },
};
