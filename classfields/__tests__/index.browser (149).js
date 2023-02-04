import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { Streets } from '..';

import { geo, getStreets, getFilteredStreets } from './mocks';

const commonProps = {
    geo,
    link: () => {}
};

describe('Streets', () => {
    it('рисует список с пагинатором', async() => {
        await render(
            <Streets {...commonProps} streets={getStreets()} />,
            { viewport: { width: 1000, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует список с выбранной буквой', async() => {
        await render(
            <Streets {...commonProps} streets={getFilteredStreets()} />,
            { viewport: { width: 1000, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует компонент без фильтра', async() => {
        await render(
            <Streets {...commonProps} streets={{ ...getStreets(), letters: [ '3' ] }} />,
            { viewport: { width: 1000, height: 360 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует компонент без ссылок', async() => {
        await render(
            <Streets {...commonProps} streets={{ ...getStreets(), items: [] }} />,
            { viewport: { width: 1000, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
