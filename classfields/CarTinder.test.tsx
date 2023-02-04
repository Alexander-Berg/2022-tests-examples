import React from 'react';
import { Provider } from 'react-redux';
import userEvent from '@testing-library/user-event';
import { render, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import mockStore from 'autoru-frontend/mocks/mockStore';

import equipmentDictionaryMock from 'auto-core/react/dataDomain/equipmentDictionary/mocks/dictionary';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import type { Props } from './CarTinder';
import CarTinder from './CarTinder';

const INITIAL_PROPS: Props = {
    slideList: [
        cloneOfferWithHelpers(offerMock).withSaleId('111-111').value(),
        cloneOfferWithHelpers(offerMock).withSaleId('222-222').value(),
        cloneOfferWithHelpers(offerMock).withSaleId('333-333').value(),
    ],
    userOffer: offerMock,
    onLike: jest.fn(),
    onDislike: jest.fn(),
    onCurrentSlideSet: jest.fn(),
    onFiltersClick: jest.fn(),
    onLoadMore: jest.fn(),
    isLoading: false,
    isEmpty: false,
    hasError: false,
    shouldShowOnboardingReturnTooltip: false,
    showAutoclosableMessage: jest.fn(),
    hideMessage: jest.fn(),
};

const INITIAL_STATE = {
    equipmentDictionary: equipmentDictionaryMock,
    cardGroupComplectations: { data: {} },
};

describe('CarTinder', () => {
    it('при клике на шаг назад отправляет метрику и вызывает onCurrentSlideSet', async() => {
        const onCurrentSlideSetMocked = jest.fn();

        const props = {
            ...INITIAL_PROPS,
            onCurrentSlideSet: onCurrentSlideSetMocked,
        };

        render(getComponent(props));

        const dislikeButton = document.getElementsByClassName('Tinder__mainControl_dislike')[0];
        const returnButton = document.getElementsByClassName('CarTinder__control_back')[0];

        userEvent.click(dislikeButton);
        userEvent.click(returnButton);

        expect(onCurrentSlideSetMocked).toHaveBeenCalledTimes(3);
        const parametersOfCalls = onCurrentSlideSetMocked.mock.calls.map(call => call[0]);
        expect(parametersOfCalls).toEqual([
            cloneOfferWithHelpers(offerMock).withSaleId('111-111').value(),
            cloneOfferWithHelpers(offerMock).withSaleId('222-222').value(),
            cloneOfferWithHelpers(offerMock).withSaleId('111-111').value(),
        ]);
        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'car-tinder', 'card', 'back', 'click' ]);
    });

    it('при клике на карточку отправит метрику и откроет модал', async() => {
        const onCurrentSlideSetMocked = jest.fn();

        const props = {
            ...INITIAL_PROPS,
            onCurrentSlideSet: onCurrentSlideSetMocked,
        };

        const { container } = render(getComponent(props));

        userEvent.click(container.getElementsByClassName('CarTinderSlide__content')[0]);

        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'car-tinder', 'card', 'info', 'click' ]);
        await waitFor(() => {
            const tooltipContent = document.querySelector('.CarTinderInfoCurtain');
            expect(tooltipContent).toBeInTheDocument();
        });
    });
});

function getComponent(props: Props = INITIAL_PROPS) {
    const Context = createContextProvider(contextMock);

    return (
        <Provider store={ mockStore(INITIAL_STATE) }>
            <Context>
                <CarTinder { ...props }/>
            </Context>
        </Provider>
    );
}
