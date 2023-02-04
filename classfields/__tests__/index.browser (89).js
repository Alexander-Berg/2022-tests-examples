import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SitePlansGroupedList } from '..';

import { getRooms, getPlans, getCard, getInitialState } from './mocks';

describe('SitePlansGroupedList', () => {
    it('рисует схлопнутое состояние', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansGroupedList
                    card={getCard()}
                    rooms={getRooms()}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует схлопнутое состояние (апартаменты)', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansGroupedList
                    card={getCard({ isApartment: true })}
                    rooms={getRooms()}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует загрузку комнатности', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansGroupedList
                    rooms={[]}
                    areStatsLoading
                />
            </AppProvider>,
            { viewport: { width: 360, height: 120 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует раскрытую комнатность без пагинатора', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansGroupedList
                    selectedRoomType={3}
                    card={getCard()}
                    rooms={getRooms()}
                    plans={getPlans({ withoutPager: true })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 900 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует раскрытую комнатность с пагинатором', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansGroupedList
                    selectedRoomType={3}
                    card={getCard()}
                    rooms={getRooms()}
                    plans={getPlans()}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 1000 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует раскрытую комнатность с пагинатором (последняя страница)', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansGroupedList
                    selectedRoomType={3}
                    card={getCard()}
                    rooms={getRooms()}
                    plans={getPlans({ page: 2 })}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 1000 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует раскрытую комнатность в состоянии загрузки', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansGroupedList
                    selectedRoomType={3}
                    card={getCard()}
                    rooms={getRooms()}
                    plans={{ items: [] }}
                    arePlansLoading
                />
            </AppProvider>,
            { viewport: { width: 360, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует раскрытую комнатность в состоянии загрузки (повторная)', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansGroupedList
                    selectedRoomType={3}
                    card={getCard()}
                    rooms={getRooms()}
                    plans={getPlans()}
                    arePlansLoading
                />
            </AppProvider>,
            { viewport: { width: 360, height: 1000 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует ошибку загрузки раскрытой комнатности', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansGroupedList
                    selectedRoomType={3}
                    card={getCard()}
                    rooms={getRooms()}
                    plans={getPlans()}
                    arePlansFailed
                />
            </AppProvider>,
            { viewport: { width: 360, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует ошибку загрузки статистики', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansGroupedList
                    selectedRoomType={3}
                    card={getCard()}
                    rooms={getRooms()}
                    plans={getPlans()}
                    areStatsFailed
                />
            </AppProvider>,
            { viewport: { width: 360, height: 280 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует пустую выдачу', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SitePlansGroupedList
                    rooms={[]}
                />
            </AppProvider>,
            { viewport: { width: 360, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
