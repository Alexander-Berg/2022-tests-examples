module.exports = {
    selectorTimeout: 1000,
    navigationTimeout: 45000,
    jestTimeout: 600000,
    maxInfuseWaitSec: 360, // Maximum time to wait for infuse in seconds
    infuseWait: 10000, // Delay between checks for infuse success
    passportUrl: process.env.PASSPORT_URL || 'https://passport.yandex-team.ru',
    authCheckUrl: process.env.MULTIK_AUTH_CHECK_URL || '/api/v1/ping/auth/',
    login: process.env.MULTIK_TEST_USER_LOGIN || 'robot-multik-tester',
    password: process.env.MULTIK_TEST_USER_PASSWORD,
    multikHost: process.env.MULTIK_TEST_HOST,
    multikProject: process.env.MULTIK_PROJECT,
    infuseStaticCategories: process.env.INFUSE_STATIC_CATEGORIES !== 'false',
};
