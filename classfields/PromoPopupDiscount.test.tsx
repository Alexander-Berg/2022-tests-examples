jest.mock('auto-core/react/dataDomain/state/actions/paymentModalOpen');
jest.mock('auto-core/react/dataDomain/cookies/actions/set', () => {
    return () => () => {};
});

const logVasEventPromise = Promise.resolve();
// eslint-disable-next-line @typescript-eslint/no-unused-vars
const mockLogVasEvent = jest.fn((a: any) => logVasEventPromise);
jest.mock('auto-core/lib/util/vas/logger', () => {
    return jest.fn(() => ({
        logVasEvent: mockLogVasEvent,
    }));
});

import React from 'react';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';

import { userPromoPopupDiscount } from 'autoru-frontend/mockData/state/state.mock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';

import paymentModalOpen from 'auto-core/react/dataDomain/state/actions/paymentModalOpen';
import { AutoPopupNames } from 'auto-core/react/dataDomain/autoPopup/types';
import type { AutoPopupVasDiscountModal } from 'auto-core/react/dataDomain/autoPopup/popupTypes';

import type { AppState } from './PromoPopupDiscount';
import PromoPopupDiscount from './PromoPopupDiscount';

const paymentModalOpenMock = paymentModalOpen as jest.MockedFunction<typeof paymentModalOpen>;

paymentModalOpenMock.mockImplementation(((() => () => {}) as unknown as typeof paymentModalOpen));

let state: AppState;

beforeEach(() => {
    state = {
        autoPopup: {
            id: AutoPopupNames.VAS_DISCOUNT,
            data: userPromoPopupDiscount.data,
        } as AutoPopupVasDiscountModal,
        cookies: {},
        searchID: {
            searchID: 'searchID',
            parentSearchId: 'parentSearchID',
        },
    };
});

it('правильно логирует показы васа при рендере', () => {
    renderComponent({ state });

    expect(mockLogVasEvent).toHaveBeenCalledTimes(3);
    expect(mockLogVasEvent.mock.calls[0][0]).toMatchSnapshot();
    expect(mockLogVasEvent.mock.calls[1][0]).toMatchSnapshot();
    expect(mockLogVasEvent.mock.calls[2][0]).toMatchSnapshot();
});

describe('при клике на вас', () => {
    beforeEach(async() => {
        const page = renderComponent({ state });
        const packageTurboItem = page.find('.PromoPopupDiscount__item_view_package_turbo');
        packageTurboItem.simulate('click', 'package_turbo', userPromoPopupDiscount.data.saleId);
    });

    it('передаст правильные параметры в модал оплаты', () => {
        expect(paymentModalOpen).toHaveBeenCalledTimes(1);
        expect(paymentModalOpenMock.mock.calls[0]).toMatchSnapshot();
    });

    it('правильно залогирует событие клика', () => {
        expect(mockLogVasEvent).toHaveBeenCalledTimes(4);
        expect(mockLogVasEvent.mock.calls[3][0]).toMatchSnapshot();
    });
});

describe('если у пользователя больше одного объявления', () => {
    let page: ShallowWrapper;

    beforeEach(() => {
        state.autoPopup = {
            id: AutoPopupNames.VAS_DISCOUNT,
            data: {
                ...userPromoPopupDiscount.data,
                hasMoreThanOneOffer: true,
            },
        } as AutoPopupVasDiscountModal;
        page = renderComponent({ state });
    });

    it('не будет отправять событие показа', () => {
        expect(mockLogVasEvent).not.toHaveBeenCalled();
    });

    it('не будет отправять событие клика', () => {
        const packageTurboItem = page.find('.PromoPopupDiscount__item_view_package_turbo');
        packageTurboItem.simulate('click', 'package_turbo', userPromoPopupDiscount.data.saleId);

        expect(mockLogVasEvent).not.toHaveBeenCalled();
    });

    it('не будет открывать модал оплаты', () => {
        const packageTurboItem = page.find('.PromoPopupDiscount__item_view_package_turbo');
        packageTurboItem.simulate('click', 'package_turbo', userPromoPopupDiscount.data.saleId);

        expect(paymentModalOpen).not.toHaveBeenCalled();
    });
});

function renderComponent({ state }: { state: AppState }) {
    const context = { ...contextMock, store: mockStore(state) };

    return shallow(
        <PromoPopupDiscount/>,
        { context },
    ).dive();
}
