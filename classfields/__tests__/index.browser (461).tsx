import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SamoletMenu, ISamoletMenuProps } from '../';
import styles from '../styles.module.css';

import { items, supportedRgids, phone, rgid, onChangeGeo, navigate, link } from './mocks';

const props = { items, phone, navigate, link, supportedRgids, rgid, onChangeGeo } as ISamoletMenuProps;

const widths = [[1100], [1400]];

describe('SamoletMenu', () => {
    it.each(widths)('рендерится корректно в разрешении %d', async (width) => {
        await render(
            <AppProvider initialState={{}}>
                <SamoletMenu {...props} />
            </AppProvider>,
            { viewport: { width, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('гео селектор', async () => {
        await render(
            <AppProvider initialState={{}}>
                <SamoletMenu {...props} />
            </AppProvider>,
            { viewport: { width: 1400, height: 400 } }
        );

        await page.click(`.${styles.geoSelector}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.hover(`.${styles.geoSelectorPopupItem}:nth-child(3)`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });
});
