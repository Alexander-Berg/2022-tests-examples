/**
 * This module is expected to run only in Nirvana environment.
 *
 * Environment variables and additional files will be generated
 * in runtime.
 */

const dotenv = require('dotenv'); dotenv.config();
const startrekProvider = require('./utils/startrek-provider');
const createProject = require('./toloka/create-project');
const createPool = require('./toloka/create-pool');
const createTaskSuites = require('./toloka/create-task-suites');
const createInteractivitySuites = require('./toloka/create-interactivity-suites');
const {getFailedTests, getTestsStats} = require('./utils/get-failed-tests');
const {formatTestsResults} = require('./utils/comments');
const sandboxData = require('./sandbox.json');

async function main() {
    const issue = await startrekProvider.getIssue(process.env.ST_ISSUE);
    const failedTests = getFailedTests();

    const project = await createProject(issue);
    const pool = await createPool(project.id, issue.versionSingle.display);

    await createTaskSuites(failedTests, pool.id);
    await createInteractivitySuites(pool.id);

    const projectName = project.public_name.replace(`[${issue.key}] `, '');
    const comment = [
        `Для проверки изменений перейдите по ссылке: https://sandbox.toloka.yandex.ru в проект ${projectName}`,
        formatTestsResults(getTestsStats(), sandboxData.download_link)
    ].join('\n');

    await startrekProvider.createComment(issue.key, comment);
}

main().catch((e) => console.error(e));
