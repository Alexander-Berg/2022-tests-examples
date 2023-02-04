/* global describe, it */

var assert = require('chai').assert,
    mockUser = require('./mock/user'),
    mockRegion = require('./mock/region'),
    User = require('../lib/user'),
    nock = require('nock'),
    SOURCES = require('../components/region').SOURCES,
    expected = require('./expect/region');

describe('Region component', function() {

    function createReq(mock) {
        return {
            cookies : mock.cookies,
            headers : {}
        };
    }

    function createRes() {
        return {
            setHeader : function() {},
            getHeader : function() {}
        };
    }

    function regionInit(user, region, regionParams, url) {
        user = mockUser[user];
        region = mockRegion[region];

        var req = createReq(user),
            res = createRes();

        if (user.passportData) {
            nock('http://blackbox-mimino.yandex.net')
                .filteringPath(function() {
                    return '/blackbox';
                })
                .get('/blackbox')
                .reply(200, JSON.parse(JSON.stringify(user.passportData)));
        }

        return new User(req, res, {
                clientIp : region.clientIp,
                url : url || region.url,
                l10n : {},
                region : regionParams || {}
            })
            .init('region');
    }

    describe('Fresh user, Yandex region via ip', function() {

        it('is rewritten to moscow', function(done) {
            regionInit('noob', 'yandex')
                .then(function(user) {
                    assert.strictEqual(user.region.id, 213);

                    done();
                })
                .done();
        });

        it('not rewritten', function(done) {
            regionInit('noob', 'yandex', { isRewriteYandexRegion : false })
                .then(function(user) {
                    assert.strictEqual(user.region.id, 9999);

                    done();
                })
                .done();
        });

    });

    describe('Fresh user, Moscow', function() {

        it('id[s] and name[s] are correct', function(done) {
            regionInit('noob', 'yandex')
                .then(function(user) {
                    var region = user.region;

                    assert.strictEqual(region.id, expected[213].id);
                    assert.deepEqual(region.ids, [expected[213].id]);

                    assert.strictEqual(region.name, expected[213].name);
                    assert.deepEqual(region.names, [expected[213].name]);

                    assert.strictEqual(region.source, SOURCES.DETECTED);

                    done();
                })
                .done();
        });

    });

    describe('region setters', function() {

        it('setId and then return for detected', function(done) {
            regionInit('noob', 'yandex')
                .then(function(user) {
                    var region = user.region,
                        prev = user.region.id;

                    region.setId(2);

                    assert.strictEqual(region.id, expected[2].id);
                    assert.deepEqual(region.ids, [expected[2].id]);
                    assert.strictEqual(region.name, expected[2].name);
                    assert.deepEqual(region.names, [expected[2].name]);
                    assert.strictEqual(region.source, SOURCES.EXTRA);
                    assert.strictEqual(region.portalId, expected[213].id);

                    region.setId(prev);

                    assert.strictEqual(region.id, expected[213].id);
                    assert.deepEqual(region.ids, [expected[213].id]);
                    assert.strictEqual(region.name, expected[213].name);
                    assert.deepEqual(region.names, [expected[213].name]);
                    assert.strictEqual(region.source, SOURCES.EXTRA);

                    done();
                })
                .done();
        });

        it('setIds also works', function(done) {
            regionInit('noob', 'yandex')
                .then(function(user) {
                    var region = user.region;

                    region.setIds([2, 213]);

                    assert.strictEqual(region.id, expected[2].id);
                    assert.deepEqual(region.ids, [expected[2].id, expected[213].id]);
                    assert.strictEqual(region.name, expected[2].name);
                    assert.deepEqual(region.names, [expected[2].name, expected[213].name]);
                    assert.strictEqual(region.source, SOURCES.EXTRA);
                    assert.strictEqual(region.portalId, expected[213].id);

                    done();
                })
                .done();
        });

    });

    describe('linguistics', function() {

        it('default linguistics are correct', function(done) {
            regionInit('noob', 'yandex')
                .then(function(user) {
                    assert.deepEqual(user.region.linguistics, expected[213].linguistics);

                    done();
                })
                .done();
        });

        it('linguisticsById result is correct', function(done) {
            regionInit('noob', 'yandex')
                .then(function(user) {
                    assert.deepEqual(user.region.linguisticsById(2), expected[2].linguistics);

                    done();
                })
                .done();
        });

    });

    describe('get region name by id', function() {

        it('nameById', function(done) {
            regionInit('noob', 'yandex')
                .then(function(user) {
                    assert.strictEqual(user.region.nameById(213), expected[213].linguistics.nominative);

                    done();
                })
                .done();
        });

        it('namesByIds', function(done) {
            regionInit('noob', 'yandex')
                .then(function(user) {
                    assert.deepEqual(user.region.namesByIds([2, 213]),
                        [expected[2].linguistics.nominative, expected[213].linguistics.nominative]);

                    done();
                })
                .done();
        });

    });

    describe('idIsIn', function() {

        it('Saint-Petersburg is in Russia', function(done) {
            regionInit('noob', 'yandex')
                .then(function(user) {
                    assert.isTrue(user.region.idIsIn(2, 225));

                    done();
                })
                .done();
        });

        it('Moscow is not in Turkey', function(done) {
            regionInit('noob', 'yandex')
                .then(function(user) {
                    assert.isFalse(user.region.idIsIn(2, 983));

                    done();
                })
                .done();
        });

    });

    describe('isInternalNetwork', function() {

       it('User from local network', function(done) {
            regionInit('noob', 'yandex')
                .then(function(user) {
                    assert.isTrue(user.region.isInternalNetwork);

                    done();
                })
                .done();
       });

       it('User from external network', function(done) {
            regionInit('noob', 'google')
                .then(function(user) {
                    assert.isFalse(user.region.isInternalNetwork);

                    done();
                })
                .done();
       });

    });

    describe('get region params from URL', function() {

        it('urlId, but no id in URL', function(done) {
            regionInit('noob', 'yandex')
                .then(function(user) {
                    assert.isNull(user.region.urlId);

                    done();
                })
                .done();
        });

        it('urlId', function(done) {
            regionInit('noob', 'yandex', null, 'http://auto.yandex.ru/?rid=2')
                .then(function(user) {
                    assert.strictEqual(user.region.urlId, 2);

                    done();
                })
                .done();
        });

        it('urlIds', function(done) {
            regionInit('noob', 'yandex', { regionParamName : 'lr' }, 'http://auto.yandex.ru/?lr=1&lr=213')
                .then(function(user) {
                    assert.deepEqual(user.region.urlIds, [ 1, 213 ]);

                    done();
                })
                .done();
        });

    });

    describe('linguistics cache', function() {

        it('check via coverage that cache if working', function(done) {
            regionInit('noob', 'yandex')
                .then(function(user) {
                    assert.strictEqual(user.region.name, expected[213].linguistics.nominative);
                    assert.deepEqual(user.region.linguisticsById(213), expected[213].linguistics);

                    done();
                })
                .done();
        });

    });

    describe('detecting region ids from url', function() {

        it('ids from url are detected and have corresponding priority', function(done) {

            regionInit('noob', 'yandex', null, 'http://auto.yandex.ru/?rid=2&rid=1')
                .then(function(user) {
                    var region = user.region;

                    assert.strictEqual(region.id, 2);
                    assert.deepEqual(region.ids, [2, 1]);

                    assert.strictEqual(region.source, SOURCES.URL);

                    assert.strictEqual(region.urlId, 2);
                    assert.deepEqual(region.urlIds, [2, 1]);

                    assert.strictEqual(region.portalId, 213);

                    // установленные вручную получили больший приоритет
                    region.setId(213);

                    assert.strictEqual(region.id, 213);
                    assert.strictEqual(region.source, SOURCES.EXTRA);

                    done();
                })
                .done();

        });

        it('same for the single region id', function(done) {

            regionInit('noob', 'yandex', null, 'http://auto.yandex.ru/?rid=1')
                .then(function(user) {
                    var region = user.region;

                    assert.strictEqual(region.id, 1);
                    assert.deepEqual(region.ids, [1]);

                    assert.strictEqual(region.source, SOURCES.URL);

                    assert.strictEqual(region.urlId, 1);
                    assert.deepEqual(region.urlIds, [1]);

                    assert.strictEqual(region.portalId, 213);

                    // установленные вручную получили больший приоритет
                    region.setId(213);

                    assert.strictEqual(region.id, 213);
                    assert.strictEqual(region.source, SOURCES.EXTRA);

                    done();
                })
                .done();

        });
    });

    describe('wrong region ids', function() {

        it('should be rejected', function(done) {

            regionInit('noob', 'yandex', null, 'http://auto.yandex.ru/?rid=2000001')
                .then(function(user) {
                    var region = user.region;

                    assert.notEqual(region.id, 2000001);
                    assert.notDeepEqual(region.ids, [2000001]);

                    assert.notStrictEqual(region.source, SOURCES.URL);
                    assert.strictEqual(region.idBySource(SOURCES.URL), null);
                    assert.deepEqual(region.idsBySource(SOURCES.URL), null);

                    assert.notStrictEqual(region.urlId, 2000001);
                    assert.notDeepEqual(region.urlIds, [2000001]);

                    assert.equal(region.id, 213);
                    assert.deepEqual(region.ids, [213]);

                    done();
                })
                .done();
        });

        it('should be rejected exclusively', function(done) {

            regionInit('noob', 'yandex', null, 'http://auto.yandex.ru/?rid=2000001&rid=20001&rid=2')
                .then(function(user) {
                    var region = user.region;

                    assert.equal(region.id, 2);
                    assert.deepEqual(region.ids, [2]);

                    assert.strictEqual(region.source, SOURCES.URL);
                    assert.strictEqual(region.idBySource(SOURCES.URL), 2);
                    assert.deepEqual(region.idsBySource(SOURCES.URL), [2]);

                    assert.strictEqual(region.urlId, 2);
                    assert.deepEqual(region.urlIds, [2]);

                    assert.equal(region.id, 2);
                    assert.deepEqual(region.ids, [2]);

                    done();
                })
                .done();
        });
    });

    describe('all stuff about sources', function() {
        var instance = regionInit('noob', 'yandex', null, 'http://auto.yandex.ru/?rid=2&rid=1');

        it('SOURCES are exported and correct', function(done) {
            instance
                .then(function(user) {
                    assert.deepEqual(user.region.SOURCES, SOURCES);

                    done();
                })
                .done();
        });

        it('source getter', function(done) {
            instance
                .then(function(user) {
                    assert.deepEqual(user.region.source, SOURCES.URL);

                    done();
                })
                .done();
        });

        it('idBySource', function(done) {
            instance
                .then(function(user) {
                    var region = user.region;

                    region.setId(100);
                    region.setId(1, region.SOURCES.SETTINGS);

                    assert.strictEqual(region.id, 100);
                    assert.strictEqual(region.idBySource(SOURCES.DEFAULT), 213);
                    assert.strictEqual(region.idBySource(SOURCES.DETECTED), 213);
                    assert.strictEqual(region.idBySource(SOURCES.SETTINGS), 1);
                    assert.strictEqual(region.idBySource(SOURCES.URL), 2);
                    assert.strictEqual(region.idBySource(SOURCES.EXTRA), 100);

                    done();
                })
                .done();
        });

        it('idsBySource', function(done) {
            instance
                .then(function(user) {
                    var region = user.region;

                    region.setIds([100, 200]);
                    region.setIds([200, 100], region.SOURCES.SETTINGS);

                    assert.deepEqual(region.ids, [100, 200]);
                    assert.deepEqual(region.idsBySource(SOURCES.DEFAULT), [213]);
                    assert.deepEqual(region.idsBySource(SOURCES.DETECTED), [213]);
                    assert.deepEqual(region.idsBySource(SOURCES.SETTINGS), [200, 100]);
                    assert.deepEqual(region.idsBySource(SOURCES.URL), [2, 1]);
                    assert.deepEqual(region.idsBySource(SOURCES.EXTRA), [100, 200]);

                    done();
                })
                .done();
        });

    });

    describe('check country data is correct', function() {
        var instance = regionInit('noob', 'yandex');

        it('country id of current region', function(done) {
            instance
                .then(function(user) {
                    var region = user.region;

                    // Москва
                    assert.strictEqual(region.country, 225);

                    // Стамбул
                    region.setId(11508);

                    assert.strictEqual(region.country, 983);

                    // Киев
                    region.setId(143);

                    assert.strictEqual(region.country, 187);

                    done();
                })
                .done();
        });
    });

    describe('check info is correct', function() {
        var instance = regionInit('noob', 'yandex'),
            compareStandardFields = function(obj) {
                // тут мы проверяем только те поля, к которым есть эталон
                // многие поля меняются слишком часто, их мы заигнорируем
                // delete не работает, потому что геобаза возвращает немодифицируемый объект
                var standard = expected[2].data,
                    ret = {};

                for (var prop in standard) {
                    if (obj.data.hasOwnProperty(prop)) {
                        ret[prop] = obj.data[prop];
                    }
                }

                obj.data = ret;

                return obj;
            };

        it('by current region', function(done) {
            instance
                .then(function(user) {
                    var region = user.region;

                    assert.deepEqual(compareStandardFields(region.info), expected[213]);

                    region.setId(2);

                    assert.deepEqual(compareStandardFields(region.info), expected[2]);

                    done();
                })
                .done();
        });

        it('by infoById', function(done) {
            instance
                .then(function(user) {
                    assert.deepEqual(compareStandardFields(user.region.infoById(2)), expected[2]);

                    done();
                })
                .done();
        });
    });

    describe('check countryById for different types of regions', function() {
        var instance = regionInit('noob', 'yandex');

        it('by city', function(done) {
            instance
                .then(function(user) {
                    assert.strictEqual(user.region.infoById(2).country, 225);

                    done();
                })
                .done();
        });

        it('by country', function(done) {
            instance
                .then(function(user) {
                    assert.strictEqual(user.region.infoById(225).country, 225);

                    done();
                })
                .done();
        });

        it('by unknown city or country', function(done) {
            instance
                .then(function(user) {
                    assert.strictEqual(user.region.infoById(10001).country, 0);

                    done();
                })
                .done();
        });
    });

    describe('timezone', function() {

        function assertTimezone(data, expectedData) {
            // dst меняется в зависимости от времени года (зимнее/летнее время).
            // Достаточно проверить, что там Boolean
            assert.isBoolean(data.dst, '"dst" field is incorrect');

            var keys = Object.keys(data),
                dstIdx = keys.indexOf('dst');

            keys.splice(dstIdx, 1);

            keys.forEach(function(key) {
                assert.deepEqual(data[key], expectedData[key]);
            });
        }

        it('default timezone is correct', function(done) {
            regionInit('noob', 'yandex')
                .then(function(user) {
                    assertTimezone(user.region.timezone, expected.timezone[213]);
                    done();
                })
                .done();
        });

        it('timezone result is correct', function(done) {
            regionInit('noob', 'yandex')
                .then(function(user) {
                    assertTimezone(user.region.timezoneById(51), expected.timezone[51]);
                    done();
                })
                .done();
        });

    });

});
