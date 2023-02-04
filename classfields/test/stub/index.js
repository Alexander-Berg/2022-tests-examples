var nock = require('nock'),
    data = require('./data'),
    assign = require('lodash').assign;

nock.disableNetConnect();

exports.createStub = function createStub(name) {
    return data[name];
};

exports.createRequest = function createReq(data) {
    return {
        method: data.method || 'GET',
        headers: assign({}, data.headers),
        cookies: assign({}, data.cookies)
    };
};

exports.createResponse = function createRes(headers) {
    return {
        setHeader: function(name, val) {
            headers[name] = val;
        },
        getHeader: function(name) {
            return headers[name];
        }
    };
};

exports.createBlackbox = function createBlackbox(data) {
    var server = nock('http://blackbox-mimino.yandex.net').filteringPath(function() {
        return '/blackbox';
    });

    if (data) {
        server.get('/blackbox').reply(200, JSON.parse(JSON.stringify(data)));
    } else if (data === null) {
        server.get('/blackbox').reply(200);
    }

    return server;
};
