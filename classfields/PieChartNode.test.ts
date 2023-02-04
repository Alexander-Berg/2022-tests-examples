import type { YogaNode } from 'app/server/tmpl/YogaNode';
import { PaletteColor } from 'app/server/tmpl/android/v1/helpers/Palette';
import { spreadBackgroundColor, spreadTextColor } from 'app/server/tmpl/android/v1/models/Models';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';


describe('pie chart', () => {
    it('node', () => {
        const ratingNode: YogaNode = {
            name: 'stack',
            id: 'pie_chart_node',
            flexDirection: 'column',
            ...spreadBackgroundColor(PaletteColor.surface),
            children: [
                {
                    name: 'frame',
                    width: 50,
                    height: 50,
                    children: [
                        {
                            name: 'pieGraph',
                            id: 'pie_1',
                        },
                        {
                            name: 'stack',
                            width: 50,
                            height: 50,
                            alignItems: 'center',
                            justifyContent: 'center',
                            children: [
                                {
                                    name: 'text',
                                    ...spreadTextColor(PaletteColor.onSurfaceEmphasisHigh),
                                    text: '70',
                                },
                            ],
                        },
                    ],
                },
                {
                    name: 'pieGraph',
                    id: 'pie_2',
                    width: 50,
                    height: 50,
                },
            ],
        };
        const xml = yogaToXml([ ratingNode ]);
        expect(xml).toMatchSnapshot();
    });
});
