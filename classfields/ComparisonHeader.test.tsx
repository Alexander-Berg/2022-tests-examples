import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';
import { mockAllIsIntersecting } from 'react-intersection-observer/test-utils';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import ComparisonHeader from './ComparisonHeader';

const renderSortableItemMock = jest.fn(() => {
    return <p>Sortable Item</p>;
});

const props = {
    itemIds: [ '1', '2', '3', '4' ],
    onControlClick: jest.fn(),
    onSortEnd: jest.fn(),
    onDragMove: jest.fn(),
    onAddNewModel: jest.fn(),
    isStart: true,
    isEnd: false,
    isModels: false,
    pageSize: 4,
    renderSortableItem: renderSortableItemMock,
};

describe('кнопки навигации', () => {
    it('должен отрендерить', () => {
        mockAllIsIntersecting(false);
        const wrapper = shallow(
            <ComparisonHeader { ...props } hasControls/>,
            { context: { ...contextMock, store: mockStore({}) } },
        );

        const observer = wrapper.find('InView').renderProp('children')({} as never);
        expect(observer.find('.ComparisonHeader__arrowLeft')).toExist();
    });

    it('не должен отрендерить', () => {
        const wrapper = shallow(
            <ComparisonHeader { ...props }/>,
            { context: { ...contextMock, store: mockStore({}) } },
        );

        const observer = wrapper.find('InView').renderProp('children')({} as never);
        expect(observer.find('.ComparisonHeader__arrowLeft')).not.toExist();
    });
});

describe('кнопка добавления новой модели', () => {
    it('должен отрендерить в сравнении моделей', () => {
        const propsFromModels = {
            ...props,
            itemIds: [ '1', '2' ],
            isModels: true,
        };
        const wrapper = shallow(
            <ComparisonHeader { ...propsFromModels }/>,
            { context: { ...contextMock, store: mockStore({}) } },
        );

        const observer = wrapper.find('InView').renderProp('children')({} as never);
        expect(observer.find('.ComparisonHeader__addItem')).toExist();
    });

    it('не должен отрендерить в сравнении офферов', () => {
        const propsFromModels = {
            ...props,
            itemIds: [ '1', '2' ],
        };
        const wrapper = shallow(
            <ComparisonHeader { ...propsFromModels }/>,
            { context: { ...contextMock, store: mockStore({}) } },
        );

        const observer = wrapper.find('InView').renderProp('children')({} as never);
        expect(observer.find('.ComparisonHeader__addItem')).not.toExist();
    });

    it('не должен отрендерить в сравнении моделей, если не пролистали шапку до конца', () => {
        const propsFromModels = {
            ...props,
            isModels: true,
        };
        const wrapper = shallow(
            <ComparisonHeader { ...propsFromModels }/>,
            { context: { ...contextMock, store: mockStore({}) } },
        );
        const observer = wrapper.find('InView').renderProp('children')({} as never);
        expect(observer.find('.ComparisonHeader__addItem')).not.toExist();
    });
});
