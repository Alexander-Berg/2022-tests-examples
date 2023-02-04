import * as libxml from 'libxmljs';
import {expect} from 'chai';
import * as extend from 'extend';
import {
    toYmapsml,
    SourceMap,
    SourceObject,
    ConvertOptions
} from 'src/v2/converters/ymapsml';
import 'tests/chai-extensions';
import {validateYmapsmlSchema} from 'tests/xml-schema';

const NS_URIS = {
    ymaps: 'http://maps.yandex.ru/ymaps/1.x',
    gml: 'http://www.opengis.net/gml',
    repr: 'http://maps.yandex.ru/representation/1.x'
};

function convert(map: SourceMap, objects: SourceObject[], options?: ConvertOptions) {
    const xmlString = toYmapsml(map, objects, {formatOutput: true, ...options});
    validateYmapsmlSchema(xmlString);
    return libxml.parseXmlString(xmlString);
}

describe('convert map to YMapsML', () => {
    function createMap(values?: any): SourceMap {
        const map = {
            properties: {
                name: 'map name',
                description: 'map description'
            },
            state: {
                type: 'yandex#map',
                center: [37.64, 55.76],
                zoom: 10,
                size: [640, 480]
            }
        };
        return extend(true, map, values);
    }

    describe('map name and description', () => {
        function getName(doc: libxml.Document) {
            return doc.get('/ymaps:ymaps/ymaps:GeoObjectCollection/gml:name', NS_URIS)!.text();
        }

        function getDescription(doc: libxml.Document) {
            return doc.get('/ymaps:ymaps/ymaps:GeoObjectCollection/gml:description', NS_URIS)!.text();
        }

        it('should convert map name and description', () => {
            const doc = convert(createMap(), []);

            expect(getName(doc)).to.equal('map name');
            expect(getDescription(doc)).to.equal('map description');
        });

        it('should sanitize map name and description if "sanitize" option is true', () => {
            const map = createMap({
                properties: {
                    name: 'name<script>alert("hack")</script>',
                    description: 'description<script>alert("hack")></script>'
                }
            });
            const doc = convert(map, [], {sanitize: true});

            expect(getName(doc)).to.equal('name');
            expect(getDescription(doc)).to.equal('description');
        });

        describe('when "mergeNameWithDescription" option is true', () => {
            it('should merge name with description ', () => {
                const map = createMap({
                    properties: {
                        name: 'map name',
                        description: 'map description'
                    }
                });
                const doc = convert(map, [], {mergeNameWithDescription: true});

                expect(getName(doc)).to.equal('');
                expect(getDescription(doc)).to.equal('map name\nmap description');
            });

            it('should correct merge empty name', () => {
                const map = createMap({
                    properties: {
                        name: '',
                        description: 'map description'
                    }
                });
                const doc = convert(map, [], {mergeNameWithDescription: true});

                expect(getName(doc)).to.equal('');
                expect(getDescription(doc)).to.equal('map description');
            });

            it('should correct merge empty description', () => {
                const map = createMap({
                    properties: {
                        name: 'map name',
                        description: ''
                    }
                });
                const doc = convert(map, [], {mergeNameWithDescription: true});

                expect(getName(doc)).to.equal('');
                expect(getDescription(doc)).to.equal('map name');
            });

            it('should correct merge empty name with empty description', () => {
                const map = createMap({
                    properties: {
                        name: '',
                        description: ''
                    }
                });
                const doc = convert(map, [], {mergeNameWithDescription: true});

                expect(getName(doc)).to.equal('');
                expect(getDescription(doc)).to.equal('');
            });
        });
    });

    describe('map type', () => {
        const types: {[index: string]: string} = {
            'yandex#map': 'MAP',
            'yandex#hybrid': 'HYBRID',
            'yandex#satellite': 'SATELLITE',
            'yandex#uknown': 'MAP',
            mytype: 'MAP',
            '': 'MAP'
        };

        Object.keys(types).forEach((sourceType) => {
            const destType = types[sourceType];

            it(`should convert "${sourceType}" to "${destType}"`, () => {
                const map = createMap({
                    state: {
                        type: sourceType
                    }
                });
                const doc = convert(map, []);

                expect(doc.get('/ymaps:ymaps/repr:Representation/repr:View/repr:mapType', NS_URIS)!.text())
                    .to.equal(destType);
            });
        });
    });

    describe('map bounds', () => {
        function getBoundedBy(doc: libxml.Document) {
            return doc.get('/ymaps:ymaps/repr:Representation/repr:View/gml:boundedBy', NS_URIS);
        }

        it('should compute map bounds from center, zoom and size', () => {
            const map = createMap({
                state: {
                    center: [37.64, 55.76],
                    zoom: 10,
                    size: [800, 500]
                }
            });
            const doc = convert(map, []);
            const boundedBy = getBoundedBy(doc);

            expect(boundedBy).to.exist;
            // For expected values see https://gist.github.com/ikokostya/4978d0636a3986869b5cb5e7b81b5647
            expect(boundedBy!.get('gml:Envelope/gml:lowerCorner', NS_URIS)!.text().split(' '))
                .to.roughlyEqualPoint([37.09068359374998, 55.56593064640367]);
            expect(boundedBy!.get('gml:Envelope/gml:upperCorner', NS_URIS)!.text().split(' '))
                .to.roughlyEqualPoint([38.18931640624999, 55.953104267103924]);
        });
    });

    describe('objects', () => {
        const featureMembersXPath = '/ymaps:ymaps/ymaps:GeoObjectCollection/gml:featureMembers';
        let map: SourceMap;
        let propertiesFixture: SourceObject['properties'];

        beforeEach(() => {
            map = createMap();
            propertiesFixture = {
                name: 'object name',
                description: 'object description'
            };
        });

        it('should convert map without objects', () => {
            const doc = convert(map, []);
            expect(doc.find(`${featureMembersXPath}/*`, NS_URIS)).to.have.length(0);
        });

        it('should convert Point geometry', () => {
            const point: SourceObject = {
                geometry: {
                    type: 'Point',
                    coordinates: [1, 2]
                },
                properties: propertiesFixture,
                options: {}
            };

            const doc = convert(map, [point]);
            expect(doc.get(`${featureMembersXPath}/ymaps:GeoObject/gml:Point/gml:pos`, NS_URIS)!.text())
                .to.equal('1 2');
        });

        it('should convert LineString geometry', () => {
            const line: SourceObject = {
                geometry: {
                    type: 'LineString',
                    coordinates: [[1, 2], [3, 4]]
                },
                properties: propertiesFixture,
                options: {}
            };

            const doc = convert(map, [line]);
            expect(doc.get(`${featureMembersXPath}/ymaps:GeoObject/gml:LineString/gml:posList`, NS_URIS)!.text())
                .to.equal('1 2 3 4');
        });

        it('should convert Polygon geometry', () => {
            const polygon: SourceObject = {
                geometry: {
                    type: 'Polygon',
                    coordinates: [
                        [[1, 2], [3, 4]],
                        [[5, 6], [7, 8]],
                        [[9, 10], [11, 12]]
                    ]
                },
                properties: propertiesFixture,
                options: {}
            };

            const doc = convert(map, [polygon]);
            const polygonElem = doc.get(`${featureMembersXPath}/ymaps:GeoObject/gml:Polygon`, NS_URIS);
            const exteriorRings = polygonElem!.find('gml:exterior/gml:LinearRing/gml:posList', NS_URIS);
            expect(exteriorRings).to.have.length(1);
            expect(exteriorRings[0].text()).to.equal('1 2 3 4');

            const interiorRings = polygonElem!.find('gml:interior/gml:LinearRing/gml:posList', NS_URIS);
            expect(interiorRings).to.have.length(2);
            expect(interiorRings[0].text()).to.equal('5 6 7 8');
            expect(interiorRings[1].text()).to.equal('9 10 11 12');
        });

        it('should ignore unknown geometry', () => {
            const objects: SourceObject[] = [
                {
                    geometry: {
                        type: 'Circle',
                        coordinates: [1, 2],
                        radius: 10
                    },
                    properties: propertiesFixture,
                    options: {}
                },
                {
                    geometry: {
                        type: 'Rectangle',
                        coordinates: [[1, 2], [3, 4]]
                    },
                    properties: propertiesFixture,
                    options: {}
                }
            ];

            const doc = convert(map, objects);
            expect(doc.find(`${featureMembersXPath}/*`, NS_URIS)).to.have.length(0);
        });

        describe('name and description', () => {
            function getName(doc: libxml.Document) {
                return doc.get(`${featureMembersXPath}/ymaps:GeoObject/gml:name`, NS_URIS)!.text();
            }

            function getDescription(doc: libxml.Document) {
                return doc.get(`${featureMembersXPath}/ymaps:GeoObject/gml:description`, NS_URIS)!.text();
            }

            it('should convert object name and description', () => {
                const object: SourceObject = {
                    geometry: {
                        type: 'Point',
                        coordinates: [1, 2]
                    },
                    properties: {
                        name: 'Object name',
                        description: 'Object description'
                    },
                    options: {}
                };

                const doc = convert(map, [object], {sanitize: true});
                expect(getName(doc)).to.equal('Object name');
                expect(getDescription(doc)).to.equal('Object description');
            });

            it('should sanitize object name and description if "sanitize" option is true', () => {
                const object: SourceObject = {
                    geometry: {
                        type: 'Point',
                        coordinates: [1, 2]
                    },
                    properties: {
                        name: 'name<script>alert("hack")</script>',
                        description: 'description<script>alert("hack")></script>'
                    },
                    options: {}
                };

                const doc = convert(map, [object], {sanitize: true});
                expect(getName(doc)).to.equal('name');
                expect(getDescription(doc)).to.equal('description');
            });

            it('should merge name with description when "mergeNameWithDescription" option is true', () => {
                const object: SourceObject = {
                    geometry: {
                        type: 'Point',
                        coordinates: [1, 2]
                    },
                    properties: {
                        name: 'Object name',
                        description: 'Object description'
                    },
                    options: {}
                };

                const doc = convert(map, [object], {mergeNameWithDescription: true});
                expect(getName(doc)).to.equal('');
                expect(getDescription(doc)).to.equal('Object name\nObject description');
            });
        });

        describe('object options', () => {
            it('should convert object options', () => {
                const object: SourceObject = {
                    geometry: {
                        type: 'Point',
                        coordinates: [1, 2]
                    },
                    properties: propertiesFixture,
                    options: {
                        iconContent: 5,
                        iconCaption: 'icon caption text',
                        zIndex: 3
                    }
                };

                const doc = convert(map, [object]);
                const metaData = doc.get(
                    `${featureMembersXPath}/ymaps:GeoObject/gml:metaDataProperty/ymaps:AnyMetaData`,
                    NS_URIS
                );
                expect(metaData!.get('ymaps:number', NS_URIS)!.text()).to.equal('5');
                expect(metaData!.get('ymaps:iconCaption', NS_URIS)!.text()).to.equal('icon caption text');
                expect(metaData!.get('ymaps:zIndex', NS_URIS)!.text()).to.equal('3');
            });

            it('should ignore non-number iconContent', () => {
                const object: SourceObject = {
                    geometry: {
                        type: 'Point',
                        coordinates: [1, 2]
                    },
                    properties: propertiesFixture,
                    options: {
                        iconContent: 'text icon content'
                    }
                };

                const doc = convert(map, [object]);
                const metaData = doc.get(
                    `${featureMembersXPath}/gml:metaDataProperty/ymaps:AnyMetaData`,
                    NS_URIS
                );
                expect(metaData).to.not.exist;
            });
        });

        describe('object styles', () => {
            function findStyles(doc: libxml.Document) {
                return doc.find(
                    '/ymaps:ymaps/repr:Representation/repr:Style[@gml:id != \'userobject\']',
                    NS_URIS
                );
            }

            function findGeoObjects(doc: libxml.Document) {
                return doc.find(`${featureMembersXPath}/ymaps:GeoObject`, NS_URIS);
            }

            it('should group equal styles of different objects', () => {
                const commonLineStyles = {
                    strokeWidth: 5,
                    strokeColor: 'ed4543e6'
                };
                const objects: SourceObject[] = [
                    {
                        geometry: {
                            type: 'LineString',
                            coordinates: [[1, 2], [3, 4]]
                        },
                        properties: propertiesFixture,
                        options: commonLineStyles
                    },
                    {
                        geometry: {
                            type: 'LineString',
                            coordinates: [[1, 2], [3, 4]]
                        },
                        properties: propertiesFixture,
                        options: {
                            // Same styles, but in different order.
                            strokeColor: commonLineStyles.strokeColor,
                            strokeWidth: commonLineStyles.strokeWidth,
                            // This option should be ignored.
                            fill: true
                        }
                    },
                    {
                        geometry: {
                            type: 'LineString',
                            coordinates: [[1, 2], [3, 4]]
                        },
                        properties: propertiesFixture,
                        options: {
                            strokeWidth: 1
                        }
                    }
                ];

                const doc = convert(map, objects);
                const styles = findStyles(doc);
                expect(styles).to.have.length(2);

                expect(styles[0].get('repr:lineStyle/repr:strokeColor', NS_URIS)!.text())
                    .to.equal(commonLineStyles.strokeColor);
                expect(styles[0].get('repr:lineStyle/repr:strokeWidth', NS_URIS)!.text())
                    .to.equal(String(commonLineStyles.strokeWidth));
                expect(styles[1].get('repr:lineStyle/repr:strokeColor', NS_URIS))
                    .to.not.exist;
                expect(styles[1].get('repr:lineStyle/repr:strokeWidth', NS_URIS)!.text())
                    .to.equal('1');

                const styleRefs = styles.map((style) => '#' + style.attr('id')!.value());
                const geoObjects = findGeoObjects(doc);
                expect(geoObjects[0].get('ymaps:style', NS_URIS)!.text()).to.equal(styleRefs[0]);
                expect(geoObjects[1].get('ymaps:style', NS_URIS)!.text()).to.equal(styleRefs[0]);
                expect(geoObjects[2].get('ymaps:style', NS_URIS)!.text()).to.equal(styleRefs[1]);
            });

            it('should use "preset" option of Point as style reference', () => {
                const point: SourceObject = {
                    geometry: {
                        type: 'Point',
                        coordinates: [1, 2]
                    },
                    properties: propertiesFixture,
                    options: {
                        preset: 'islands#blueIcon'
                    }
                };

                const doc = convert(map, [point]);
                const styles = findStyles(doc);
                expect(styles).to.have.length(0);

                const geoObjects = findGeoObjects(doc);
                expect(geoObjects[0].get('ymaps:style', NS_URIS)!.text()).to.equal('islands#blueIcon');
            });

            it('should not add style reference for Point without "preset" option', () => {
                const point: SourceObject = {
                    geometry: {
                        type: 'Point',
                        coordinates: [1, 2]
                    },
                    properties: propertiesFixture,
                    options: {
                        // This options will be ignored, because object is Point.
                        strokeColor: 'ed4543e6',
                        strokeWidth: 5,
                        fillColor: 'ed454399',
                        fill: true,
                        outline: true
                    }
                };

                const doc = convert(map, [point]);
                const styles = findStyles(doc);
                expect(styles).to.have.length(0);

                const geoObjects = findGeoObjects(doc);
                expect(geoObjects[0].get('ymaps:style', NS_URIS)).to.not.exist;
            });

            it('should not use "preset" option of LineString or Polygon as style reference', () => {
                const objects: SourceObject[] = [
                    {
                        geometry: {
                            type: 'LineString',
                            coordinates: [[1, 2], [3, 4]]
                        },
                        properties: propertiesFixture,
                        options: {
                            preset: 'default#line'
                        }
                    },
                    {
                        geometry: {
                            type: 'Polygon',
                            coordinates: [
                                [[1, 2], [3, 4]]
                            ]
                        },
                        properties: propertiesFixture,
                        options: {
                            preset: 'default#polygon'
                        }
                    }
                ];

                const doc = convert(map, objects);
                const styles = findStyles(doc);
                expect(styles).to.have.length(0);

                const geoObjects = findGeoObjects(doc);
                expect(geoObjects[0].get('ymaps:style', NS_URIS)).to.not.exist;
                expect(geoObjects[1].get('ymaps:style', NS_URIS)).to.not.exist;
            });

            it('should convert style of the Polygon', () => {
                const doc = convert(map, [
                    {
                        geometry: {
                            type: 'Polygon',
                            coordinates: [
                                [[1, 2], [3, 4]]
                            ]
                        },
                        properties: propertiesFixture,
                        options: {
                            strokeColor: 'ffffffff',
                            strokeWidth: 5,
                            fillColor: 'dddddddd',
                            fill: true,
                            outline: false
                        }
                    }
                ]);

                const styles = findStyles(doc);
                expect(styles).to.have.length(1);
                expect(styles[0].get('repr:polygonStyle/repr:strokeColor', NS_URIS)!.text())
                    .to.equal('ffffffff');
                expect(styles[0].get('repr:polygonStyle/repr:strokeWidth', NS_URIS)!.text())
                    .to.equal('5');
                expect(styles[0].get('repr:polygonStyle/repr:fillColor', NS_URIS)!.text())
                    .to.equal('dddddddd');
                expect(styles[0].get('repr:polygonStyle/repr:fill', NS_URIS)!.text())
                    .to.equal('1');
                expect(styles[0].get('repr:polygonStyle/repr:outline', NS_URIS)!.text())
                    .to.equal('0');
            });

            it('should skip nonexistent styles', () => {
                const doc = convert(map, [
                    {
                        geometry: {
                            type: 'Polygon',
                            coordinates: [
                                [[1, 2], [3, 4]]
                            ]
                        },
                        properties: propertiesFixture,
                        options: {
                            strokeWidth: 5,
                            fillColor: 'ffffffff'
                        }
                    }
                ]);

                const styles = findStyles(doc);
                expect(styles).to.have.length(1);
                expect(styles[0].get('repr:polygonStyle/repr:strokeColor', NS_URIS))
                    .to.not.exist;
                expect(styles[0].get('repr:polygonStyle/repr:strokeWidth', NS_URIS)!.text())
                    .to.equal('5');
                expect(styles[0].get('repr:polygonStyle/repr:fillColor', NS_URIS)!.text())
                    .to.equal('ffffffff');
                expect(styles[0].get('repr:polygonStyle/repr:fill', NS_URIS))
                    .to.not.exist;
                expect(styles[0].get('repr:polygonStyle/repr:outline', NS_URIS))
                    .to.not.exist;
            });
        });
    });
});
