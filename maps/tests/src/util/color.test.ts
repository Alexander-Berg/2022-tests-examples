import {expect} from 'chai';
import {parseCSSColor, create} from '../../../src/vector_render_engine/util/color';
import {areColorsFuzzyEqual} from './color';

describe('util/color', () => {
    describe('parseCSSColor', () => {
        it('should parse hex color', () => {
            const color = parseCSSColor('#333');
            expect(color).not.equals(null);
            expect(areColorsFuzzyEqual(color!, create(0x33 / 0xff, 0x33 / 0xff, 0x33 / 0xff, 1)));
        });
        it('should parse rgb() color', () => {
            const color = parseCSSColor('rgb(50, 100, 150)');
            expect(color).not.equals(null);
            expect(areColorsFuzzyEqual(color!, create(50 / 0xff, 100 / 0xff, 150 / 0xff, 1)));
        });
    });
});
