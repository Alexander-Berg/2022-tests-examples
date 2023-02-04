jest.mock('auto-core/react/dataDomain/state/actions/openDealCancelPopup', () => jest.fn(
    () => ({ type: '' }),
));

import React from 'react';
import { shallow } from 'enzyme';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import openDealCancelPopup from 'auto-core/react/dataDomain/state/actions/openDealCancelPopup';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import CardSafeDeal from './CardSafeDeal';

const DEAL_ID = '399eb4fe-3c2c-4407-b4f7-1e41fc2dfd66';
const offer = cloneOfferWithHelpers(offerMock).withIsOwner(false).value();

it('ссылка должна вести на список сделок, если сделка на шаге ожидания', () => {
    const expectedUrl = 'link/my-deals/?';
    const wrapper = renderWrapper('DEAL_CREATED').dive();

    expect(wrapper.find('Link').last().prop('url')).toBe(expectedUrl);
});

it('ссылка должна вести на список сделок, если сделка отменена', () => {
    const expectedUrl = 'link/my-deals/?';
    const wrapper = renderWrapper('DEAL_DECLINED', 'CANCELLED_BY_SELLER').dive();

    expect(wrapper.find('Link').last().prop('url')).toBe(expectedUrl);
});

it('ссылка должна вести на страницу сделки, если сделка принята', () => {
    const expectedUrl = `link/deal/?deal_id=${ DEAL_ID }`;
    const wrapper = renderWrapper('DEAL_INVITE_ACCEPTED').dive();

    expect(wrapper.find('Link').last().prop('url')).toBe(expectedUrl);
});

it('не должен рэндерить блок, если покупатель отменил запрос', () => {
    const wrapper = renderWrapper('DEAL_DECLINED', 'CANCELLED_BY_BUYER').dive();

    expect(wrapper.isEmptyRender()).toBe(true);
});

it('должен показать модал выбора причины отмены при клике на "Отменить запрос"', () => {
    const wrapper = renderWrapper('DEAL_CREATED').dive();

    wrapper.find('Link').first().simulate('click');

    expect(openDealCancelPopup).toHaveBeenCalledWith('BUYER', '399eb4fe-3c2c-4407-b4f7-1e41fc2dfd66');
});

function mockDealStore(dealStep: string, dealCancelledBy?: string) {
    const deal = {
        id: '399eb4fe-3c2c-4407-b4f7-1e41fc2dfd66',
        step: dealStep,
        cancelled_by: dealCancelledBy,
    };

    return mockStore({
        safeDeal: {
            deal,
        },
    });
}

function renderWrapper(dealStep: string, dealCancelledBy?: string) {
    const store = mockDealStore(dealStep, dealCancelledBy);

    return shallow(
        <CardSafeDeal offer={ offer }/>,
        { context: { ...contextMock, store } },
    );
}
