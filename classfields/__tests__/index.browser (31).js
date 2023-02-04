import React from 'react';
import { render } from 'jest-puppeteer-react';
import merge from 'lodash/merge';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProviders } from 'view/libs/test-helpers/AppProviders';

import { ClientTariffsIndividualPlacementDiscountsContainer } from '../container';
import styles from '../styles.module.css';

import defaultStoreMock from './storeMock';

const mockWithFilledInputs = merge({}, defaultStoreMock, {
    client: {
        tariffs: {
            data: {
                individualPlacementDiscountsOptions: {
                    from: '22.10.2011',
                    to: '23.11.2020',
                    discount: 30
                }
            }
        }
    }
});

const context = {
    router: {
        entries: [ { page: 'clientTariffs', params: { clientId: '1337' } } ]
    }
};

const Component = ({ store }) => (
    <AppProviders store={store} context={context}>
        <ClientTariffsIndividualPlacementDiscountsContainer />
    </AppProviders>
);

const bindPriceListButtonSelector = `.${styles.inputs}:last-of-type .Button`;
const deletePriceListButtonSelector = `.${styles.item}:first-of-type .Link`;
const acceptPopupActionButtonSelector = `.${styles.button}:first-of-type`;
const declinePopupActionButtonSelector = `.${styles.button}:last-of-type`;

describe('ClientTariffsIndividualPlacementDiscounts', () => {
    it('correct draw', async() => {
        await render(<Component store={defaultStoreMock} />, { viewport: { width: 1000, height: 330 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw with filled placement discount inputs', async() => {
        await render(<Component store={mockWithFilledInputs} />, { viewport: { width: 1000, height: 330 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw bind placement discount popup', async() => {
        await render(<Component store={mockWithFilledInputs} />, { viewport: { width: 1000, height: 330 } });

        await page.click(bindPriceListButtonSelector);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.skip('correct draw error message when placement discount deleting failed', async() => {
        await render(<Component store={defaultStoreMock} />, { viewport: { width: 1000, height: 330 } });

        await page.click(deletePriceListButtonSelector);

        await page.click(acceptPopupActionButtonSelector);

        await page.waitFor(10);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.skip('correct draw error message when placement discount binding failed', async() => {
        await render(<Component store={mockWithFilledInputs} />, { viewport: { width: 1000, height: 330 } });

        await page.click(bindPriceListButtonSelector);
        await page.click(acceptPopupActionButtonSelector);

        await page.waitFor(10);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct close popup', async() => {
        await render(<Component store={defaultStoreMock} />, { viewport: { width: 1000, height: 330 } });

        await page.click(deletePriceListButtonSelector);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(declinePopupActionButtonSelector);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw without permissions', async() => {
        const mock = {
            ...defaultStoreMock,
            user: {
                ...defaultStoreMock.user,
                permissions: []
            }
        };

        await render(<Component store={mock} />, { viewport: { width: 1000, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
