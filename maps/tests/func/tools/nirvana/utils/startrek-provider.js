const got = require('got');

/**
 * @see https://wiki.yandex-team.ru/tracker/api/
 */
module.exports = {
    findIssues: (params) => send('post', 'issues/_search', params),
    getIssue: (id) => send('get', `issues/${id}`),
    getChangelog: (id, field) => send('get', `issues/${id}/changelog?field=${field}&perPage=100`),
    updateIssue: (id, {status, ...body}) =>
        status
            ? send('post', `issues/${id}/transitions/${status}/_execute`, body)
            : send('patch', `issues/${id}`, body),
    createIssue: (body) => send('post', 'issues', body),
    createComment: (issue, text) => send('post', `issues/${issue}/comments?isAddToFollowers=false`, {text}),
    bulkChange: (type, body) => send('post', `bulkchange/_${type}?notify=true`, body),
    getVersions: (id) => send('get', `queues/${id}/versions`),
    getVersion: (id) => send('get', `versions/${id}`),
    createVersion: (body) => send('post', 'versions', body),
    patchVersion: (id, body) => send('patch', `versions/${id}`, body),
    releaseVersion: (id) => send('post', `versions/${id}/_release`),
    linkIssue: (issue, relationship, link) => send('post', `issues/${issue}/links?`, {relationship, issue: link}),
    unlinkIssue: (issue, link) => send('delete', `issues/${issue}/links/${link}`),
    getIssueLinks: (issue) => send('get', `issues/${issue}/links`)
};

function send(method, path, body) {
    const options = {
        headers: {
            'Content-Type': 'application/json',
            Authorization: `OAuth ${process.env.STARTREK_OAUTH_TOKEN}`
        },
        json: true,
        body
    };
    return got[method](`https://st-api.yandex-team.ru/v2/${path}`, options).then(({body}) => body);
}
