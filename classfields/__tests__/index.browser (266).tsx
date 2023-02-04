import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { OffersMapSerpModalBaseInfo } from '../';

import { initialState } from './mocks';

describe('OffersMapSerpModalBaseInfo', () => {
    it('рендерится корректно', async () => {
        await render(
            <AppProvider initialState={initialState}>
                <OffersMapSerpModalBaseInfo
                    id="42"
                    title="Заголовок"
                    developerName="Застройщик"
                    linkUrl="http://test.com"
                    linkText={'Все квартиры от застройщика'}
                    badges={[
                        { id: 'first', name: 'Первый' },
                        { id: 'second', name: 'Второй' },
                    ]}
                    favoriteType="SITE"
                    onFavoritesToggle={() => null}
                />
            </AppProvider>,
            { viewport: { width: 400, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится корректно с длинными названиями', async () => {
        await render(
            <AppProvider initialState={initialState}>
                <OffersMapSerpModalBaseInfo
                    id="42"
                    title="Заголовок на две а может быть и больше строчек"
                    developerName="Застройщик с очень-очень-очень длинным названием"
                    linkUrl="http://test.com"
                    linkText={'Все квартиры от застройщика'}
                    badges={[
                        { id: 'first', name: 'Первый' },
                        { id: 'second', name: 'Второй' },
                        { id: 'third', name: 'Третий' },
                        { id: 'fourth', name: 'Червёртый' },
                        { id: 'fifth', name: 'Пятый' },
                    ]}
                    favoriteType="SITE"
                    onFavoritesToggle={() => null}
                />
            </AppProvider>,
            { viewport: { width: 400, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится корректно с длинными названиями без пробелов', async () => {
        await render(
            <AppProvider initialState={initialState}>
                <AppProvider initialState={initialState}>
                    <OffersMapSerpModalBaseInfo
                        id="42"
                        title={'Заголовок'.repeat(7)}
                        developerName={'Застройщик'.repeat(7)}
                        linkUrl="http://test.com"
                        linkText={'Все квартиры от застройщика'}
                        badges={[
                            { id: 'first', name: 'Первый' },
                            { id: 'second', name: 'Второй' },
                        ]}
                        favoriteType="SITE"
                        onFavoritesToggle={() => null}
                    />
                </AppProvider>
                ,
            </AppProvider>,
            { viewport: { width: 400, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
