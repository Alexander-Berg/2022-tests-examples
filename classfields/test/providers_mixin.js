/* global describe, it, beforeEach */

var assert = require('chai')
        .use(require('chai-nodules-helpers'))
        .assert,
    vow = require('vow'),
    extend = require('extend'),
    disableDebugOutput = require('./lib/disable_debug_output');

describe('ProvidersMixin', function() {
    var controllers = require('../lib'),
        Controller = controllers.Controller,
        ProvidersMixin = controllers.ProvidersMixin,
        TestController;

    beforeEach(function() {
        TestController = Controller.create(function TestController() {
            TestController.__super.apply(this, arguments);
        });
    });

    function applyProvidersMixin() {
        TestController.mixin(ProvidersMixin);
    }

    describe('Mixin', function() {
        it('should be mixed properly using `Controller.mixin()`', function() {
            assert.canBeMixed(TestController, ProvidersMixin, 'ProvidersMixin',
                [ 'name', 'prototype' ], ['constructor' ]);
        });
    });

    describe('overriden Controller.create()', function() {
        it('should always skip `providers` and `blocks` static properties on inheritance', function() {
            var BLOCKS = { block : 'page' },
                NewController;

            TestController.mixin(ProvidersMixin);
            TestController.blocks = BLOCKS;
            TestController.dataProviderDecl('empty', function() {});

            assert.doesNotThrow(function() {
                NewController = TestController.create();
            });

            assert.strictEqual(NewController.blocks, null);
            assert.deepEqual(NewController.providers, {});
        });

        it('should correctly works on long inheritance chains', function() {
            var TEST_PROVIDER_NAME = 'one',
                NewController1,
                NewController2;

            TestController.mixin(ProvidersMixin);
            TestController.dataProviderDecl(TEST_PROVIDER_NAME, function() {});

            assert.doesNotThrow(function() {
                NewController1 = TestController.create();
            });
            assert.notProperty(NewController1.providers, TEST_PROVIDER_NAME);
            assert.doesNotThrow(function() {
                NewController1.dataProviderDecl(TEST_PROVIDER_NAME);
                NewController2 = NewController1.create();
            });
            assert.notProperty(NewController2.providers, TEST_PROVIDER_NAME);
        });
    });

    describe('.dataProviderDecl()', function() {
        beforeEach(applyProvidersMixin);

        it('should define data provider by name and function', function() {
            var PROVIDER_NAME = 'echo',
                PROVIDER_FN = function() {};

            assert.doesNotThrow(function() {
                TestController.dataProviderDecl(PROVIDER_NAME, PROVIDER_FN);
            });

            assert.property(TestController.providers, PROVIDER_NAME,
                'provider correctly added to index');
            assert.strictEqual(TestController.providers[PROVIDER_NAME].fn, PROVIDER_FN,
                'provider descriptor has correct link to function');
            assert.isArray(TestController.providers[PROVIDER_NAME].deps,
                'provider descriptor has array of dependencies');
            assert.strictEqual(TestController.providers[PROVIDER_NAME].deps.length, 0,
                'provider dependencies are empty');
        });

        it('should define data provider with single dependency passed as single string', function() {
            var PROVIDER_NAME = 'echo',
                PROVIDER_DEP = 'some',
                PROVIDER_FN = function() {};

            assert.doesNotThrow(function() {
                TestController.dataProviderDecl(PROVIDER_DEP, PROVIDER_FN);
                TestController.dataProviderDecl(PROVIDER_NAME, PROVIDER_DEP, PROVIDER_FN);
            });

            assert.strictEqual(TestController.providers[PROVIDER_NAME].deps.length, 1,
                'provider has one dependency');

            assert.include(TestController.providers[PROVIDER_NAME].deps, PROVIDER_DEP,
                'provider has correct dependency');
        });

        it('should define data provider with multiple dependencies passed as array of strings', function() {
            var PROVIDER_NAME = 'echo',
                PROVIDER_DEP_1 = 'some1',
                PROVIDER_DEP_2 = 'some2',
                deps = [ PROVIDER_DEP_1, PROVIDER_DEP_2],
                PROVIDER_FN = function() {};

            assert.doesNotThrow(function() {
                TestController.dataProviderDecl(PROVIDER_DEP_1, PROVIDER_FN);
                TestController.dataProviderDecl(PROVIDER_DEP_2, PROVIDER_FN);
                TestController.dataProviderDecl(PROVIDER_NAME, deps, PROVIDER_FN);
            });

            assert.sameMembers(TestController.providers[PROVIDER_NAME].deps, deps,
                'provider has correct dependencies');
        });

        it('should declare inherited provider by name', function() {
            var PROVIDER_NAME = 'one',
                Inheritor = TestController.create();

            TestController.dataProviderDecl(PROVIDER_NAME, function() {});

            assert.doesNotThrow(function() {
                 Inheritor.dataProviderDecl(PROVIDER_NAME);
            });

            assert.deepEqual(TestController.providers, Inheritor.providers);
        });

        it('should throw an error if inherited provider function is not found', function() {
            var Inheritor = TestController.create();

            assert.throwTerror(function() {
                Inheritor.dataProviderDecl('not-exists');
            }, ProvidersMixin.ProvidersError, 'DATA_PROVIDER_FUNCTION_NOT_DEFINED');
        });

        it('should declare inherited provider with single dependency', function() {
            var PROVIDER_NAME = 'echo',
                PROVIDER_DEP = 'some',
                PROVIDER_FN = function() {},
                Inheritor = TestController.create();

            TestController.dataProviderDecl(PROVIDER_DEP, PROVIDER_FN);
            TestController.dataProviderDecl(PROVIDER_NAME, PROVIDER_DEP, PROVIDER_FN);

            Inheritor.dataProviderDecl(PROVIDER_DEP);

            assert.doesNotThrow(function() {
                Inheritor.dataProviderDecl(PROVIDER_NAME, PROVIDER_DEP);
            });
        });

        it('should declare inherited provider with multiple dependencies', function() {
            var PROVIDER_NAME = 'echo',
                PROVIDER_DEP_1 = 'some1',
                PROVIDER_DEP_2 = 'some2',
                deps = [ PROVIDER_DEP_1, PROVIDER_DEP_2],
                PROVIDER_FN = function() {},
                Inheritor = TestController.create();

            TestController.dataProviderDecl(PROVIDER_DEP_1, PROVIDER_FN);
            TestController.dataProviderDecl(PROVIDER_DEP_2, PROVIDER_FN);
            TestController.dataProviderDecl(PROVIDER_NAME, deps, PROVIDER_FN);

            Inheritor.dataProviderDecl(PROVIDER_DEP_1);
            Inheritor.dataProviderDecl(PROVIDER_DEP_2);

            assert.doesNotThrow(function() {
                Inheritor.dataProviderDecl(PROVIDER_NAME, deps);
            });
        });

        it('should override provider if exists', function() {
            var PROVIDER_NAME = 'one',
                Inheritor = TestController.create(),
                PROVIDER_FN_ORIGIN = function() {},
                PROVIDER_FN_OVERRIDEN = function() {};

            TestController.dataProviderDecl(PROVIDER_NAME, PROVIDER_FN_ORIGIN);

            assert.doesNotThrow(function() {
                Inheritor.dataProviderDecl(PROVIDER_NAME, PROVIDER_FN_OVERRIDEN);
            });

            assert.strictEqual(Inheritor.getProvider(PROVIDER_NAME), PROVIDER_FN_OVERRIDEN);
        });
    });

    describe('.getProvider()', function() {
        beforeEach(applyProvidersMixin);

        it('should returns existing provider function', function() {
            var PROVIDER_NAME = 'echo',
                PROVIDER_FN = function() {};

            TestController.dataProviderDecl(PROVIDER_NAME, PROVIDER_FN);

            assert.strictEqual(TestController.getProvider(PROVIDER_NAME), PROVIDER_FN);
        });

        it('should throw an appropriate error if requested provider is not exists', function() {
            assert.throwTerror(function() {
                TestController.getProvider('not-exists');
            }, ProvidersMixin.ProvidersError, 'DATA_PROVIDER_NOT_EXISTS');
        });
    });

    describe('.blocks and .providers relationship', function() {
        var BLOCKS = {
                block: 'page',
                content: [
                    {
                        block: 'header',
                        data: [ 'user', 'geo-info' ]
                    },
                    {
                        block: 'content',
                        data: 'page-content'
                    },
                    {
                        block: 'footer',
                        data: [ 'copyright', 'geo-info' ]
                    },
                    {
                        block: 'paranja',
                        actions: [ 'action1' ],
                        data: 'opacity'
                    },
                    {
                        block: 'spinner',
                        data: 'speed'
                    },
                    {
                        block: 'spinner',
                        actions: 'action1',
                        data: 'direction-left'
                    },
                    {
                        block: 'spinner',
                        actions: [ 'action2' ],
                        data: 'direction-right'
                    }
                ]
            },
            SINGLE_BLOCK = 'footer',
            SINGLE_BLOCK_PROVIDERS = [ 'copyright', 'geo-info' ],
            MULTIPLE_BLOCKS = [ 'header', SINGLE_BLOCK ],
            MULTIPLE_BLOCKS_PROVIDERS = [ 'user' ].concat(SINGLE_BLOCK_PROVIDERS),
            SINGLE_NOT_EXISTS = 'not-exists',
            ONE_NOT_EXISTS = [ SINGLE_NOT_EXISTS, 'header' ],
            ALL_PROVIDERS = [
                'user', 'geo-info', 'page-content', 'copyright',
                'opacity', 'speed', 'direction-left', 'direction-right'
            ],
            ALL_BLOCKS = [ 'page', 'header', 'content', 'footer', 'paranja', 'spinner' ];

        beforeEach(function() {
            applyProvidersMixin();

            TestController.blocks = BLOCKS;
            ALL_PROVIDERS.forEach(function(pName) {
                TestController.dataProviderDecl(pName, function() {});
            });
        });

        describe('.getProvidersForBlocks()', function() {
            it('should return providers for single block passed as single string', function() {
                assert.sameMembers(TestController.getProvidersForBlocks(SINGLE_BLOCK), SINGLE_BLOCK_PROVIDERS);
            });

            it('should return providers for multiple blocks passed as array of strings', function() {
                assert.sameMembers(TestController.getProvidersForBlocks(MULTIPLE_BLOCKS), MULTIPLE_BLOCKS_PROVIDERS);
            });

            it('should return all providers if block is not found in the blocks tree', function() {
                assert.sameMembers(TestController.getProvidersForBlocks(SINGLE_NOT_EXISTS), ALL_PROVIDERS);
            });

            it('should return all providers if any block from passed array is not found in blocks tree', function() {
                assert.sameMembers(TestController.getProvidersForBlocks(ONE_NOT_EXISTS), ALL_PROVIDERS);
            });

            it('should return all providers if no arguments are passed', function() {
                assert.sameMembers(TestController.getProvidersForBlocks(), ALL_PROVIDERS);
            });

            it('should return all providers if blocks tree is not defined for controller', function() {
                var ControllerWithoutBlocks = TestController.create();

                ControllerWithoutBlocks.providers = extend({}, TestController.providers);

                assert.sameMembers(ControllerWithoutBlocks.getProvidersForBlocks('page'), ALL_PROVIDERS);
            });

            it('should return providers for requested blocks with array of blocks in Page.blocks', function() {
                TestController.blocks = [
                    TestController.blocks,
                    {
                        block: 'page-mobile',
                        content: {
                            block: 'footer',
                            data: 'user'
                        }
                    }
                ];

                assert.sameMembers(TestController.getProvidersForBlocks(SINGLE_BLOCK), MULTIPLE_BLOCKS_PROVIDERS);
            });

            it('should return providers for blocks with required action', function() {
                TestController.blocks = {
                    block: 'page',
                    data: 'page-data',
                    content: [
                        {
                            block: 'content',
                            content: [
                                {
                                    block: 'form',
                                    data: 'form-data'
                                },
                                {
                                    block: 'spinner',
                                    actions: 'action1',
                                    data: 'spinner-action1-data',
                                    content: {
                                        block: 'spinner-inner',
                                        data: 'spinner-inner-data'
                                    }
                                },
                                {
                                    block: 'spinner',
                                    actions: [ 'action2' ],
                                    data: 'spinner-action2-data'
                                }
                            ]
                        },
                        {
                            block: 'paranja',
                            actions: [ 'action1' ],
                            data: 'paranja-data'
                        }
                    ]
                };

                assert.sameMembers(
                    TestController.getProvidersForBlocks([ 'page' ]),
                    [
                        'page-data',
                        'spinner-action1-data', 'spinner-action2-data', 'spinner-inner-data',
                        'form-data', 'paranja-data'
                    ]);

                assert.sameMembers(
                    TestController.getProvidersForBlocks([ 'page' ], 'build'),
                    [ 'page-data', 'form-data' ]);

                assert.sameMembers(
                    TestController.getProvidersForBlocks([ 'page' ], 'action1'),
                    [ 'page-data', 'form-data', 'spinner-action1-data', 'spinner-inner-data', 'paranja-data' ]);

                assert.sameMembers(
                    TestController.getProvidersForBlocks([ 'page' ], 'action2'),
                    [ 'page-data', 'form-data', 'spinner-action2-data' ]);

                assert.sameMembers(
                    TestController.getProvidersForBlocks([ 'spinner', 'paranja' ], 'action1'),
                    [ 'spinner-action1-data', 'spinner-inner-data', 'paranja-data' ]);
            });

        });

        describe('#getAllowedBlocks()', function() {
            it('should return all blocks allowed to request from controller', function() {
                var tc = new TestController({ req: {} });

                disableDebugOutput(tc);

                assert.sameMembers(tc.getAllowedBlocks(), ALL_BLOCKS);
            });

            it('should reduce passed array of blocks to some are present in blocks tree of controller', function() {
                var tc = new TestController({ req: {} });

                disableDebugOutput(tc);

                assert.sameMembers(tc.getAllowedBlocks(MULTIPLE_BLOCKS), MULTIPLE_BLOCKS);
                assert.sameMembers(tc.getAllowedBlocks([ SINGLE_NOT_EXISTS ].concat(MULTIPLE_BLOCKS)), MULTIPLE_BLOCKS);
                assert.sameMembers(tc.getAllowedBlocks(SINGLE_BLOCK), [ SINGLE_BLOCK ]);
                assert.sameMembers(tc.getAllowedBlocks(SINGLE_NOT_EXISTS), []);
            });
        });
    });

    describe('#getData()', function() {
        var BLOCKS = {
                block: 'page',
                content: [
                    { block: 'header', data: [ 'user', 'geo-info' ] },
                    { block: 'content', data: 'page-content' },
                    { block: 'footer', data: [ 'geo-info', 'copyright' ] }
                ]
            },
            PROVIDERS = {
                user: {
                    property: 'user',
                    response: function() {
                        return { name: 'John Doe' };
                    }
                },
                'geo-info': {
                    property: 'geoInfo',
                    response: function() {
                        return { region: 'russia', city: 'moscow' };
                    }
                },
                'page-content': {
                    property: 'pageContent',
                    deps: [ 'user', 'geo-info' ],
                    response: function(data) {
                        return 'Hello, ' + data.user.name + '! How are the ' + data.geoInfo.city + '?';
                    }
                },
                copyright: {
                    property: 'copyright',
                    deps: 'geo-info',
                    response: function(data) {
                        return '2013 &copy; ' + (data.geoInfo.region === 'russia' ? 'ООО Яндекс' : 'Yandex LLC');
                    }
                }
            };

        beforeEach(function() {
            applyProvidersMixin();

            TestController.blocks = BLOCKS;

            Object.keys(PROVIDERS)
                .forEach(function(pName) {
                    var pDesc = PROVIDERS[pName];

                    function pFn(data) {
                        data['#' + pName] = true;
                        data[pDesc.property] = pDesc.response(data);
                    }

                    if (PROVIDERS[pName].deps) {
                        TestController.dataProviderDecl(pName, pDesc.deps, pFn);
                    } else {
                        TestController.dataProviderDecl(pName, pFn);
                    }
                });
        });

        it('should return promise for the chain of the providers for a requested block', function(done) {
            var tc = new TestController({ req: {} }),
                promise = tc.getData('page');

            disableDebugOutput(tc);

            assert.ok(vow.isPromise(promise), '#getData returns a vow promise');

            promise
                .then(function(data) {
                    var pNames = Object.keys(PROVIDERS);

                    assert.isObject(data);

                    pNames.forEach(function(pName) {
                        var pDesc = PROVIDERS[pName];

                        assert.property(data, '#' + pName,
                            'provider "' + pName + '" was called');
                        assert.deepEqual(data[pDesc.property], pDesc.response(data),
                            'provider "' + pName + '" response is correct');
                    });

                    done();
                })
                .done();
        });

        it('should return the promise the chain of the providers for a requested multiple blocks', function(done) {
            var tc = new TestController({ req: {} }),
                blocks = [ 'header', 'footer' ],
                promise = tc.getData('page');

            disableDebugOutput(tc);

            assert.ok(vow.isPromise(promise), '#getData returns a vow promise');

            promise
                .then(function(data) {
                    var pNames = TestController.getProvidersForBlocks(blocks);

                    assert.isObject(data);

                    pNames.forEach(function(pName) {
                        var pDesc = PROVIDERS[pName];

                        assert.property(data, '#' + pName,
                            'provider "' + pName + '" was called');
                        assert.deepEqual(data[pDesc.property], pDesc.response(data),
                            'provider "' + pName + '" response is correct');
                    });

                    done();
                })
                .done();
        });

        it('providers errors should fall-through to returned promise', function(done) {
            var errorMessage = 'First provider must fail',
                error = new Error(errorMessage),
                tc = new TestController({ req: {} });

            disableDebugOutput(tc);

            TestController.dataProviderDecl(Object.keys(PROVIDERS)[0], function() {
                return vow.reject(error);
            });

            tc.getData()
                .then(function() {
                    assert.ok(false, '#getData promise must fall');
                    done();
                })
                .fail(function(err) {
                    assert.strictEqual(err, error, 'error fall-though from provider to promise.fail handler');
                    done();
                })
                .done();
        });

        it('should reject promise with ProvidersError if one of required providers is not found', function(done) {
            var pName = Object.keys(PROVIDERS)[0],
                pDesc = PROVIDERS[pName],
                tc = new TestController({ req: {} });

            disableDebugOutput(tc);

            TestController.dataProviderDecl(pName, 'not-exists', function(data) {
                data['#' + pName] = true;
                data[pDesc.property] = pDesc.response(data);
            });

            tc.getData()
                .then(function() {
                    assert.ok(false, '#getData promise must fall');
                    done();
                })
                .fail(function(err) {
                    assert.ok(err instanceof ProvidersMixin.ProvidersError);
                    assert.strictEqual(err.code, ProvidersMixin.ProvidersError.CODES.UNRESOLVED_PROVIDERS_DEPENDENCY);
                    done();
                })
                .done();
        });
    });
});
