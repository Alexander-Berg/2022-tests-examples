import * as libxml from 'libxmljs';
import {expect} from 'chai';
import {
    toKml,
    SourceMap,
    SourceObject,
    ConvertOptions
} from 'src/v2/converters/kml';
import {validateKmlSchema} from 'tests/xml-schema';

const NS_URIS = {
    kml: 'http://www.opengis.net/kml/2.2'
};

function convert(map: SourceMap, objects: SourceObject[], options?: ConvertOptions) {
    const xmlString = toKml(map, objects, {formatOutput: true, ...options});
    validateKmlSchema(xmlString);
    return libxml.parseXmlString(xmlString);
}

describe('convert map to KML', () => {
    describe('map name and description', () => {
        function getName(doc: libxml.Document) {
            return doc.get('/kml:kml/kml:Document/kml:Folder/kml:name', NS_URIS)!.text();
        }

        function getDescription(doc: libxml.Document) {
            return doc.get('/kml:kml/kml:Document/kml:Folder/kml:description', NS_URIS)!.text();
        }

        it('should convert map name and description', () => {
            const map = {
                properties: {
                    name: 'Map name',
                    description: 'Map description'
                }
            };

            const doc = convert(map, []);
            expect(getName(doc)).to.equal('Map name');
            expect(getDescription(doc)).to.equal('Map description');
        });

        it('should sanitize map name and description if "sanitize" option is true', () => {
            const map = {
                properties: {
                    name: 'Map <script>alert("hack")</script>name',
                    description: 'Map <script>alert("hack")></script>description'
                }
            };

            const doc = convert(map, [], {sanitize: true});
            expect(getName(doc)).to.equal('Map name');
            expect(getDescription(doc)).to.equal('Map description');
        });
    });

    describe('objects', () => {
        const placemarksXPath = '//kml:Folder/kml:Placemark';
        let map: SourceMap;
        let propertiesFixture: SourceObject['properties'];

        beforeEach(() => {
            map = {
                properties: {
                    name: 'Map name',
                    description: 'Map description'
                }
            };
            propertiesFixture = {
                name: 'object name',
                description: 'object description'
            };
        });

        it('should convert map without objects', () => {
            const doc = convert(map, []);
            expect(doc.find(placemarksXPath, NS_URIS)).to.have.length(0);
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
            expect(doc.get(`${placemarksXPath}/kml:Point/kml:coordinates`, NS_URIS)!.text())
                .to.equal('1,2');
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
            expect(doc.get(`${placemarksXPath}/kml:LineString/kml:coordinates`, NS_URIS)!.text())
                .to.equal('1,2 3,4');
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
            const polygonElem = doc.get(`${placemarksXPath}/kml:Polygon`, NS_URIS)!;
            const exteriorRings = polygonElem.find(
                'kml:outerBoundaryIs/kml:LinearRing/kml:coordinates',
                NS_URIS
            );
            expect(exteriorRings).to.have.length(1);
            expect(exteriorRings[0].text()).to.equal('1,2 3,4');

            const interiorRings = polygonElem.find(
                'kml:innerBoundaryIs/kml:LinearRing/kml:coordinates',
                NS_URIS
            );
            expect(interiorRings).to.have.length(2);
            expect(interiorRings[0].text()).to.equal('5,6 7,8');
            expect(interiorRings[1].text()).to.equal('9,10 11,12');
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
            expect(doc.find(`${placemarksXPath}/*`, NS_URIS)).to.have.length(0);
        });

        describe('name and description', () => {
            function getName(doc: libxml.Document) {
                return doc.get(`${placemarksXPath}/kml:name`, NS_URIS)!.text();
            }

            function getDescription(doc: libxml.Document) {
                return doc.get(`${placemarksXPath}/kml:description`, NS_URIS)!.text();
            }

            it('should convert object name and description', () => {
                const point: SourceObject = {
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

                const doc = convert(map, [point]);
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
        });

        describe('object styles', () => {
            function findStyles(doc: libxml.Document) {
                return doc.find('/kml:kml/kml:Document/kml:Style', NS_URIS);
            }

            function findPlacemarks(doc: libxml.Document) {
                return doc.find(placemarksXPath, NS_URIS);
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
                expect(styles[0].get('kml:LineStyle/kml:color', NS_URIS)!.text())
                    .to.equal('e64345ed');
                expect(styles[0].get('kml:LineStyle/kml:width', NS_URIS)!.text())
                    .to.equal(String(commonLineStyles.strokeWidth));
                expect(styles[1].get('kml:LineStyle/kml:color', NS_URIS))
                    .to.not.exist;
                expect(styles[1].get('kml:LineStyle/kml:width', NS_URIS)!.text())
                    .to.equal('1');

                const styleRefs = styles.map((style: libxml.Element) => '#' + style.attr('id')!.value());
                const placemarks = findPlacemarks(doc);
                expect(placemarks[0].get('kml:styleUrl', NS_URIS)!.text()).to.equal(styleRefs[0]);
                expect(placemarks[1].get('kml:styleUrl', NS_URIS)!.text()).to.equal(styleRefs[0]);
                expect(placemarks[2].get('kml:styleUrl', NS_URIS)!.text()).to.equal(styleRefs[1]);
            });

            it('should add style for Point with "preset" option', () => {
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
                expect(styles).to.have.length(1);
                expect(styles[0].get('kml:IconStyle/kml:Icon/kml:href', NS_URIS)).to.exist;

                const placemarks = findPlacemarks(doc);
                expect(placemarks[0].get('kml:styleUrl', NS_URIS)!.text())
                    .to.equal('#' + styles[0].attr('id')!.value());
            });

            it('should not add style for Point without "preset" option', () => {
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

                const placemarks = findPlacemarks(doc);
                expect(placemarks[0].get('kml:styleUrl', NS_URIS)).to.not.exist;
                expect(placemarks[0].get('kml:Style', NS_URIS)).to.not.exist;
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

                const placemarks = findPlacemarks(doc);
                expect(placemarks[0].get('kml:styleUrl', NS_URIS)).to.not.exist;
                expect(placemarks[1].get('kml:styleUrl', NS_URIS)).to.not.exist;
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
                expect(styles[0].get('kml:PolyStyle/kml:color', NS_URIS)!.text())
                    .to.equal('dddddddd');
                expect(styles[0].get('kml:PolyStyle/kml:fill', NS_URIS)!.text())
                    .to.equal('1');
                expect(styles[0].get('kml:PolyStyle/kml:outline', NS_URIS)!.text())
                    .to.equal('0');
            });
        });
    });
});
