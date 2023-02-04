import React from 'react';
import { DeepPartial } from 'utility-types';
import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import reducer, { ICommonPageStore } from 'view/react/deskpad/reducers/roots/common';

import { transactionWithOffer } from '../../__tests__/stub/transactions';

import { WalletTransactionsContainer } from '../container';

import { store } from './stub/store';

const Component: React.FunctionComponent<{ store?: DeepPartial<ICommonPageStore>; Gate?: Record<string, unknown> }> = (
    props
) => (
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    <AppProvider rootReducer={reducer as any} Gate={props.Gate} initialState={props.store} context={{}}>
        <WalletTransactionsContainer />
    </AppProvider>
);

advanceTo(new Date('2021-02-26'));

describe('WalletTransactions', () => {
    describe('подгрузка транзакций', () => {
        it('успех', async () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'wallet.getTransactions': {
                            return Promise.resolve({
                                transactions: [
                                    { ...transactionWithOffer, id: { ...transactionWithOffer.id, id: '6' } },
                                    { ...transactionWithOffer, id: { ...transactionWithOffer.id, id: '7' } },
                                    { ...transactionWithOffer, id: { ...transactionWithOffer.id, id: '8' } },
                                    { ...transactionWithOffer, id: { ...transactionWithOffer.id, id: '9' } },
                                    { ...transactionWithOffer, id: { ...transactionWithOffer.id, id: '10' } },
                                ],
                                offers: {},
                            });
                        }
                    }
                },
            };

            await render(<Component store={store} Gate={Gate} />);
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click('[data-test="wallet-transactions-load"]');
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('неудача', async () => {
            const Gate = {
                create: (path: string) => {
                    switch (path) {
                        case 'wallet.getTransactions': {
                            return Promise.reject();
                        }
                    }
                },
            };

            await render(<Component store={store} Gate={Gate} />);
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

            await page.click('[data-test="wallet-transactions-load"]');
            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });
    });

    it('модалка транзакции', async () => {
        await render(<Component store={store} />);

        await page.click('[data-test="wallet-transaction-modal-trigger"]');
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
