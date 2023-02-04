/* global describe, it */
/*jslint bitwise: true */

var User = require('../lib/user'),
    assert = require('chai').assert,
    RoleError = require('../components/role').Error,
    defaultConfig = require('../lib/config').defaultConfig.role,
    customConfig = {
        role : {
            roles : {
                READER : [ 'COMMENT_READ' ],
                WRITER : [ 'COMMENT_READ', 'COMMENT_WRITE' ],
                ADMIN : [ 'FULL_ACCESS' ]
            },

            getRoles : function(cb) {
                cb(null, [ this.ROLES.READER, this.ROLES.WRITER, this.ROLES.ADMIN ]);
            }
        }
    };

describe('Role component', function() {

    function roleInit(config) {
        var req = { headers : {}, cookies : {} },
            res = {};

        return new User(req, res, config).init('role');
    }

    it('should have default configuration', function(done) {
        roleInit({ role : {} }).then(function(user) {
            assert.deepEqual(user.config.get('role'), defaultConfig);

            done();
        })
        .done();
    });

    it('should have custom configuration', function(done) {
        roleInit(customConfig).then(function(user) {
            assert.deepEqual(user.config.get('role'), customConfig.role);

            done();
        })
        .done();
    });

    it('should have `ROLES` list', function(done) {
        roleInit(customConfig).then(function(user) {
            assert.deepEqual(user.role.ROLES, {
                READER : 'READER',
                WRITER : 'WRITER',
                ADMIN : 'ADMIN'
            });

            done();
        })
        .done();
    });

    it('should have `ACCESS` list', function(done) {
        roleInit(customConfig).then(function(user) {
            assert.deepEqual(user.role.ACCESS, {
                COMMENT_READ : 1,
                COMMENT_WRITE : 2,
                FULL_ACCESS : 4
            });

            done();
        })
        .done();
    });

    it('should have added roles', function(done) {
        roleInit(customConfig).then(function(user) {
            var role = user.role;

            assert.isTrue(role.hasAccess(role.ACCESS.COMMENT_READ));
            assert.isTrue(role.hasAccess(role.ACCESS.COMMENT_WRITE));
            assert.isTrue(role.hasAccess(role.ACCESS.COMMENT_READ | role.ACCESS.COMMENT_WRITE));
            assert.isTrue(role.hasAccess(role.ACCESS.FULL_ACCESS));

            done();
        })
        .done();
    });

    it('should have unique `ROLES`, `ACCESS` properties for each instance', function(done) {
        roleInit(customConfig).then(function(user1) {
            customConfig.role.roles.MODERATOR = ['COMMENT_WRITE', 'COMMENT_READ', 'CHANGE_USER'];

            roleInit(customConfig).then(function(user2) {
                assert.notDeepEqual(user1.role.ROLES, user2.role.ROLES);
                assert.notDeepEqual(user1.role.ACCESS, user2.role.ACCESS);

                done();
            })
            .done();
        })
        .done();
    });

    it('should get error from getRoles', function(done) {
        customConfig.role.getRoles = function(cb) {
            cb(new Error());
        };

        roleInit(customConfig).fail(function(error) {
            assert.strictEqual(error.code, RoleError.CODES.GET_ROLES);
            done();
        })
        .done();
    });

});
