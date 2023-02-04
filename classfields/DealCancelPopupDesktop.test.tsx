jest.mock('auto-core/react/components/common/DealCancelPopup/hooks/useDealCancelPopup');

import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';

import { ParticipantType } from '@vertis/schema-registry/ts-types/vertis/safe_deal/common';

import mockStore from 'autoru-frontend/mocks/mockStore';

import type { TStateState } from 'auto-core/react/dataDomain/state/TStateState';
import useDealCancelPopup from 'auto-core/react/components/common/DealCancelPopup/hooks/useDealCancelPopup';

import { CANCEL_REASONS } from 'auto-core/data/deal/cancel_reasons';

import DealCancelPopupDesktop from './DealCancelPopupDesktop';

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

it('должен показывать ошибку на селекте если причина не выбрана', () => {
    useDealCancelPopupMock.mockImplementation(() => (defaultImplentation));
    const wrapper = render();
    wrapper.find('.DealCancelPopupDesktop__button').simulate('click');
    expect(wrapper.find('Select').prop('hasError')).toBe(true);
});

it('должен показывать ошибку на инпуте если поле ввода причины пустое', () => {
    useDealCancelPopupMock.mockImplementation(() => ({
        ...defaultImplentation,
        isInputShown: () => true,
        cancelReasonInputError: true,
    }));
    const wrapper = render();
    expect(wrapper.find('TextArea').prop('error')).toBe(true);
});

it('должен отдавать null если cancelParty || dealId = undefined', () => {
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
            <DealCancelPopupDesktop/>
        </Provider>,
    ).dive().dive();
}
