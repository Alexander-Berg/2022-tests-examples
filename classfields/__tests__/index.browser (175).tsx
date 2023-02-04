import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { Tariff } from 'realty-core/types/tariff';

import { TariffType, TariffPayment } from '../../types';

import { TariffPopup } from '..';

const TARIFF_PAIRS: [TariffType, TariffPayment][] = [
    [TariffType.MINIMUM, TariffPayment.CALLS],
    [TariffType.MINIMUM, TariffPayment.LISTING],
    [TariffType.EXTENDED, TariffPayment.CALLS],
    [TariffType.EXTENDED, TariffPayment.LISTING],
    [TariffType.MAXIMUM, TariffPayment.CALLS],
    [TariffType.MAXIMUM, TariffPayment.LISTING],
];

describe('TariffPopup', () => {
    TARIFF_PAIRS.forEach(([type, payment]) => {
        it(`Рисует попап тарифа (${payment}${type})`, async () => {
            await render(<TariffPopup isOpened type={type} payment={payment} activeTariff={Tariff.CALLS_EXTENDED} />, {
                viewport: { width: 1280, height: 900 },
            });
            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
