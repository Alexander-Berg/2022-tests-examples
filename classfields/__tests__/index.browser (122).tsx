import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { CardBrand } from 'realty-core/types/payment/purchase';

import { PaymentMethod } from '../index';
import { withPaymentMethodContainer } from '../withPaymentMethodContainer';

import {
    bankCardPaymentMethod,
    getTiedCard,
    sberbankPaymentMethod,
    walletPaymentMethod,
    yooMoneyPaymentMethod,
} from './mocks';

const PaymentMethodContainer = withPaymentMethodContainer()(PaymentMethod);
const Component: React.FunctionComponent<Omit<React.ComponentProps<typeof PaymentMethodContainer>, 'handleClick'>> = (
    props
) => <PaymentMethodContainer {...props} handleClick={noop} />;

const DEFAULT_WIDTH = 300;
const DEFAULT_HEIGHT = 100;

describe('PaymentMethodContainer', () => {
    it('active, disabled', async () => {
        const component = <Component active method={bankCardPaymentMethod} disabled hovered={false} />;

        await render(component, { viewport: { width: DEFAULT_WIDTH, height: DEFAULT_HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('not active, disabled', async () => {
        const component = <Component method={bankCardPaymentMethod} disabled hovered={false} />;

        await render(component, { viewport: { width: DEFAULT_WIDTH, height: DEFAULT_HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('bank card', async () => {
        const component = <Component method={bankCardPaymentMethod} hovered={false} />;

        await render(component, { viewport: { width: DEFAULT_WIDTH, height: DEFAULT_HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('bank card active not hovered', async () => {
        const component = <Component method={bankCardPaymentMethod} hovered={false} active />;

        await render(component, { viewport: { width: DEFAULT_WIDTH, height: DEFAULT_HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('bank card hovered not active', async () => {
        const component = <Component method={bankCardPaymentMethod} hovered active={false} />;

        await render(component, { viewport: { width: DEFAULT_WIDTH, height: DEFAULT_HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('bank card hovered and active', async () => {
        const component = <Component method={bankCardPaymentMethod} hovered active />;

        await render(component, { viewport: { width: DEFAULT_WIDTH, height: DEFAULT_HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('sberbank', async () => {
        const component = <Component method={sberbankPaymentMethod} active hovered={false} />;

        await render(component, { viewport: { width: DEFAULT_WIDTH, height: DEFAULT_HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('yooMoney', async () => {
        const component = <Component method={yooMoneyPaymentMethod} active hovered={false} />;

        await render(component, { viewport: { width: DEFAULT_WIDTH, height: DEFAULT_HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('mastercard', async () => {
        const component = <Component method={getTiedCard(CardBrand.MASTERCARD)} active hovered={false} />;

        await render(component, { viewport: { width: DEFAULT_WIDTH, height: DEFAULT_HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('maestro', async () => {
        const component = <Component method={getTiedCard(CardBrand.MAESTRO)} active hovered={false} />;

        await render(component, { viewport: { width: DEFAULT_WIDTH, height: DEFAULT_HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('visa', async () => {
        const component = <Component method={getTiedCard(CardBrand.VISA)} active hovered={false} />;

        await render(component, { viewport: { width: DEFAULT_WIDTH, height: DEFAULT_HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('mir', async () => {
        const component = <Component method={getTiedCard(CardBrand.MIR)} active hovered={false} />;

        await render(component, { viewport: { width: DEFAULT_WIDTH, height: DEFAULT_HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('wallet', async () => {
        const component = <Component method={walletPaymentMethod} active hovered={false} />;

        await render(component, { viewport: { width: DEFAULT_WIDTH, height: DEFAULT_HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('noname', async () => {
        const component = <Component method={getTiedCard(CardBrand.UNKNOWN_CARD_BRAND)} active hovered={false} />;

        await render(component, { viewport: { width: DEFAULT_WIDTH, height: DEFAULT_HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
