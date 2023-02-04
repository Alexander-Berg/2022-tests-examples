var User = require('nodules-user'),
    OAuth = require('../'),
    chai = require('chai'),
    stub = require('./stub');

var expect = chai.expect;
var createStub = stub.createStub;

chai.use(require('chai-interface'));
chai.use(require('chai-as-promised'));

describe('OAuth component', function() {
    var CLIENT_UUID = 'xxxx0xx-d1d2d3d4-xxxx0xxx';

    before(function() {
        User.registerComponent('oauth', OAuth);
    });

    beforeEach(function() {
        this.user = function(data) {
            stub.createBlackbox(data.response);

            var req = stub.createRequest(data.request),
                res = stub.createResponse({});

            var userConfig = {
                clientIp: '127.0.0.1',
                clientUuid: CLIENT_UUID,
                auth: {}
            };

            return new User(req, res, userConfig);
        };
    });

    it('valid oauth token', function() {
        var stub = createStub('oauth_user');

        return this.user(stub).init('oauth').then(function(user) {
            expect(user).to.have.property('oauth');

            expect(user.oauth.isAuth).to.be.true;
            expect(user.oauth.uid).to.equal('11229033');
            expect(user.oauth.login).to.equal('usernamed1d2');
            expect(user.oauth.token).to.equal('fake04uthd1d2d3d4d5d6d7d8d9dfake');
            expect(user.oauth.dbField('subscription.login.0')).to.equal('usernamed1d2');

            var type = user.oauth.type;

            expect(type).to.have.interface({
                kind: String,
                id: String
            });
            expect(type.kind).to.equal('uid');
            expect(type.id).to.equal(user.oauth.uid);
        });
    });

    it('no oauth token', function() {
        var stub = createStub('no_oauth');

        return this.user(stub).init('oauth').then(function(user) {
            expect(user.oauth.isAuth).to.be.false;

            expect(user.oauth.uid).not.to.be.ok;
            expect(user.oauth.login).not.to.be.ok;
            expect(user.oauth.token).to.be.null;
            expect(user.oauth.dbField('subscription.login.0')).to.equal('');

            var type = user.oauth.type;

            expect(type).to.have.interface({
                kind: String,
                id: String
            });

            expect(type.kind).to.equal('mobile');
            expect(type.id).to.equal(CLIENT_UUID);
        });
    });
});
