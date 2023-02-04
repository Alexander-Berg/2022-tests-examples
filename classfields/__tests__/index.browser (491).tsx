import React from 'react';
import { DeepPartial } from 'utility-types';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import reducer, { ICommonPageStore } from 'view/react/deskpad/reducers/roots/common';

import { WalletCardsContainer } from '../container';

import { oneCardStore, severalCardsStore } from './stub/store';

const Component: React.FunctionComponent<{ store?: DeepPartial<ICommonPageStore>; Gate?: Record<string, unknown> }> = (
    props
) => (
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    <AppProvider rootReducer={reducer as any} Gate={props.Gate} initialState={props.store} context={{}}>
        <WalletCardsContainer />
    </AppProvider>
);

const selectors = {
    dropdownTrigger: '[data-test="wallet-cards-dropdown-trigger"]',
    cardMastercard: '[data-test="wallet-card-mastercard"]',
    delete: '[data-test="wallet-card-delete"]',
    confirmModalContinue: '[data-test="confirm-modal-continue"]',
    confirmModalCancel: '[data-test="confirm-modal-cancel"]',
    preferred: '[data-test="wallet-card-preferred"]',
};

const renderOptions = { viewport: { width: 1000, height: 200 } };

describe('WalletCards', () => {
    it('нет карт', async () => {
        await render(<Component />, renderOptions);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('одна карта', async () => {
        await render(<Component store={oneCardStore} />, renderOptions);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('выбор карты', async () => {
        await render(<Component store={severalCardsStore} />, renderOptions);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(selectors.dropdownTrigger);
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(selectors.cardMastercard);
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    describe('удаление карты', () => {
        it('потдверждение', async () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'wallet.deleteCard': {
                            return Promise.resolve();
                        }
                        case 'payment.get-renewal-problems': {
                            return Promise.resolve();
                        }
                    }
                },
            };

            await render(<Component Gate={Gate} store={severalCardsStore} />);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.delete);
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.confirmModalContinue);
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('отмена', async () => {
            await render(<Component store={severalCardsStore} />);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.delete);
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click(selectors.confirmModalCancel);
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });

    it('основная карта', async () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'wallet.preferCard': {
                        return Promise.resolve();
                    }
                }
            },
        };

        await render(<Component Gate={Gate} store={severalCardsStore} />, renderOptions);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(selectors.preferred);
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
