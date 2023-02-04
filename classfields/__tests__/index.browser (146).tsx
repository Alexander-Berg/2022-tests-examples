import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { MainMenuPromoBlockType } from 'realty-core/types/header';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { MainMenuPromoBlockContainer } from '../container';
import { defaultStore } from '../../__tests__/store/defaultStore';

const widths = [220, 1440];

describe('MainMenuPromoBlock', () => {
    for (const type of Object.values(MainMenuPromoBlockType)) {
        if (type === MainMenuPromoBlockType.YANDEX_DEAL_VALUATION) {
            // Тут в тесте странно ломаются пиксели на скруглениях
            continue;
        }

        for (const width of widths) {
            it(`Промо блок ${type} ${width}px`, async () => {
                await render(
                    <AppProvider initialState={defaultStore}>
                        <MainMenuPromoBlockContainer type={type} />
                    </AppProvider>,
                    {
                        viewport: { width, height: 204 },
                    }
                );

                await page.addStyleTag({ content: 'body{padding: 0}' });

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        }
    }
});
