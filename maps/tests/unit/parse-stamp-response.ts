import * as fs from 'fs';
import {expect} from 'chai';
import {parseStampResponse, Stamp} from '../../src/v2/parse-stamp-response';

function parseFile(filename: string): Stamp | null {
    const xmlString = fs.readFileSync(`tests/unit/stamp-responses/${filename}`, 'utf8');
    return parseStampResponse(xmlString);
}

describe('parseStampResponse()', () => {
    it('should parse "trf" layer info', () => {
        expect(parseFile('trf.xml')).to.deep.equal({
            version: '1497037342',
            zoomRange: [1, 18]
        });
    });

    it('should parse "hph" layer info', () => {
        expect(parseFile('hph.xml')).to.deep.equal({
            version: '0.27.4-0.1.3.0-0.2017.06.09.22.00',
            zoomRange: [0, 0]
        });
    });
});
