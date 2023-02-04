require('ts-node').register();

const got = require('got'),
    querystring = require('querystring'),
    backendConfig = require('../../src/configs/' + (process.env.YENV || 'development') + '/backend').default,
    credentials = require('./credentials'),
    { ALL_OBJECTS_FILTER_EXPRESSION_ID, CLEANUP_INTERVAL } = require('./constants');
let cleanupQueue = Promise.resolve();

/**
 * @name cleanup
 * @param {Object[]} coordinates
 */
module.exports = (coordinates) => {
    cleanupQueue = cleanupQueue.then(() => doCleanup(coordinates));
    return cleanupQueue;
};

function doCleanup(coordinates) {
    const longtasksServantUrl = backendConfig.default.longtasks.servant.url;

    return doRequest(
        'http://' + longtasksServantUrl + '/tasks?uid=' + credentials.cleanup.uid + '&type=groupedit&branch=0',
        '<task xmlns="http://maps.yandex.ru/mapspro/tasks/1.x">' +
            '<request-groupedit>' +
                '<aoi>' +
                    '<geometry>' +
                        JSON.stringify({
                            type: 'Polygon',
                            coordinates: [coordinates]
                        }) +
                    '</geometry>' +
                '</aoi>' +
            '<filter-expression id="' + ALL_OBJECTS_FILTER_EXPRESSION_ID + '"/>' +
            '<params><state>deleted</state></params>' +
            '</request-groupedit>' +
        '</task>')
        .then(xml => {
            const errorStatus = getMatch(xml, getXmlElemAttrRegExp('error', 'status')),
                errorMessage = getMatch(xml, getXmlElemContentRegExp('error')),
                taskId = getMatch(xml, getXmlElemAttrRegExp('task', 'id')),
                status = getMatch(xml, getXmlElemAttrRegExp('task', 'status')),
                token = getMatch(xml, getXmlElemContentRegExp('token'));

            if(errorStatus) {
                throw new Error('Cleanup failed, error ' + errorStatus + ' (' + errorMessage + ')');
            }
            else if(!status || status === 'failed') {
                throw new Error('Cleanup failed, see task ' + taskId + ' in mpro for details');
            }

            return pollUntilSatisfies(
                () => doRequest('http://' + longtasksServantUrl + '/tasks/' + taskId + '?' + querystring.stringify({ token }))
                    .then(xml => getMatch(xml, getXmlElemAttrRegExp('task', 'status'))),
                status => status === 'success' || status === 'failed',
                CLEANUP_INTERVAL
            ).then(
                status => {
                    if(status === 'failed') {
                        throw new Error('Cleanup failed, see task ' + taskId + ' in mpro for details');
                    }
                    return status;
                },
                err => {
                    throw new Error('Cleanup failed with ' + err + '. See task ' + taskId + ' in mpro for details');
                }
            );
        });
}

function doRequest(url, body) {
    return got({
        url,
        timeout: 30000,
        headers: body? { 'Content-type': 'text/xml' } : {},
        body,
        method: body? 'POST' : 'GET'
    }).then(({ body }) => body);
}

function getXmlElemContentRegExp(elem) {
    return '<' + elem + '(?:\\s[^<>]+)?>(.*?)<\/' + elem + '>';
}

function getXmlElemAttrRegExp(elem, attr) {
    return '<' + elem + '\\s(?:[^<>]+\\s)?' + attr + '="([^"]+)"(?:[^<>]+)?>';
}

function getMatch(str, regExpStr) {
    const matches = new RegExp(regExpStr).exec(str);
    return matches && matches[1];
}

function pollUntilSatisfies(attempt, test, interval, maxAttempts) {
    return new Promise((resolve, reject) => {
        attempt().then(
            res => {
                if(test(res) || maxAttempts === 1) {
                    return resolve(res);
                }

                setTimeout(
                    () => {
                        pollUntilSatisfies(attempt, test, interval, !!maxAttempts && maxAttempts - 1).then(resolve, reject);
                    },
                    interval
                );
            },
            reject
        );
    });
}
