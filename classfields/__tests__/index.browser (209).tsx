import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IHeatmap, IMetro } from 'realty-core/view/react/common/types/egrnPaidReport';
import { IPark, IPond, TransportDistanceType } from 'realty-core/types/location';

import { EGRNPaidReportLocationFeaturesBlock } from '../';

const props = {
    pondList: [
        {
            name: 'Новоозёрский пруд',
            timeOnFoot: 420,
        },
        {
            name: 'Новопрудный пруд',
            timeOnFoot: 500,
        },
    ] as Array<IPond>,
    parkList: [
        {
            name: 'Девичий парк',
            timeOnFoot: 650,
        },
        {
            name: 'Женский парк',
            timeOnFoot: 600,
        },
    ] as Array<IPark>,
    metroList: [
        {
            name: 'Чувашская',
            timeToMetro: 20,
            metroTransport: TransportDistanceType.ON_FOOT,
        },
    ] as Array<IMetro>,
    heatmapList: [
        {
            title: 'Инфраструктура',
            description: 'Её крутость',
            level: 5,
            maxLevel: 9,
        },
        {
            title: 'Доступность',
            description: 'Так себе',
            level: 3,
            maxLevel: 9,
        },
        {
            title: 'Цена жилья',
            description: 'Дёшево',
            level: 8,
            maxLevel: 9,
        },
    ] as Array<IHeatmap>,
};

describe('EGRNPaidReportLocationFeaturesBlock', () => {
    it('рендерится', async () => {
        await render(<EGRNPaidReportLocationFeaturesBlock {...props} />, {
            viewport: { width: 350, height: 550 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
