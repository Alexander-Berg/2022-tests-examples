import React from 'react';
import { shallow } from 'enzyme';

import ItemGroupRoot from 'auto-core/react/components/islands/ItemGroupRoot/index';
import Item from 'auto-core/react/components/islands/Item/Item';

import type { State } from './ListItemGroup';
import ListItemGroup from './ListItemGroup';

it('при рендере state opened должен быть false', () => {
    const wrapper = shallow(
        <ListItemGroup onItemClick={ jest.fn } type="check">
            <ItemGroupRoot>
                Элемент
            </ItemGroupRoot>
        </ListItemGroup>,
    );

    const state = wrapper.state() as State;

    expect(state.opened).toEqual(false);
});

it('при вызове onArrowClick должен открыть детей в AnimatedHeight', () => {
    const wrapper = shallow(
        <ListItemGroup onItemClick={ jest.fn } type="check">
            <ItemGroupRoot>
                Элемент
            </ItemGroupRoot>
            <Item>Элемента намба ту</Item>
        </ListItemGroup>,
    );

    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    wrapper.find('ListItem').invoke('onArrowClick')();

    const state = wrapper.state() as State;

    expect(state.opened).toEqual(true);
    expect(wrapper.find('AnimatedHeight').prop('opened')).toEqual(true);
});

it('сразу отрендерит детей, и не будет менять стейт при вызове onArrowClick, если в пропсах был передан openPersistent', () => {
    const wrapper = shallow(
        <ListItemGroup onItemClick={ jest.fn } type="check" openedPersistent={ true }>
            <ItemGroupRoot>
                Элемент
            </ItemGroupRoot>
            <Item>Элемента намба ту</Item>
        </ListItemGroup>,
    );

    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    wrapper.find('ListItem').at(0).invoke('onArrowClick')();

    const state = wrapper.state() as State;

    expect(state.opened).toEqual(false);
    expect(wrapper.find('AnimatedHeight')).not.toExist();
});
