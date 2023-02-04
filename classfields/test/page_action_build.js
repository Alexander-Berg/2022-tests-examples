/* global describe, it, beforeEach */

var path = require('path'),
    assert = require('chai')
        .use(require('chai-nodules-helpers'))
        .assert,
    disableDebugOutput = require('./lib/disable_debug_output');

describe('PageActionBuild', function() {
    var controllers = require('../lib'),
        BEMTemplateMixin = controllers.BEMTemplateMixin,
        Page = controllers.Page,
        PageActionBuild = controllers.PageActionBuild;

    describe('mixin', function() {
        it('should dynamically mix the #action_build() to the inheritors of the Page', function() {
            var MyPage = Page.create()
                .mixin(PageActionBuild);

            assert.isFunction(MyPage.prototype.action_build);
        });

        it('should not mix anything to ctors, which is not inheritors of the Page', function() {
            var MyController = controllers.Controller.create()
                .mixin(PageActionBuild);

            assert.notProperty(MyController.prototype, 'action_build');
        });

        it('should not override existing #action_build()', function() {
            var MyPage = Page.create();

            MyPage.action({
                name: 'build',
                fn: function(data) { return data; }
            });

            var expected = MyPage.prototype.action_build;

            MyPage.mixin(PageActionBuild);

            assert.strictEqual(MyPage.prototype.action_build, expected);
        });
    });

    describe('#action_build()', function() {
        var MyPage,
            TEST_PRIV = path.resolve(__dirname, 'mocks/test.priv.js');

        beforeEach(function() {
            MyPage = Page.create()
                .mixin(BEMTemplateMixin, TEST_PRIV);
        });

        /**
         * @param {String} [defaultBlock]
         * @returns {MyPage}
         */
        function createPage(defaultBlock, req, debug) {
            var page;

            MyPage.mixin(PageActionBuild, defaultBlock, debug);
            page = new MyPage({
                req: req || {},
                res: {
                    statusCode: 200,
                    headers: {},
                    setHeader: function(header, value) {
                        this.headers[header.toLowerCase()] = value;
                    },
                    end: function() {}
                }
            });
            disableDebugOutput(page);

            return page;
        }

        it('should render "b-page" block if another is not defined as mixing argument', function(done) {
            createPage()
                .callAction('build')
                .then(function(html) {
                    assert.include(html.split(','), 'b-page');
                })
                .done(done);
        });

        it('should render block defined as mixing argument', function(done) {
            createPage('b-promo-page')
                .callAction('build')
                .then(function(html) {
                    assert.include(html.split(','), 'b-promo-page');
                })
                .done(done);
        });

        it('should render additional "i-debug" block if debug enabled', function(done) {
            var page = createPage('b-page', null, true);

            page
                .callAction('build')
                .then(function(html) {
                    assert.includeMembers(html.split(','), [ 'b-page', 'i-debug' ]);
                })
                .done(done);
        });

        it('should redirect if `data.isRedirect` flag was set', function(done) {
            var LOCATION = 'http://yandex.ru',
                page = createPage();

            MyPage.blocks = {
                block: 'b-page',
                data: [ 'redirect' ]
            };

            MyPage.dataProviderDecl('redirect', function(data) {
                data.isRedirect = true;
                data.redirectTo = LOCATION;
            });

            page
                .callAction('build')
                .then(function() {
                    assert.strictEqual(page.res.statusCode, 302, 'status code was set to 302');
                    assert.strictEqual(page.res.headers.location, LOCATION, '"location" header was set correctly');
                })
                .done(done);
        });

        it('should set response status code to 404 if `data.is404` flag was set', function(done) {
            var page = createPage();

            MyPage.blocks = {
                block: 'b-page',
                data: [ 'not-found' ]
            };

            MyPage.dataProviderDecl('not-found', function(data) {
                data.is404 = true;
            });

            page
                .callAction('build')
                .then(function() {
                    assert.strictEqual(page.res.statusCode, 404, 'status code was set to 404');
                })
                .done(done);
        });

        it('should set response status code to 500 if `data.isServerDown` flag was set', function(done) {
            var page = createPage();

            MyPage.blocks = {
                block: 'b-page',
                data: [ 'fail' ]
            };

            MyPage.dataProviderDecl('fail', function(data) {
                data.isServerDown = true;
            });

            page
                .callAction('build')
                .then(function() {
                    assert.strictEqual(page.res.statusCode, 500, 'status code was set to 500');
                })
                .done(done);
        });
    });
});
