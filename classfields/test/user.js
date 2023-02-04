/* global describe, it, before, beforeEach, afterEach */

var vow = require('vow'),
    sinon = require('sinon'),
    assert = require('chai').assert,
    User = require('../lib/user'),
    UserError = require('../lib/error'),
    mock = {
        req : { headers : {} },
        res : { a : 'c' },
        config : {}
    },
    user = null;

function createMockComponent(name) {
    /**
     * Стабовый компонент реализующий минимальную функциональность
     * @constructor
     */
    return function MockComponent() {
        return {
            _name : name,
            getName : function() { return this._name; }
        };
    };
}

describe('User', function() {
    before(function() {
        // @note varankinv@: манкипатчим реестр компонентов `components`,
        // добавляем компонент `mock`, чтобы тесты на `user` не зависили
        // от реализации какого-то конкретного компонента.
        User.registerComponent('mock', createMockComponent('mock'));
    });

    beforeEach(function() {
        user = new User(mock.req, mock.res, mock.config);
    });

    afterEach(function() {
        user = null;
    });

    describe('constructor', function() {
        it('should return proper instance', function() {
            assert.deepEqual(user.req, mock.req);
            assert.deepEqual(user.res, mock.res);
        });

        it('user\'s properties could not be instantly redefined', function() {
            user.req = { foo : 'req' };
            user.res = { foo : 'res' };

            assert.deepEqual(user.req, mock.req);
            assert.deepEqual(user.res, mock.res);
        });
    });

    describe('init', function() {
        it('should return promise', function() {
            assert.isTrue(vow.isPromise(user.init('mock')));
        });

        it('should throw if no component name passed', function(done) {
            try {
                user.init();
            } catch(e) {
                assert.isTrue(e instanceof UserError);
                assert.strictEqual(e.code, UserError.CODES.COMPONENT_NAME_REQUIRED);

                done();
            }
        });

        it('should throw for unknown component name', function(done) {
            try {
                user.init('trololo');
            } catch(e) {
                assert.isTrue(e instanceof UserError);
                assert.strictEqual(e.code, UserError.CODES.UNKNOWN_COMPONENT_REQUIRED);

                done();
            }
        });

        it('should be fulfilled with the same User instance', function(done) {
            assert.notProperty(user, 'mock');

            user
                .init('mock')
                .then(function(user) {
                    assert.deepEqual(user.req, mock.req);
                    assert.deepEqual(user.res, mock.res);

                    assert.property(user, 'mock', 'component name should become a property of instance');

                    done();
                })
                .done();
        });

        it('same component should not be initialized twice', function(done) {
            var registry = sinon.spy(user._componentManager, 'registry');

            user
                .init('mock')
                .then(function(user) {
                    return user.init('mock');
                })
                .then(function() {
                    assert.isTrue(registry.calledOnce);
                    done();
                })
                .always(function(err) {
                    registry.restore();
                    err.isRejected() && done(err.valueOf());
                })
                .done();
        });
    });

    describe('hasComponents', function() {
        it('should return "false" by default', function() {
            assert.isFalse(user.hasComponents());
        });

        it('should return "false" for unregistered component', function() {
            assert.isFalse(user.hasComponents('a'));
        });

        it('should return "true" if components are registered', function(done) {
            user
                .init('mock')
                .then(function(user) {
                    assert.isTrue(user.hasComponents('mock'));
                    done();
                })
                .done();
        });

        it('should return "false" if any of components is unregistered', function(done) {
            user
                .init('mock')
                .then(function(user) {
                    assert.isFalse(user.hasComponents('mock', 'a'));
                    done();
                })
                .done();
        });
    });

    describe('registerComponent', function() {
        it('should register new component class', function(done) {
            var componentCls = createMockComponent('fake-component');

            assert.throws(function() {
                user.init('fake-component');
            }, UserError);

            User.registerComponent('fake-component', componentCls);

            assert.doesNotThrow(function() {
                user.init('fake-component').then(function(user) {
                    assert.isTrue(user.hasComponents('fake-component'));
                    done();
                })
                .done();
            });
        });

        it('should pass config to component constructor when provided', function(done) {
            var namespace = {},
                componentConfig = {},
                spy;

            namespace.componentCls = createMockComponent('fake-component');
            spy = sinon.spy(namespace, 'componentCls');

            User.registerComponent(
                'fake-component',
                namespace.componentCls,
                componentConfig
            );

            user.init('fake-component').then(function(user) {
                assert.ok(
                    spy.calledWithExactly(
                        sinon.match.same(user),
                        sinon.match.same(componentConfig)
                    ),
                    'component constructor has been called with wrong arguments'
                );
                done();
            }).fail(done);
        });
    });
});
