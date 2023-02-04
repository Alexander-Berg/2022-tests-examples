import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IOfferCard } from 'realty-core/types/offerCard';
import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { IOfferContact } from 'realty-core/types/phones';

import { OfferCardSummary } from '../index';

import { baseState, disabledOffer, offer, actionsRef, onActionsClick } from './mocks';

const Component: React.FC<{
    targetOffer: IOfferCard;
    isOwner: boolean;
    offerPhones?: Record<number | string, IOfferContact[]>;
}> = ({ targetOffer, isOwner, offerPhones = {} }) => (
    <AppProvider initialState={{ ...baseState, offerPhones }}>
        <OfferCardSummary
            site={undefined}
            offer={targetOffer}
            isOwner={isOwner}
            actionsRef={actionsRef}
            onActionsClick={onActionsClick}
        />
    </AppProvider>
);

describe('OfferCardSummary', function () {
    it('Базовая отрисовка', async () => {
        await render(<Component targetOffer={offer} isOwner={false} />, {
            viewport: { width: 400, height: 750 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка неактивного оффера', async () => {
        await render(<Component targetOffer={disabledOffer} isOwner={false} />, {
            viewport: { width: 400, height: 450 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка оффера для владельца без редактирования', async () => {
        const offerPhones = {
            [offer.offerId]: [
                {
                    shouldShowRedirectIndicator: false,
                    withBilling: false,
                    phones: [{ phoneNumber: '+74950000002' }],
                },
            ],
        };

        await render(<Component targetOffer={offer} isOwner offerPhones={offerPhones} />, {
            viewport: { width: 400, height: 650 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка оффера для владельца с редактированием', async () => {
        const offerPhones = {
            [offer.offerId]: [
                {
                    shouldShowRedirectIndicator: false,
                    withBilling: false,
                    phones: [{ phoneNumber: '+74950000002' }],
                },
            ],
        };

        await render(<Component targetOffer={{ ...offer, isEditable: true }} isOwner offerPhones={offerPhones} />, {
            viewport: { width: 400, height: 650 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
