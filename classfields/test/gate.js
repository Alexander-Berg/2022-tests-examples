/* global describe, it, beforeEach */

var //path = require('path'),
    assert = require('chai')
        .use(require('chai-nodules-helpers'))
        .assert,
    vow = require('vow'),
    sinon = require('sinon'),
    disableDebugOutput = require('./lib/disable_debug_output');

describe('Gate', function() {
    var controllers = require('../lib'),
        createMockParams = require('./lib/controller_mock_params'),
        Gate = controllers.Gate,
        GateError = Gate.GateError;

    it('should be an inheritor of Controller', function() {
        assert.instanceOf(new Gate(createMockParams()), controllers.Controller);
    });

    it('should use params from parsed request body if request method is POST', function() {
        var BODY = {},
            params = createMockParams();

        params.req.body = BODY;
        params.req.method = 'POST';

        assert.strictEqual((new Gate(params))._params, BODY);
    });

    it('should add "before" method', function() {

    });

    describe('methods', function() {
        var ACTION_NAME = 'parse',
            isActionExecuted = false,
            ACTION_FN = function() { isActionExecuted = true;},
            MyGate,
            GateEmptyValidateSignature = function() {};

        GateEmptyValidateSignature.prototype.validateSignature = function() {
            return true;
        };

        beforeEach(function() {
            MyGate = Gate.create('MyGate');
            isActionExecuted = false;
        });

        describe('.create()', function() {
            it('should copy `Gate.prototype._signedActions` array when create inheritor', function() {
                MyGate.action({ name: ACTION_NAME, fn: ACTION_FN });

                var ACTION_NAME_2 = ACTION_NAME + '2',
                    MyGateNext = MyGate.create('MyGateNext')
                        .action({ name: ACTION_NAME_2, fn: ACTION_FN });

                assert.sameMembers(MyGate.prototype._signedActions, [ ACTION_NAME ]);
                assert.sameMembers(MyGateNext.prototype._signedActions, [ ACTION_NAME, ACTION_NAME_2 ]);
            });
        });

        describe('.action()', function() {
            it('should declare action method', function(done) {
                MyGate.mixin({ override: true }, GateEmptyValidateSignature);
                MyGate.action({ name: ACTION_NAME, fn: ACTION_FN });

                assert.strictEqual(MyGate.prototype['action_' + ACTION_NAME], ACTION_FN);

                disableDebugOutput(new MyGate(createMockParams()))
                    .callAction(ACTION_NAME)
                    .then(function() {
                        assert.ok(isActionExecuted);
                    })
                    .done(done);
            });

            it('should mark action as "should be signed" by default', function() {
                MyGate.action({ name: ACTION_NAME, fn: ACTION_FN });

                assert.include(MyGate.prototype._signedActions, ACTION_NAME);
            });

            it('should correctly add multiple signed actions', function() {
                var ACTIONS = [ 'action1', 'action2' ];

                ACTIONS.forEach(function(actionName) {
                    MyGate.action({ name: actionName, fn: ACTION_FN });
                });

                assert.sameMembers(MyGate.prototype._signedActions, ACTIONS);
            });

            it('should not mark action as "should be signed" if `signed` flag set to `false`',
                function(done) {
                    MyGate.action({ name: ACTION_NAME, fn: ACTION_FN, signed: false });

                    assert.notProperty(MyGate.prototype, '_signedActions');

                    disableDebugOutput(new MyGate(createMockParams()))
                        .callAction(ACTION_NAME)
                        .then(function() {
                            assert.ok(isActionExecuted);
                        })
                        .done(done);
                });

            it('should remove unsigned action from _signedActions property if it was added', function() {
                var MyGateInheritor;

                MyGate.action({
                    name: ACTION_NAME,
                    signed: true
                });

                MyGateInheritor = MyGate.create('MyGateInheritor');

                MyGateInheritor.action({
                    name: ACTION_NAME,
                    signed: false
                });

                assert.isTrue(new MyGate(createMockParams()).isActionMustBeSigned(ACTION_NAME));
                assert.isFalse(new MyGateInheritor(createMockParams()).isActionMustBeSigned(ACTION_NAME));
            });
        });

        describe('#validateSignature()', function() {
            beforeEach(function() {
                // remove default action post-processing
                MyGate.prototype.after = null;
            });

            it('is abstract method and throw an error if not overriden', function(done) {
                MyGate.action({ name: ACTION_NAME, fn: ACTION_FN });

                disableDebugOutput(new MyGate(createMockParams()))
                    .callAction(ACTION_NAME)
                    .then(function() {
                        assert.ok(false, 'this branch must be unreachable');
                    })
                    .fail(function(error) {
                        assert.instanceOf(error, GateError, 'throws GateError');
                        assert.strictEqual(error.code, GateError.CODES.METHOD_NOT_IMPLEMENTED);
                        assert.strictEqual(error.data.method, '#validateSignature');
                    })
                    .done(done);
            });

            it('should fail action chain if returns `false`', function(done) {
                MyGate.action({ name: ACTION_NAME, fn: ACTION_FN });

                MyGate.prototype.validateSignature = function(signature) {
                    return signature === 'it is valid, i promise!';
                };

                disableDebugOutput(new MyGate(createMockParams()))
                    .callAction(ACTION_NAME)
                    .then(function() {
                        assert.ok(false, 'this branch must be unreachable');
                    })
                    .fail(function(error) {
                        assert.instanceOf(error, GateError);
                        assert.strictEqual(error.code, GateError.CODES.INVALID_SIGNATURE);
                    })
                    .done(done);
            });

            it('should call Gate#onValidationError if #validateSignature returns false', function(done) {
                var catchedError = false;

                MyGate.action({ name: ACTION_NAME, fn: ACTION_FN });

                MyGate.prototype.validateSignature = function() {
                    return false;
                };

                MyGate.prototype.onValidationError = function() {
                    catchedError = true;

                    return;
                };

                disableDebugOutput(new MyGate(createMockParams()))
                    .callAction(ACTION_NAME)
                    .then(function() {
                        assert.isTrue(catchedError);
                    })
                    .fail(function(error) {
                        console.log(error);
                        assert.ok(false, 'this branch must be unreachable');
                    })
                    .done(done);
            });

            it('should allow action chain execution if returns `true`', function(done) {
                var VALID_SIGNATURE = 'it is valid, i promise!';

                MyGate.action({ name: ACTION_NAME, fn: ACTION_FN });

                MyGate.prototype.validateSignature = function(signature) {
                    return signature === VALID_SIGNATURE;
                };

                vow.all([
                        // 'crc' in request body
                        disableDebugOutput(
                            new MyGate(createMockParams({ req: { method: 'POST',  body: { crc: VALID_SIGNATURE } } })))
                                .callAction(ACTION_NAME)
                                .then(function() {
                                    assert.ok(isActionExecuted, 'action is done');
                                }),
                        // 'crc' in params passed to Gate constructor
                        disableDebugOutput(
                            new MyGate(createMockParams({ params: { crc: VALID_SIGNATURE } })))
                                .callAction(ACTION_NAME)
                                .then(function() {
                                    assert.ok(isActionExecuted, 'action is done');
                                })
                    ])
                    .then(function() {
                        return;
                    })
                    .done(done);
            });
        });

        describe('#isActionMustBeSigned()', function() {
            it('should return `true` for action which must be signed', function() {
                MyGate.action({ name: ACTION_NAME, fn: ACTION_FN });

                assert.isTrue(new MyGate(createMockParams()).isActionMustBeSigned(ACTION_NAME));
            });

            it('should return `false` for action which must be signed', function() {
                MyGate.action({ name: ACTION_NAME, fn: ACTION_FN, signed: false });

                assert.isFalse(new MyGate(createMockParams()).isActionMustBeSigned(ACTION_NAME));
            });
        });

        describe('#serializeError()', function() {
            it('should take `code`, `codeName` and `message` fields of error', function() {
                var myGateError = {
                        code: 'SOME_SPECIAL_CODE',
                        codeName: 'SOME_SPECIAL_CODENAME',
                        message: 'AND_MESSAGE'
                    },
                    serializationResult = MyGate.prototype.serializeError(myGateError),
                    deserializedResult;

                assert.doesNotThrow(function() {
                    deserializedResult = JSON.parse(serializationResult);
                });

                assert.deepEqual(myGateError, deserializedResult);
            });
        });

        describe('"after" chain member', function() {
            it('should set "Content-Type" header value to "application/json"', function(done) {
                var params = createMockParams();

                MyGate.action({ name: ACTION_NAME, fn: ACTION_FN });

                disableDebugOutput(new MyGate(params))
                    .callAction(ACTION_NAME)
                    .then(function() {
                        assert.strictEqual(params.res.headers['content-type'], 'application/json; charset=utf-8');
                    })
                    .done(done);
            });

            it('should prevent errors bubbling from #before', function(done) {
                MyGate.action({ name: ACTION_NAME, fn: ACTION_FN });

                vow.all([
                        // non-debug
                        disableDebugOutput(new MyGate(createMockParams()))
                            .callAction(ACTION_NAME),
                        // debug
                        (function() {
                            var MyDebugGate = MyGate.create('MyDebugGate');

                            MyDebugGate._DEBUG = true;

                            return disableDebugOutput(new MyDebugGate(createMockParams())).callAction(ACTION_NAME);
                        })()
                    ])
                    .then(function(results) {
                        results
                            .map(function(r) {
                                var _r;

                                assert.isString(r);
                                assert.doesNotThrow(function() {
                                    _r = JSON.parse(r);
                                });

                                return _r;
                            })
                            .forEach(checkResponseWithError(GateError.CODES.METHOD_NOT_IMPLEMENTED));
                    })
                    .done(done);
            });

            it('should prevent errors bubbling from action method', function(done) {
                var ERROR_MESSAGE = 'Unbelievable!';

                MyGate.action({
                    name: ACTION_NAME,
                    fn: function() {
                        throw new Error(ERROR_MESSAGE);
                    }
                });

                MyGate.prototype.before = null;

                vow.all([
                        // non-debug
                        disableDebugOutput(new MyGate(createMockParams()))
                            .callAction(ACTION_NAME),
                        // debug
                        (function() {
                            var MyDebugGate = MyGate.create('MyDebugGate');

                            MyDebugGate._DEBUG = true;

                            return disableDebugOutput(new MyDebugGate(createMockParams())).callAction(ACTION_NAME);
                        })()
                    ])
                    .then(function(results) {
                        results
                            .map(function(r) {
                                var _r;

                                assert.isString(r);
                                assert.doesNotThrow(function() {
                                    _r = JSON.parse(r);
                                });

                                return _r;
                            })
                            .forEach(checkResponseWithError(GateError.CODES.UNEXPECTED_ERROR, { type: 'Error' }));
                    })
                    .done(done);
            });

            it('should wrap all errors, except GateError and GateError inheritors into GateError.UNEXPECTED_ERROR',
                function(done) {
                    var MyGateError = GateError.create('MyGateError', { TEST_ERROR: 'Special test error' }),
                        ACTION_NAME_2 = ACTION_NAME + '2';

                    MyGate.prototype.before = null;

                    MyGate.action({
                        name: ACTION_NAME,
                        fn: function() {
                            throw new Error('test error');
                        }
                    });

                    MyGate.action({
                        name: ACTION_NAME_2,
                        fn: function() {
                            throw MyGateError.createError(MyGateError.CODES.TEST_ERROR);
                        }
                    });

                    vow
                        .all([
                            disableDebugOutput(new MyGate(createMockParams())).callAction(ACTION_NAME),
                            disableDebugOutput(new MyGate(createMockParams())).callAction(ACTION_NAME_2)
                        ])
                        .spread(function(rError, rMyGateError) {
                            var _rError, _rMyGateError;

                            assert.doesNotThrow(function() {
                                _rError = JSON.parse(rError);
                            });
                            assert.doesNotThrow(function() {
                                _rMyGateError = JSON.parse(rMyGateError);
                            });
                            checkResponseWithError(
                                MyGateError.CODES.UNEXPECTED_ERROR, { type: 'Error' }, MyGateError)(_rError);
                            checkResponseWithError(MyGateError.CODES.TEST_ERROR, {}, MyGateError)(_rMyGateError);
                        })
                        .done(done);
                });

            it('should handle non-serializable result of the action', function(done) {
                MyGate.action({
                    name: ACTION_NAME,
                    signed: false,
                    fn: function() {
                        var result = { x: 1 };

                        // cycle
                        result.y = result;

                        return result;
                    }
                });

                disableDebugOutput(new MyGate(createMockParams()))
                    .callAction(ACTION_NAME)
                    .then(function(r) {
                        var _r;

                        assert.isString(r);
                        assert.doesNotThrow(function() {
                            _r = JSON.parse(r);
                        });

                        assert.isObject(_r);
                        assert.isObject(_r.error);
                        assert.strictEqual(_r.error.code, GateError.CODES.CAN_NOT_SERIALIZE_RESULT);
                    })
                    .done(done);
            });

            it('should handle unexpected empty response from action', function(done) {
                MyGate.action({
                    name: ACTION_NAME,
                    signed: false,
                    fn: function() {
                        return null;
                    }
                });

                disableDebugOutput(new MyGate(createMockParams()))
                    .callAction(ACTION_NAME)
                    .then(function(r) {
                        var _r;

                        assert.isString(r);
                        assert.doesNotThrow(function() {
                            _r = JSON.parse(r);
                        });

                        assert.isObject(_r);
                        assert.isObject(_r.error);
                        assert.strictEqual(_r.error.code, GateError.CODES.UNEXPECTED_RESULT);
                    })
                    .done(done);
            });

            it('should process incoming error using #processActionError()', function(done) {
                var MyGateError = GateError.create('MyGateError', { TEST_ERROR: 'Special test error' });

                MyGate.action({
                    name: ACTION_NAME,
                    signed: false,
                    fn: function() {
                        throw new Error('fuckup');
                    }
                });

                MyGate.prototype.processActionError = function(error) {
                    return MyGateError.createError(MyGateError.CODES.TEST_ERROR, error);
                };

                disableDebugOutput(new MyGate(createMockParams()))
                    .callAction(ACTION_NAME)
                    .then(function(r) {
                        var _r;

                        assert.isString(r);
                        assert.doesNotThrow(function() {
                            _r = JSON.parse(r);
                        });

                        assert.isObject(_r);
                        assert.isObject(_r.error);
                        assert.strictEqual(_r.error.code, MyGateError.CODES.TEST_ERROR);
                    })
                    .done(done);
            });

            it('should use #serializeError for errors serialization', function(done) {
                var MyGateError = GateError.create('MyGateError', { TEST_ERROR: 'Special test error' }),
                    myGateError = MyGateError.createError(MyGateError.CODES.TEST_ERROR),
                    extraordinarySerializationResult = {
                        ARE_WE_HUMAN: 'OR ARE WE DANCERS'
                    };

                MyGate.action({
                    name: ACTION_NAME,
                    signed: false,
                    fn: function() {
                        throw myGateError;
                    }
                });

                MyGate.prototype.serializeError = sinon.stub().returns(
                    JSON.stringify(extraordinarySerializationResult)
                );

                disableDebugOutput(new MyGate(createMockParams()))
                    .callAction(ACTION_NAME)
                    .then(function(r) {
                        var _r;

                        assert.isTrue(MyGate.prototype.serializeError.calledWith(myGateError));

                        assert.isString(r);
                        assert.doesNotThrow(function() {
                            _r = JSON.parse(r);
                        });

                        assert.isObject(_r);
                        assert.isObject(_r.error);
                        assert.deepEqual(_r.error, extraordinarySerializationResult);
                    })
                    .done(done);
            });

            it('should serialize non-null action response', function(done) {
                var RESPONSE = { hello: 'world!' };

                MyGate.action({
                    name: ACTION_NAME,
                    signed: false,
                    fn: function() {
                        return RESPONSE;
                    }
                });

                disableDebugOutput(new MyGate(createMockParams()))
                    .callAction(ACTION_NAME)
                    .then(function(r) {
                        var _r;

                        assert.isString(r);
                        assert.doesNotThrow(function() {
                            _r = JSON.parse(r);
                        });

                        assert.isObject(_r);
                        assert.notProperty(_r, 'error');

                        assert.deepEqual(_r.response, RESPONSE);
                    })
                    .done(done);
            });

            it('should pass `_rawResponse` field of action result "as-is"', function(done) {
                var RESPONSE = 'raw response string';

                MyGate.action({
                    name : ACTION_NAME,
                    signed : false,
                    fn : function() {
                        return this.createRawResponse(RESPONSE);
                    }
                });

                disableDebugOutput(new MyGate(createMockParams()))
                    .callAction(ACTION_NAME)
                    .then(function(response) {
                        assert.strictEqual(response, RESPONSE);
                    })
                    .done(done);
            });

            it('should add refresh-tag to response if request has the param "version" which is not equal to expected',
                function(done) {
                    var VERSION = '1.0-001',
                        UNEQUAL_VERSION = VERSION + '1';

                    MyGate
                        .setVersion(VERSION)
                        .action({ name: ACTION_NAME, signed: false, fn: ACTION_FN });

                    vow.all([
                            disableDebugOutput(
                                new MyGate(createMockParams({ params: { version: UNEQUAL_VERSION } })))
                                    .callAction(ACTION_NAME),
                            disableDebugOutput(
                                new MyGate(createMockParams({ req: { body: { version: UNEQUAL_VERSION } } })))
                                    .callAction(ACTION_NAME)
                        ])
                        .then(function(results) {
                            results
                                .map(function(r) {
                                    var _r;

                                    assert.doesNotThrow(function() {
                                        _r = JSON.parse(r);
                                    }, 'default refresh-tag is valid JSON response member');

                                    return _r;
                                })
                                .forEach(function(r) {
                                    assert.isObject(r);
                                    assert.notProperty(r, 'error');
                                    assert.isArray(r.tasks);
                                    assert.deepEqual(r.tasks, [ { name : 'refresh' } ]);
                                });
                        })
                        .done(done);
                });

            it('should not add refresh-tag if request has not the param "version", but Gate has',
                function(done) {
                    MyGate
                        .setVersion('1.0-001')
                        .action({ name: ACTION_NAME, signed: false, fn: ACTION_FN });

                    disableDebugOutput(new MyGate(createMockParams()))
                        .callAction(ACTION_NAME)
                        .then(function(r) {
                            var _r;

                            assert.doesNotThrow(function() {
                                _r = JSON.parse(r);
                            });

                            assert.isObject(_r);
                            assert.notProperty(_r, 'error');
                            assert.notProperty(_r, 'tasks');
                        })
                        .done(done);
                });

            it('should correctly form the error message for UNEXPECTED_ERROR if non-terror error has been catched',
                function(done) {
                    var messageTemplate = GateError.MESSAGES[GateError.CODES.UNEXPECTED_ERROR],
                        originalLogger = GateError.prototype.logger,
                        logger = GateError.prototype.logger = sinon.spy(GateError.prototype.logger),
                        originalError = new Error('Oh, no!');

                    MyGate.action({
                        name: ACTION_NAME,
                        signed: false,
                        fn: function() {
                            throw originalError;
                        }
                    });

                    disableDebugOutput(new MyGate(createMockParams()))
                        .callAction(ACTION_NAME)
                        .then(function(r) {
                            var _r;

                            assert.doesNotThrow(function() {
                                _r = JSON.parse(r);
                            });

                            assert.isObject(_r);
                            assert.isObject(_r.error);
                            assert.strictEqual(_r.error.code, GateError.CODES.UNEXPECTED_ERROR);
                            assert.strictEqual(
                                _r.error.message,
                                GateError.createError(GateError.CODES.UNEXPECTED_ERROR)
                                    .bind({ type: originalError.constructor.name }).message);

                            assert.match(
                                logger.lastCall.args[0],
                                new RegExp(messageTemplate.replace(/%[^%]+%/g, '.*')));

                            assert.match(
                                logger.lastCall.args[0],
                                new RegExp(originalError.message));

                            GateError.prototype.logger = originalLogger;
                        })
                        .done(done);
                });

            /**
             * @param {Number} referenceError Код эталонной ошибки
             * @param {Object} [bindParams] Параметры ошибки. Если не заданы, message ошибки не проверяется
             * @param {Function} [ErrorCtor=GateError] Конструктор ошибки
             * @returns {Function} Функция проверки ответа с ошибкой
             */
            function checkResponseWithError(referenceError, bindParams, ErrorCtor) {
                if (typeof ErrorCtor === 'undefined') {
                    ErrorCtor = GateError;
                }

                return function(r, debug) {
                    assert.isObject(r.error);

                    assert.strictEqual(r.error.code, referenceError);
                    if (bindParams) {
                        assert.strictEqual(
                            r.error.message,
                            ErrorCtor.createError(referenceError).bind(bindParams).message);
                    }

                    if (debug) {
                        assert.isArray(r.debug);
                    } else {
                        assert.notProperty(r, 'debug');
                    }
                };
            }
        });

        describe('#pushTask()', function() {
            it('should add task to response which will be built by the "after" method', function(done) {
                var TASK_NAME = 'test',
                    TASK_OPTIONS = { async: true, name: 'test_1' };

                MyGate
                    .setVersion('1.0-001')
                    .action({
                        name: ACTION_NAME,
                        signed: false,
                        fn: function() {
                            this.pushTask(TASK_NAME, TASK_OPTIONS);
                        }
                    });

                disableDebugOutput(new MyGate(createMockParams({ params: { version: '0.9beta' } })))
                    .callAction(ACTION_NAME)
                    .then(function(r) {
                        var _r;

                        assert.doesNotThrow(function() {
                            _r = JSON.parse(r);
                        });

                        assert.isObject(_r);
                        assert.notProperty(_r, 'error');
                        assert.isArray(_r.tasks);

                        assert.sameMembers(_r.tasks.map(function(t) { return t.name; }), [ 'refresh', TASK_NAME ]);
                        assert.deepEqual(_r.tasks.reduce(function(pv, t) {
                                return t.name === TASK_NAME ? t : pv;
                            }, null), { name: TASK_NAME, options: TASK_OPTIONS });
                    })
                    .done(done);
            });
        });
    });
});
