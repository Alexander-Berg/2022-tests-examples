/* global describe, it, beforeEach, afterEach */

var path = require('path'),
    assert = require('chai')
        .use(require('chai-nodules-helpers'))
        .assert,
    disableDebugOutput = require('./lib/disable_debug_output');

describe('templates', function() {
    var controllers = require('../lib'),
        Controller = controllers.Controller,
        TemplateMixin = controllers.TemplateMixin,
        TestController;

    beforeEach(function() {
        TestController = Controller.create(function TestController() {
            TestController.__super.apply(this, arguments);
        });
    });

    function applyTemplateMixin() {
        TestController.mixin(TemplateMixin);
    }

    describe('TemplateMixin', function() {
        it('should be mixed properly using `Controller.mixin()`', function() {
            assert.canBeMixed(TestController, TemplateMixin, 'TemplateMixin',
                [ 'name', 'prototype', 'super_', 'create', '__super', 'mixin' ],
                [ 'constructor' ]);
        });

        describe('methods', function() {
            var tc;

            beforeEach(function() {
                applyTemplateMixin();
                tc = new TestController({ req: {} });
                disableDebugOutput(tc);
            });

            describe('#getHTML()', function() {
                it('should throw an error because it is an abstract method', function() {
                    assert.throwTerror(function() {
                        tc.getHTML();
                    }, TemplateMixin.TemplateError, 'METHOD_NOT_IMPLEMENTED');
                });
            });
        });
    });

    describe('BEMTemplateMixin', function() {
        var BEMTemplateMixin = controllers.BEMTemplateMixin,
            VALID_PRIV = path.resolve(__dirname, 'mocks/_common.priv.js'),
            TEST_PRIV = path.resolve(__dirname, 'mocks/test.priv.js');

        beforeEach(applyTemplateMixin);

        afterEach(function() {
            // clear cached templates
            delete require.cache[require.resolve(VALID_PRIV)];
            delete require.cache[require.resolve(TEST_PRIV)];
        });

        describe('mixin', function() {
            it('should be mixed properly using `Controller.mixin()`', function() {
                assert.canBeMixed(TestController, BEMTemplateMixin, 'BEMTemplateMixin',
                    [ 'name', 'prototype', 'super_', 'create', '__super', 'mixin' ],
                    [ 'constructor' ],
                    [ VALID_PRIV ]);
            });

            it('should throw if priv.js file can not be required', function() {
                assert.throwTerror(function() {
                    TestController.mixin(BEMTemplateMixin, './no/file/here/priv.js');
                }, BEMTemplateMixin.BEMTemplateError, 'PRIV_EXECUTION_FAILED');
            });

            it('should throw if priv.js do not export "BEMHTML" function', function() {
                assert.throwTerror(function() {
                    TestController.mixin(BEMTemplateMixin, path.resolve(__dirname, 'mocks/no_bemhtml.priv.js'));
                }, BEMTemplateMixin.BEMTemplateError, 'BEMHTML_NOT_DEFINED');
            });

            it('should not throw if priv.js exports "BEMHTML" object with "apply" method', function() {
                assert.doesNotThrow(function() {
                    TestController.mixin(BEMTemplateMixin, path.resolve(__dirname, 'mocks/has_bemhtml_object.priv.js'));
                });
            });

            it('should throw if priv.js do not export "blocks" hash', function() {
                assert.throwTerror(function() {
                    TestController.mixin(BEMTemplateMixin, path.resolve(__dirname, 'mocks/no_blocks.priv.js'));
                }, BEMTemplateMixin.BEMTemplateError, 'BLOCKS_NOT_DEFINED');
            });

            it('should not throw if priv.js blocks contains properties which is not a blocks functions', function() {
                assert.doesNotThrow(function() {
                    TestController.mixin(BEMTemplateMixin, path.resolve(__dirname, 'mocks/not_block.priv.js'));
                });
            });

            it('should be properly mixed without template path passed to .mixin() method', function() {
                assert.doesNotThrow(function() {
                    TestController.mixin(BEMTemplateMixin);
                });

                assert.notProperty(TestController.prototype, '_templatePriv');
            });

            it('should not wrap blocks functions twice if setPriv called with the same priv.js template', function() {
                var blockName = 'b-paranja',
                    blockFn;

                TestController.mixin(BEMTemplateMixin, VALID_PRIV);

                blockFn = TestController.prototype._templatePriv.blocks[blockName];

                assert.doesNotThrow(function() {
                    TestController.setPriv(VALID_PRIV, false);
                });

                assert.strictEqual(TestController.prototype._templatePriv.blocks[blockName], blockFn,
                    'block function wrapped only once');
            });
        });

        describe('methods', function() {
            var BEMTemplateMixin = controllers.BEMTemplateMixin,
                BLOCK_NAME = 'b-paranja',
                BLOCK_BEMJSON = {
                    block: 'b-paranja',
                    js: true,
                    content: {
                        block: 'b-spin', mods: { size: '45', theme: 'grey-45' }
                    }
                },
                BLOCK_HTML =
                    '<div class="b-paranja i-bem" onclick="return {&quot;b-paranja&quot;:{}}">' +
                    '<div class="b-spin b-spin_size_45 b-spin_theme_grey-45 i-bem" ' +
                    'onclick="return {&quot;b-spin&quot;:{}}">' +
                    '<img class="b-icon b-spin__icon" ' +
                    'src="//yandex.st/lego/_/La6qi18Z8LwgnZdsAr1qy1GwCwo.gif" alt=""/>' +
                    '</div></div>',
                tc;

            beforeEach(function() {
                applyTemplateMixin();
                TestController.mixin(BEMTemplateMixin, VALID_PRIV);
                tc = new TestController({ req: {} });
                disableDebugOutput(tc);
            });

            describe('#prepareData()', function() {
                it('should return passed object copy with additional methods', function() {
                    var data = { x: { y: 1 } },
                        prepared = tc.prepareData(data),
                        noData = tc.prepareData({});

                    assert.strictEqual(data.x, prepared.x);
                    Object.keys(noData)
                        .forEach(function(prop) {
                            assert.strictEqual(noData[prop], prepared[prop], 'for property "' + prop + '"');
                        });
                });

                it('should return object if arguments is `null` or `undefined`', function() {
                    var expected = tc.prepareData({});

                    assert.deepEqual(tc.prepareData(null), expected, 'argument is `null`');
                    assert.deepEqual(tc.prepareData(), expected, 'arguments is `undefined`');
                });
            });

            describe('.setPriv()', function() {
                it('should override template initialized on mixing', function() {
                    var originalTemplate = TestController.prototype._templatePriv;

                    TestController.setPriv(TEST_PRIV);
                    assert.notStrictEqual(TestController.prototype._templatePriv, originalTemplate);
                });
            });

            describe('#setPriv()', function() {
                it('should override inherited class template', function() {
                    assert.strictEqual(tc._templatePriv, TestController.prototype._templatePriv);
                    tc.setPriv(TEST_PRIV);
                    assert.notStrictEqual(tc._templatePriv, TestController.prototype._templatePriv);
                });
            });

            describe('#getBEMJSON()', function() {
                it('should return `null` if block is not defined in the priv.js', function() {
                    assert.strictEqual(tc.getBEMJSON('b-super-block-which-never-exists'), null);
                });

                it('should return BEMJSON object for block', function() {
                    var bemjson = null;

                    assert.doesNotThrow(function() {
                        bemjson = tc.getBEMJSON(BLOCK_NAME);
                    });

                    assert.deepEqual(bemjson, BLOCK_BEMJSON);
                });

                it('should return empty string if block execution is failed and log an error', function() {
                    var Err = BEMTemplateMixin.BEMTemplateError,
                        logger = Err.prototype.logger,
                        errLinesCount = 0;

                    Err.setLogger(function() {
                        errLinesCount++;
                        assert.strictEqual(this.code, Err.CODES.BLOCK_EXECUTION_FAILED,
                            'block execution error should be logged');
                    });

                    tc.setPriv(TEST_PRIV);

                    assert.strictEqual(tc.getBEMJSON('i-broken'), '',
                        'block wrapper should return an empty string on block execution error if debug turned off');
                    assert.ok(errLinesCount > 0, 'block execution error should be logged');

                    Err.setLogger(logger);
                });

                it('should return error string if template wrapped with enabled debug', function() {
                    var Err = BEMTemplateMixin.BEMTemplateError,
                        logger = Err.prototype.logger,
                        errLinesCount = 0;

                    Err.setLogger(function() {
                        errLinesCount++;
                        assert.strictEqual(this.code, Err.CODES.BLOCK_EXECUTION_FAILED,
                            'block execution error should be logged');
                    });

                    tc.setPriv(TEST_PRIV, true);

                    assert.match(tc.getBEMJSON('i-broken'), /^blocks\["i-broken"\] .+$/ig,
                        'block wrapper should return error message on block execution error if debug turned on');
                    assert.ok(errLinesCount > 0, 'block execution error should be logged');

                    Err.setLogger(logger);
                });
            });

            describe('#getHTML()', function() {
                it('should return HTML string for block', function() {
                    assert.strictEqual(tc.getHTML(BLOCK_NAME), BLOCK_HTML);
                });

                it('should return empty string and log error if block is not defined in priv.js', function() {
                    assert.strictEqual(tc.getHTML('b-super-block-which-never-exists'), '');
                });

                it('should return empty string and log error if block execution failed', function() {
                    var Err = BEMTemplateMixin.BEMTemplateError,
                        logger = Err.prototype.logger,
                        errLinesCount = 0;

                    tc.prepareData = function() { return null; };

                    Err.setLogger(function() {
                        errLinesCount++;
                        assert.strictEqual(this.code, Err.CODES.BLOCK_EXECUTION_FAILED,
                            'block execution error should be logged');
                    });

                    assert.strictEqual(tc.getHTML('b-offers'), '');

                    assert.ok(errLinesCount > 0, 'block execution error should be logged');

                    Err.setLogger(logger);
                });

                it('should return empty string and log error if BEMHTML execution failed', function() {
                    var Err = BEMTemplateMixin.BEMTemplateError,
                        logger = Err.prototype.logger,
                        errLinesCount = 0;

                    tc.setPriv(TEST_PRIV);

                    Err.setLogger(function() {
                        errLinesCount++;
                        assert.strictEqual(this.code, Err.CODES.BEMHTML_APPLY_ERROR,
                            'BEMHTML execution error should be logged');
                    });

                    assert.strictEqual(tc.getHTML('i-broke-bemhtml'), '');

                    assert.ok(errLinesCount > 0, 'block execution error should be logged');

                    Err.setLogger(logger);
                });
            });
        });
    });
});
