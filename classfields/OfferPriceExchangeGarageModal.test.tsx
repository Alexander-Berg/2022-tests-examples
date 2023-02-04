import React from 'react';
import _ from 'lodash';
import { render, screen } from '@testing-library/react';

import { CardTypeInfo_CardType } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/garage/garage_api_model';

import garageCardMock from 'auto-core/models/garageCard/mocks/mockChain';

import OfferPriceExchangeGarageModal from './OfferPriceExchangeGarageModal';

describe('OfferPriceExchangeGarageModal', () => {
    it('рендерит только CURRENT_CAR', async() => {
        const CARDS = [
            garageCardMock.withMarkInfoName('BMW').withModelInfoName('X5').withCardType(CardTypeInfo_CardType.CURRENT_CAR).value(),
            garageCardMock.withMarkInfoName('BMW').withModelInfoName('X3').withCardType(CardTypeInfo_CardType.CURRENT_CAR).value(),
            garageCardMock.withMarkInfoName('Audi').withModelInfoName('A5').withCardType(CardTypeInfo_CardType.DREAM_CAR).value(),
            garageCardMock.withMarkInfoName('Audi').withModelInfoName('A4').withCardType(CardTypeInfo_CardType.EX_CAR).value(),
        ].map((card, index) => {
            card.id = String(index);
            return card;
        });

        render(
            <OfferPriceExchangeGarageModal
                visible
                selected="123"
                onRequestHide={ _.noop }
                onSelect={ _.noop }
                cards={ CARDS }
            />,
        );

        const bmwX5 = await screen.queryByText('BMW X5');
        const bwmX3 = await screen.queryByText('BMW X3');
        const audiA5 = await screen.queryByText('AUDI A5');
        const audiA4 = await screen.queryByText('AUDI A4');

        expect(bmwX5).toBeTruthy();
        expect(bwmX3).toBeTruthy();
        expect(audiA5).not.toBeTruthy();
        expect(audiA4).not.toBeTruthy();
    });
});
