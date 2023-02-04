var assert = require('chai').assert,
    util = require('../lib/util'),
    nodeUtil = require('util'),
    obj1 = {
        mark : 'Audi',
        model : 'A4',
        active : true
    },
    data = {
        compare : {
            selected : [
                {
                    approved : [
                    ]
                }
            ]
        }
    };

module.exports = {
    'check native util' : function() {
        [
            'format', 'debug', 'log',
            'inspect',
            'isArray', 'isRegExp', 'isDate', 'isError',
        ].forEach(function(method) {
                assert.strictEqual(typeof util[method], 'function', 'native method ' + method + ' is a function');
            });
        assert.strictEqual(util.isArray([]), nodeUtil.isArray([]), 'isArray is ok');
        assert.strictEqual(util.isDate(new Date()), nodeUtil.isDate(new Date()), 'isDate is ok');
    },
    'check isEmptyObject' : function() {
        assert.strictEqual(util.isEmptyObject(obj1), false, 'non empty object');
        assert.strictEqual(util.isEmptyObject({}), true, 'empty object');
    },
    'check extend' : function() {
        assert.deepEqual(
            util.extend(
                obj1,
                { mark : 'Opel', selected : true }
            ),
            { mark : 'Opel', model : 'A4', active : true, selected : true },
            'object is correctly extended'
        );
    },
    'check safeGet' : function() {
        assert.strictEqual(data.safeGet('compare.selected.length', 0), 1, 'selected has one element');
        assert.strictEqual(data.safeGet('compare.selected.0.approved.length', 'empty'),
            0, 'approved has no elements, but has length property');
        assert.strictEqual(data.safeGet('compare.trololo'), undefined, 'prop is absent, no default value');
        assert.strictEqual(data.safeGet('compare.trololo.ololo.0.lol', 'WOW'),
            'WOW', 'prop is absent, default value returned');
    },
    'check safeSet' : function() {
        var value  = { foo : 'bar' },
            localData = util.extend({}, data),
            C = function() {};

        // Simple case
        localData.safeSet('gaga.ooh.la.la', value);
        assert.strictEqual(localData.gaga.ooh.la.la, value);

        // Rewrite property if it is not object
        localData.safeSet('foo', 'foo');
        localData.safeSet('foo.bar', 'bar');
        assert.strictEqual(localData.foo.bar, 'bar');

        // Rewrite null property
        localData.safeSet('foo.bar', null);
        localData.safeSet('foo.bar.baz', 'baz');
        assert.strictEqual(localData.foo.bar.baz, 'baz');

        // Add data to existent object-like property
        localData.safeSet('foo.bar.quux', 'quux');
        assert.deepEqual(localData.foo.bar, { baz : 'baz', quux : 'quux' });

        // Add data to inherited property
        C.prototype.structure = { some : { complex : 'structure' } };
        localData = new C();
        localData.safeSet('structure.newProp', 'value');
        assert.deepEqual(localData.structure, { some : { complex : 'structure' }, newProp : 'value' });

        // But don't override prototype
        var localData2 = new C();
        assert.deepEqual(localData2.structure, { some : { complex : 'structure' } });
    },
    'check isDef' : function() {
        assert.strictEqual(data.isDef('compare.selected.length'), true, 'prop is in place');
        assert.strictEqual(data.isDef('compare.selected.0.approved'), true, 'though empty, array is in place');
        assert.strictEqual(data.isDef('compare.trololo'), false, 'no prop');
        assert.strictEqual(data.isDef('compare.trololo.ololo.0.lol'), false, 'no prop');
    },
    'check deepEqual' : function() {
        assert.strictEqual(util.deepEqual({ a : [ 2, 3 ], b : [ 4 ] }, { a : [ 2, 3 ], b : [ 4 ] }),
            true, 'deep equal objs');
        assert.strictEqual(util.deepEqual({ x : 5, y : [6] }, { x : 5, y : 6 }), false, 'not deep equal objs');
        assert.strictEqual(util.deepEqual([ null, null, null ], [ null, null, null ]), true, 'nested nulls');
        assert.strictEqual(util.deepEqual(data, data), true, 'check same objs');
        assert.strictEqual(util.deepEqual(data, {}), false, 'check different keys amount');
        assert.strictEqual(util.deepEqual(
            { util : { lol : 1, lol2 : { ololo : true } } },
            { util : { lol : 1, lol2 : { ololo : true } } }
        ), true, 'check equal nested objs');
        assert.strictEqual(util.deepEqual(
            { util : { lol : 1, lol2 : { ololo : true } } },
            { util : { lol : 1, lol2 : { ololo : true, trololo : false } } }
        ), false, 'check non-equal nested objs');
        assert.strictEqual(util.deepEqual(
            { util : { lol : 1, lol2 : true } },
            { util : { lol : 1, lol2 : 'true' } }
        ), false, 'check different types');
        assert.strictEqual(util.deepEqual(
            { util : { lol : 1, lol2 : true } },
            { util : { lol : 1, lol2 : false } }
        ), false, 'check different values of same type');
        assert.strictEqual(util.deepEqual(
            { lol : 1, lol2 : data },
            { lol : 1, lol2 : data }
        ), true, 'check same nested objs');
        assert.strictEqual(util.deepEqual(
            { lol : 1, lol2 : util.isArray },
            { lol : 1, lol2 : util.isArray }
        ), true, 'check same nested functions');
        assert.strictEqual(util.deepEqual(
            { lol : 1, lol2 : util.isArray },
            { lol : 1, lol2 : util.isEmptyObject }
        ), false, 'check different nested functions');
    }
};
