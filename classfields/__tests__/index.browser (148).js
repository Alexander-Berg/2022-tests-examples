import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { Street } from '..';

import { geo, sites as mockSites, links as mockLinks } from './mocks';
import styles from './styles.module.css';

const commonProps = {
    streetName: 'Ленинградский проспект',
    regionName: 'в Москве',
    geo,
    link: () => {}
};

const Component = ({ links = mockLinks, sites = mockSites }) => (
    <div className={styles.wrapper}>
        <AppProvider>
            <Street {...commonProps} sites={sites} links={links} />
        </AppProvider>
    </div>
);

describe('Street', () => {
    it('рисует полностью заполненный компонент на широком экране', async() => {
        await render(
            <Component />,
            { viewport: { width: 1280, height: 3000 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует полностью заполненный компонент на узком экране', async() => {
        await render(
            <Component />,
            { viewport: { width: 1000, height: 3000 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует компонент без ссылок', async() => {
        await render(
            <Component links={{}} />,
            { viewport: { width: 1000, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует компонент без карусели новостроек', async() => {
        await render(
            <Component sites={[]} />,
            { viewport: { width: 1000, height: 2800 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
