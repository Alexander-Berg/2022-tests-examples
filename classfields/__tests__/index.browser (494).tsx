import React from 'react';
import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import {
    transactionWithOffer,
    transactionWithOffers,
    transactionWithEGRNReport,
    transactionWithAddingMoneyToWallet,
    // eslint-disable-next-line @realty-front/no-relative-imports
} from '../../../__tests__/stub/transactions';
import { WalletTransactionsItem } from '../';

const renderOptions = { viewport: { width: 1000, height: 100 } };
const context = {
    link: (): string => 'link',
};

advanceTo(new Date('2021-02-26'));

describe('WalletTransactionsItem', () => {
    it('транзакция с протухшими офферами', async () => {
        await render(
            <AppProvider context={context}>
                <WalletTransactionsItem
                    transaction={transactionWithOffer}
                    onClick={() => undefined}
                    isActiveOffer={() => false}
                />
            </AppProvider>,
            renderOptions
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('транзакция с одним оффером', async () => {
        await render(
            <AppProvider context={context}>
                <WalletTransactionsItem
                    transaction={transactionWithOffer}
                    onClick={() => undefined}
                    isActiveOffer={() => true}
                />
            </AppProvider>,
            renderOptions
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('транзакция с несколькими офферами', async () => {
        await render(
            <AppProvider context={context}>
                <WalletTransactionsItem
                    transaction={transactionWithOffers}
                    onClick={() => undefined}
                    isActiveOffer={() => true}
                />
            </AppProvider>,
            renderOptions
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('транзакция с ЕГРН отчетом', async () => {
        await render(
            <AppProvider context={context}>
                <WalletTransactionsItem
                    transaction={transactionWithEGRNReport}
                    onClick={() => undefined}
                    isActiveOffer={() => true}
                />
            </AppProvider>,
            renderOptions
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('транзакция с пополнением кошелька', async () => {
        await render(
            <AppProvider context={context}>
                <WalletTransactionsItem
                    transaction={transactionWithAddingMoneyToWallet}
                    onClick={() => undefined}
                    isActiveOffer={() => true}
                />
            </AppProvider>,
            renderOptions
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
