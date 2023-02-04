jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn().mockImplementation(() => Promise.resolve([ {} ])),
}));

import React from 'react';
import { shallow } from 'enzyme';

import { getResource } from 'auto-core/react/lib/gateApi';

import LazyComponent from './LazyComponent';

const renderContent = () => (<h1>Content</h1>);

describe('должен не рендерить контент', () => {

    it('если ни разу не был во вьюпорте', () => {
        const tree = shallowRenderComponent();
        const observer = tree.find('InView').children();
        expect(observer).toEqual({});
    });

    it('если данных нет', () => {
        const tree = shallowRenderComponent();

        tree.setState({
            data: null,
            visible: false,
            shouldUpdate: true,
        });
        const observer = tree.find('InView').children();
        expect(observer).toEqual({});
    });

});

describe('должен запросить данные', () => {

    it('только при первом попадании во вьюпорт', () => {

        const observer = shallowRenderComponent().find('InView');

        observer.simulate('change', true);
        observer.simulate('change', false);
        observer.simulate('change', true);

        expect(getResource).toHaveBeenCalledTimes(1);
    });

    it('после изменения пропсов при попадании во вьюпорт', () => {
        const tree = shallowRenderComponent();
        const observer = tree.find('InView');

        observer.simulate('change', true);
        observer.simulate('change', false);

        tree.setProps({ resourceParams: { param: 'new value' } });
        observer.simulate('change', true);

        expect(getResource).toHaveBeenCalledTimes(2);
    });

});

it('должен не удалять контент при выходе из вьюпорта', () => {
    const tree = shallowRenderComponent();
    const observer = tree.find('InView');

    observer.simulate('change', true);
    observer.simulate('change', false);

    expect(tree.find('h1')).toExist();
});

it('не должен содержать класс LazyComponent_empty, если данные есть (isEmpty возвращает false)', () => {
    const tree = shallowRenderComponent();
    tree.setProps({
        isEmpty: () => false,
    });
    expect(tree.find('.LazyComponent_empty')).toHaveLength(0);
});

it('должен содержать класс LazyComponent_empty, если данных нет (isEmpty возвращает true)', () => {
    const tree = shallowRenderComponent();
    tree.setProps({
        isEmpty: () => true,
    });
    expect(tree.find('.LazyComponent_empty')).toHaveLength(1);
});

it('должен содержать переданный css класс, если данные есть (isEmpty возвращает false)', () => {
    const tree = shallowRenderComponent();
    tree.setProps({
        className: 'SomeClass',
        isEmpty: () => false,
    });
    expect(tree.find('.SomeClass')).toHaveLength(1);
});

it('не должен содержать переданный css класс, если данных нет (isEmpty возвращает true)', () => {
    const tree = shallowRenderComponent();
    tree.setProps({
        className: 'SomeClass',
        isEmpty: () => true,
    });
    expect(tree.find('.SomeClass')).toHaveLength(0);
});

function shallowRenderComponent() {
    return shallow(
        <LazyComponent
            renderContent={ renderContent }
            resourceName=""
        />,
    );
}
