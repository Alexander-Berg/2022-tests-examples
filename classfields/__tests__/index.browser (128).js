import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { getCases } from 'realty-core/view/react/modules/filters/common/__tests__/test-cases';

import MapSidebarv3 from 'view/react/modules/map-serp/deskpad/MapSidebarv3';
import { AppProvider } from 'view/react/libs/test-helpers';

import reducer from 'view/react/deskpad/reducers/roots/common';

// Нужно подключить их тут, так как они есть только в OffersSearchFilters
import 'realty-core/view/react/modules/filters/common/FiltersForm/view/cols.css';
import 'realty-core/view/react/modules/filters/common/FiltersForm/layout/vertical.css';
import 'realty-core/view/react/modules/filters/common/FiltersForm/view/cols-vertical.css';
import '../../filtersv3.css';

import OffersMapSearchFilters from '..';

import { getInitialState } from './mocks';

const renderComponent = ({ state, height }) => render(
    <AppProvider initialState={getInitialState(state)} rootReducer={reducer}>
        <div style={{ backgroundColor: 'grey', position: 'relative', height: `${height}px` }}>
            <MapSidebarv3
                tabs={{
                    filters: () => (
                        <OffersMapSearchFilters
                            withSearchHistory={false}
                            isAllowedToUpdateCounter={false}
                            isAllowedToInstantSubmit={false}
                        />
                    )
                }}
                activeTab='filters'
            />
        </div>
    </AppProvider>,
    { viewport: { width: 500, height } }
);

describe('OffersMapSearchFilters', () => {
    getCases('map').forEach(([ name, state ]) => it(name, async() => {
        await renderComponent({ state, height: 2300 });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    }));

    it('Взаимодействие с селектом в домах', async() => {
        const state = {
            ctype: 'SELL',
            category: 'HOUSE',
            houseType: []
        };

        await renderComponent({ state: { forms: state }, height: 500 });

        await page.click('.FormField_name_houseType');
        expect(await takeScreenshot()).toMatchImageSnapshot();

        /* eslint-disable */
        for (const i of [1, 2, 3, 4]) {
            await page.click(`.Menu__item_mode_check:nth-child(${i})`);
            expect(await takeScreenshot()).toMatchImageSnapshot();
        }
        /* eslint-enable */

        await page.click('.FormField_name_houseType');
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Переключение из квартир в коммерческую', async() => {
        const state = {
            ctype: 'SELL',
            category: 'APARTMENTS'
        };

        await renderComponent({ state: { forms: state }, height: 500 });

        await page.click('.FormField_name_category');
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click('.Menu__item_mode_radio:nth-child(6)');
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Взаимодействие с селектом в коммерческой', async() => {
        const state = {
            ctype: 'SELL',
            category: 'COMMERCIAL'
        };

        await renderComponent({ state: { forms: state }, height: 550 });

        await page.click('.FormField_name_commercialType');
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click('.Menu__item_mode_check:nth-child(5)');
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click('.Menu__item_mode_check:nth-child(4)');
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click('.FormField_name_commercialType');
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
