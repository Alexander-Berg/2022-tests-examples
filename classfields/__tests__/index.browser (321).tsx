import React from 'react';

import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { YaDealValuationContainer } from '../container';

import { mockNetwork } from './mocks';

const defaultStore = {
    yaDealValuation: {
        fields: {
            ADDRESS: {
                id: 'ADDRESS',
                value: '',
                isHouse: false,
                title: '',
                subtitle: '',
            },
            ROOMS: {
                id: 'ROOMS',
                value: '2',
            },
            AREA: {
                id: 'AREA',
                value: '',
            },
        },
        network: mockNetwork,
    },
};

function Component(props: Record<string, unknown>) {
    return (
        <AppProvider initialState={{ ...defaultStore, page: props?.page }}>
            <YaDealValuationContainer />
        </AppProvider>
    );
}

describe('Q&A block in YaDealValuation', () => {
    it('Показывается блок Q&A', async () => {
        const pageParamsOneRoom = {
            flatType: 'odnokomnatnaya',
        };

        await render(<Component page={{ params: pageParamsOneRoom }} />, {
            viewport: { width: 480, height: 800 },
        });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('Не показывается блок Q&A', async () => {
        const pageParamsOneRoom = {
            flatType: 'dvuhkomnatnaya',
        };

        await render(<Component page={{ params: pageParamsOneRoom }} />, {
            viewport: { width: 480, height: 800 },
        });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
