/* global describe, it, beforeEach */

var path = require('path'),
    assert = require('chai')
        .use(require('chai-nodules-helpers'))
        .assert,
    vow = require('vow'),
    disableDebugOutput = require('./lib/disable_debug_output');

describe('Page', function() {
    var controllers = require('../lib'),
        BEMTemplateMixin = controllers.BEMTemplateMixin,
        Page = controllers.Page;

    it('should be an inheritor of Controller with mixed TemplateMixin and ProvidersMixin', function() {
        assert.instanceOf(new Page({ req: {} }), controllers.Controller);

        assert.isMixed(Page, controllers.TemplateMixin, 'TemplateMixin',
            [ 'name', 'prototype', 'super_', 'create', '__super', 'mixin' ],
            [ 'constructor' ]);

        assert.isMixed(Page, controllers.ProvidersMixin, 'ProvidersMixin',
            [ 'name', 'prototype', 'super_', 'create', '__super', 'mixin' ],
            [ 'constructor' ]);
    });

    describe('methods', function() {
        var VALID_PRIV = path.resolve(__dirname, 'mocks/_common.priv.js'),
            MyPage,
            page;

        beforeEach(function() {
            MyPage = Page.create()
                .mixin(BEMTemplateMixin, VALID_PRIV);

            MyPage.blocks = {
                block: 'b-paranja',
                data: [ 'content' ]
            };

            MyPage.dataProviderDecl('content', function(data) {
                if (this.mock) {
                    data.content = this.mock;
                } else {
                    throw (this.error || new Error('mock or error is not provided for MyPage test providers'));
                }
            });

            page = new MyPage({ req: {}, res: {} });
            disableDebugOutput(page);
        });

        describe('#getAndProcessData()', function() {
            it('should return data if providers chain was executed successfully', function(done) {
                var data = { message: 'Hello, World!' },
                    promise;

                page.mock = data;

                promise = page.getAndProcessData('b-paranja');

                assert.ok(vow.isPromise(promise));

                promise
                    .then(function(result) {
                        assert.strictEqual(result.content, data);
                    })
                    .done(done);
            });

            it('should handle `error.is404` property', function(done) {
                function NotFound() {
                    Error.apply(this, arguments);
                    this.is404 = true;
                }

                page.error = new NotFound('data');

                page.getAndProcessData('b-paranja')
                    .then(function(data) {
                        assert.deepEqual(data, { is404: true, pageType: '404' });
                    })
                    .done(done);
            });

            it('should handle `error.redirect` property', function(done) {
                var REDIRECT_URL = 'http://yandex.com';

                function Redirect() {
                    Error.apply(this, arguments);
                    this.redirect = REDIRECT_URL;
                    this.isRealRedirect = true;
                }

                page.error = new Redirect('data');

                page.getAndProcessData('b-paranja')
                    .then(function(data) {
                        assert.deepEqual(data, {
                                isRedirect: true,
                                isRealRedirect: true,
                                redirectTo: REDIRECT_URL
                            });
                    })
                    .done(done);
            });

            it('should return `data = { isServerDown: true }` if catch unexpected error', function(done) {
                page.error = new Error('ooops!');

                page.getAndProcessData('b-paranja')
                    .then(function(data) {
                        assert.deepEqual(data, {
                            isServerDown: true,
                            pageType: 'server-down'
                        });
                    })
                    .done(done);
            });

            it('should execute overridden onGetDataError method', function(done) {
                MyPage.prototype.onGetDataError = function() {
                    var data = { overridden: true };

                    return data;
                };

                page.error = new Error();

                page.getAndProcessData('b-paranja')
                    .then(function(data) {
                        assert.isTrue(data.overridden);
                    })
                    .done(done);
            });

            it('should evaluate `#getData` execution time', function(done) {
                var timerId = page.buildTimerId('getAndProcessData');

                page.mock = {};

                assert.notProperty(page.req.debug._timers, timerId);

                page.getAndProcessData('b-paranja')
                    .then(function() {
                        assert.isNumber(page.req.debug._timers[timerId]);
                    })
                    .done(done);
            });
        });

        describe('.action()', function() {
            it('should declare a method Page#action_<name>', function(done) {
                var TEST_ACTION_NAME = 'test',
                    TEST_ACTION_RESULT = { test: 'yo!' },
                    TEST_ACTION_FN_EXECUTED = false;

                function testActionFn() {
                    TEST_ACTION_FN_EXECUTED = true;

                    return TEST_ACTION_RESULT;
                }

                MyPage.action({
                    name: TEST_ACTION_NAME,
                    fn: testActionFn
                });

                assert.isFunction(MyPage.prototype['action_' + TEST_ACTION_NAME]);

                assert.ok( ! TEST_ACTION_FN_EXECUTED);
                page.callAction(TEST_ACTION_NAME)
                    .then(function(result) {
                        assert.ok(TEST_ACTION_FN_EXECUTED);
                        assert.strictEqual(result, TEST_ACTION_RESULT,
                            'produced method returns promise resolved with `action.fn` result');
                    })
                    .done(done);
            });

            it('produced method should call #getAndProcessData() with appropriate blocks', function(done) {
                var TEST_BLOCK_NAME = 'b-paranja',
                    TEST_ACTION_NAME = 'test',
                    inPromiseAssertions = 0;

                function testActionFn(data) { return data; }

                MyPage.action({
                    name: TEST_ACTION_NAME,
                    blocks: TEST_BLOCK_NAME,
                    fn: testActionFn
                });

                page.getAndProcessData = function(blocks) {
                    assert.strictEqual(blocks, TEST_BLOCK_NAME);
                    inPromiseAssertions++;

                    return this.constructor.prototype.getAndProcessData.apply(this, arguments);
                };

                page.callAction(TEST_ACTION_NAME)
                    .then(function() {
                        assert.strictEqual(inPromiseAssertions, 1);
                    })
                    .done(done);
            });

            it('produced method should call `action.fn` only if `getAndProcessData()` finished successfully',
                function(done) {
                    var TEST_ACTION_NAME = 'test',
                        TEST_ERR_MSG = 'oh, no!';

                    function testActionFn() {
                        assert.fail('`action.fn` must not be called!');
                    }

                    MyPage.action({
                        name: TEST_ACTION_NAME,
                        fn: testActionFn
                    });

                    page.getAndProcessData = function() {
                        return vow.reject(new Error(TEST_ERR_MSG));
                    };

                    page.callAction(TEST_ACTION_NAME)
                        .then(function() {
                            assert.fail('MyPage#callAction promise must be rejected');
                        })
                        .fail(function(err) {
                            assert.strictEqual(err.message, TEST_ERR_MSG);
                        })
                        .done(done);

                });

            it('produced method should render blocks listed in the `data.renderBlocks` if `render` = true',
                function(done) {
                    var TEST_ACTION_NAME = 'test',
                        TEST_BLOCK_IN = 'b-paranja',
                        TEST_ADD_BLOCK = 'i-debug',
                        TEST_BLOCKS_OUT = [ TEST_BLOCK_IN, TEST_ADD_BLOCK ];

                    MyPage.action({
                        name: TEST_ACTION_NAME,
                        blocks: TEST_BLOCK_IN,
                        render: true,
                        fn: function(data) {
                            assert.isArray(data.renderBlocks);
                            assert.strictEqual(data.renderBlocks.indexOf(TEST_BLOCK_IN), 0);
                            assert.strictEqual(data.renderBlocks.length, 1);

                            data.renderBlocks.push(TEST_ADD_BLOCK);

                            return data;
                        }
                    });

                    page.callAction(TEST_ACTION_NAME)
                        .then(function(html) {
                            TEST_BLOCKS_OUT.forEach(function(block) {
                                assert.include(html, block, 'produced HTML contains block "' + block + '"');
                            });
                        })
                        .done(done);
                });

            it('produced method should resolve with empty string if no `data.renderingBlocks` and `render` = true',
                function(done) {
                    var TEST_ACTION_NAME = 'test',
                        TEST_BLOCK_IN = 'b-paranja';

                    MyPage.action({
                        name: TEST_ACTION_NAME,
                        blocks: TEST_BLOCK_IN,
                        render: true,
                        fn: function(data) {
                            data.renderBlocks = null;

                            return data;
                        }
                    });

                    page.callAction(TEST_ACTION_NAME)
                        .then(function(html) {
                            assert.strictEqual(html, '');
                        })
                        .done(done);
                });

            it('produced method should support result of page.getHTML() as vow-promise',
                function(done) {
                    var TEST_ACTION_NAME = 'test',
                        TEST_BLOCK = 'testBlock',
                        TEST_PROMISE_HTML = 'promise result';

                    MyPage.action({
                        name: TEST_ACTION_NAME,
                        blocks: TEST_BLOCK,
                        render: true,
                        fn: function(data) { return data; }
                    });

                    page.getHTML = function() {
                        return vow.resolve(TEST_PROMISE_HTML);
                    };

                    page.callAction(TEST_ACTION_NAME)
                        .then(function(html) {
                            assert.strictEqual(html, TEST_PROMISE_HTML);
                        })
                        .done(done);
                });
        });
    });

});
