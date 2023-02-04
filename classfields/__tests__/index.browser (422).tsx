import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IMetro } from 'realty-core/view/react/common/types/egrnPaidReport';
import { TransportDistanceType } from 'realty-core/types/location';

import { EGRNPaidReportAddressBlock } from '../';

const metros = [
    {
        name: 'Ломоносовская', // рендерится
        timeToMetro: 15,
        rgbColor: '00ff00',
        metroTransport: TransportDistanceType.ON_CAR,
    },
    {
        name: 'Ломоносовская', // не рендерится (дубликат)
        timeToMetro: 9,
        rgbColor: '00ff00',
        metroTransport: TransportDistanceType.ON_TRANSPORT,
    },
    {
        name: 'Ломоносовская', // не рендерится (добираться дольше 15 минут, дубликат)
        timeToMetro: 18,
        rgbColor: '00ff00',
        metroTransport: TransportDistanceType.ON_FOOT,
    },
    {
        name: 'Кржижановского', // рендерится
        timeToMetro: 12,
        rgbColor: '0000ff',
        metroTransport: TransportDistanceType.ON_FOOT,
    },
    {
        name: 'Кржижановского', // не рендерится (дубликат)
        timeToMetro: 8,
        rgbColor: '0000ff',
        metroTransport: TransportDistanceType.ON_TRANSPORT,
    },
    {
        name: 'Александра Невского', // не рендерится (добираться дольше 15 минут)
        timeToMetro: 25,
        rgbColor: 'ff0000',
        metroTransport: TransportDistanceType.ON_CAR,
    },
    {
        name: 'Александра Невского', // не рендерится (добираться дольше 15 минут, дубликат)
        timeToMetro: 25,
        rgbColor: 'ff0000',
        metroTransport: TransportDistanceType.ON_TRANSPORT,
    },
    {
        name: 'Маршала Безрыбова', // рендерится
        timeToMetro: 7,
        rgbColor: 'ff0000',
        metroTransport: TransportDistanceType.ON_TRANSPORT,
    },
    {
        name: 'Нерендерова', // не рендерится (максимум - три станции)
        timeToMetro: 7,
        rgbColor: 'ff0000',
        metroTransport: TransportDistanceType.ON_TRANSPORT,
    },
] as Array<IMetro>;

describe('EGRNPaidReportAddressBlock', () => {
    it(
        'макс. 3 станции метро' + ' отсекая дубли метро, а также метро, до которых добираться дольше 10 мин.',
        async () => {
            await render(<EGRNPaidReportAddressBlock metros={metros} address="ул. Петрикова 87, кв 8" />, {
                viewport: { width: 400, height: 150 },
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        }
    );
});
