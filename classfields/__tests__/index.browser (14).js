import React from 'react';
import { createStore } from 'redux';
import { Provider } from 'react-redux';
import { render } from 'jest-puppeteer-react';

import legacyContext from '@realty-front/jest-utils/puppeteer/tests-helpers/legacy-context';
import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import TuzInformer from '../index';

const LinkContext = legacyContext({
    link: () => {}
});

const Component = ({ store = {}, ...props }) => {
    const providerStore = createStore(state => state, {
        ...store
    });

    return (
        <Provider store={providerStore}>
            <LinkContext>
                <TuzInformer {...props} />
            </LinkContext>
        </Provider>
    );
};

describe('TuzInformer', () => {
    it('By default without link', async() => {
        await render(<Component />, { viewport: { width: 550, height: 250 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Render with link', async() => {
        await render(<Component showLink />, { viewport: { width: 550, height: 250 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
