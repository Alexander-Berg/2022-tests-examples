import * as assert from 'assert';
import {generateMetrokitStyleDictionaryWithOverride} from '../../server/editor/format-utils/metrokit-style-helpers';

const colorsCompressed: {[key: string]: string[]} = {
    background: ['ffffff', '292929'],
    label: ['12161a', 'ffffff']
};

const colorsDeflated = {
    background: 'ffffff',
    label: '12161a',
    $override: [{
        format: 'v2',
        values: {
            background: {
                type: 'Stylized',
                values: {
                    light: 'ffffff',
                    dark: '292929'
                }
            },
            label: {
                type: 'Stylized',
                values: {
                    light: '12161a',
                    dark: 'ffffff'
                }
            }
        }
    }]
};

const styles = ['light', 'dark'];

describe('generateMetrokitStyleDictionaryWithOverride', () => {
    it('should deflate styles', () => {
        const result = generateMetrokitStyleDictionaryWithOverride(colorsCompressed, styles);
        assert.deepEqual(result, colorsDeflated);
    });
});
