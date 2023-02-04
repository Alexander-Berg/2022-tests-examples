import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IMetro } from 'realty-core/view/react/common/types/egrnPaidReport';
import { TransportDistanceType } from 'realty-core/types/location';

import { EGRNPaidReportMetroTime } from '../index';

const metro: IMetro = {
    name: 'Ломоносовская',
    timeToMetro: 15,
    lineColors: ['ff0000'],
    rgbColor: '00ff00',
    metroGeoId: 123,
    latitude: 123,
    longitude: 123,
};

describe('EGRNPaidReportMetroTime', () => {
    it('рендерится с иконкой пешехода', async () => {
        await render(<EGRNPaidReportMetroTime metro={{ ...metro, metroTransport: TransportDistanceType.ON_FOOT }} />, {
            viewport: { width: 300, height: 60 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с иконкой транспорта', async () => {
        await render(
            <EGRNPaidReportMetroTime metro={{ ...metro, metroTransport: TransportDistanceType.ON_TRANSPORT }} />,
            {
                viewport: { width: 300, height: 60 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится с иконкой автомобиля', async () => {
        await render(<EGRNPaidReportMetroTime metro={{ ...metro, metroTransport: TransportDistanceType.ON_CAR }} />, {
            viewport: { width: 300, height: 60 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
