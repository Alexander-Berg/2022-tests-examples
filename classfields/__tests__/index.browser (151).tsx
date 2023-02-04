import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { MortgageDisclaimer } from '../';

import { getBank } from './mocks';

describe('MortgageDisclaimer', () => {
    it('рисует максимально заполненный дисклеймер', async () => {
        await render(
            <MortgageDisclaimer bank={getBank(['legalName', 'licenseNumber', 'licenseDate', 'headOfficeAddress'])} />,
            {
                viewport: { width: 320, height: 300 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует дисклеймер без информации о банке', async () => {
        await render(<MortgageDisclaimer bank={getBank(['licenseDate', 'headOfficeAddress'])} />, {
            viewport: { width: 320, height: 300 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует дисклеймер без информации о банке (нет банка вообще)', async () => {
        await render(<MortgageDisclaimer />, {
            viewport: { width: 320, height: 300 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует дисклеймер без юр. лица и адреса', async () => {
        await render(<MortgageDisclaimer bank={getBank(['licenseNumber', 'licenseDate'])} />, {
            viewport: { width: 320, height: 300 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует дисклеймер без даты у лицензии', async () => {
        await render(<MortgageDisclaimer bank={getBank(['legalName', 'licenseNumber', 'headOfficeAddress'])} />, {
            viewport: { width: 320, height: 300 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует дисклеймер без лицензии', async () => {
        await render(<MortgageDisclaimer bank={getBank(['legalName', 'headOfficeAddress'])} />, {
            viewport: { width: 320, height: 300 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
