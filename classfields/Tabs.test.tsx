import React from 'react';
import { mount } from 'enzyme';

import { Tabs } from './Tabs';
import { TabsList } from './TabsList/TabsList';
import { TabsItem } from './TabsItem/TabsItem';
import { TabsContent } from './TabsContent/TabsContent';

it('правильно выставляет aria атрибуты', () => {
    const wrapper = mount(<WrapperComponent/>);

    expect(wrapper.find('.root').at(0).props().role).toBe('tablist');

    expect(wrapper.find('button').at(0).props()['aria-selected']).toBe(true);
    expect(wrapper.find('button').at(0).props().disabled).toBe(true);
    expect(wrapper.find('button').at(0).props().role).toBe('tab');

    expect(wrapper.find('ul').props().role).toBe('tabpanel');
});

it('правильно пробрасывает className', () => {
    const wrapper = mount(<WrapperComponent className="testclass"/>);

    expect(wrapper.find('div').at(0).props().className).toBe('testclass');
});

const mockItems = [
    { value: 'first', title: 'Первый' },
    { value: 'second', title: 'Второй' },
    { value: 'third', title: 'Третий' },
    { value: 'fourth', title: 'Четвёртый' },
];

function WrapperComponent({ className }: { className?: string}) {
    return (
        <Tabs value="first" className={ className }>
            <TabsList>
                { mockItems.map((item) => (
                    <TabsItem key={ item.value } value={ item.value }>
                        { item.title }
                    </TabsItem>
                )) }
            </TabsList>

            <TabsContent align="center" as="ul">
                Привет
            </TabsContent>
        </Tabs>
    );
}
