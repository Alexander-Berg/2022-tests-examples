import React, { useState } from 'react';
import _ from 'lodash';
import { render, screen } from '@testing-library/react';

import { CardTypeInfo_CardType } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/garage/garage_api_model';

import garageCardMock from 'auto-core/models/garageCard/mocks/mockChain';
import type { Card } from 'auto-core/types/proto/auto/api/vin/garage/garage_api_model';

import OfferPriceExchangeGaragePopup from './OfferPriceExchangeGaragePopup';

const Test = ({ cards }: { cards: Array<Card> }) => {
    const [ anchor, setAnchor ] = useState<HTMLDivElement | null>(null);
    return (
        <div>
            <div ref={ setAnchor }>Еще</div>
            <OfferPriceExchangeGaragePopup
                anchor={ anchor }
                cards={ cards }
                onRequestHide={ _.noop }
                onSelect={ _.noop }
                selected="123"
            />
        </div>
    );
};

describe('OfferPriceExchangeGaragePopup', () => {
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

        render(<Test cards={ CARDS }/>);

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
