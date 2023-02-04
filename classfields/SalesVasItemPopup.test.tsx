import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';
import MockDate from 'mockdate';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import getServiceInfoMerged from 'auto-core/react/lib/offer/getServiceInfoMerged';

import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';

import SalesVasItemPopup from './SalesVasItemPopup';

const defaultOffer = cloneOfferWithHelpers(offerMock)
    .withActiveVas([])
    .withCreationDate('1640258052084')
    .value();

beforeEach(() => {
    MockDate.set('2021-12-31');
});

it('откроет модал оплаты если юзер кликнет на цену', () => {
    const ContextProvider = createContextProvider(contextMock);

    const mockOnBuyService = jest.fn();

    const page = shallow(
        <ContextProvider>
            <SalesVasItemPopup
                offer={ defaultOffer }
                onAutoProlongationToggle={ jest.fn() }
                onAutoRenewChange={ jest.fn() }
                service={ getServiceInfoMerged(defaultOffer, TOfferVas.VIP) }
                onOfferLoadStats={ jest.fn() }
                onBuyService={ mockOnBuyService }
                hasTiedCards
            >
                <div className="anchorClass">anchor</div>
            </SalesVasItemPopup>
        </ContextProvider>,
    ).dive().dive();

    page.find('.SalesVasItemPopup__button').simulate('click');

    // проп вызывается без аргументов. Аргументы проставляются в родительском компоненте
    expect(mockOnBuyService).toHaveBeenCalled();
});

it('при наведении на попап подгрузит стату, если ее нет', () => {
    const ContextProvider = createContextProvider(contextMock);

    const mockOnOfferLoadStats = jest.fn();

    const page = shallow(
        <ContextProvider>
            <SalesVasItemPopup
                offer={ defaultOffer }
                onAutoProlongationToggle={ jest.fn() }
                onAutoRenewChange={ jest.fn() }
                service={ getServiceInfoMerged(defaultOffer, TOfferVas.VIP) }
                onOfferLoadStats={ mockOnOfferLoadStats }
                onBuyService={ jest.fn() }
                hasTiedCards
            >
                <div className="anchorClass">anchor</div>
            </SalesVasItemPopup>
        </ContextProvider>,
    ).dive();

    page.find('InfoPopup').simulate('showPopup');

    expect(mockOnOfferLoadStats).toHaveBeenCalledWith({
        category: 'cars',
        creationDate: '1640258052084',
        offerId: '1085562758-1970f439',
    });
});
