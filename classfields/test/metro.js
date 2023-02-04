/* global describe, it, BEM */

const assert = require('chai').assert;

const metroFactory = require('../lib/metro');
const metroI18N = require('../build/data/i18n/ru.json');

const CITY_WITH_METRO = { id: 213, ll: [ 55.755768, 37.617671 ] };
const CITY_WITHOUT_METRO = { id: 35, ll: [ 45.035407, 38.975277 ] };
const NOT_STATION_ID = -1;
const NOT_LINE_ID = -1;
const LINE = {
    alias: 'Синяя',
    color: '#16bdf0',
    id: '2_2',
    cityId: 2,
    isRing: false,
    name: 'Московско-Петроградская линия',
    stationsIds: [
        20341, 102531, 20319, 20320, 20321, 20322, 20323, 20336, 20335, 20347,
        20340, 20342, 20310, 20309, 20308, 20307, 20306, 20305
    ]
};
const STATION = {
    id: 20335,
    name: 'Горьковская',
    ll: [ 59.956156, 30.318859 ],
    isInRing: false,
    lines: [ LINE ],
    transitionsIds: [],
    ambientRingLinesIds: []
};
const LANG_RU = 'ru';
const CITY_ID_WITH_RING_LINES = 213;
const CITY_ID_WITHOUT_RING_LINES = 2;

const LANG_EN = 'en';
const LANG_UNKNOWN = 'xxx';
const I18N = (function() {
    const i18n = function(key) {
        return metroI18N[key] || '';
    };

    i18n.lang = function() {
        return 'ru';
    };

    return i18n;
})();

describe('metro.js', () => {
    it('should have a wrapper function', function() {
        assert.isFunction(metroFactory);
    });

    describe('I18N', () => {
        assert.strictEqual(metroFactory(I18N).getStationById(STATION.id).name, 'Горьковская');
        assert.strictEqual(metroFactory(LANG_UNKNOWN).getStationById(STATION.id).name, 'Горьковская');
        assert.strictEqual(metroFactory(LANG_RU).getStationById(STATION.id).name, 'Горьковская');
        assert.strictEqual(metroFactory(LANG_EN).getStationById(STATION.id).name, 'Gorkovskaya');
    });

    const metro = metroFactory(LANG_RU);

    it('#hasMetro', () => {
        assert.isFunction(metro.hasMetro);
        assert.isFalse(metro.hasMetro(CITY_WITHOUT_METRO.id));
        assert.isTrue(metro.hasMetro(CITY_WITH_METRO.id));
    });

    it('#getStationById', () => {
        assert.isFunction(metro.getStationById);
        assert.isNull(metro.getStationById(NOT_STATION_ID));
        assert.deepEqual(metro.getStationById(STATION.id), STATION);
    });

    it('#getLineById', () => {
        assert.isFunction(metro.getLineById);
        assert.isNull(metro.getLineById(NOT_LINE_ID));
        assert.deepEqual(metro.getLineById(LINE.id), LINE);
    });

    it('#getLinesIdsByCityId', () => {
        let result;

        assert.isFunction(metro.getLinesIdsByCityId);

        result = metro.getLinesIdsByCityId(CITY_WITHOUT_METRO.id);
        assert.isArray(result);
        assert.strictEqual(result.length, 0);

        result = metro.getLinesIdsByCityId(CITY_WITH_METRO.id);
        assert.isArray(result);
        assert.isAbove(result.length, 0);
    });

    it('#getStationsIdsByLineId', () => {
        let result;

        assert.isFunction(metro.getStationsIdsByLineId);

        result = metro.getStationsIdsByLineId(NOT_LINE_ID);
        assert.isArray(result);
        assert.strictEqual(result.length, 0);

        result = metro.getStationsIdsByLineId(LINE.id);
        assert.isArray(result);
        assert.isAbove(result.length, 0);
    });

    it('#getStationsIdsByCityId', () => {
        let result;

        assert.isFunction(metro.getStationsIdsByCityId);

        result = metro.getStationsIdsByCityId(CITY_WITHOUT_METRO.id);
        assert.isArray(result);
        assert.strictEqual(result.length, 0);

        result = metro.getStationsIdsByCityId(CITY_WITH_METRO.id);
        assert.isArray(result);
        assert.isAbove(result.length, 0);
    });

    it('#hasRingLine', () => {
        assert.isFunction(metro.hasRingLine);
        assert.isFalse(metro.hasRingLine(CITY_ID_WITHOUT_RING_LINES));
        assert.isTrue(metro.hasRingLine(CITY_ID_WITH_RING_LINES));
    });
});
