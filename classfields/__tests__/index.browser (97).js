import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { Streets } from '..';

import { geo, getStreets, getFilteredStreets } from './mocks';

const commonProps = {
    geo,
    link: () => {}
};

const initialState = {
    config: {}
};

const Component = ({ streets }) => (
    <AppProvider initialState={initialState}>
        <Streets {...commonProps} streets={streets} />
    </AppProvider>
);

describe('Streets(touch)', () => {
    it('рисует список с пагинатором', async() => {
        await render(
            <Component streets={getStreets()} />,
            { viewport: { width: 320, height: 1000 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует список с выбранной буквой', async() => {
        await render(
            <Component streets={getFilteredStreets()} />,
            { viewport: { width: 320, height: 1000 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует компонент без фильтра', async() => {
        await render(
            <Component streets={{ ...getStreets(), letters: [ '3' ] }} />,
            { viewport: { width: 320, height: 1000 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует компонент без ссылок', async() => {
        await render(
            <Component streets={{ ...getStreets(), items: [], totalPages: 0 }} />,
            { viewport: { width: 320, height: 1000 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
