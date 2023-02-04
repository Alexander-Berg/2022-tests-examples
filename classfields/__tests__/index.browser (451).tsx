import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import OffersSearchPage from '../index';

import { initialStore } from './mock';

const renderComponentWithInitState = async (initState: Record<string, unknown>) => {
    return await render(
        <AppProvider initialState={initState} context={{ link: () => '' }}>
            <OffersSearchPage
                loadAdsByPriority={() => ({})}
                seoTexts={{
                    extraText: 'Данный текст должен рендерится как можно ближе к h1 для улучшения индексации',
                }}
            />
        </AppProvider>,
        { viewport: { width: 1300, height: 400 } }
    );
};

describe('В разметке h1 и seoText рендерятся над другими элементами страницы', () => {
    it('рендрер для типа страницы "Самолет"', async () => {
        await renderComponentWithInitState(initialStore);

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('рендер для страницы без промо', async () => {
        const store = {
            ...initialStore,
            siteSpecialProjectSecondPackage: null,
        };

        await renderComponentWithInitState(store);

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
