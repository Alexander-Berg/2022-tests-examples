jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn().mockResolvedValue([]),
    };
});

import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';
import { cloneDeep } from 'lodash';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';

import creditProduct from 'auto-core/react/dataDomain/credit/mocks/creditProduct.mock';
import banksData from 'auto-core/react/dataDomain/credit/mocks/banksData.mock';
import configMock from 'auto-core/react/dataDomain/config/mocks/config';
import cardMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import type { StateCreditProducts, TStateCredit } from 'auto-core/react/dataDomain/credit/TStateCredit';
import type { StateConfig } from 'auto-core/react/dataDomain/config/StateConfig';
import type { TStateListing } from 'auto-core/react/dataDomain/listing/TStateListing';

import type { TOfferListing } from 'auto-core/types/TOfferListing';
import type { Bank, CreditProduct } from 'auto-core/types/TCreditBroker';
import type {
    CreditCalculatorConditions,
    CreditCalculatorData,
    CreditCalculatorInitialData,
    CreditUserFormData,
} from 'auto-core/types/TCredit';

import CreditApplicationForm from './CreditApplicationForm';

type State = {
    config: StateConfig;
    credit: TStateCredit;
    listing?: TStateListing;
};

interface Props {
    fullName?: string;
    email?: string;
    banks: Array<Bank>;
    products: Array<CreditProduct>;
    isMobile?: boolean;
    className?: string;
    showHeader?: boolean;
    pageType?: string;
    conditions: CreditCalculatorConditions;
    initialData: CreditCalculatorInitialData;
    isPromo?: boolean;
    isSubmitting?: boolean;
    renderAdditionalInfo?: () => JSX.Element | string | null;
    onFormSubmit: (data: CreditUserFormData) => void;
    onCalculatorChange: (data: CreditCalculatorData) => void;
    onSubmitButtonClick?: () => void;
}

const defaultStore = {
    listing: {
        data: {
            search_parameters: {
                on_credit: true,
                credit_initial_fee: 0,
            },
        } as TOfferListing,
    } as TStateListing,
    credit: {
        banks: {
            data: banksData,
        },
        products: {
            data: {
                credit_products: [ creditProduct ],
                banks: banksData,
            },
        } as StateCreditProducts,
        productCalculator: {
            data: creditProduct,
        },
    } as TStateCredit,
};

const user = { email: 'test@test-mail.ru', name: 'Ivan', phone: '79002221133' };

it('должен отправить 2 цели на главной странице', () => {
    const config = cloneDeep(configMock);
    config.data.pageType = 'index';

    const store = mockStore<State>({
        config,
        ...defaultStore,
    });

    const tree = shallow(
        <Provider store={ store }>
            <CreditApplicationForm/>
        </Provider>,
        { context: contextMock },
    ).dive();

    const props = tree.dive().props() as Props;
    props.onFormSubmit(user);

    expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(2);
    expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('popup-credit-form_submit_success');
    expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('all-page-credit-form_submit_success');
});

it('должен отправить 2 цели на карточке оффера', () => {
    const config = cloneDeep(configMock);
    config.data.pageType = 'card';

    const store = mockStore<State>({
        config,
        ...defaultStore,
    });

    const tree = shallow(
        <Provider store={ store }>
            <CreditApplicationForm/>
        </Provider>,
        { context: contextMock },
    ).dive();

    const props = tree.dive().props() as Props;
    props.onFormSubmit(user);

    expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(2);
    expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('card-credit-form_submit_success');
    expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('all-page-credit-form_submit_success');
});

it('должен отправить 1 цель на листинге', () => {
    const config = cloneDeep(configMock);
    config.data.pageType = 'listing';

    const store = mockStore<State>({
        config,
        ...defaultStore,
    });

    const tree = shallow(
        <Provider store={ store }>
            <CreditApplicationForm/>
        </Provider>,
        { context: contextMock },
    ).dive();

    const props = tree.dive().props() as Props;
    props.onFormSubmit(user);

    expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(1);
    expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('all-page-credit-form_submit_success');
});

it('должен отправить 2 цели на лендинге кредит', () => {
    const config = cloneDeep(configMock);
    config.data.pageType = 'broker-promo';

    const store = mockStore<State>({
        config,
        ...defaultStore,
    });

    const tree = shallow(
        <Provider store={ store }>
            <CreditApplicationForm/>
        </Provider>,
        { context: contextMock },
    ).dive();

    const props = tree.dive().props() as Props;
    props.onFormSubmit(user);

    expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(2);
    expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('broker-promo_credit-form_submit_success');
    expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('all-page-credit-form_submit_success');
});

it('должен отправить 2 цели на листинге', () => {
    const config = cloneDeep(configMock);
    config.data.pageType = 'listing';

    const store = mockStore<State>({
        config,
        ...defaultStore,
    });

    const tree = shallow(
        <Provider store={ store }>
            <CreditApplicationForm
                offer={ cardMock }
            />
        </Provider>,
        { context: contextMock },
    ).dive();

    const props = tree.dive().props() as Props;
    props.onFormSubmit(user);

    expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(2);
    expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('listing-credit-form_submit_success');
    expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('all-page-credit-form_submit_success');
});
