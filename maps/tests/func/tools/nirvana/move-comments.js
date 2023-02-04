/**
 * This module is expected to run only in Nirvana environment.
 *
 * Environment variables and additional files will be generated
 * in runtime.
 */

const dotenv = require('dotenv'); dotenv.config();
const getPools = require('./toloka/get-pools');
const getComments = require('./toloka/get-comments');
const {formatCommentsFromToloka} = require('./utils/comments');
const startrekProvider = require('./utils/startrek-provider');
const closePool = require('./toloka/close-pool');
const closeProject = require('./toloka/close-project');

async function main() {
    const issue = await startrekProvider.getIssue(process.env.ST_ISSUE);
    const version = issue.versionSingle.display;
    const {items: pools} = await getPools();
    const pool = pools.filter((pool) => pool.private_name.includes(version)).pop();

    if (!pool) {
        return await startrekProvider.createComment(issue.key, `Для версии ${version} нет заданий в толоке`);
    }

    const {items: comments} = await getComments(pool.id);


    if (comments.length === 0) {
        return await startrekProvider.createComment(issue.key, 'Комментариев из толоки нет');
    }

    const trackerComment = formatCommentsFromToloka(comments);

    await startrekProvider.createComment(issue.key, trackerComment);
    await closePool(pool.id);
    await closeProject(pool.project_id);
}

main().catch((e) => console.error(e));
