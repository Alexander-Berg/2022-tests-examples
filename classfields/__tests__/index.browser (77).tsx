import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { RequestStatus } from 'realty-core/types/network';

import { AppProvider } from 'view/libs/test-helpers';

import { SelectionContainer } from '../container';

const renderOptions = [{ viewport: { width: 1000, height: 600 } }, { viewport: { width: 415, height: 600 } }];

const getStore = (status: RequestStatus) => {
    switch (status) {
        case RequestStatus.LOADED:
            return {
                spa: {
                    status: RequestStatus.LOADED,
                },
            };
        case RequestStatus.PENDING:
            return {
                spa: {
                    status: RequestStatus.PENDING,
                },
            };
        default:
            return {
                spa: {
                    status: RequestStatus.LOADED,
                },
            };
    }
};

const Component: React.FunctionComponent<{ status: RequestStatus }> = ({ status }) => (
    <AppProvider initialState={getStore(status)} bodyBackgroundColor={AppProvider.PageColor.USER_LK}>
        <SelectionContainer />
    </AppProvider>
);

describe('Разводящая страница', () => {
    renderOptions.forEach((renderOption) => {
        it(`Внешний вид ${renderOption.viewport.width} px`, async () => {
            await render(<Component status={RequestStatus.LOADED} />, renderOption);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    renderOptions.forEach((renderOption) => {
        it(`Показ скелетонов ${renderOption.viewport.width} px`, async () => {
            await render(<Component status={RequestStatus.PENDING} />, renderOption);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
