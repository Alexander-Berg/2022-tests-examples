require('ts-node').register();

const got = require('got'),
    backendConfig = require('../../../src/configs/' + (process.env.YENV || 'development') + '/backend').default,
    credentials = require('../credentials'),
    USER_NOT_EXISTS = 'ERR_USER_NOT_EXISTS';

/**
 * User credentials.newbie is deleted by credentials.cleanup
 * @name browser.resetUser
 */
module.exports = function async() {
    return this.getMeta('host').then((host) => {
        const aclServant = (host in backendConfig? backendConfig[host] : backendConfig.default).acl;
        return doRequest('http://' + aclServant.servant.url + '/users/' + credentials.newbie.uid, credentials.cleanup.uid).then(
            xml => {
                const errorStatus = getMatch(xml, getXmlElemAttrRegExp('error', 'status'));

                if(errorStatus && errorStatus !== USER_NOT_EXISTS) {
                    throw new Error('Delete user failed, error status: ' + errorStatus);
                }

                this.debugLog('User is deleted');
                return true;
            },
            err => {
                this.debugLog('Got delete user error ', err);
                throw new Error(err);
            });
    });
};

function doRequest(url, adminUid) {
    return got({
        url,
        searchParams: {uid: adminUid},
        method: 'DELETE'
    }).then(({ body }) => body);
}

function getXmlElemAttrRegExp(elem, attr) {
    return '<' + elem + '\\s(?:[^<>]+\\s)?' + attr + '="([^"]+)"(?:[^<>]+)?>';
}

function getMatch(str, regExpStr) {
    const matches = new RegExp(regExpStr).exec(str);
    return matches && matches[1];
}
