jest.mock('auto-core/react/dataDomain/booking/actions/getBookingTerms');
jest.mock('auto-core/react/dataDomain/notifier/actions/notifier');

import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import userStateMock from 'auto-core/react/dataDomain/user/mocks/withAuth.mock';
import dealerStateMock from 'auto-core/react/dataDomain/user/mocks/dealerWithAccess.mock';
import getBookingTerms from 'auto-core/react/dataDomain/booking/actions/getBookingTerms';
import { showAutoclosableMessage } from 'auto-core/react/dataDomain/notifier/actions/notifier';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import BookingModal from './BookingModal';

const showAutoclosableMessageMock = showAutoclosableMessage as jest.MockedFunction<typeof showAutoclosableMessage>;
const getBookingTermsMock = getBookingTerms as jest.MockedFunction<typeof getBookingTerms>;

const storeMock = {
    cookies: {},
    user: _.cloneDeep(userStateMock),
    booking: {},
    bunker: {
        'common/booking': {
            texts: {
                cardButtonDesktop: 'Забронировать автомобиль',
            },
        },
    },
};
const offer: Offer = cloneOfferWithHelpers({}).value();

it('должен отрисовать кнопку, если объявление можно забронировать и пользователь не дилер', () => {
    const wrapper = shallow(
        <BookingModal offer={ offer }/>,
        { context: { ...contextMock, store: mockStore(storeMock) } },
    ).dive();

    expect(wrapper.find('.BookingModal__card-button')).toExist();
});

it('не должен отрисовать кнопку, если объявление для дилера', () => {
    const store = { ...storeMock, user: dealerStateMock };

    const wrapper = shallow(
        <BookingModal offer={ offer }/>,
        { context: { ...contextMock, store: mockStore(store) } },
    ).dive();

    expect(wrapper.find('.BookingModal__card-button')).not.toExist();
});

it('должен показать модал, если корректно отработал запрос за данными о стоимости', () => {
    const mockRequest = Promise.resolve();
    getBookingTermsMock.mockImplementation(() => () => mockRequest);

    const wrapper = shallow(
        <BookingModal offer={ offer }/>,
        { context: { ...contextMock, store: mockStore(storeMock) } },
    ).dive();

    wrapper.find('.BookingModal__card-button').simulate('click');

    return mockRequest
        .then(() => {
            expect(wrapper.find('Modal').props()).toHaveProperty('visible', true);
        });
});

it('должен вызвать нотифайку об ошибке, если не отработал запрос за данными о стоимости', () => {
    const mockRequest = Promise.reject();
    getBookingTermsMock.mockImplementation(() => () => mockRequest);

    const wrapper = shallow(
        <BookingModal offer={ offer }/>,
        { context: { ...contextMock, store: mockStore(storeMock) } },
    ).dive();

    wrapper.find('.BookingModal__card-button').simulate('click');

    return mockRequest.then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        async() => {
            await new Promise((resolve) => setTimeout(resolve));
            expect(wrapper.find('Modal').props()).toHaveProperty('visible', false);
            expect(showAutoclosableMessageMock).toHaveBeenCalled();
        },
    );
});
