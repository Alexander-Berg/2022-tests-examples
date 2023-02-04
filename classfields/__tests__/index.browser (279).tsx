import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { PaymentProductType } from 'realty-core/types/payment';

import { PaymentModalHeader } from '../';

const smallDimension = { viewport: { width: 320, height: 400 } };
const mediumDimension = { viewport: { width: 370, height: 400 } };
const wideDimenstion = { viewport: { width: 660, height: 400 } };

const dimensions = [smallDimension, mediumDimension, wideDimenstion];

const renderSeveralResolutions = async (Component: React.ReactElement, dimensions: typeof smallDimension[]) => {
    for (const dimension of dimensions) {
        await render(Component, dimension);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    }
};

describe('PaymentModalHeader', () => {
    it('Отчет ЕГРН', async () => {
        await renderSeveralResolutions(
            <PaymentModalHeader price={100} basePrice={200} services={[PaymentProductType.EGRN_PAID_REPORT]} />,
            dimensions
        );
    });

    it('Размещение', async () => {
        await renderSeveralResolutions(
            <PaymentModalHeader price={100} basePrice={200} services={[PaymentProductType.PLACEMENT]} />,
            dimensions
        );
    });

    it('Продвижение', async () => {
        await renderSeveralResolutions(
            <PaymentModalHeader price={100} basePrice={200} services={[PaymentProductType.PROMOTION]} />,
            dimensions
        );
    });

    it('Поднятие', async () => {
        await renderSeveralResolutions(
            <PaymentModalHeader price={100} services={[PaymentProductType.RAISING]} />,
            dimensions
        );
    });

    it('Премиум', async () => {
        await renderSeveralResolutions(
            <PaymentModalHeader price={5000} services={[PaymentProductType.PREMIUM]} />,
            dimensions
        );
    });

    it('Турбо', async () => {
        await renderSeveralResolutions(
            <PaymentModalHeader price={1200} basePrice={1500} services={[PaymentProductType.PACKAGE_TURBO]} />,
            dimensions
        );
    });

    it('Кошелек', async () => {
        await renderSeveralResolutions(
            <PaymentModalHeader price={5000} services={[PaymentProductType.WALLET]} />,
            dimensions
        );
    });
});
