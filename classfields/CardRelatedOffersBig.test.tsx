/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */
jest.mock('auto-core/react/hooks/useLazyResourceWithPagination');
jest.mock('auto-core/react/dataDomain/notifier/actions/notifier', () => ({
    showAutoclosableErrorMessage: jest.fn().mockReturnValue({ type: '' }),
}));

import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import useLazyResourceWithPagination from 'auto-core/react/hooks/useLazyResourceWithPagination';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import salonMock from 'auto-core/react/dataDomain/salonInfo/mocks';
import { showAutoclosableErrorMessage } from 'auto-core/react/dataDomain/notifier/actions/notifier';

import CardRelatedOffersBig from './CardRelatedOffersBig';

const useLazyResourceWithPaginationMock = useLazyResourceWithPagination as jest.MockedFunction<typeof useLazyResourceWithPagination>;

const dealerOfferMock = cloneOfferWithHelpers(offerMock).withDealerServicePrices().withCustomActiveServices([])
    .withSalon(salonMock.value().data).withSellerName('Какой-то дилер');

const mockDefaultValues = {
    init: jest.fn(),
    hasNextPage: false,
    loadNextPage: jest.fn(),
    isError: false,
    isLoading: false,
    data: null,
    origResponse: null,
};

const mockState = {
    card: offerMock,
};

it('не должен рендерится, если нет данных', () => {
    useLazyResourceWithPaginationMock.mockImplementationOnce(() => mockDefaultValues);
    const wrapper = render();
    expect(wrapper.dive().find('.CardRelatedOffersBig')).not.toExist();
});

it('должен вызвать init по показу блока и отправить метрику', () => {
    useLazyResourceWithPaginationMock.mockImplementationOnce(() => mockDefaultValues);
    const wrapper = render();
    wrapper.find('InView').simulate('change', true);
    expect(mockDefaultValues.init).toHaveBeenCalled();
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'used_related_bottom_bar', 'show' ]);
});

it('должен отрендерить тайтл без инфы о дилере', () => {
    useLazyResourceWithPaginationMock.mockImplementationOnce(() => ({
        ...mockDefaultValues,
        data: [ offerMock ],
        origResponse: {
            response_flags: {},
        },
    }));
    const wrapper = render();
    wrapper.find('InView').simulate('change', true);
    expect(wrapper.dive().find('.CardRelatedOffersBig__title').text()).toBe('Похожие предложения');
});

it('должен отрендерить тайтл с инфой о дилере', () => {
    useLazyResourceWithPaginationMock.mockImplementationOnce(() => ({
        ...mockDefaultValues,
        data: [ offerMock ],
        origResponse: {
            response_flags: { dealer_special: true },
        },
    }));
    const wrapper = render({ card: dealerOfferMock.value() });
    wrapper.find('InView').simulate('change', true);
    expect(wrapper.find('.CardRelatedOffersBig__title').text()).toBe('Похожие предложения от <Link />');
    const link = wrapper.find('.CardRelatedOffersBig__title').find('Link');
    expect(link.prop('url')).toBe('link/dealer-page/?category=cars&section=used&dealer_code=avtograd_moskva');
    expect(link.children().text()).toBe('Какой-то дилер');
});

it('должен отправить метрику по клику на оффер', () => {
    useLazyResourceWithPaginationMock.mockImplementationOnce(() => ({
        ...mockDefaultValues,
        data: [ offerMock ],
        origResponse: {
            response_flags: { dealer_special: true },
        },
    }));
    const wrapper = render();
    wrapper.find('InView').simulate('change', true);
    wrapper.find('ListingCarouselItem').dive().find('Link').simulate('click');
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'used_related_bottom_bar', 'offer', 'click' ]);
});

it('должен вызвать метод запроса новой страницы и отправить метрику по клику на ещё', () => {
    useLazyResourceWithPaginationMock.mockImplementationOnce(() => ({
        ...mockDefaultValues,
        data: [ offerMock ],
        origResponse: {
            response_flags: { dealer_special: true },
        },
        hasNextPage: true,
    }));
    const wrapper = render();
    wrapper.find('InView').simulate('change', true);
    wrapper.find('.CardRelatedOffersBig__button').simulate('click');
    expect(mockDefaultValues.loadNextPage).toHaveBeenCalled();
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'used_related_bottom_bar', 'more', 'click' ]);
});

it('должен показать статус загрузки в кнопке', () => {
    useLazyResourceWithPaginationMock.mockImplementationOnce(() => ({
        ...mockDefaultValues,
        data: [ offerMock ],
        origResponse: {
            response_flags: { dealer_special: true },
        },
        hasNextPage: true,
        isLoading: true,
    }));
    const wrapper = render();
    wrapper.find('InView').simulate('change', true);
    const button = wrapper.find('.CardRelatedOffersBig__button');
    expect(button.prop('loading')).toBe(true);
});

it('должен показать статус ошибки', () => {
    jest.spyOn(React, 'useEffect').mockImplementation(f => f());
    useLazyResourceWithPaginationMock.mockImplementationOnce(() => ({
        ...mockDefaultValues,
        hasNextPage: true,
        data: [ offerMock ],
        origResponse: {
            response_flags: {},
        },
        isError: true,
    }));
    const wrapper = render();
    wrapper.find('InView').simulate('change', true);
    expect(showAutoclosableErrorMessage).toHaveBeenCalled();
});

function render(state = mockState) {
    const store = mockStore(state);
    return shallow(
        <Provider store={ store }>
            <CardRelatedOffersBig className=""/>
        </Provider>,
        { context: contextMock },
    ).dive().dive();
}
