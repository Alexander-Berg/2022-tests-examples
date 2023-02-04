jest.mock('auto-core/react/dataDomain/state/actions/sellerPopupOpen');

import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';
import _ from 'lodash';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import mockStore from 'autoru-frontend/mocks/mockStore';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import type { TStateSellerPopup } from 'auto-core/react/dataDomain/state/TStateState';
import sellerPopupOpen from 'auto-core/react/dataDomain/state/actions/sellerPopupOpen';
import offer from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import userMock from 'auto-core/react/dataDomain/user/mocks/withAuth.mock';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import SellerPopup from './SellerPopup';
import type { AppState } from './SellerPopup';

const sellerPopupOpenMock = sellerPopupOpen as jest.MockedFunction<typeof sellerPopupOpen>;
sellerPopupOpenMock.mockImplementation(() => ({ type: 'FOO' }));

let initialState: Partial<AppState>;

beforeEach(() => {
    initialState = {
        state: {
            sellerPopup: {
                isOpened: true,
                tab: 'place',
                source: 'a',
                offerId: '1085562758-1970f439',
            },
            authModal: {},
        },
        card: offer,
        user: userMock,
    };
});

it('на вкладке с картой при клике на "показать телефон" вызовет корректный экшен', () => {
    const page = shallowRenderComponent({ initialState });
    const button = page.find('.SellerPopup__mapToggle');
    button.simulate('click');

    expect(sellerPopupOpenMock).toHaveBeenCalledTimes(1);
    expect(sellerPopupOpenMock.mock.calls[0][0]).toEqual(
        expect.objectContaining({
            tab: 'phone',
            source: 'phonePopup',
        }),
    );
});

it('отрендерит пормо безопасной сделки', () => {
    const state = _.cloneDeep(initialState);
    state.state = {
        sellerPopup: {
            isOpened: true,
            tab: 'phone',
            source: 'a',
            offerId: '1085562758-1970f439',
        },
        authModal: {},
    };
    state.card?.tags?.push('allowed_for_safe_deal');
    state.card!.additional_info!.is_owner = false;

    const page = shallowRenderComponent({ initialState: state });
    expect(page.find('SellerPopupSafeDeal')).toExist();
});

it('на вкладке с телефоном при клике на адрес вызовет корректный экшен', () => {
    const state = _.cloneDeep(initialState);
    state.state = {
        sellerPopup: {
            isOpened: true,
            tab: 'phone',
            source: 'a',
            offerId: '1085562758-1970f439',
        },
        authModal: {},
    };

    const page = shallowRenderComponent({ initialState: state });
    const footer = page.find('SellerPopupFooter');
    footer.simulate('click');

    expect(sellerPopupOpenMock).toHaveBeenCalledTimes(1);
    expect(sellerPopupOpenMock.mock.calls[0][0]).toEqual(
        expect.objectContaining({
            tab: 'place',
            source: 'phonePopup',
        }),
    );
});

it('не должен рендерить basements, если попап скрыт', () => {
    const state = _.cloneDeep(initialState);
    const stateWithCard = {
        ...state,
        state: {
            authModal: {},
            sellerPopup: {
                isOpened: false,
                source: 'a',
                offerId: '1085584716-10acd424',
            },
        },
        card: {
            id: '1085584716',
            hash: '10acd424',
        } as Offer,
    };

    const page = shallowRenderComponent({ initialState: stateWithCard });
    const Modal = page.find('Modal');
    expect(Modal.props()).toHaveProperty('basements', undefined);
});

describe('ссылка на публичный профиль перекупа', () => {
    const state = {
        state: {
            sellerPopup: {
                isOpened: true,
                tab: 'place' as TStateSellerPopup['tab'],
                source: 'a',
                offerId: '1085562758-1970f439',
            },
            authModal: {},
        },
        card: cloneOfferWithHelpers(offer).withEncryptedUserId('some_encrypted_id').value(),
        user: userMock,
    };

    it('рендерит, если пришел encrypted_user_id', () => {
        render(getComponentForTestingLibrary({ initialState: state }));

        const link = screen.getByRole('link', { name: /дмитрий/i });

        expect(link.getAttribute('href')).toBe('link/reseller-public-page/?encrypted_user_id=some_encrypted_id');
    });

    it('отправит на маунт метрику, если ссылка есть', () => {
        render(getComponentForTestingLibrary({ initialState: state }));

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'reseller_public', 'link-show', 'sellerPopup' ]);
    });

    it('отправит метрику на клик по ссылке', () => {
        render(getComponentForTestingLibrary({ initialState: state }));

        const link = screen.getByRole('link', { name: /дмитрий/i });
        userEvent.click(link);

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'reseller_public', 'link-show', 'sellerPopup' ]);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'reseller_public', 'link-click', 'sellerPopup' ]);
    });

    it('не будет отправлять метрику на маунт, если ссылки нет', () => {
        render(getComponentForTestingLibrary({ initialState: {
            ...state,
            card: offer,
        } }));

        expect(contextMock.metrika.sendPageEvent).not.toHaveBeenCalled();
    });
});

function shallowRenderComponent({ initialState }: { initialState: Partial<AppState> }) {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore(initialState);

    const page = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <SellerPopup/>
            </Provider>
        </ContextProvider>,
    );

    return page.dive().dive().dive();
}

function getComponentForTestingLibrary({ initialState }: { initialState: Partial<AppState> }) {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore(initialState);

    return (
        <ContextProvider>
            <Provider store={ store }>
                <SellerPopup/>
            </Provider>
        </ContextProvider>
    );
}
