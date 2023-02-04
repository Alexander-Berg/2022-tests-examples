const got = require('got');
const pMap = require('p-map');

const buildViewerUrl = require('../utils/build-viewer-url');
const sandboxData = require('../sandbox.json');

function createTaskSuites(failedTests, poolId) {
    return pMap(failedTests, (test) => got.post('https://sandbox.toloka.yandex.ru/api/v1/task-suites/', {
        headers: {
            Authorization: `OAuth ${process.env.TOLOKA_TOKEN}`
        },
        json: true,
        body: {
            pool_id: poolId,
            tasks: [
                {
                    input_values: {
                        name: test.name,
                        url: buildViewerUrl(test),
                        diffUrl: `${sandboxData.download_link}/${test.result.imagesInfo[0].diffImg.path}`
                    }
                }
            ],
            infinite_overlap: true
        },
        query: {
            open_pool: true
        }
    }).then(({body}) => body), {concurrency: process.env.CONCURRENCY || 2});
}

module.exports = createTaskSuites;
