/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { mount } from 'enzyme';
import 'jest-enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import iconFilter16 from 'auto-core/icons/filter-16.svg';

import IconSvg from '../IconSvg/IconSvg';

import Tag, { TYPE } from './Tag';

const Context = createContextProvider(contextMock);

describe('onClick', () => {
    let onClickTag: jest.Mock;
    let onClickIcon: jest.Mock;
    beforeEach(() => {
        onClickTag = jest.fn();
        onClickIcon = jest.fn();
    });

    it('вызовет onClick', () => {
        const wrapper = mount(
            <Context>
                <Tag type={ TYPE.FILTER } onClick={ onClickTag }/>
            </Context>,
        );
        wrapper.find('.Tag').simulate('click');

        expect(onClickTag).toHaveBeenCalledTimes(1);
    });

    it('вызовет onClick и метрику sendPageEvent', () => {
        const wrapper = mount(
            <Context>
                <Tag metrika="foo,bar" type={ TYPE.FILTER } onClick={ onClickTag }/>
            </Context>,
        );
        wrapper.find('.Tag').simulate('click');

        expect(onClickTag).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'foo', 'bar' ]);
    });

    it('вызовет onClick и метрику reachGoal', () => {
        const wrapper = mount(
            <Context>
                <Tag metrikaGoal="foo,bar" type={ TYPE.FILTER } onClick={ onClickTag }/>
            </Context>,
        );
        wrapper.find('.Tag').simulate('click');

        expect(onClickTag).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(2);
        expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('foo');
        expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('bar');
    });

    it('вызовет onClick при клике на иконку и не вызовет общий onClick', () => {
        const wrapper = mount(
            <Context>
                <Tag metrikaGoal="foo,bar" type={ TYPE.FILTER } onClick={ onClickTag }>
                    <IconSvg id={ iconFilter16 } size={ IconSvg.SIZE.SIZE_16 } onClick={ onClickIcon }/>
                    Параметры
                </Tag>
            </Context>,
        );

        wrapper.find('.IconSvg').simulate('click');

        expect(onClickIcon).toHaveBeenCalledTimes(1);
        expect(onClickTag).toHaveBeenCalledTimes(0);
    });

    it('вызовет onClick при клике на обертку иконки и не вызовет общий onClick', () => {
        const wrapper = mount(
            <Context>
                <Tag metrikaGoal="foo,bar" type={ TYPE.FILTER } onClick={ onClickTag }>
                    <IconSvg id={ iconFilter16 } size={ IconSvg.SIZE.SIZE_16 } onClick={ onClickIcon }/>
                    Параметры
                </Tag>
            </Context>,
        );

        wrapper.find('.Tag__child_action').simulate('click');

        expect(onClickIcon).toHaveBeenCalledTimes(1);
        expect(onClickTag).toHaveBeenCalledTimes(0);
    });
});

it('не будет рисовать лишний элемент', () => {
    const hasIcon = false;
    const wrapper = mount(
        <Context>
            <Tag metrikaGoal="foo,bar" type={ TYPE.FILTER } onClick={ () => {} }>
                { hasIcon && <IconSvg id={ iconFilter16 } size={ IconSvg.SIZE.SIZE_16 } onClick={ () => {} }/> }
            </Tag>
        </Context>,
    );

    expect(wrapper.find('.Tag__child')).not.toExist();
});
