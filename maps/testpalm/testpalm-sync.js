/* eslint-disable no-console */
const config = require('../../.palmsync.conf');
const {
    // clone,
    sync
} = require('./testpalm');

process.env.NODE_TLS_REJECT_UNAUTHORIZED = 0;

// const VERSION_TAG = process.env.TRENDBOX_TAG;
const isMasterBranch = process.env.TRENDBOX_BRANCH === 'master';

async function main() {
    const project = config.project;
    /*
        До необходимости отключаем создание новых проектов в Testaplm
    */

    // const isVerTag = Boolean(VERSION_TAG);

    // if (isVerTag) {
    //     const newProject = `tycoon-ver${VERSION_TAG.replace(/\./g, '_')}`;

    //     const title = `Tycoon ver${VERSION_TAG}`;

    //     await clone({ project, newProject, title });
    //     await sync(newProject);
    // }

    if (isMasterBranch) {
        await sync(project);
    }

    console.info('>>> done');
}

main().catch((error) => {
    console.error(error);
    process.exit(1);
});
