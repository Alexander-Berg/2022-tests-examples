import React from 'react';
import { shallow } from 'enzyme';

import { Actionable } from 'core/client/components/Actionable/Actionable';

import { ButtonMediaNavigation } from './ButtonMediaNavigation';

it('вызываются обработчики переключения при клике', async() => {
    const handlePreviousIndexMock = jest.fn();
    const handleNextIndexMock = jest.fn();

    const wrapper = shallow(
        <ButtonMediaNavigation
            onPreviousIndex={ handlePreviousIndexMock }
            onNextIndex={ handleNextIndexMock }
        />
    );

    wrapper.find(Actionable).at(0).simulate('click');
    wrapper.find(Actionable).at(1).simulate('click');

    expect(handlePreviousIndexMock.mock.calls.length).toBe(1);
    expect(handleNextIndexMock.mock.calls.length).toBe(1);
});

it('кнопки имеют состояние disabled при передаче параметра для отключения', () => {
    const wrapper = shallow(
        <ButtonMediaNavigation
            onPreviousIndex={ () => {} }
            onNextIndex={ () => {} }
            isDisabledPreviousAction
            isDisabledNextAction
        />
    );

    const previous = wrapper.find(Actionable).at(0);
    const next = wrapper.find(Actionable).at(1);

    expect(previous.prop('disabled')).toBe(true);
    expect(next.prop('disabled')).toBe(true);
});
