import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { Street } from '..';

import { sites as mockSites, links as mockLinks, links2 as mockLinks2, geo } from './mocks';

const commonProps = {
    streetName: 'Ленинградский проспект',
    regionName: 'в Москве',
    geo,
    link() {}
};

const initialState = {
    config: {}
};

const Component = ({ links = mockLinks, sites = mockSites }) => (
    <AppProvider initialState={initialState}>
        <Street {...commonProps} sites={sites} links={links} />
    </AppProvider>
);

describe('Street(touch)', () => {
    it('рисует полностью заполненный компонент', async() => {
        await render(<Component />, { viewport: { width: 500, height: 4800 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует компонент без ссылок', async() => {
        await render(<Component links={{}} />, { viewport: { width: 500, height: 400 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует компонент без карусели новостроек', async() => {
        await render(<Component sites={[]} />, { viewport: { width: 500, height: 4500 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует компонент с одной новостройкой', async() => {
        await render(<Component sites={mockSites.slice(0, 1)} />, { viewport: { width: 500, height: 4800 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует мало ссылок', async() => {
        await render(<Component links={mockLinks2} sites={[]} />, { viewport: { width: 500, height: 500 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
