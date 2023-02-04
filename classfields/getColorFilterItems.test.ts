import type { StateCardGroupComplectations } from 'auto-core/react/dataDomain/cardGroupComplectations/types';

import getColorFilterItems from './getColorFilterItems';

const state = {
    cardGroupComplectations: {
        data: {
            complectations: [
                {
                    colors_hex: [
                        '040001',
                        'CACECB',
                        'FAFBFB',
                    ],
                },
                {
                    colors_hex: [
                        '040001',
                        'FAFBFB',
                        '200204',
                    ],
                },
                {
                    colors_hex: [
                        '040001',
                        'FAFBFB',
                    ],
                },
            ],
        },
    } as unknown as StateCardGroupComplectations,
};

it('должен вернуть набор цветов для фильтра по цвету', () => {
    expect(getColorFilterItems(state)).toEqual([
        {
            hex: '040001',
            value: '040001',
            title: 'чёрный',
            titleShort: 'чёрный',
            visibleHex: '000000',
        },
        {
            gradient: 'linear-gradient(to bottom, #f0f0f0, #c1c1c1)',
            hex: 'CACECB',
            value: 'CACECB',
            title: 'серебристый',
            titleShort: 'серебро',
            visibleHex: 'c1c1c1',
        },
        {
            hex: 'FAFBFB',
            value: 'FAFBFB',
            title: 'белый',
            titleShort: 'белый',
            visibleHex: 'ffffff',
        },
        {
            hex: '200204',
            value: '200204',
            title: 'коричневый',
            titleShort: 'коричн.',
            visibleHex: '926547',
        },
    ]);
});
