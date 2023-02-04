import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { AnyObject } from 'realty-core/types/utils';

import { SearchCategories } from '../index';

import {
    initialState,
    initialStateWithoutLinks,
    initialStateWithoutNewbuildings,
    initialStateWithoutNewbuildingsAndWithFewLinks,
    initialStateWithOneNewbuilding,
} from './mocks';

const DEFAULT_WIDTH = 1000;
const DEFAULT_HEIGHT = 800;

const renderComponent = (store: AnyObject, width = DEFAULT_WIDTH, height = DEFAULT_HEIGHT) => {
    return render(
        <AppProvider initialState={store}>
            <SearchCategories />
        </AppProvider>,
        { viewport: { width: width, height: height } }
    );
};

describe('SearchCategories', function () {
    it('рисует полностью заполненный компонент на узком экране', async () => {
        await renderComponent(initialState);
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует полностью заполненный компонент на широком экране', async () => {
        await renderComponent(initialState, 1280, 800);
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует компонент без карусели новостроек', async () => {
        await renderComponent(initialStateWithoutNewbuildings);
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует компонент с одной новостройкой', async () => {
        await renderComponent(initialStateWithOneNewbuilding);
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует мало ссылок', async () => {
        await renderComponent(initialStateWithoutNewbuildingsAndWithFewLinks);
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рисует без ссылок', async () => {
        await renderComponent(initialStateWithoutLinks);
        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
