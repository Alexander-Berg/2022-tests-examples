/* global describe, it, Router, assert */

describe('preBuild option', function() {
    var Route = Router.Route;

    it('preBuild must be a function', function(done) {
        var route = Route({
            pattern : '/opa/<param>',
            preBuild : true
        });

        assert.deepEqual(route.build({ param : 'value' }), '/opa/value');

        done();
    });

    it('/opa/<param> with preBuild function', function(done) {
        var route = Route({
            pattern : '/opa/<param>',
            preBuild : function(params) {
                if (params) {
                    params.foo = 'bar';
                }

                if (params && params.param === 'value1') {
                    return {
                        param : 'value2'
                    };
                }

                return params;
            }
        });

        assert.deepEqual(route.build({ param : 'value' }), '/opa/value?foo=bar');
        assert.deepEqual(route.build({ param : 'value1' }), '/opa/value2');
        assert.deepEqual(route.build({ param : 'value', foo : 'bar1' }), '/opa/value?foo=bar');

        done();
    });

    it('should preBuild called for each pattern', function(done) {
        var route = Route({
            patterns : [
                {
                    pattern : '/opa/<param1>',
                    preBuild : function(params) {
                        params.param3 = 'value31';

                        return params;
                    }
                },
                {
                    pattern : '/opa',
                    preBuild : function(params) {
                        params.param3 = 'value32';

                        return params;
                    }
                },
            ],
            preBuild : function(params) {
                params.param2 = 'value2';

                return params;
            }
        });

        assert.deepEqual(route.build({ param1 : 'value' }), '/opa/value?param2=value2&param3=value31');
        assert.deepEqual(route.build({}), '/opa?param2=value2&param3=value32');

        done();
    });
});
