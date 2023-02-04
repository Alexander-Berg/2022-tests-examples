/**
 * @name browser.verifyNoErrors
 */
module.exports = function() {
    return this.log('browser')
        .then(function(browserLog) {
            return browserLog.value
                .filter(function(message) {
                    return message.level === 'SEVERE';
                })
                .map(function(message) {
                    let msg;
                    try {
                        msg = JSON.parse(message.message);
                        if(msg.message && msg.message.text) {
                            return msg.message.text;
                        }
                        return message.message;
                    }
                    catch(e) {
                        return message.message;
                    }
                })
                .filter(function(message) {
                    return !/Failed to load resource/.test(message) && !/in keyset/.test(message);
                });
        }, function() {
            return [];
        })
        .then(function(log) {
            if(log.length) {
                return this.setMeta('errors', log).then(() => {
                    throw new Error(
                        (log.length === 1? 'JS error' : log.length + ' JS errors') +
                        ' on the page:\n    ' + log.join('\n    '));
                });
            }
            return true;
        });
};
