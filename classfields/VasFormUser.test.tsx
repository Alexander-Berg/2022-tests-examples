/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */
jest.mock('auto-core/react/dataDomain/card/actions/toggleAutoProlongation');
jest.mock('auto-core/react/dataDomain/formFields/actions/setFormErrorText');
jest.mock('auto-core/react/dataDomain/formFields/actions/changePendingState');

import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

import { getBunkerMock } from 'autoru-frontend/mockData/state/bunker.mock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import { CHANGE_PENDING_STATE, SET_FORM_ERROR_TEXT } from 'auto-core/react/dataDomain/formFields/types';
import toggleAutoProlongation from 'auto-core/react/dataDomain/card/actions/toggleAutoProlongation';
import setFormErrorText from 'auto-core/react/dataDomain/formFields/actions/setFormErrorText';
import changePendingState from 'auto-core/react/dataDomain/formFields/actions/changePendingState';
import formVasStateMock from 'auto-core/react/dataDomain/formVas/mocks';
import formFieldsStateMock from 'auto-core/react/dataDomain/formFields/mocks';
import configStateMock from 'auto-core/react/dataDomain/config/mock';
import userStateMock from 'auto-core/react/dataDomain/user/mocks';
import cardStateMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';

import { TVasSnippets } from '../utils';

import VasFormUser from './VasFormUserConnected';
import type { OwnProps } from './VasFormUser';
import type { TAppState } from './connector';

const toggleAutoProlongationMock = toggleAutoProlongation as jest.MockedFunction<typeof toggleAutoProlongation>;
toggleAutoProlongationMock.mockReturnValue(() => Promise.resolve());

const changePendingStateMock = changePendingState as jest.MockedFunction<typeof changePendingState>;
changePendingStateMock.mockImplementation((isPending: boolean) =>
    ({ type: CHANGE_PENDING_STATE, payload: isPending }));

const setFormErrorTextMock = setFormErrorText as jest.MockedFunction<typeof setFormErrorText>;
setFormErrorTextMock.mockImplementation((text: string) => ({ type: SET_FORM_ERROR_TEXT, payload: text }));

let props: OwnProps;
let initialState: TAppState;

beforeEach(() => {
    props = {
        onSubmitButtonClick: jest.fn(),
        isOfferInactive: false,
    };
    initialState = {
        bunker: getBunkerMock([ 'common/vas', 'common/vas_vip', 'common/form_vas_text' ]),
        config: configStateMock.withPageParams({ category: 'cars' }).value(),
        formFields: formFieldsStateMock.value(),
        formVas: formVasStateMock.value(),
        user: userStateMock.value(),
        cookies: {},
        card: cloneOfferWithHelpers(cardStateMock)
            .withCustomVas({ service: TOfferVas.VIP, recommendation_priority: 0 })
            .withCustomActiveServices([])
            .value(),
    };

    contextMock.logVasEvent.mockClear();
});

afterEach(() => {
    contextMock.hasExperiment.mockReset();
});

it('если нет активных сервисов покажет сниппеты', () => {
    const page = shallowRenderComponent({ props, initialState });
    const snippets = page.find('.VasFormUser__snippets');
    const serviceSet = page.find('VasFormUserServiceSet');

    expect(snippets.isEmptyRender()).toBe(false);
    expect(serviceSet.isEmptyRender()).toBe(true);
});

it('если есть активный сервис покажет набор сервисов', () => {
    initialState.card = cloneOfferWithHelpers(cardStateMock).withCustomActiveServices([ { service: TOfferVas.COLOR } ]).value();
    const page = shallowRenderComponent({ props, initialState });
    const snippets = page.find('.VasFormUser__snippets');
    const serviceSet = page.find('VasFormUserServiceSet');

    expect(snippets.isEmptyRender()).toBe(false);
    expect(serviceSet.isEmptyRender()).toBe(true);
});

it('если есть активный пакет покажет компонент с набором сервисов', () => {
    initialState.card = cloneOfferWithHelpers(cardStateMock).withCustomActiveServices([ { service: TOfferVas.TURBO } ]).value();
    const page = shallowRenderComponent({ props, initialState });
    const snippets = page.find('.VasFormUser__snippets');
    const serviceSet = page.find('VasFormUserServiceSet');

    expect(snippets.isEmptyRender()).toBe(true);
    expect(serviceSet.isEmptyRender()).toBe(false);
});

