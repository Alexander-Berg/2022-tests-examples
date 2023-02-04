import { yogaToXml } from 'app/server/tmpl/XMLBuilder';
import { PaletteColor } from '../helpers/Palette';
import { getBadgeNode } from './BadgeNode';

describe('badge', () => {
    it('node', () => {
        const badgeNode = getBadgeNode('TEST TEXT', PaletteColor.onPrimaryEmphasisHigh, PaletteColor.primary);
        const xml = yogaToXml([ badgeNode ]);
        expect(xml).toMatchSnapshot();
    });
});
