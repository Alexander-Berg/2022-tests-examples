import React from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider, createRootReducer } from 'realty-core/view/react/libs/test-helpers';

import { OfferSerpSnippetActionPanel } from '..';

import { baseProps } from './mocks';

const rootReducer = createRootReducer({});
const render = ({ height = 500 }: { height?: number }) =>
    _render(
        <AppProvider rootReducer={rootReducer}>
            <OfferSerpSnippetActionPanel {...baseProps} />
        </AppProvider>,
        { viewport: { width: 320, height } }
    );

describe('OfferSerpSnippetActionPanel', () => {
    it('дефолтное состояние', async () => {
        await render({ height: 100 });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('состояние с кнопкой чатов', async () => {
        await render({ height: 100 });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
