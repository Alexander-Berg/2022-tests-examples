import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

import { ParticipantType } from '@vertis/schema-registry/ts-types-snake/vertis/safe_deal/common';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import safeDealMock from 'auto-core/react/dataDomain/safeDeal/mocks/safeDeal.mock';
import type { PaymentData } from 'auto-core/react/components/common/PageDeal/PageDealForm/DealPaymentSection/DealBankDetails/DealBankDetails';

import { BuyerStep } from 'auto-core/proto/compiled/vertis/safe_deal/common';

import DealPaymentSection from './DealPaymentSection';

const Context = createContextProvider(contextMock);

it('должен правильно рассчитать оставшуюся сумму для оплаты', () => {
    safeDealMock.deal.party!.buyer!.total_provided_rub = '100000';
    const wrapper = shallow(
        <Context>
            <Provider store={ mockStore({ safeDeal: safeDealMock }) }>
                <DealPaymentSection
                    dealId="123"
                    onUpdate={ () => Promise.resolve() }
                    dealPageType={ ParticipantType.BUYER.toLowerCase() }
                    role={ ParticipantType.BUYER }
                    step={ BuyerStep.BUYER_PROVIDING_MONEY }
                    isMobile
                    formStep="money"
                />
            </Provider>
        </Context>,
    ).dive().dive().dive();
    const props = wrapper.find('DealBankDetails').prop('paymentData') as PaymentData;

    expect(props.remainingPrice).toEqual(9554);
});
