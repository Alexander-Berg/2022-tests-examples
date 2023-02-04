var extend = require('extend');

module.exports = function createMockParams(addition) {
    return extend(true, {
        req: {
            headers: {}
        },
        res: {
            statusCode: 200,
            headers: {},
            setHeader: function(header, value) {
                this.headers[header.toLowerCase()] = value;
            }
        }
    }, addition);
};
