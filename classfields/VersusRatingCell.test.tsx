import React from 'react';
import { shallow } from 'enzyme';
import 'jest-enzyme';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import type { TVersusModel } from 'auto-core/react/dataDomain/versus/TStateVersus';
import versusMock from 'auto-core/react/dataDomain/versus/mock';

import VersusRatingCell from './VersusRatingCell';

const ContextProvider = createContextProvider(contextMock);
const versus = versusMock.value();

it('не должен падать и что-то рисовать, если не передана модель', () => {
    const tree = shallow(
        <ContextProvider>
            <VersusRatingCell model={{} as TVersusModel}/>
        </ContextProvider>,
    ).dive();

    expect(tree).toBeEmptyRender();
});

it('передаёт правильные параметры для ссылки на отзывы', () => {
    const tree = shallow(
        <ContextProvider>
            <VersusRatingCell model={ versus[0] }/>
        </ContextProvider>,
    ).dive();

    const link = tree.find('RatingInfo').dive().find('Link');

    expect(link).toHaveProp('url', 'link/reviews-listing/?mark=FORD&model=ECOSPORT&super_gen=20104320&parent_category=cars');
});

it('если отзывов нет - пишет соответствующее сообщение', () => {
    const tree = shallow(
        <ContextProvider>
            <VersusRatingCell model={ versus[1] }/>
        </ContextProvider>,
    ).dive();

    const text = tree.find('.VersusRatingCell').text();

    expect(text).toEqual('На эту модель пока нет отзывов');
});
