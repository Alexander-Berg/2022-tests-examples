/* global describe, it, Router, assert */

describe('postMatch option', function() {
    var Route = Router.Route;

    it('postMatch must be a function', function(done) {
        var route = Route({
            pattern : '/opa/<param>',
            postMatch : true
        });

        assert.deepEqual(route.match('/opa/value'), { param : 'value' });

        done();
    });

    it('/opa/<param> with postMatch function', function(done) {
        var route = Route({
            pattern : '/opa/<param>',
            postMatch : function(params) {
                params.foo = 'bar';

                if (params.param === 'value1') {
                    return null;
                }

                if (params.param === 'value2') {
                    return;
                }

                if (params.param === 'value3') {
                    return 1;
                }

                if (params.param === 'value4') {
                    return 'a';
                }

                return params;
            }
        });

        assert.deepEqual(route.match('/opa'), null);
        assert.deepEqual(route.match('/opa/value'), { param : 'value', foo : 'bar' });
        assert.deepEqual(route.match('/opa/value1'), null);
        assert.deepEqual(route.match('/opa/value2'), null);
        assert.deepEqual(route.match('/opa/value3'), null);
        assert.deepEqual(route.match('/opa/value4'), null);
        assert.deepEqual(route.match('/opa/value?foo=bar1&foo1=bar2'),
            { param : 'value', foo1 : 'bar2', foo : 'bar' });

        done();
    });

    it('should postMatch called for each pattern', function(done) {
        var route = Route({
            patterns : [
                {
                    pattern : '/opa/<param1>',
                    postMatch : function(params) {
                        params.param3 = 'value31';

                        return params;
                    }
                },
                {
                    pattern : '/opa',
                    postMatch : function(params) {
                        params.param3 = 'value32';

                        return params;
                    }
                },
            ],
            postMatch : function(params) {
                params.param2 = 'value2';

                return params;
            }
        });

        assert.deepEqual(route.match('/opa/value1'), { param1 : 'value1', param2 : 'value2', param3 : 'value31' });
        assert.deepEqual(route.match('/opa'), { param2 : 'value2', param3 : 'value32' });

        done();
    });
});
