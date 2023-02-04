jest.mock('auto-core/react/components/common/DealCancelPopup/hooks/useDealCancelPopup');

import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';

import { ParticipantType } from '@vertis/schema-registry/ts-types/vertis/safe_deal/common';

import mockStore from 'autoru-frontend/mocks/mockStore';

import type { TStateState } from 'auto-core/react/dataDomain/state/TStateState';
import useDealCancelPopup from 'auto-core/react/components/common/DealCancelPopup/hooks/useDealCancelPopup';

import { CANCEL_REASONS } from 'auto-core/data/deal/cancel_reasons';

import DealCancelPopupMobile from './DealCancelPopupMobile';

const defaultState: Partial<TStateState> = {
    dealCancelPopup: {
        isOpened: true,
        cancelParty: ParticipantType.BUYER,
        dealId: '123',
    },
};

const useDealCancelPopupMock = useDealCancelPopup as jest.MockedFunction<typeof useDealCancelPopup>;

const cancelDeal = jest.fn();
const onCancelReasonInputChange = jest.fn();

const defaultImplentation = {
    cancelDeal, onCancelReasonInputChange,
    reasons: CANCEL_REASONS.BUYER,
    isInputShown: () => false,
    cancelReasonInputError: false,
    cancelReasonInput: '',
};

it('должен отменить сделку по клику на причину', () => {
    useDealCancelPopupMock.mockImplementation(() => defaultImplentation);
    const wrapper = render();
    wrapper.find('Memo(DealCancelPopupItem)').at(0).dive().simulate('click');
    expect(cancelDeal).toHaveBeenCalledWith('BUYER_DEAL_WITH_ANOTHER_SELLER');
});

it('должен отменять сделку при клике на кнопку отправить если выбрана другая причина', () => {
    useDealCancelPopupMock.mockImplementation(() => ({
        ...defaultImplentation,
        cancelReasonInput: 'test',
        isInputShown: () => true,
    }));
    const wrapper = render();
    wrapper.find('Memo(DealCancelPopupItem)').at(4).dive().simulate('click');

    wrapper.find('.DealCancelPopupMobile__link').simulate('click');
    expect(cancelDeal).toHaveBeenCalledWith('BUYER_ANOTHER_REASON');
});

it('должен возвращать null если cancelParty || dealId = undefined', () => {
    useDealCancelPopupMock.mockImplementation(() => (defaultImplentation));
    defaultState!.dealCancelPopup!.dealId = undefined;
    defaultState!.dealCancelPopup!.cancelParty = undefined;
    const wrapper = render(defaultState);
    expect(wrapper).toBeEmptyRender();
});

function render(state = defaultState) {
    const store = mockStore({ state });
    return shallow(
        <Provider store={ store }>
            <DealCancelPopupMobile/>
        </Provider>,
    ).dive().dive();
}
