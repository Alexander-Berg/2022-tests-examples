/* global describe, it */

const fs = require('fs');
const path = require('path');
const assert = require('chai').assert;
const _ = require('lodash');
const xpath = require('xpath');
const Dom = require('xmldom').DOMParser;

const Metro = require('../');
const { AVAILABLE_LANGS } = require('../lib/i18n');
const ATTR_PREFIX = 'MetroMap_';

const maps = fs.readdirSync(path.resolve(__dirname, '../build/maps')).map(fileName => {
    const splited = fileName.split('.');

    return {
        fileName: fileName,
        data: fs.readFileSync(path.resolve(__dirname, '../build/maps', fileName), 'utf-8'),
        cityId: parseInt(splited[0], 10),
        lang: splited[1]
    };
});

describe('Maps and data should be consistent', () => {

    maps.forEach(map => {
        describe(`Map ${map.fileName}`, () => {
            const metro = Metro(map.lang);

            it(`Map file name ${map.fileName} contains correct lang`, () => {
                assert.isTrue(AVAILABLE_LANGS.includes(map.lang));
            });

            it(`Map file name ${map.fileName} contains correct cityId`, () => {
                assert.isTrue(metro.hasMetro(map.cityId));
            });

            describe(`Map ${map.fileName} should contain correct set of stations and lines`, () => {
                const data = map.data;
                const doc = new Dom().parseFromString(data);

                const stationsIdsShouldBeOnMap = metro.getStationsIdsByCityId(map.cityId);
                const linesIdsShouldBeOnMap = metro.getLinesIdsByCityId(map.cityId);

                const stationsLabelsNodes = xpath.select(`//*[@id='${ATTR_PREFIX}stations']` +
                    `//*[contains(@id, '${ATTR_PREFIX}station_')]`, doc);
                const stationsStopsNodes = xpath.select(`//*[@id='${ATTR_PREFIX}stops']` +
                    `//*[contains(@class, '${ATTR_PREFIX}to_')]`, doc);
                const linesNodes = xpath.select(`//*[@id='${ATTR_PREFIX}lines']` +
                    `//*[contains(@id, '${ATTR_PREFIX}line_')]`, doc);

                const stationsIdsOnLabels = stationsLabelsNodes.map(
                    node => parseInt(node.getAttribute('id').replace(`${ATTR_PREFIX}station_`, ''), 10));
                const stationsIdsOnStops = stationsStopsNodes.map(
                    node => parseInt(node.getAttribute('class').match(/.*to_(\d+)/)[1], 10));

                const linesIdsOnMap = linesNodes.map(node => node.getAttribute('id')
                    .replace(`${ATTR_PREFIX}line_`, ''));

                stationsIdsShouldBeOnMap.forEach(stationId => {
                    it(`Map should contain station label with id=${stationId}`, () => {
                        assert.isTrue(stationsIdsOnLabels.includes(stationId));
                    });
                    it(`Map should contain station stop with id=${stationId}`, () => {
                        assert.isTrue(stationsIdsOnStops.includes(stationId));
                    });
                });

                it(`Map should contain only unique stations ids`, () => {
                    assert.strictEqual(
                        [...stationsIdsShouldBeOnMap].sort().join(','),
                        [...stationsIdsOnLabels].sort().join(',')
                    );
                    assert.strictEqual(
                        [...stationsIdsShouldBeOnMap].sort().join(','),
                        [...stationsIdsOnStops].sort().join(',')
                    );
                });

                linesIdsShouldBeOnMap.forEach(lineId => {
                    it(`Map should contain line with id=${lineId}`, () => {
                        assert.isTrue(linesIdsOnMap.includes(lineId));
                    });
                });

                it(`Map should contain only unique lines ids`, () => {
                    assert.strictEqual(
                        [...linesIdsShouldBeOnMap].sort().join(','),
                        [...linesIdsOnMap].sort().join(',')
                    );
                });

                linesIdsShouldBeOnMap.forEach(lineId => {
                    const expectedColor = metro.getLineById(lineId).color.toLowerCase();

                    it(`Map should contain right color ${expectedColor} for line with id=${lineId}`, () => {
                        const node = xpath.select1(`//*[@id='${ATTR_PREFIX}line_${lineId}']` +
                            `/*[@class='${ATTR_PREFIX}line']/*[last()]/@stroke`, doc);

                        assert.isObject(node);
                        assert.strictEqual(expectedColor, node.value.toLowerCase());
                    });
                });

            });
        });
    });

    // describe(`All cities should have a map`, () => {
    //     Metro(DEFAULT_LANG).getCitiesIds().forEach(cityId => {
    //         AVAILABLE_LANGS.forEach(lang => {
    //             const fileName = `${cityId}.${lang}.svg`;
    //
    //             it(`Should be map ${fileName}`, () => {
    //                 assert.isTrue(maps.some(map => map.fileName === fileName));
    //             });
    //         });
    //     });
    // });

});
