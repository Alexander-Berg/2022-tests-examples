module.exports = function (selector, timeout) {
    return this.waitForVisible(selector ? selector : PO.map.map(), timeout ? timeout : 20000).waitForVisible(selector ? selector : 'body.tileload', timeout ? timeout : 20000);
};
