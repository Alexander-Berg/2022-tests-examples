import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import WalletModal from '../index';
import WalletAmountStyles from '../WalletAmountScreen/styles.module.css';

import stages from './mockData/stages.json';

const price = stages.init.price;
const methods = stages.init.methods;
const [ WIDTH, HEIGHT ] = [ 800, 600 ];

const Component = props => (
    <WalletModal
        visible
        onModalClose={() => {}}
        onModalOpen={() => {}}
        performPayment={() => {}}
        updatePurchaseStatus={() => Promise.resolve()}
        updatePurchaseStatusTimeout={() => Promise.resolve()}
        isLoading={false}
        hasError={false}
        price={price}
        methods={methods}
        currentStageName='init'
        retryPayment={() => {}}
        offerIds={[ '1' ]}
        services={[ 'wallet' ]}
        isLoaded
        promocodes={{}}
        changeCurrentIndex={() => () => {}}
        defaultIndex={0}
        currentIndex={0}
        needConfirmation={false}
        url={''}
        kassaShopId={666}
        showAmountScreen
        setPaymentAmount={() => {}}
        status='SUCCESS'
        {...props}
    />
);

describe('WalletModal', () => {
    it('wallet main screen', async() => {
        const component = <Component showAmountScreen={false} />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('wallet amount screen', async() => {
        const component = <Component />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('wallet amount screen invalid amount', async() => {
        const component = <Component />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        await page.click('.TextInput__clear_visible');
        await page.click(`.${WalletAmountStyles.continueButton}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
