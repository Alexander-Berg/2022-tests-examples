import React from 'react';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';
import MockDate from 'mockdate';

import mockStore from 'autoru-frontend/mocks/mockStore';

import gateApi from 'auto-core/react/lib/gateApi';

import PromoFeatures from './PromoFeatures';
import PromoFeaturesDumb from './PromoFeaturesDumb';

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const getResource = gateApi.getResource as jest.MockedFunction<typeof gateApi.getResource>;

const initialState = {
    promoFeatures: {
        features: [],
        isLoading: false,
    },
};

const FEATURE_1 = {
    id: 'id',
    tag: 'boost',
    label: '99\u00a0₽, осталось 10',
    name: 'Поднятие в поиске',
    count: '10 шт.',
    deadline_origin: '2021-11-06T12:31:19.654+03:00',
    value: '99\u00a0₽',
};

const FEATURE_2 = {
    id: 'id',
    tag: 'offers-history-reports-1',
    label: 'скидка 50%, осталось 1',
    name: 'История по VIN',
    count: '1 шт.',
    deadline_origin: undefined,
    value: 'Скидка 50%',
};

const CASHBACK = {
    id: 'id',
    tag: 'cashback',
    label: '150\u00a0₽',
    name: 'CASHBACK',
    count: undefined,
    deadline: undefined,
    value: '150\u00a0₽',
};

beforeEach(() => {
    getResource.mockClear();
    MockDate.set('2020-11-06');
});

afterEach(() => {
    MockDate.reset();
});

it('должен отрендерить компонент PromoFeatures без доступных фич', () => {
    const { wrapper } = render(initialState);
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен отрендерить компонент PromoFeatures со списком доступных промофич', () => {
    const state = { promoFeatures: { features: [ FEATURE_1, FEATURE_2, CASHBACK ] } };
    const { wrapper } = render(state);
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен отрендерить компонент PromoFeatures c ошибкой', () => {
    const { wrapper } = render(initialState);
    const state = { error: 'Такого промокода не существует', code: 'promo-code' };
    wrapper.setState(state, () => {
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });
});

it('должен обработать ввод корректного промокода в PromoFeaturesDumb', () => {
    const onApply = jest.fn(() => Promise.resolve());
    const loadPromoFeatures = jest.fn();

    const wrapper = shallow(
        <PromoFeaturesDumb
            features={ [] }
            onApply={ onApply }
            loadPromoFeatures={ loadPromoFeatures }
        />,
    );

    wrapper.instance().setState({ code: 'promo-code' });

    wrapper.find('Button').simulate('click');

    return new Promise<void>((done) => {
        setTimeout(() => {
            expect(onApply).toHaveBeenCalled();
            expect(loadPromoFeatures).toHaveBeenCalled();
            expect(wrapper.instance().state).toEqual({ error: undefined, code: '' });
            done();
        }, 100);
    });
});

it('PromoFeaturesDumb должен обработать ввод некорректного промокода', () => {
    const onApply = jest.fn(() => Promise.reject());
    const loadPromoFeatures = jest.fn();

    const wrapper = shallow(
        <PromoFeaturesDumb
            features={ [] }
            onApply={ onApply }
            loadPromoFeatures={ loadPromoFeatures }
        />,
    );

    wrapper.instance().setState({ code: 'promo-code' });

    wrapper.find('Button').simulate('click');

    return new Promise<void>((done) => {
        setTimeout(() => {
            expect(onApply).toHaveBeenCalled();
            expect(loadPromoFeatures).toHaveBeenCalledTimes(0);
            expect(wrapper.instance().state).toEqual({ error: 'Такого промокода не существует', code: 'promo-code' });
            done();
        }, 100);
    });
});

it('PromoFeatures должен обработать ввод промокода по клавише Enter', () => {
    const enter = { key: 'Enter' };
    const { wrapper } = render(initialState);
    /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
    const instance: any = wrapper.instance();

    instance.onApplyCode = jest.fn();
    wrapper.setState({ code: 'promo-code' });

    wrapper.find('TextInput').simulate('keyPress', enter);
    expect(instance.onApplyCode).toHaveBeenCalled();
});

/**
 * @param {object} state
 * @returns {ShallowWrapper}
 */
/* eslint-disable-next-line @typescript-eslint/no-explicit-any */
function render(state: any) {
    const store = mockStore(state);
    const wrapper = shallow(<PromoFeatures/>, { context: { store } }).dive();

    return { store, wrapper };
}
