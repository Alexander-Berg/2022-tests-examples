import React from 'react';
import { createStore } from 'redux';
import { Provider } from 'react-redux';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import OnlineShowPanel from '../index';

const defaultState = {
    offerForm: {
        onlineShowPossible: false
    }
};

const makeComponent = (initialState = defaultState) => {
    const providerStore = createStore(state => state, initialState);

    return () => (
        <Provider store={providerStore}>
            <OnlineShowPanel />
        </Provider>
    );
};

describe('OnlineShowPanel', () => {
    it('Render by default', async() => {
        const Component = makeComponent();

        await render(<Component />, { viewport: { width: 930, height: 260 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Render on small screen by default', async() => {
        const Component = makeComponent();

        await render(<Component />, { viewport: { width: 740, height: 260 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Render with checked state', async() => {
        const Component = makeComponent({
            offerForm: {
                onlineShowPossible: true
            }
        });

        await render(<Component />, { viewport: { width: 930, height: 260 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
