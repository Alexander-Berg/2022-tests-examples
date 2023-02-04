jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn().mockImplementation(() => {
        return Promise.resolve(() => {
            setTimeout(() => ({ status: 'SUCCESS' }), 100);
        });
    }),
}));

import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';
import _ from 'lodash';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import { getBunkerMock } from 'autoru-frontend/mockData/state/bunker.mock';

import offer from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import DealerCreditUserForm from 'auto-core/react/components/common/DealerCreditUserForm/DealerCreditUserForm';
import { getResource } from 'auto-core/react/lib/gateApi';

import DealerCreditForm from './DealerCreditForm';

const ContextProvider = createContextProvider(contextMock);

const defaultProps = {
    offer: cloneOfferWithHelpers(offer).withSalon().withDealerCredit().value(),
    isMobile: false,
    isAuth: true,
};

it('не должен послать второй запрос на заявку, если не вернулся первый', () => {
    const tree = shallowRenderComponent();

    const bankForm = tree.find(DealerCreditUserForm);

    bankForm.simulate('submit');
    bankForm.simulate('submit');

    expect(getResource).toHaveBeenCalledTimes(1);
});

it('должен показать шторку с айфреймом еКредита после отправления заявки, если у дилера подключен еКредит', () => {
    const tree = shallowRenderComponent();
    const bankForm = tree.find(DealerCreditUserForm);

    bankForm.simulate('submit');

    expect(tree.state('isCurtainOpened')).toEqual(true);
});

it('не должен показать шторку с айфреймом еКредита после отправления заявки, если у дилера не подключен еКредит', () => {
    const offer = _.cloneDeep(defaultProps.offer);
    offer.dealer_credit_config!.EXTERNAL_INTEGRATIONS![0].enabled = false;

    const tree = shallowRenderComponent({ ...defaultProps, offer });
    const bankForm = tree.find(DealerCreditUserForm);

    bankForm.simulate('submit');

    expect(tree.state('isCurtainOpened')).toEqual(false);
});

it('не должен показать шторку с айфреймом еКредита после отправления заявки, если у дилера подключен еКредит и новый тег', () => {
    const offer = _.cloneDeep(defaultProps.offer);
    offer.dealer_credit_config!.EXTERNAL_INTEGRATIONS![0].tags = [ 'SEND_TO_ECREDIT_API' ];

    const tree = shallowRenderComponent({ ...defaultProps, offer });
    const bankForm = tree.find(DealerCreditUserForm);

    bankForm.simulate('submit');

    expect(tree.state('isCurtainOpened')).toEqual(false);
});

function shallowRenderComponent(props = defaultProps) {
    const store = mockStore({
        bunker: getBunkerMock([ 'common/dealers_with_multiple_locations' ]),
        user: { data: {} },
    });
    return shallow(
        <ContextProvider>
            <Provider store={ store }>
                <DealerCreditForm { ...props }/>
            </Provider>
        </ContextProvider>,
    ).dive().dive().dive();
}
