import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { PaymentModalPaymentButton } from '../index';

const Component: React.FunctionComponent<React.ComponentProps<typeof PaymentModalPaymentButton>> = (props) => (
    <PaymentModalPaymentButton {...props} />
);

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

describe('PaymentModalPaymentButton', () => {
    it(' Базовое состояние', async () => {
        await renderSeveralResolutions(<Component text={'Денюжки плоти'} onClick={noop} />, dimensions);
    });

    it('Заблокированная кнопка', async () => {
        await renderSeveralResolutions(<Component text={'Денюжки плоти'} disabled onClick={noop} />, dimensions);
    });

    it('С промокодами в описании', async () => {
        await renderSeveralResolutions(<Component text={'Денюжки плоти'} withPromocodes onClick={noop} />, dimensions);
    });
});
