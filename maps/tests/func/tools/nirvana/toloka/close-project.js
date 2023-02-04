const got = require('got');

function closeProject(projectId) {
    return got.post(`https://sandbox.toloka.yandex.ru/api/v1/projects/${projectId}/archive`, {
        headers: {
            Authorization: `OAuth ${process.env.TOLOKA_TOKEN}`
        },
        json: true
    }).then(({body}) => body);
}

module.exports = closeProject;
