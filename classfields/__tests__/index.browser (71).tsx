import React from 'react';
import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { IPaymentCard } from 'types/paymentCard';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { userReducer } from 'view/entries/user/reducer';
import ModalDisplay from 'view/components/ModalDisplay';

import { OwnerPaymentMethodsCardListingContainer } from '../container';

import { store } from './stub';

const renderOptions = [{ viewport: { width: 580, height: 300 } }];

const Component: React.FunctionComponent<
    {
        store: DeepPartial<IUniversalStore>;
        Gate?: AnyObject;
    } & React.ComponentProps<typeof OwnerPaymentMethodsCardListingContainer>
> = ({ store, Gate, ...otherProps }) => (
    <AppProvider rootReducer={userReducer} Gate={Gate} initialState={store}>
        <OwnerPaymentMethodsCardListingContainer {...otherProps} />
        <ModalDisplay />
    </AppProvider>
);

describe('OwnerPaymentMethodsCardListing', () => {
    describe(`Внешний вид`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(
                    <Component store={store} cards={store.payments!.data!.cards as IPaymentCard[]} />,
                    renderOption
                );

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
