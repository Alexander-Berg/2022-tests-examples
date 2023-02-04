module.exports = {
    version: process.env._DEPLOY_APP_VERSION,
    branch: process.env._DEPLOY_BRANCH,
    canary: process.env._DEPLOY_CANARY,
    imageBuildTime: process.env.BUILD_TIME,
    imageBuildRevision: process.env.REVISION,
    debug: false
};
