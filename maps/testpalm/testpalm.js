/* eslint-disable no-console */
const TestpalmAPI = require('@yandex-int/testpalm-api').default;
const PalmSync = require('@yandex-int/palmsync');

const TOKEN = process.env.palmsync_testpalmToken;

function clone({ project, newProject, title }) {
    let api = new TestpalmAPI(TOKEN);

    console.info(`>>> clone: "${project}" -> "${newProject}"`);

    return api.getProject(newProject)
        .then(() => console.info(`>>> project "${newProject}" already exists`))
        .catch((error) => {
            if (error.statusCode !== 404) {
                return Promise.reject(error);
            }

            let testPalmParams = {
                cloneTestRuns: false,
                cloneVersions: false
            };

            if (title) {
                testPalmParams.title = title;
            }

            return api.cloneProject(project, newProject, testPalmParams);
        });
}

function sync(project) {
    let options = {};

    if (project) {
        options.project = project;
    }

    console.info(`>>> sync: "${project}"`);

    return PalmSync
        .create(options)
        .loadPlugins()
        .synchronize()
        .then(() => console.info('>>> synchronization has been completed'))
        .catch((error) => {
            throw new Error(
                (error && error.stack || error) + '\n' +
                '>>> synchronization has been failed'
            );
        });
}

module.exports = {
    clone,
    sync
};
