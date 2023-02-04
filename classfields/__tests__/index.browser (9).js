import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import PaymentModal from '../index';
import BankCardStyles from '../../BankCardScreen/styles.module.css';
import PaymentEmailStyles from '../../BankCardScreen/PaymentEmail/styles.module.css';
import SberbankStyles from '../../SberbankScreen/styles.module.css';

import stages from './mockData/stages.json';

const price = stages.init.price;
const methods = stages.init.methods;
const [ WIDTH, HEIGHT ] = [ 800, 600 ];

const Component = props => (
    <AppProvider>
        <PaymentModal
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
            services={[ 'promotion' ]}
            showPromocodesInfoScreen
            isLoaded
            promocodes={{}}
            changeCurrentIndex={() => () => {}}
            defaultIndex={0}
            currentIndex={0}
            needConfirmation={false}
            url={''}
            kassaShopId={666}
            skipPromocodesScreen={() => {}}
            status='SUCCESS'
            {...props}
        />
    </AppProvider>
);

describe('PaymentModal', () => {
    it('bankCard init stage', async() => {
        const currentIndex = methods.findIndex(m => m.type === 'bankCard');

        const component = <Component currentIndex={currentIndex} />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('bankCard init stage invalid email', async() => {
        const currentIndex = methods.findIndex(m => m.type === 'bankCard');

        const component = <Component currentIndex={currentIndex} />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        await page.type(`.${PaymentEmailStyles.input} input`, 'bad_email');
        await page.click(`.${BankCardStyles.button}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('exit screen', async() => {
        const component = (
            <Component currentStageName='init' status={undefined} />);

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });
        await page.click('.CloseModalButton');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('tiedCard with 3ds check status', async() => {
        const selectedIndex = methods.findIndex(m => m.type === 'tiedCard');

        const component = (
            <Component
                currentStageName='perform'
                currentIndex={selectedIndex}
                needConfirmation
                url={''}
            />);

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('sberbank init stage', async() => {
        const currentIndex = methods.findIndex(m => m.type === 'sberbank');

        const component = <Component currentIndex={currentIndex} currentStageName='init' isLoading={false} />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('sberbank check status', async() => {
        const currentIndex = methods.findIndex(m => m.type === 'sberbank');

        const component = <Component currentIndex={currentIndex} currentStageName='perform' isLoading={false} />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('sberbank invalid phone', async() => {
        const currentIndex = methods.findIndex(m => m.type === 'sberbank');

        const component = <Component currentIndex={currentIndex} currentStageName='init' isLoading={false} />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        await page.click(`.${SberbankStyles.button}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('init loading screen', async() => {
        const component = <Component isLoading />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('common error screen', async() => {
        const component = <Component hasError />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('yooMoney init stage', async() => {
        const currentIndex = methods.findIndex(m => m.type === 'yooMoney');

        const component = <Component currentIndex={currentIndex} />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('wallet init stage', async() => {
        const currentIndex = methods.findIndex(m => m.type === 'wallet');

        const component = <Component currentIndex={currentIndex} />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('tiedCard init stage', async() => {
        const currentIndex = methods.findIndex(m => m.type === 'tiedCard');

        const component = <Component currentIndex={currentIndex} />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('perform payment loading', async() => {
        const component = <Component currentStageName='perform' isLoading />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('perform payment refresh status', async() => {
        const component = <Component currentStageName='perform' />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('perform payment - CARD_EXPIRED', async() => {
        const component = <Component currentStageName='perform' hasError error="CARD_EXPIRED" />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('perform payment - INTERNAL_ERROR', async() => {
        const component = <Component currentStageName='perform' hasError error="INTERNAL_ERROR" />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('perform payment - INVALID_CARD_CREDENTIALS', async() => {
        const component = <Component currentStageName='perform' hasError error="INVALID_CARD_CREDENTIALS" />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('perform payment - NO_ENOUGH_FUNDS', async() => {
        const component = <Component currentStageName='perform' hasError error="NO_ENOUGH_FUNDS" />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('perform payment - INVALID_CARD_NUMBER', async() => {
        const component = <Component currentStageName='perform' hasError error="INVALID_CARD_NUMBER" />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('perform payment - INVALID_CARD_CSC', async() => {
        const component = <Component currentStageName='perform' hasError error="INVALID_CARD_CSC" />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('payment successifully finished', async() => {
        const component = <Component currentStageName='status' />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('payment finished with timeout', async() => {
        const component = <Component currentStageName='status' status="TIMEOUT" />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('payment finished with error', async() => {
        const component = <Component currentStageName='status' status="ERROR" />;

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендер модалки для ЕГРН, промокодов нет', async() => {
        const component = (
            <Component
                services={[ 'egrnPaidReport' ]}
                price={{
                    base: 400,
                    effective: 400
                }}
            />
        );

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендер модалки для ЕГРН, промокод покрывает стоимость', async() => {
        const component = (
            <Component
                services={[ 'egrnPaidReport' ]}
                methods={[ { type: 'promocodesOnly' } ]}
                price={{
                    base: 400,
                    effective: 0
                }}
                promocodes={{
                    money: {
                        discount: 500
                    }
                }}
            />
        );

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендер модалки для ЕГРН, промокод не покрывает стоимость', async() => {
        const component = (
            <Component
                services={[ 'egrnPaidReport' ]}
                price={{
                    base: 400,
                    effective: 350
                }}
                promocodes={{
                    money: {
                        discount: 50
                    }
                }}
            />
        );

        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
