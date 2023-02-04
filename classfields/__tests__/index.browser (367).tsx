import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IOfferCard } from 'realty-core/types/offerCard';
import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { IOfferContact } from 'realty-core/types/phones';

import { OfferCardBottomBlock } from '../index';

import { baseState, disabledOffer, offer, actionsRef, onActionsClick } from './mocks';

const Component: React.FC<{
    targetOffer: IOfferCard;
    isOwner?: boolean;
    offerPhones?: Record<number | string, IOfferContact[]>;
    experiments?: string[];
}> = ({ targetOffer, offerPhones = {}, isOwner = false, experiments }) => (
    <AppProvider initialState={{ ...baseState, offerPhones }} experiments={experiments}>
        <OfferCardBottomBlock
            offer={targetOffer}
            isOwner={isOwner}
            actionsRef={actionsRef}
            onActionsClick={onActionsClick}
        />
    </AppProvider>
);

describe('OfferCardBottomBlock', function () {
    it('Базовая отрисовка', async () => {
        await render(<Component targetOffer={offer} />, {
            viewport: { width: 900, height: 150 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Базовая отрисовка для владельца', async () => {
        const offerPhones = {
            [offer.offerId]: [
                {
                    shouldShowRedirectIndicator: false,
                    withBilling: false,
                    phones: [{ phoneNumber: '+79991150830' }],
                },
            ],
        };

        await render(<Component targetOffer={{ ...offer, isEditable: true }} isOwner offerPhones={offerPhones} />, {
            viewport: { width: 900, height: 150 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка неактивного оффера', async () => {
        await render(<Component targetOffer={disabledOffer} />, {
            viewport: { width: 900, height: 150 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
