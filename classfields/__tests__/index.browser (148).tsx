import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import MapHousingObjects from '../';

import { housingList } from './mocks';

interface IItem {
    id: string;
    quarter?: number;
    year?: number;
    color: string;
}

const renderComponent = (list: IItem[]) =>
    render(
        <div style={{ backgroundColor: '#000', width: 700, height: 60, position: 'relative' }}>
            <MapHousingObjects list={list} controller="newbuilding" />
        </div>,
        { viewport: { width: 700, height: 100 } }
    );

describe('MapHousingObjects', () => {
    it('рендерится в дефолтном состоянии', async () => {
        await renderComponent(housingList.slice(0, 4));

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится корректно с переполнением', async () => {
        await renderComponent(housingList);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('не рендерит пустой список', async () => {
        await renderComponent([]);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
