/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */
jest.mock('auto-core/react/dataDomain/garageCard/actions/update', () => jest.fn());
jest.mock('auto-core/react/dataDomain/garage/actions/getListing', () => jest.fn());
jest.mock('auto-core/react/dataDomain/garageCard/actions/setState', () => jest.fn());

import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import type { UpdateCardAction } from 'auto-core/react/dataDomain/garageCard/types';
import updateCard from 'auto-core/react/dataDomain/garageCard/actions/update';
import getListing from 'auto-core/react/dataDomain/garage/actions/getListing';
import setCardState from 'auto-core/react/dataDomain/garageCard/actions/setState';

import garageCardMock from 'auto-core/models/garageCard/mocks/mock-license_place';

import GarageCardEditForm from './GarageCardEditForm';

const store = mockStore();

const originalCard = garageCardMock.card;
const updatedCard = _.cloneDeep(garageCardMock.card);
updatedCard.id = '111222333hahaha';

const updateCardMocked = updateCard as jest.MockedFunction<typeof updateCard>;
updateCardMocked.mockImplementation(() => {
    return {
        type: 'GARAGE_CARD_UPDATE',
        payload: { card: updatedCard },
    } as UpdateCardAction;
});

const getListingMocked = getListing as jest.MockedFunction<typeof getListing>;
getListingMocked.mockImplementation(() => jest.fn().mockResolvedValue(1));

const setCardStateMocked = setCardState as jest.MockedFunction<typeof setCardState>;
setCardStateMocked.mockImplementation(() => jest.fn().mockResolvedValue(1));

const handleDeleteMock = jest.fn();

const renderComponent = () => shallow(
    <GarageCardEditForm
        garageCard={ originalCard }
        onDelete={ handleDeleteMock }
        type="current"
    />
    ,
    { context: { ...contextMock, store } },
);

describe('onSubmit', () => {
    it('дернет экшены обновления', () => {
        const wrapper = renderComponent();

        expect(updateCardMocked).toHaveBeenCalledTimes(0);
        wrapper.dive().simulate('submit', updatedCard);
        expect(updateCardMocked).toHaveBeenCalledTimes(1);
        expect(updateCardMocked).toHaveBeenCalledWith(updatedCard);
    });

    it('дернет экшен перезапроса листинга', () => {
        const wrapper = renderComponent();

        expect(getListingMocked).toHaveBeenCalledTimes(0);
        wrapper.dive().simulate('submit');
        expect(getListingMocked).toHaveBeenCalledTimes(1);
    });

    it('дернет экшен смены состояния гаражной карточки', () => {
        const wrapper = renderComponent();

        expect(setCardStateMocked).toHaveBeenCalledTimes(0);
        wrapper.dive().simulate('submit');
        expect(setCardStateMocked).toHaveBeenCalledTimes(1);
        expect(setCardStateMocked).toHaveBeenCalledWith('VIEW');
    });
});

describe('onDelete', () => {
    it('дернет колбек удаления карточки', () => {
        const wrapper = renderComponent();

        expect(handleDeleteMock).toHaveBeenCalledTimes(0);
        wrapper.dive().simulate('delete');
        expect(handleDeleteMock).toHaveBeenCalledTimes(1);
    });
});
