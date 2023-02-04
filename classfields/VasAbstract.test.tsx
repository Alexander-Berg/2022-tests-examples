jest.mock('auto-core/react/dataDomain/state/actions/paymentModalOpen');

import type { ThunkDispatch } from 'redux-thunk';
import { Provider } from 'react-redux';
import type { Action } from 'redux';
import React from 'react';
import { shallow } from 'enzyme';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import paymentModalOpen from 'auto-core/react/dataDomain/state/actions/paymentModalOpen';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import { TBillingFrom } from 'auto-core/types/TBilling';
import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';
import type { Offer, TOfferCategory, TOfferSubCategory } from 'auto-core/types/proto/auto/api/api_offer_model';

import VasAbstract from './VasAbstract';

let props: Props;

const paymentModalOpenMock = paymentModalOpen as jest.MockedFunction<typeof paymentModalOpen>;
paymentModalOpenMock.mockReturnValue((() => () => { }) as unknown as Action);

beforeEach(() => {
    props = {
        offer: cloneOfferWithHelpers(offerMock)
            .withActiveVas()
            .withCategory('moto' as TOfferCategory)
            .withSubCategory('scooters' as TOfferSubCategory)
            .value(),
        params: {
            from: TBillingFrom.MOBILE_CARD,
        },
        pageType: 'card',
        dispatch: jest.fn(),
    };

    contextMock.logVasEvent.mockClear();
    contextMock.metrika.sendParams.mockClear();
});

describe('при клике на кнопку "купить" на карточке', () => {
    beforeEach(() => {
        const instance = shallowRenderComponent({ props });
        instance.handleBuyServiceClick(TOfferVas.VIP);
    });

    it('откроет модал оплаты', () => {
        expect(paymentModalOpenMock).toHaveBeenCalledTimes(1);
        expect(paymentModalOpenMock.mock.calls[0]).toMatchSnapshot();
    });

    it('залогирует клик', () => {
        expect(contextMock.logVasEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.logVasEvent.mock.calls[0]).toMatchSnapshot();
    });
});

describe('при клике на кнопку "купить" в лк', () => {
    beforeEach(() => {
        props.pageType = 'sales';
        const instance = shallowRenderComponent({ props });
        instance.handleBuyServiceClick(TOfferVas.PLACEMENT);
    });

    it('откроет модал оплаты', () => {
        expect(paymentModalOpenMock).toHaveBeenCalledTimes(1);
        expect(paymentModalOpenMock.mock.calls[0]).toMatchSnapshot();
    });
});

interface Props {
    offer: Offer;
    params: {
        from: TBillingFrom;
    };
    pageType: string;
    dispatch: () => void;
}

function shallowRenderComponent({ props }: { props: Props }) {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore({});

    class ComponentMock extends VasAbstract<Props & { dispatch: ThunkDispatch<any, any, any>}, unknown> {
        render() {
            return <div>Buy</div>;
        }
    }

    const page = shallow<ComponentMock>(
        <ContextProvider>
            <Provider store={ store }>
                <ComponentMock { ...props }/>
            </Provider>
        </ContextProvider>,
    );

    return page.dive().dive().instance() as ComponentMock;
}
