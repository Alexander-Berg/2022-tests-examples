import 'jest-enzyme';
import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import BestPriceDesktop, { TYPES } from './BestPriceDesktop';

const store = mockStore({
    matchApplication: {
        allowedMarksModels: { BMW: [ '1', '2', '3' ] },
        marks: [],
        models: [],
        isPending: false,
        hasError: false,
    },
    user: { data: {} },
});

it('должен скрыть блок после отправки заявки', () => {
    const wrapper = shallow(
        <Provider store={ store }>
            <BestPriceDesktop
                title=""
                type={ TYPES.CARD }
                geoId={ [ '213' ] }
            />
        </Provider>,
        { context: contextMock },
    ).dive().dive();

    expect(wrapper).not.toBeEmptyRender();

    const MatchApplicationButtonDumbWrapper = wrapper.find('Connect(MatchApplicationButtonDumb)').dive().find('MatchApplicationButtonDumb').dive();

    MatchApplicationButtonDumbWrapper.find('Button').simulate('click');

    // Проверяем, выполняется ли сокрытие блока когда в MatchApplicationModalDesktop вызывается onClose
    // У компонентов внутри большая вложенность. И функция вызывается только в одном из стейтов MatchApplicationModalDesktop
    // Поэтому сделал так
    const modalContent = MatchApplicationButtonDumbWrapper.find('MatchApplicationModalDesktop');
    (modalContent.props() as any).onSuccessClose();

    expect(wrapper).toBeEmptyRender();
});
