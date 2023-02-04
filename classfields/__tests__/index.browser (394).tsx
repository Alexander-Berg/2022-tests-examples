import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IOfferCard } from 'realty-core/types/offerCard';

import { AppProvider } from 'view/react/libs/test-helpers';

import { OfferCardSummaryHeader } from '../index';

import {
    baseState,
    hourBeforeCreatedOffer,
    hourBeforeEditedOffer,
    offer,
    offerWithoutViews,
    onActionsClick,
    stateWithOfferInFavorites,
    yesterdayCreatedOffer,
    yesterdayEditedOffer,
    CURRENT_DATE_TIME,
} from './mocks';

const Component: React.FC<{ targetOffer: IOfferCard; state?: Record<string, unknown> }> = ({
    targetOffer,
    state = baseState,
}) => (
    <AppProvider
        initialState={state}
        fakeTimers={{
            now: new Date(CURRENT_DATE_TIME).getTime(),
        }}
    >
        <OfferCardSummaryHeader offer={targetOffer} onActionsClick={onActionsClick} />
    </AppProvider>
);

describe('OfferCardSummaryHeader', function () {
    it('Базовая отрисовка', async () => {
        await render(<Component targetOffer={offer} />, {
            viewport: { width: 400, height: 150 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка с оффером в избранном', async () => {
        await render(<Component state={stateWithOfferInFavorites} targetOffer={offer} />, {
            viewport: { width: 400, height: 150 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка отредактированного час назад оффера', async () => {
        await render(<Component targetOffer={hourBeforeEditedOffer} />, {
            viewport: { width: 400, height: 150 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка отредактированного день назад оффера', async () => {
        await render(<Component targetOffer={yesterdayEditedOffer} />, {
            viewport: { width: 400, height: 150 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка созданного час назад оффера', async () => {
        await render(<Component targetOffer={hourBeforeCreatedOffer} />, {
            viewport: { width: 400, height: 150 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка созданного день назад оффера', async () => {
        await render(<Component targetOffer={yesterdayCreatedOffer} />, {
            viewport: { width: 400, height: 150 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Если просмотры не переданы, то не пишем информацию о них', async () => {
        await render(<Component targetOffer={offerWithoutViews} />, {
            viewport: { width: 400, height: 150 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