it('если объява не активна покажет компонент с набором сервисов', () => {
    props.isOfferInactive = true;
    const page = shallowRenderComponent({ props, initialState });
    const snippets = page.find('.VasFormUser__snippets');
    const serviceSet = page.find('VasFormUserServiceSet');

    expect(snippets.isEmptyRender()).toBe(true);
    expect(serviceSet.isEmptyRender()).toBe(false);
});

it('при переключении автопролонгации сервиса вызовет соотвествующий экшен', () => {
    initialState.card = cloneOfferWithHelpers(cardStateMock).withCustomActiveServices([ { service: TOfferVas.TURBO } ]).value();
    const page = shallowRenderComponent({ props, initialState });
    const serviceSet = page.find('VasFormUserServiceSet');
    serviceSet.simulate('autoProlongationToggle', TOfferVas.TURBO, true);

    expect(toggleAutoProlongationMock).toHaveBeenCalledTimes(1);
    expect(toggleAutoProlongationMock).toHaveBeenCalledWith(true, { product: TOfferVas.TURBO });
});

describe('пакет ВИП:', () => {
    it('предложит если есть рекомендация от бэка', () => {
        initialState.card = cloneOfferWithHelpers(cardStateMock)
            .withCustomVas({ service: TOfferVas.VIP, recommendation_priority: 10 })
            .withCustomActiveServices([])
            .value();
        const page = shallowRenderComponent({ props, initialState });
        const vipSnippet = page.find('ConnectSocialAccountHoc(VasFormUserSnippet)').find({ type: TVasSnippets.VIP });

        expect(vipSnippet.isEmptyRender()).toBe(false);
    });

    it('не предложит если есть рекомендация от бэка но не категория не cars', () => {
        initialState.card = cloneOfferWithHelpers(cardStateMock)
            .withCustomVas({ service: TOfferVas.VIP, recommendation_priority: 10 })
            .withCustomActiveServices([])
            .value();
        initialState.config = configStateMock.withPageParams({ parent_category: 'trucks' }).value();

        const page = shallowRenderComponent({ props, initialState });
        const vipSnippet = page.find('ConnectSocialAccountHoc(VasFormUserSnippet)').find({ type: TVasSnippets.VIP });

        expect(vipSnippet.isEmptyRender()).toBe(true);
    });

    it('предложит если нет рекомендации, но цена удовлетворяет условию', () => {
        initialState.formFields = formFieldsStateMock.withCustomFields({ price: { value: 1000000 } }).value();
        const page = shallowRenderComponent({ props, initialState });
        const vipSnippet = page.find('ConnectSocialAccountHoc(VasFormUserSnippet)').find({ type: TVasSnippets.VIP });

        expect(vipSnippet.isEmptyRender()).toBe(false);
    });

    it('не предложит если нет рекомендации и цена ниже установленного лимита', () => {
        initialState.formFields = formFieldsStateMock.withCustomFields({ price: { value: 500000 } }).value();
        const page = shallowRenderComponent({ props, initialState });
        const vipSnippet = page.find('ConnectSocialAccountHoc(VasFormUserSnippet)').find({ type: TVasSnippets.VIP });

        expect(vipSnippet.isEmptyRender()).toBe(true);
    });
});

describe('при сабмите', () => {
    beforeEach(() => {
        const page = shallowRenderComponent({ props, initialState });
        const snippet = page.find('ConnectSocialAccountHoc(VasFormUserSnippet)').at(0);
        snippet.simulate('submitButtonClick', { foo: 'bar' }, { baz: 42 });
    });

    it('вызовет экнш на смену состояния формы', () => {
        expect(changePendingStateMock).toHaveBeenCalledTimes(1);
        expect(changePendingStateMock).toHaveBeenCalledWith(true);
    });

    it('вызовет проп', () => {
        expect(props.onSubmitButtonClick).toHaveBeenCalledTimes(1);
        expect(props.onSubmitButtonClick).toHaveBeenCalledWith({ foo: 'bar' }, { baz: 42 });
    });
});

function shallowRenderComponent({ initialState, props }: { props: OwnProps; initialState: TAppState }) {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore(initialState);

    const page = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <VasFormUser { ...props }/>
            </Provider>
        </ContextProvider>,
    );

    return page.dive().dive().dive();
}
