import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/react/libs/test-helpers';

import SitePlansGroupedList from '..';

import { getRooms, getPlans } from './mocks';

describe('SitePlansGroupedList', () => {
    it('рисует схлопнутое состояние', async() => {
        await render(
            <SitePlansGroupedList
                rooms={getRooms()}
                apartmentsType="FLATS"
            />,
            { viewport: { width: 840, height: 350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует ховер (1 элемент)', async() => {
        await render(
            <SitePlansGroupedList
                rooms={getRooms()}
                apartmentsType="FLATS"
            />,
            { viewport: { width: 840, height: 350 } }
        );

        // eslint-disable-next-line no-undef
        await page.hover('.SitePlansGroupedList__row');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует загрузку комнатности', async() => {
        await render(
            <SitePlansGroupedList
                rooms={[]}
                areStatsLoading
                apartmentsType="FLATS"
            />,
            { viewport: { width: 840, height: 70 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует планировки без пагинатора', async() => {
        await render(
            <AppProvider>
                <SitePlansGroupedList
                    selectedRoomType={3}
                    rooms={getRooms()}
                    plans={getPlans({ withoutPager: true })}
                    apartmentsType="FLATS"
                />
            </AppProvider>,
            { viewport: { width: 840, height: 1350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует планировки без пагинатора (апартаменты)', async() => {
        await render(
            <AppProvider>
                <SitePlansGroupedList
                    selectedRoomType={3}
                    rooms={getRooms()}
                    plans={getPlans({ withoutPager: true })}
                    apartmentsType="APARTMENTS"
                />
            </AppProvider>,
            { viewport: { width: 840, height: 1350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует планировки без пагинатора (смешанная выдача)', async() => {
        await render(
            <AppProvider>
                <SitePlansGroupedList
                    selectedRoomType={3}
                    rooms={getRooms()}
                    plans={getPlans({ withoutPager: true })}
                    apartmentsType="APARTMENTS_AND_FLATS"
                />
            </AppProvider>,
            { viewport: { width: 840, height: 1350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует планировки c пагинатором', async() => {
        await render(
            <AppProvider>
                <SitePlansGroupedList
                    selectedRoomType={3}
                    rooms={getRooms()}
                    plans={getPlans()}
                    apartmentsType="FLATS"
                />
            </AppProvider>,
            { viewport: { width: 840, height: 1350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует планировки c пагинатором (2 страница)', async() => {
        await render(
            <AppProvider>
                <SitePlansGroupedList
                    selectedRoomType={3}
                    rooms={getRooms()}
                    plans={getPlans({ page: 2 })}
                    apartmentsType="FLATS"
                />
            </AppProvider>,
            { viewport: { width: 840, height: 1350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует загрузку планировок', async() => {
        await render(
            <AppProvider>
                <SitePlansGroupedList
                    selectedRoomType={3}
                    rooms={getRooms()}
                    plans={{ items: [] }}
                    arePlansLoading
                    apartmentsType="FLATS"
                />
            </AppProvider>,
            { viewport: { width: 840, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует загрузку планировок (повторная)', async() => {
        await render(
            <AppProvider>
                <SitePlansGroupedList
                    selectedRoomType={3}
                    rooms={getRooms()}
                    plans={getPlans()}
                    arePlansLoading
                    apartmentsType="FLATS"
                />
            </AppProvider>,
            { viewport: { width: 840, height: 1350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует ошибку загрузки планировок', async() => {
        await render(
            <AppProvider>
                <SitePlansGroupedList
                    selectedRoomType={3}
                    rooms={getRooms()}
                    plans={getPlans()}
                    arePlansFailed
                    apartmentsType="FLATS"
                />
            </AppProvider>,
            { viewport: { width: 840, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует ошибку загрузки статистики', async() => {
        await render(
            <AppProvider>
                <SitePlansGroupedList
                    selectedRoomType={3}
                    rooms={getRooms()}
                    plans={getPlans()}
                    areStatsFailed
                    apartmentsType="FLATS"
                />
            </AppProvider>,
            { viewport: { width: 840, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует пустую выдачу', async() => {
        await render(
            <SitePlansGroupedList
                rooms={[]}
                apartmentsType="FLATS"
            />,
            { viewport: { width: 840, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
