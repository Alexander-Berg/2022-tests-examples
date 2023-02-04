jest.mock('auto-core/react/dataDomain/state/actions/userPromoPopupOpen');

import MockDate from 'mockdate';
import React from 'react';
import { shallow } from 'enzyme';
import type { Action } from 'redux';

import userPromoPopupOpen from 'auto-core/react/dataDomain/state/actions/userPromoPopupOpen';

import VasDiscountPlate from './VasDiscountPlate';
import type { Props } from './VasDiscountPlate';

const userPromoPopupOpenMock = userPromoPopupOpen as jest.MockedFunction<typeof userPromoPopupOpen>;

userPromoPopupOpenMock.mockReturnValue((() => () => {}) as unknown as Action<any>);

let props: Props;

beforeEach(() => {
    props = {
        value: 70,
        expireDate: '2020-02-13T21:00:00Z',
        dispatch: jest.fn(),
    };
});

it('скроет компонент когда таймер выйдет', () => {
    MockDate.set('2020-02-13T17:00:00Z');
    const page = shallowRenderComponent({ props });
    const timer = page.find('Timer');
    timer.simulate('timerFinish');

    expect(page.isEmptyRender()).toBe(true);
});

it('при клике вызовет модал', () => {
    MockDate.set('2020-02-13T17:00:00Z');
    const page = shallowRenderComponent({ props });
    page.simulate('click');

    expect(userPromoPopupOpen).toHaveBeenCalledTimes(1);
});

function shallowRenderComponent({ props }: { props: Props }) {
    return shallow(
        <VasDiscountPlate { ...props }/>,
    );
}
