/* global describe, it, afterEach, beforeEach, after, before */

var path = require('path'),
    vow = require('vow'),
    assert = require('chai').assert,
    sinon = require('sinon'),
    extend = require('extend'),
    Url = require('@vertis/nodules-libs').Url,
    disableDebugOutput = require('./lib/disable_debug_output');

describe('Controller', function() {
    var AbstractControllersError = require('../lib/error'),
        Controller = require('../lib').Controller,
        originalLogger = AbstractControllersError.prototype.logger,
        defaultControllersPath = Controller.path,
        createMockParams = require('./lib/controller_mock_params'),
        terrorLog = [];

    function resetControllersPath() {
        Controller.path = defaultControllersPath;
    }

    before(function() {
        // вместо стандартного вывода пишем логи ошибок контроллеров
        // в массив, откуда каждый тест может их получить
        AbstractControllersError.setLogger(function(message, level) {
            terrorLog.push({ message: message, level: level });
        });
    });

    afterEach(function() {
        // очищаем лог после каждого теста
        terrorLog = [];
    });

    after(function() {
        // восстанавливаем логгер
        AbstractControllersError.setLogger(originalLogger);
    });

    describe('constructor', function() {
        it('should return proper instance', function() {
            var p = createMockParams(),
                controller = new Controller(p);

            assert.instanceOf(controller, Controller);
            assert.strictEqual(controller.req, p.req);
            assert.strictEqual(controller.res, p.res);
        });
    });

    describe('.create()', function() {
        it('should create proper inheritor constructor', function() {
            var MyController = Controller.create(),
                myController = new MyController(createMockParams());

            assert.instanceOf(myController, MyController);
            assert.instanceOf(myController, Controller);

            assert.strictEqual(MyController.__super, Controller);
        });

        it('should setup #type field if type argument passed', function() {
            var myControllerType = 'MyController',
                MyController = Controller.create(myControllerType),
                MyControllerWithCtor = MyController.create(function() {
                    MyControllerWithCtor.__super.apply(this, arguments);
                });

            assert.strictEqual(MyController.prototype.type, myControllerType);
            assert.strictEqual(MyControllerWithCtor.prototype.type, null);
        });

        it('should copy "before" and "after" chains if any', function() {
            var Controller1 = Controller.create(),
                Controller2,
                SPECIFIC = {
                    before: function() {},
                    after: function() {}
                },
                CHAINS = {
                    before: [ function() {}, function() {} ],
                    after: [ function() {}, function() {}, function() {} ]
                },
                CHAINS_2 = extend(true, {}, CHAINS),
                chainNames = Object.keys(CHAINS);

            function assertChains(Ctrl, chains) {
                chainNames.forEach(function(chainName) {
                    assert.sameMembers(Ctrl.prototype['_' + chainName + 'Chain'], chains[chainName]);
                });
            }

            chainNames.forEach(function(chainName) {
                CHAINS[chainName].forEach(function(fn) {
                    Controller1[chainName](fn);
                });
            });

            assertChains(Controller1, CHAINS);

            Controller2 = Controller1.create();

            assertChains(Controller2, CHAINS);

            chainNames.forEach(function(chainName) {
                CHAINS_2[chainName] = CHAINS_2[chainName].concat(SPECIFIC[chainName]);
                Controller2[chainName](SPECIFIC[chainName]);
            });

            assertChains(Controller1, CHAINS);
            assertChains(Controller2, CHAINS_2);

            chainNames.forEach(function(chainName) {
                var _chainName = '_' + chainName + 'Chain',
                    chain1 = Controller1.prototype[_chainName],
                    chain2 = Controller2.prototype[_chainName];

                assert.notStrictEqual(chain1, chain2);
                assert.includeMembers(chain2, chain1);
            });
        });
    });

    describe('.path and .setPath()', function() {
        var TEST_PATH = '/tmp';

        afterEach(resetControllersPath);

        it('default value of path is result of process.cwd()', function() {
            assert.strictEqual(Controller.path, process.cwd());
        });

        it('Controller.path = x and Controller.setPath(x) has an equal effect', function() {
            Controller.path = TEST_PATH;
            assert.strictEqual(Controller.path, TEST_PATH);

            Controller.path = defaultControllersPath;
            Controller.setPath(TEST_PATH);
            assert.strictEqual(Controller.path, TEST_PATH);
        });

        it('should define global controllers search path', function() {
            var MyController = Controller.create();

            MyController.setPath(TEST_PATH);

            assert.strictEqual(MyController.path, TEST_PATH);
            assert.strictEqual(MyController.path, Controller.path);
        });
    });

    describe('.factory()', function() {
        var CONTROLLER_TYPE = 'example_watched',
            ExampleWatchedController = require('./mocks/' + CONTROLLER_TYPE),
            originalGetDataForFactoryError = Controller.getDataForFactoryError;

        function createFactoryMockParams(addition) {
            return createMockParams(
                extend(true, { type: CONTROLLER_TYPE }, addition));
        }

        before(function() {
            Controller.path = path.resolve(__dirname, 'mocks');

            // сбрасываем статистику перед тестами на всякий случай
            ExampleWatchedController.reset();

            Controller.getDataForFactoryError = sinon.spy(Controller.getDataForFactoryError);
        });

        after(function() {
            Controller.getDataForFactoryError = originalGetDataForFactoryError;
            resetControllersPath();
        });

        afterEach(function() {
            // сбрасываем статистику после каждого теста
            ExampleWatchedController.reset();
            Controller.getDataForFactoryError.reset();
        });

        it('should call proper controller constructor using "type" option', function() {
            var exampleController = Controller.factory(createFactoryMockParams());

            assert.strictEqual(exampleController.type, CONTROLLER_TYPE);
            assert.instanceOf(exampleController, ExampleWatchedController);

            assert.strictEqual(ExampleWatchedController.callCount, 1);
            assert.isTrue(ExampleWatchedController.lastCall.calledWithNew());
        });

        it('should correctly resolve controller path using viewTypePrefix and Url#viewType() from nodules-libs',
            function() {
                var DESKTOP_URL = new Url({ url: 'http://example.com/' }),
                    MOBILE_URL = new Url({ url: 'http://m.example.com/' }),
                    desktopController = Controller.factory(createFactoryMockParams({ req: { url_: DESKTOP_URL } })),
                    mobileController = Controller.setViewTypeDirectoryPrefix('special')
                        .factory(createFactoryMockParams({ req: { url_: MOBILE_URL } }));

                assert.strictEqual(desktopController.type, 'pages/desktop/example_watched');
                assert.strictEqual(mobileController.type, 'special/mobile/example_watched');

                assert.strictEqual(Controller.viewTypeDirectoryPrefix, 'special');
                Controller.viewTypeDirectoryPrefix = 'pages';
                assert.strictEqual(Controller.viewTypeDirectoryPrefix, 'pages');
            });

        it('should properly pass options to controller constructor', function() {
            var p = createFactoryMockParams();

            Controller.factory(p);

            var args = ExampleWatchedController.lastCall.args;

            assert.strictEqual(args.length, 1);
            assert.deepEqual(args[0], p);
        });

        it('should return `null` if `type` option is not defined', function() {
            assert.strictEqual(Controller.factory({}), null);
        });

        it('should return `null` if controller path is going outside project controllers root', function() {
            assert.strictEqual(Controller.factory({ directory: '../../../..', type: 'test' }), null);
        });

        it('should return `null` and log an error due to controller require failure', function() {
            var ControllerError = Controller.ControllerError,
                messageTemplate = ControllerError.MESSAGES[ControllerError.CODES.ERROR_IN_CONSTRUCTOR_FACTORY],
                originalLogger = ControllerError.prototype.logger,
                logger = ControllerError.prototype.logger = sinon.spy(ControllerError.prototype.logger);

            assert.strictEqual(Controller.factory({
                directory: '.',
                type: 'test',
                req: { headers: {} }
            }), null);

            assert.strictEqual(logger.callCount, 1);

            // проверяем, что логгер был вызван с нужным сообщением об ошибке
            assert.match(
                logger.lastCall.args[0],
                new RegExp(messageTemplate.replace(/%[^%]+%/g, '.*')));

            ControllerError.prototype.logger = originalLogger;
        });

        it('should call Controller.getDataForFactoryError to get additional data to log', function() {
            var args;

            assert.strictEqual(Controller.factory({
                directory: '.',
                type: 'test',
                req: { headers: {} }
            }), null);

            args = Controller.getDataForFactoryError.lastCall.args;

            assert.strictEqual(Controller.getDataForFactoryError.callCount, 1);
            assert.strictEqual(args.length, 1);
            assert.property(args[0], 'type');
            assert.property(args[0], 'directory');
        });
    });

    describe('.action()', function() {
        it('should declare action method', function(done) {
            var ACTION_NAME = 'test',
                RESULT = { hello: 'world' },
                ACTION_FN = function() {
                    return RESULT;
                },
                TestController = Controller.create('TestController');

            TestController.action( { name: ACTION_NAME, fn: ACTION_FN });

            assert.strictEqual(TestController.prototype['action_' + ACTION_NAME], ACTION_FN);

            disableDebugOutput(new TestController(createMockParams()))
                .callAction(ACTION_NAME)
                .then(function(result) {
                    assert.strictEqual(result, RESULT);
                })
                .done(done);
        });
    });

    describe('.getAction()', function() {
        var ACTION_NAME = 'test',
            TestController = null;

        beforeEach(function() {
            TestController = Controller.create('TestController');
        });

        it('should return action method by name', function() {
            var ACTION_FN = function() {};

            TestController.action({ name: ACTION_NAME, fn: ACTION_FN });

            assert.strictEqual(TestController.getAction(ACTION_NAME), ACTION_FN);
            assert.strictEqual(new TestController(createMockParams()).getAction(ACTION_NAME), ACTION_FN);
        });

        it('should return `null` if action method is not defined', function() {
            assert.strictEqual(TestController.getAction(ACTION_NAME), null);
            assert.strictEqual(new TestController(createMockParams()).getAction(ACTION_NAME), null);
        });

        it('should return `null` if property value assigned to action name is not a function', function() {
            TestController.prototype['action_' + ACTION_NAME] = { not: 'a function' };

            assert.strictEqual(TestController.getAction(ACTION_NAME), null);
            assert.strictEqual(new TestController(createMockParams()).getAction(ACTION_NAME), null);
        });
    });

    describe('#callAction()', function() {
        var PARAMS = createMockParams({
                params: {},
                directory: '.',
                type: 'example_action_index'
            }),
            // созраняем путь
            originalPath = Controller.path,
            indexController;

        before(function() {
            Controller.path = path.resolve(__dirname, './mocks');
        });

        after(function() {
            // восстанавливаем путь к каталогу контроллеров
            Controller.path = originalPath;
        });

        beforeEach(function() {
            indexController = disableDebugOutput(Controller.factory(PARAMS));
        });

        it('should call proper action method by given action name', function(done) {
            indexController.callAction('index')
                .then(function(actionResult) {
                    // промис разрешается результатом действия
                    assert.deepEqual(actionResult, { ok: true });

                    assert.strictEqual(indexController.action_index.callCount, 1);

                    // аргументы правильно передаются в метод действия
                    assert.strictEqual(indexController.action_index.lastCall.args.length, 1);
                    assert.isNull(indexController.action_index.lastCall.args[0]);
                })
                .done(done);
        });

        it('should reject promise with an error if action is not defined', function(done) {
            indexController.callAction('remove')
                .then(function() {
                    throw new Error('this branch must be unreachable');
                })
                .fail(function(error) {
                    assert.instanceOf(error, Controller.ControllerError);
                    assert.strictEqual(error.code, Controller.ControllerError.CODES.UNDEFINED_ACTION);
                })
                .done(done);
        });

        it('should call #before if defined', function(done) {
            indexController.before = sinon.spy(function() {});

            indexController.callAction('index')
                .then(function() {
                    assert.strictEqual(indexController.before.callCount, 1);
                })
                .done(done);
        });

        it('should not call #before if action is not defined', function(done) {
            indexController.before = sinon.spy(function() {});

            indexController.callAction('removed')
                .then(function() {
                    throw new Error('this branch must be unreachable');
                })
                .fail(function() {
                    assert.strictEqual(indexController.before.callCount, 0);
                })
                .done(done);
        });

        it('should call #after if defined', function(done) {
            indexController.after = sinon.spy(function(error, result) {
                if (error === null) {
                    return vow.fulfill(result);
                } else {
                    return vow.reject(error);
                }
            });

            indexController.callAction('index')
                .then(function() {
                    assert.strictEqual(indexController.after.callCount, 1);
                })
                .done(done);
        });
    });

    describe('#getReferer()', function() {
        var exampleControllerParams = createMockParams({ directory: '.', type: 'example' }),
            // созраняем путь
            originalPath = Controller.path;

        before(function() {
            Controller.path = path.resolve(__dirname, './mocks');
        });

        after(function() {
            // восстанавливаем путь к каталогу контроллеров
            Controller.path = originalPath;
        });

        it('should return value of the "Referer" HTTP header', function() {
            var REFERER = 'http://yandex.ru/',
                exampleController = Controller.factory(extend({}, exampleControllerParams,
                { req: { headers: { referer: REFERER } } }));

            assert.strictEqual(exampleController.getReferer(), REFERER);
        });

        it('should return empty value if "Referer" header is not defined', function() {
            var exampleController = Controller.factory(exampleControllerParams);

            assert.strictEqual(exampleController.getReferer(), '');
        });
    });

    describe('#getRoute()', function() {
        var exampleControllerParams = createMockParams({ directory: '.', type: 'example' }),
            // созраняем путь
            originalPath = Controller.path;

        before(function() {
            Controller.path = path.resolve(__dirname, './mocks');
        });

        after(function() {
            // восстанавливаем путь к каталогу контроллеров
            Controller.path = originalPath;
        });

        it('should return route passed to controller constructor', function() {
            var ROUTE = {},
                exampleController = Controller.factory(extend({}, exampleControllerParams, { route: ROUTE }));

            assert.strictEqual(exampleController.getRoute(), ROUTE);
        });
        
        it('should return null if route was not passed to constructor', function() {
            var exampleController = Controller.factory(exampleControllerParams);

            assert.strictEqual(exampleController.getRoute(), null);
        });
    });

    describe('#before()', function() {
        var exampleControllerParams = createMockParams({ directory: '.', type: 'example_action_index' }),
            // созраняем путь
            originalPath = Controller.path,
            exampleController;

        before(function() {
            Controller.path = path.resolve(__dirname, './mocks');
        });

        after(function() {
            // восстанавливаем путь к каталогу контроллеров
            Controller.path = originalPath;
        });

        beforeEach(function() {
            exampleController = disableDebugOutput(Controller.factory(exampleControllerParams));
        });

        it('should receive action name', function(done) {
            exampleController.before = sinon.spy(function() {});

            exampleController.callAction('index')
                .then(function() {
                    assert.strictEqual(exampleController.before.callCount, 1);
                    assert.strictEqual(exampleController.before.lastCall.args.length, 2);
                    assert.isNull(exampleController.before.lastCall.args[0]);
                    assert.strictEqual(exampleController.before.lastCall.args[1], 'index');
                })
                .done(done);
        });

        it('can cancel action method execution by rejecting promise', function(done) {
            var ERROR = new Error('failed by before');

            exampleController.before = sinon.spy(function() {
                return vow.reject(ERROR);
            });

            exampleController.callAction('index')
                .then(function() {
                    throw new Error('this branch must be unreachable');
                })
                .fail(function(error) {
                    assert.strictEqual(exampleController.before.callCount, 1);
                    assert.strictEqual(error, ERROR);
                })
                .done(done);
        });
    });

    describe('#after()', function() {
        var exampleControllerParams = createMockParams({ directory: '.', type: 'example_action_index' }),
            // созраняем путь
            originalPath = Controller.path,
            exampleController;

        before(function() {
            Controller.path = path.resolve(__dirname, './mocks');
        });

        after(function() {
            // восстанавливаем путь к каталогу контроллеров
            Controller.path = originalPath;
        });

        beforeEach(function() {
            exampleController = disableDebugOutput(Controller.factory(exampleControllerParams));
        });

        it('should receive result and name of an action method', function(done) {
            exampleController.after = sinon.spy(function() {});

            exampleController.callAction('index')
                .then(function() {
                    assert.strictEqual(exampleController.after.callCount, 1);
                    assert.deepEqual(exampleController.after.lastCall.args, [ null, { ok: true }, 'index' ]);
                })
                .done(done);
        });

        it('should receive error if an action method rejects a promise', function(done) {
            var ERROR = new Error('failed by action');

            exampleController.after = sinon.spy(function(error, result) {
                if (error === null) {
                    return vow.fulfill(result);
                } else {
                    return vow.reject(error);
                }
            });

            exampleController.action_reject = function() {
                return vow.reject(ERROR);
            };

            exampleController.callAction('reject')
                .then(function() {
                    throw new Error('this branch must be unreachable');
                })
                .fail(function() {
                    assert.strictEqual(exampleController.after.callCount, 1);
                    assert.deepEqual(exampleController.after.lastCall.args, [ ERROR, null, 'reject' ]);
                })
                .done(done);
        });

        it('should receive error if #before rejects a promise', function(done) {
            var ERROR = new Error('failed by before');

            exampleController.before = sinon.spy(function() {
                throw ERROR;
            });

            exampleController.after = sinon.spy(function(error, result) {
                if (error === null) {
                    return vow.fulfill(result);
                } else {
                    return vow.reject(error);
                }
            });

            exampleController.callAction('index')
                .then(function() {
                    throw new Error('this branch must be unreachable');
                })
                .fail(function() {
                    assert.strictEqual(exampleController.after.callCount, 1);
                    assert.deepEqual(exampleController.after.lastCall.args, [ ERROR, null, 'index' ]);
                })
                .done(done);
        });
    });

    describe('#getParam()', function() {
        var PARAMS = {
                single: '1',
                array: [ '1', '2' ],
                complex: { name: 'Joan', titles: [ 'developer', 'lead', 'CEO' ] }
            },
            controller;

        beforeEach(function() {
            controller = new Controller(createMockParams({ params: PARAMS }));
        });

        it('should return request parameter by name', function() {
            assert.strictEqual(controller.getParam('single'), PARAMS.single);
        });

        it('should return default value if parameter is not defined', function() {
            var DEFAULT_VALUE = 'no';

            assert.strictEqual(controller.getParam('married', DEFAULT_VALUE), DEFAULT_VALUE);
        });

        it('should return `null` if parameter and default value are not defined', function() {
            assert.strictEqual(controller.getParam('married'), null);
        });

        it('should return parameter if value pass the `allowedValues` filter', function() {
            var allowed = [ PARAMS.single ].concat(PARAMS.array);

            assert.strictEqual(controller.getParam('single', null, allowed), PARAMS.single);
            assert.sameMembers(controller.getParam('array', null, allowed), PARAMS.array);
        });
        
        it('should return default value if value does not pass the `allowedValues` test', function() {
            var DEFAULT_VALUE = 'ho-ho-ho',
                allowed = [ PARAMS.single + '_' ];

            assert.strictEqual(controller.getParam('single', DEFAULT_VALUE, allowed), DEFAULT_VALUE);
            assert.strictEqual(controller.getParam('array', DEFAULT_VALUE, allowed), DEFAULT_VALUE);
        });

        it('should allow single value as `allowedValues`', function() {
            assert.strictEqual(controller.getParam('single', null, PARAMS.single), PARAMS.single);
        });
        
        it('should return parameter value if it pass `allowedType` test', function() {
            var allowNumber = 'number',
                allowNumberByRegExp = /^\d+$/;

            assert.strictEqual(controller.getParam('single', null, null, allowNumber), PARAMS.single);
            assert.strictEqual(controller.getParam('single', null, null, allowNumberByRegExp), PARAMS.single);
            assert.sameMembers(controller.getParam('array', null, null, allowNumber), PARAMS.array);
            assert.sameMembers(controller.getParam('array', null, null, allowNumberByRegExp), PARAMS.array);
        });

        it('should return default value if parameter value fails the `allowedType` test', function() {
            var DEFAULT_VALUE = PARAMS.single + '_',
                allowBoolean = "boolean",
                allowBooleanByRegExp = /^(true|false)$/g;

            assert.strictEqual(controller.getParam('single', DEFAULT_VALUE, null, allowBoolean), DEFAULT_VALUE);
            assert.strictEqual(controller.getParam('single', DEFAULT_VALUE, null, allowBooleanByRegExp), DEFAULT_VALUE);
            assert.strictEqual(controller.getParam('array', DEFAULT_VALUE, null, allowBoolean), DEFAULT_VALUE);
            assert.strictEqual(controller.getParam('array', DEFAULT_VALUE, null, allowBooleanByRegExp), DEFAULT_VALUE);
        });

        it('should return shallow copy of parameter with multiple values', function() {
            var arrayParam = controller.getParam('array');

            assert.notStrictEqual(arrayParam, PARAMS.array);
            assert.sameMembers(arrayParam, PARAMS.array);
        });
    });

    describe('#getBooleanParam()', function() {
        var PARAMS = {
                paramTrue: 'true',
                param1: '1',
                paramYes: 'yes',
                paramOn: 'on',
                paramFalse: 'false',
                param0: '0',
                paramNo: 'no',
                paramOff: 'off',
                array: [ 'on', 'off' ],
                notboolean: 'foo'
            },
            controller;

        beforeEach(function() {
            controller = new Controller(createMockParams({ params: PARAMS }));
        });

        it('should call getParam with name and defaultValue arguments', function() {
            var getParam = sinon.spy(controller, 'getParam'),
                name = 'foobar' + Math.random();

            controller.getBooleanParam(name, true);
            assert.isTrue(getParam.calledWith(name, true));

            controller.getBooleanParam(name, false);
            assert.isTrue(getParam.calledWith(name, false));
        });

        it('should process array properly', function() {
            assert.deepEqual(controller.getBooleanParam('array'), [ true, false ]);
        });

        it('should return defaultValue if the parameter is not Boolean', function() {
            assert.isTrue(controller.getBooleanParam('notboolean', true));
            assert.isFalse(controller.getBooleanParam('notboolean', false));
        });

        it('should return the same value as getParam if the parameter doesn’t exist', function() {
            assert.strictEqual(
                controller.getBooleanParam('foo'),
                controller.getParam('foo')
            );
        });

        it('should convert \'true\', \'1\', \'yes\', \'on\' to true', function() {
            assert.isTrue(controller.getBooleanParam('paramTrue'));
            assert.isTrue(controller.getBooleanParam('param1'));
            assert.isTrue(controller.getBooleanParam('paramYes'));
            assert.isTrue(controller.getBooleanParam('paramOn'));
        });

        it('should convert \'false\', \'0\', \'no\', \'off\' to false', function() {
            assert.isFalse(controller.getBooleanParam('paramFalse'));
            assert.isFalse(controller.getBooleanParam('param0'));
            assert.isFalse(controller.getBooleanParam('paramNo'));
            assert.isFalse(controller.getBooleanParam('paramOff'));
        });
    });

    describe('#getNumericParam()', function() {
        var PARAMS = {
                num: '42',
                zero: '0',
                array: [ '1', '2', '3', '4' ],
                notnumeric: 'foo',
                negative: '-1',
                real: '3.14',
                exp: '2e8',
                startWithSpace: ' 1',
                endWithSpace: '1 ',
                startWithPoint: '.2',
                endWithPoint: '3.',
                dirty: '5z'
            },
            controller;

        beforeEach(function() {
            controller = new Controller(createMockParams({ params: PARAMS }));
        });

        it('should call getParam with name defaultValue and allowedValues arguments', function() {
            var getParam = sinon.spy(controller, 'getParam'),
                name = 'foobar' + Math.random(),
                defaultValue = Math.random();

            controller.getNumericParam(name, defaultValue, [ 1, 2 ]);

            assert.isTrue(getParam.calledWith(name, defaultValue, [ '1', '2' ], 'number'));
        });

        it('should work correctly with 0 in allowedValues', function() {
            var getParam = sinon.spy(controller, 'getParam');

            controller.getNumericParam('zero', null, 0);

            assert.isTrue(getParam.calledWith('zero', null, '0', 'number'));
        });

        it('should return number if param is specified', function() {
            assert.strictEqual(controller.getNumericParam('num'), parseInt(PARAMS.num, 10));
        });

        it('should process array properly', function() {
            assert.deepEqual(controller.getNumericParam('array'), [ 1, 2, 3, 4 ]);
        });

        it('should return the same value as getParam if param is not numeric', function() {
            assert.strictEqual(
                controller.getNumericParam('notnumeric'),
                controller.getParam('notnumeric', null, null, 'number')
            );
        });

        it('should return defaultValue if there is no param', function() {
            assert.strictEqual(controller.getNumericParam('blablabla', 128), 128);
        });

        it('should parse only natural numbers', function() {
            assert.isNull(controller.getNumericParam('negative'));
            assert.isNull(controller.getNumericParam('real'));
            assert.isNull(controller.getNumericParam('exp'));
            assert.isNull(controller.getNumericParam('startWithSpace'));
            assert.isNull(controller.getNumericParam('endWithSpace'));
            assert.isNull(controller.getNumericParam('startWithPoint'));
            assert.isNull(controller.getNumericParam('endWithPoint'));
            assert.isNull(controller.getNumericParam('dirty'));
        });
    });

    describe('#getParams()', function() {
        var PARAMS = {
                single: '1',
                array: [ '1', '2' ],
                complex: { name: 'Joan', titles: [ 'developer', 'lead', 'CEO' ] }
            },
            controller;

        beforeEach(function() {
            controller = new Controller(createMockParams({ params: PARAMS }));
        });

        it('should return object includes all controller parameters', function() {
            assert.deepEqual(controller.getParams(), PARAMS);
        });
        
        it('should return null if no any parameters was passed to controller constructor', function() {
            controller = new Controller(createMockParams());

            assert.strictEqual(controller.getParams(), null);
        });
        
        it('should return only parameters which names are enumerated in the `includeNames` argument', function() {
            var includes = [ 'single', 'complex' ],
                params = controller.getParams(includes);

            assert.sameMembers(Object.keys(params), includes);
        });

        it('should return `null` if `includeNames` array is empty', function() {
            assert.strictEqual(controller.getParams([]), null);
        });

        it('should not return parameters which names are enumerated in the `excludeNames` arguments', function() {
            var excludes = [ 'single' ],
                expected = Object.keys(PARAMS).filter(function(name) {
                    return excludes.indexOf(name) === -1;
                }),
                params = controller.getParams(null, excludes);

            assert.sameMembers(Object.keys(params), expected);
        });

        it('`excludeNames` argument has higher priority than the `includeNames` argument', function() {
            var includes = [ 'single', 'complex' ],
                excludes = [ 'single' ],
                expected = Object.keys(PARAMS).filter(function(name) {
                    return includes.indexOf(name) > -1 && excludes.indexOf(name) === -1;
                }),
                params = controller.getParams(includes, excludes);

            assert.sameMembers(Object.keys(params), expected);
        });
    });

    describe('#hasParam()', function() {
        var PARAMS = {
                single: '1',
                array: [ '1', '2' ],
                complex: { name: 'Joan', titles: [ 'developer', 'lead', 'CEO' ] }
            },
            controller;

        beforeEach(function() {
            controller = new Controller(createMockParams({ params: PARAMS }));
        });

        it('should return `true` if parameter is passed to controller constructor', function() {
            assert.isTrue(controller.hasParam('single'));
        });

        it('should return `false` if parameter is not passed to controller constructor', function() {
            assert.isFalse(controller.hasParam('married'));
        });

        it('should return `true` if parameter value(s) are not equal to the `value` argument', function() {
            var reverseArray = [].concat(PARAMS.array).reverse();

            assert.isTrue(controller.hasParam('single', PARAMS.single));
            assert.isTrue(controller.hasParam('array', PARAMS.array));
            assert.isTrue(controller.hasParam('array', reverseArray));
        });

        it('should return `false` if parameter value(s) are not equal to the `value` argument', function() {
            assert.isFalse(controller.hasParam('single', PARAMS.single + '_'));
            assert.isFalse(controller.hasParam('array', PARAMS.array[0]));
        });
    });

    describe('#hasParams()', function() {
        var PARAMS = {
                single: '1',
                array: [ '1', '2' ],
                complex: { name: 'Joan', titles: [ 'developer', 'lead', 'CEO' ] }
            },
            controller;

        beforeEach(function() {
            controller = new Controller(createMockParams({ params: PARAMS }));
        });

        it('should return `true` if all params is passed to controller constructor', function() {
            assert.isTrue(controller.hasParams([ 'single', 'complex' ]));
        });

        it('should return `false` if any parameter is passed to controller constructor', function() {
            assert.isFalse(controller.hasParams([ 'single', 'apple', 'complex' ]));
        });
    });

    describe('#buildTimerId()', function() {
        var TYPE = 'superpage',
            VIEW_TYPE = 'superior-tv',
            TIMER_NAME = 'test_timer',
            controller;

        beforeEach(function() {
            controller = new Controller(createMockParams({ type: TYPE }));
        });

        it('should return valid timer ID with view type "default" if req.url_ is not an object', function() {
            assert.strictEqual(controller.buildTimerId(TIMER_NAME), 'controller.default.' + TYPE + '.' + TIMER_NAME);
        });

        it('should return correct timer ID with view type from rqe.url_.viewType() if req.url_ is an object',
            function() {
                var controller = new Controller(createMockParams({
                        type: TYPE,
                        req: {
                            url_: {
                                viewType: function() { return VIEW_TYPE; }
                            }
                        }
                    }));

                assert.strictEqual(
                    controller.buildTimerId(TIMER_NAME),
                    'controller.' + VIEW_TYPE + '.' + TYPE + '.' + TIMER_NAME);
            });
    });

    function generateChainTestCases(chainName, execOrder) {
        var TestController;

        beforeEach(function() {
            TestController = Controller.create();
        });

        function buildChainPropName(chainName) {
            return '_' + chainName + 'Chain';
        }

        it('should return Controller constructor itself', function() {
            assert.strictEqual(TestController[chainName](function() {}), TestController);
        });

        it('should produce "' + chainName + '" chain and handler on first call', function() {
            var chainPropName = buildChainPropName(chainName),
                fn = function() {};

            assert.doesNotThrow(function() {
                TestController[chainName](fn);
            });

            assert.isArray(TestController.prototype[chainPropName], 'TestController#' + chainPropName + ' is array');
            assert.isFunction(TestController.prototype[chainName]);
        });

        it('should preserve previously declared #' + chainName + '() method in the chain', function() {
            var chainPropName = '_' + chainName + 'Chain',
                oldFn = function() {},
                fn = function() {};

            TestController.prototype[chainName] = oldFn;
            TestController[chainName](fn);

            assert.notEqual(TestController.prototype[chainName], oldFn);
            assert.sameMembers(TestController.prototype[chainPropName], [ oldFn, fn ]);
        });

        it('generated handler must execute chained functions in the ' + execOrder.name + ' order', function(done) {
            var stack = [],
                RESULT = execOrder.input;

            RESULT.forEach( function(result) {
                TestController[chainName](function() {
                    stack.push(result);
                });
            });

            TestController.action({
                name: 'test',
                fn: function() { return true; }
            });

            disableDebugOutput(new TestController(createMockParams()))
                .callAction('test')
                .then(function() {
                    assert.deepEqual(stack, execOrder.output);
                })
                .done(done);
        });

        it('generated handler must return a promise if chain contains any functions', function(done) {
            var fn = function() {},
                result;

            TestController[chainName](fn);
            result = TestController.prototype[chainName]();

            assert.ok(vow.isPromise(result));

            result.done(done);
        });

        it('generated handler must return undefined if chain is not an array or empty', function() {
            var fn = function() {};

            TestController[chainName](fn);
            TestController.prototype[buildChainPropName(chainName)] = null;

            assert.isUndefined(TestController.prototype[chainName]());
        });

        it('should not add same function to chain twice', function() {
            var fn = function() {};

            TestController[chainName](fn);
            TestController[chainName](fn);

            assert.strictEqual(TestController.prototype[buildChainPropName(chainName)].length, 1);
        });

        it('should add function to chain if it always in the chain and `options.force` flag set to `true`',
            function() {
                var fn = function() {};

                TestController[chainName](fn);
                TestController[chainName](fn, { force: true });

                assert.strictEqual(TestController.prototype[buildChainPropName(chainName)].length, 2);
            });

        it('should clear the chain before adding the function if `options.clear` flag set to `true`', function() {
            var fn1 = function() {},
                fn2 = function() {};

            TestController[chainName](fn1);
            TestController[chainName](fn2, { clear: true });

            assert.sameMembers(TestController.prototype[buildChainPropName(chainName)], [ fn2 ]);
        });

        it('should properly create handler on first call if `options.clear` flag set to `true`',
            function() {
                var fn = function() {};

                TestController[chainName](fn, { clear: true });

                assert.sameMembers(TestController.prototype[buildChainPropName(chainName)], [ fn ]);
                assert.isFunction(TestController.prototype[chainName]);
            });

        it('should add function to chain in opposite order if `options.reverse` flag set to `true`',
           function(done) {
                var stack = [],
                    RESULT = execOrder.input;

                RESULT.forEach( function(result) {
                    TestController[chainName](function() {
                        stack.push(result);
                    }, { reverse : true });
                });

                TestController.action({
                    name: 'test',
                    fn: function() { return true; }
                });

                disableDebugOutput(new TestController(createMockParams()))
                    .callAction('test')
                    .then(function() {
                        assert.deepEqual(stack, execOrder.output.reverse());
                    })
                    .done(done);
           });
    }

    describe('.before()', function() {
        generateChainTestCases('before', { name: 'FIFO', input: [ 1, 2, 3 ], output: [ 1, 2, 3 ] });
    });

    describe('.after()', function() {
        generateChainTestCases('after', { name: 'LIFO', input: [ 1, 2, 3 ], output: [ 3, 2, 1 ] });

        it('should reject action promises chain if action failed and "after" chain is empty', function(done) {
            var ACTION_NAME = 'test',
                ACTION_ERROR = new Error('action error'),
                TestController = Controller.create('TestController');

            TestController.prototype['action_' + ACTION_NAME] = function() {
                throw ACTION_ERROR;
            };

            TestController.after(function() {});
            TestController.prototype._afterChain = [];

            disableDebugOutput(new TestController(createMockParams()))
                .callAction(ACTION_NAME)
                .fail(function(error) {
                    assert.strictEqual(error, ACTION_ERROR);
                    throw error;
                })
                .then(function() {
                    assert.ok(false, 'this branch must be unreachable');
                })
                .fail(function(error) {
                    if (error.name === 'AssertionError') {
                        throw error;
                    }
                })
                .done(done);
        });
    });

    describe('error builders', function() {
        var PARAMS = {
                single: '1',
                array: [ '1', '2' ],
                complex: { name: 'Joan', titles: [ 'developer', 'lead', 'CEO' ] }
            },
            ControllerError = Controller.ControllerError,
            controller = new Controller(createMockParams({ params: PARAMS }));

        describe('#createBadRequestError()', function() {
            it('should produce ControllerError with EMPTY_RESPONSE code and passed reason message', function() {
                var REASON = 'nobody care',
                    error = controller.createBadRequestError(REASON);

                assert.instanceOf(error, Error);
                assert.instanceOf(error, ControllerError);
                assert.strictEqual(error.code, ControllerError.CODES.BAD_REQUEST);
                assert.strictEqual(error.data.reason, REASON);
                assert.include(error.message, REASON);
            });

            it('should produce ControllerError with EMPTY_RESPONSE code and unknown reason if argument is not passed',
                function() {
                    var error = controller.createBadRequestError();

                    assert.strictEqual(error.data.reason, 'unknown reason');
                    assert.include(error.message, 'unknown reason');
                });

            it('should produce ControllerError with BAD_REQUEST code and construct reason by passed params array',
                function() {
                    var REQUIRED_PARAMS = [ 'single', 'not_passed' ],
                        error = controller.createBadRequestError(REQUIRED_PARAMS);

                    assert.sameMembers(error.data.requiredParams, REQUIRED_PARAMS);
                    assert.include(error.data.absentParams, REQUIRED_PARAMS[1]);
                    assert.include(error.message, REQUIRED_PARAMS[1]);
                });
        });

        describe('#createEmptyResponseError()', function() {
            it('should produce ControllerError with EMPTY_RESPONSE code and passed reason message', function() {
                var REASON = 'nobody care',
                    error = controller.createEmptyResponseError(REASON);

                assert.instanceOf(error, Error);
                assert.instanceOf(error, ControllerError);
                assert.strictEqual(error.code, ControllerError.CODES.EMPTY_RESPONSE);
                assert.strictEqual(error.data.reason, REASON);
                assert.include(error.message, REASON);
            });

            it('should produce ControllerError with EMPTY_RESPONSE code and unknown reason if argument is not passed',
                function() {
                    var error = controller.createEmptyResponseError();

                    assert.strictEqual(error.data.reason, 'unknown reason');
                    assert.include(error.message, 'unknown reason');
                });
        });
    });
});
