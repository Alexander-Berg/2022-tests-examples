/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';

import { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';
import { ParticipantType, DealStep, BuyerStep } from '@vertis/schema-registry/ts-types-snake/vertis/safe_deal/common';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import safeDealMock from 'auto-core/react/dataDomain/safeDeal/mocks/safeDeal.mock';

import SafeDealListItem from './SafeDealListItem';

const DEAL_ID = '399eb4fe-3c2c-4407-b4f7-1e41fc2dfd66';

const dealMock = { ...safeDealMock.deal, id: DEAL_ID };

const mockProps = {
    offer: offerMock,
    isDealAcceptPending: false,
    approveDeal: jest.fn(),
    resumeDeal: jest.fn(),
    openDealCancelPopup: jest.fn(),
};

it('должен рэндерить ссылку на страницу сделки, если идет сделка', () => {
    const expectedUrl = `link/deal/?deal_id=${ DEAL_ID }`;
    const dealFields = { step: DealStep.DEAL_INVITE_ACCEPTED, participant_type: ParticipantType.BUYER };
    const wrapper = renderWrapper(mockProps, dealFields);

    expect(wrapper.find('Button').prop('url')).toBe(expectedUrl);
});

it('должен рэндерить ссылку на страницу сделки, если сделка успешно завершена', () => {
    const expectedUrl = `link/deal/?deal_id=${ DEAL_ID }`;
    const dealFields = { step: DealStep.DEAL_COMPLETED, participant_type: ParticipantType.BUYER };
    const wrapper = renderWrapper(mockProps, dealFields);

    expect(wrapper.find('Button').prop('url')).toBe(expectedUrl);
});

it('должен открыть чат при клике на имя', () => {
      type VertisChat = typeof window.vertis_chat;
      window.vertis_chat = {
          open_chat_for_deal: jest.fn(),
      } as Partial<VertisChat> as VertisChat;
      const dealFields = { step: DealStep.DEAL_INVITE_ACCEPTED, participant_type: ParticipantType.SELLER };
      const wrapper = renderWrapper(mockProps, dealFields);

      wrapper.find('.SafeDealListItem__infoValue').first().simulate('click');

      expect(wrapper.find('.SafeDealListItem__infoTitle').first().text()).toBe('Имя покупателя');
      expect(window.vertis_chat.open_chat_for_deal).toHaveBeenCalled();
});

it('HЕ должен открыть чат при клике на имя, если оффер не активен', () => {
     type VertisChat = typeof window.vertis_chat;
     window.vertis_chat = {
         open_chat_for_deal: jest.fn(),
     } as Partial<VertisChat> as VertisChat;

     const offer = { ...offerMock, status: OfferStatus.INACTIVE };
     const props = { ...mockProps, offer };
     const dealFields = { step: DealStep.DEAL_INVITE_ACCEPTED, participant_type: ParticipantType.SELLER };
     const wrapper = renderWrapper(props, dealFields);

     wrapper.find('.SafeDealListItem__infoValue').first().simulate('click');

     expect(wrapper.find('.SafeDealListItem__infoTitle').first().text()).toBe('Имя покупателя');
     expect(window.vertis_chat.open_chat_for_deal).not.toHaveBeenCalled();
});

it('HЕ должен рисовать кнопки, если оффер не активен и сделка отменена', () => {
    const offer = { ...offerMock, status: OfferStatus.INACTIVE };
    const props = { ...mockProps, offer };
    const dealFields = { step: DealStep.DEAL_CANCELLED, participant_type: ParticipantType.SELLER };
    const wrapper = renderWrapper(props, dealFields);

    expect(wrapper.exists('.SafeDealListItem__buttons')).toBe(false);
});

it('HЕ должен рисовать кнопки, если оффер не активен и сделка отклонена', () => {
    const offer = { ...offerMock, status: OfferStatus.INACTIVE };
    const props = { ...mockProps, offer };
    const dealFields = { step: DealStep.DEAL_DECLINED, participant_type: ParticipantType.SELLER };
    const wrapper = renderWrapper(props, dealFields);

    expect(wrapper.exists('.SafeDealListItem__buttons')).toBe(false);
});

it('должен рисовать кнопки, если оффер не активен и сделка закончена', () => {
    const offer = { ...offerMock, status: OfferStatus.INACTIVE };
    const props = { ...mockProps, offer };
    const dealFields = { step: DealStep.DEAL_COMPLETED, participant_type: ParticipantType.SELLER };
    const wrapper = renderWrapper(props, dealFields);

    expect(wrapper.exists('.SafeDealListItem__buttons')).toBe(true);
});

it('должен рисовать кнопки, если оффер не активен, но сделка идет', () => {
    const offer = { ...offerMock, status: OfferStatus.INACTIVE };
    const props = { ...mockProps, offer };
    const dealFields = { step: DealStep.DEAL_INVITE_ACCEPTED, participant_type: ParticipantType.SELLER };
    const wrapper = renderWrapper(props, dealFields);

    expect(wrapper.exists('.SafeDealListItem__buttons')).toBe(true);
});

describe('покупатель', () => {
    it('должен показать модал выбора причины отмены при клике на кнопку "Отменить запрос"', () => {
        const dealFields = { step: DealStep.DEAL_CREATED, participant_type: ParticipantType.BUYER };
        const wrapper = renderWrapper(mockProps, dealFields);

        wrapper.find('.SafeDealListItem__cancelButton').last().simulate('click');

        expect(mockProps.openDealCancelPopup).toHaveBeenCalledWith('BUYER', '399eb4fe-3c2c-4407-b4f7-1e41fc2dfd66');
    });

    it('должен открыть чат при клике на кнопку "Связаться с продавцом"', () => {
         type VertisChat = typeof window.vertis_chat;
         window.vertis_chat = {
             open_chat_for_deal: jest.fn(),
         } as Partial<VertisChat> as VertisChat;
         const dealFields = { step: DealStep.DEAL_CREATED, participant_type: ParticipantType.BUYER };
         const wrapper = renderWrapper(mockProps, dealFields);

         wrapper.find('.SafeDealListItem__contactButton').first().simulate('click');

         expect(window.vertis_chat.open_chat_for_deal).toHaveBeenCalled();
    });

    it('должен открыть модал отправки запроса при клике "Отправить запрос"', () => {
        const resumeDealMock = jest.fn(() => {});
        const props = { ...mockProps, resumeDeal: resumeDealMock };
        const dealFields = { step: DealStep.DEAL_DECLINED, participant_type: ParticipantType.BUYER };
        const wrapper = renderWrapper(props, dealFields);

        wrapper.find('.SafeDealListItem__requestButton').simulate('click');

        expect(wrapper.find('SafeDealCreateModal').prop('isVisible')).toBe(true);
    });

    it('должен открывать попап с выбором причины отмены продавца на сделку при клике на кнопку "Отменить"', () => {
        const dealFields = {
            step: DealStep.DEAL_CREATED,
            participant_type: ParticipantType.BUYER,
            buyer_step: BuyerStep.BUYER_ACCEPTING_DEAL,
        };
        const wrapper = renderWrapper(mockProps, dealFields);

        wrapper.find('.SafeDealListItem__buttonDecline').simulate('click');

        expect(mockProps.openDealCancelPopup).toHaveBeenCalledWith('BUYER', '399eb4fe-3c2c-4407-b4f7-1e41fc2dfd66');
    });

    it('должен принять запрос продавца на сделку при клике на кнопку "Подтвердить"', () => {
        const approveDealMock = jest.fn(() => {});
        const props = { ...mockProps, approveDeal: approveDealMock };
        const dealFields = {
            step: DealStep.DEAL_CREATED,
            participant_type: ParticipantType.BUYER,
            buyer_step: BuyerStep.BUYER_ACCEPTING_DEAL,
        };
        const wrapper = renderWrapper(props, dealFields);

        wrapper.find('.SafeDealListItem__buttonApprove').simulate('click');

        expect(approveDealMock.mock.calls).toHaveLength(1);
    });
});

describe('продавец', () => {
    it('должен открывать попал выбора причины отмены покупателя на сделку при клике на кнопку "Отменить"', () => {
        const dealFields = { step: DealStep.DEAL_CREATED, participant_type: ParticipantType.SELLER };
        const wrapper = renderWrapper(mockProps, dealFields);

        wrapper.find('.SafeDealListItem__buttonDecline').simulate('click');

        expect(mockProps.openDealCancelPopup).toHaveBeenCalledWith('SELLER', '399eb4fe-3c2c-4407-b4f7-1e41fc2dfd66');
    });

    it('должен принять запрос покупателя на сделку при клике на кнопку "Подтвердить"', () => {
        const approveDealMock = jest.fn(() => {});
        const props = { ...mockProps, approveDeal: approveDealMock };
        const dealFields = { step: DealStep.DEAL_CREATED, participant_type: ParticipantType.SELLER };
        const wrapper = renderWrapper(props, dealFields);

        wrapper.find('.SafeDealListItem__buttonApprove').simulate('click');

        expect(approveDealMock.mock.calls).toHaveLength(1);
    });

    it('должен показать модал с причиной отмены при клике на кнопку "Отменить запрос"', () => {
        const dealFields = {
            step: DealStep.DEAL_CREATED,
            participant_type: ParticipantType.SELLER,
            buyer_step: BuyerStep.BUYER_ACCEPTING_DEAL,
        };
        const wrapper = renderWrapper(mockProps, dealFields);

        wrapper.find('.SafeDealListItem__cancelButton').last().simulate('click');

        expect(mockProps.openDealCancelPopup).toHaveBeenCalledWith('SELLER', '399eb4fe-3c2c-4407-b4f7-1e41fc2dfd66');
    });
});

function renderWrapper(props: typeof mockProps, dealFields: Record<string, string>) {
    const deal = { ...dealMock, ...dealFields };
    const store = mockStore({
        safeDeal: {
            deal: { ...safeDealMock, deal },
        },
    });

    return shallow(
        <SafeDealListItem
            deal={ deal }
            { ...props }
        />,
        { context: { ...contextMock, store } },
    );
}
