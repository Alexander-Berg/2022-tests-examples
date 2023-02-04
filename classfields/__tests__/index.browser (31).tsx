import React from 'react';
import { render } from 'jest-puppeteer-react';

import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { rootReducer } from 'view/entries/manager/reducer';
import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';

import { ManagerUserTerms } from '../';

import { acceptedActualTerms, acceptedNotActualTerms, differentPaymentAndAcceptedTerms, newUser } from './stubs';

const renderOptions = [{ viewport: { width: 1000, height: 300 } }, { viewport: { width: 580, height: 300 } }];

const Component: React.FC<{ store: DeepPartial<IUniversalStore> }> = ({ store }) => {
    return (
        <AppProvider rootReducer={rootReducer} initialState={store}>
            <ManagerUserTerms />
        </AppProvider>
    );
};

describe('ManagerUserTerms', () => {
    describe('Новый пользователь', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={newUser} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Принятые условия - актуальные', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={acceptedActualTerms} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Разные принятые и платежные условия', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={differentPaymentAndAcceptedTerms} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe('Разные принятые и актуальные условия', () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component store={acceptedNotActualTerms} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
