import * as fs from 'fs';
import {expect} from 'chai';
import {
    parseMetaResponse,
    LayerInfo,
    parseMetaError
} from '../../src/v2/parse-meta-response';
import {config} from '../../src/config';

describe('parseMetaResponse()', () => {
    function parseFile(filename: string, lang = 'ru'): LayerInfo[] {
        const xmlString = fs.readFileSync(`tests/unit/meta-responses/${filename}`, 'utf8');
        return parseMetaResponse(xmlString, lang);
    }

    it('should parse layer zoom range', () => {
        expect(parseFile('zoom-range.xml')).to.deep.equal([
            {
                id: 'test',
                zoomRange: [1, 18]
            }
        ]);
    });

    it('should parse layer zoom range without "min" attribute', () => {
        expect(parseFile('zoom-range-without-min.xml')).to.deep.equal([
            {
                id: 'test',
                zoomRange: [0, 18]
            }
        ]);
    });

    it('should parse copyrights', () => {
        expect(parseFile('copyrights.xml')).to.deep.equal([
            {
                id: 'layer1',
                copyrights: ['© Яндекс']
            },
            {
                id: 'layer2',
                copyrights: [
                    '© Яндекс',
                    '© GeoEye, Inc'
                ]
            }
        ]);
    });

    it('should parse LayerMetaData', () => {
        expect(parseFile('layer-meta-data.xml')).to.deep.equal([
            {
                id: 'trf',
                LayerMetaData: [
                    {
                        geoId: 157,
                        archive: true,
                        events: true
                    },
                    {
                        geoId: 29630,
                        archive: true,
                        events: true
                    }
                ]
            }
        ]);
    });

    it('should translate copyright for langs defined in meta.copyrightTranslateLang in config', () => {
        for (const lang of config['meta.copyrightTranslateLang']) {
            expect(parseFile('copyrights.xml', lang)).to.deep.equal([
                {
                    id: 'layer1',
                    copyrights: ['© Yandex']
                },
                {
                    id: 'layer2',
                    copyrights: [
                        '© Yandex',
                        '© GeoEye, Inc'
                    ]
                }
            ]);
        }
    });

    it('should not translate copyright for ru, be, ua, kk langs', () => {
        for (const lang of ['ru', 'be', 'ua', 'kk']) {
            expect(parseFile('copyrights.xml', lang)).to.deep.equal([
                {
                    id: 'layer1',
                    copyrights: ['© Яндекс']
                },
                {
                    id: 'layer2',
                    copyrights: [
                        '© Яндекс',
                        '© GeoEye, Inc'
                    ]
                }
            ]);
        }
    });
});

describe('parseMetaError()', () => {
    it('should return error message', () => {
        const xmlString = fs.readFileSync('tests/unit/meta-responses/bad-request.xml', 'utf8');
        expect(parseMetaError(xmlString)).to.equal(
            'Access for span request not granted for layer map at zoom 4 and span (116.015625, ' +
            '33.629731660000004)'
        );
    });
});
