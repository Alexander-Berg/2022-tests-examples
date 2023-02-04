import React from 'react';
import { render } from 'jest-puppeteer-react';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import ItemBuilding from '../index';

import { Router } from './mocks/router';
import { initialState } from './mocks/initialState';

describe('Item building', () => {
    it('Инфа о коттеджном поселке', async () => {
        const props = {
            location: {
                populatedRgid: 587683,
                rgid: 587683,
            },
            item: {
                village: {
                    name: 'Бавария CLUB',
                    id: 'village-id',
                },
            },
            extraLinkParams: {},
            source: {},
        };

        await render(
            // eslint-disable-next-line @typescript-eslint/no-empty-function
            <AppProvider initialState={initialState} context={{ observeIntersection: () => {}, link: Router.link }}>
                <ItemBuilding {...props} />
            </AppProvider>,
            { viewport: { width: 400, height: 1040 } }
        );

        const href = await page.evaluate(() => document.querySelector('.Link')?.getAttribute('href'));
        const textContent = await page.evaluate(() => document.querySelector('.Link')?.innerHTML.toString());
        expect(href).toBe(`/${props.item.village.name}/`);
        expect(textContent).toBe(`КП «${props.item.village.name}»`);
    });
});
