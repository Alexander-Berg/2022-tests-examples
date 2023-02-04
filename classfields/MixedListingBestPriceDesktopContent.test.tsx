import 'jest-enzyme';
import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import MixedListingBestPriceDesktopContent from './MixedListingBestPriceDesktopContent';

const initialState = {
    matchApplication: {
        allowedMarksModels: { BMW: [ '1', '2', '3' ] },
        marks: [],
        models: [],
    },
    geo: { geoParents: [] },
    bunker: {},
    user: { data: {} },
};

it('должен скрыть блок MixedListingBestPriceDesktopContent после отправки заявки', () => {
    const store = mockStore(initialState);

    const wrapper = shallow(
        <Provider store={ store }>
            <MixedListingBestPriceDesktopContent
                title=""
            />
        </Provider>,
        { context: contextMock },
    ).dive().dive();

    expect(wrapper).not.toBeEmptyRender();

    // Проверяем, выполняется ли сокрытие блока когда в MatchApplicationModalDesktop вызывается onClose
    // У компонентов внутри большая вложенность. И функция вызывается только в одном из стейтов MatchApplicationModalDesktop
    // Поэтому сделал так
    const modalContent = wrapper.find('MatchApplicationModalDesktop');
    (modalContent.props() as any).onSuccessClose();

    expect(wrapper).toBeEmptyRender();
});

it('не должен рендерить блок MixedListingBestPriceDesktopContent если allowedMarksModels пустой', () => {
    const store = mockStore({
        ...initialState,
        matchApplication: {
            ...initialState.matchApplication,
            allowedMarksModels: {},
        },
    });

    const wrapper = shallow(
        <Provider store={ store }>
            <MixedListingBestPriceDesktopContent
                title=""
            />
        </Provider>,
        { context: contextMock },
    ).dive().dive();

    expect(wrapper).toBeEmptyRender();
});
