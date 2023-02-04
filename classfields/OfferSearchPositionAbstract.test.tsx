/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/localstorage', () => ({
    getItem: jest.fn(),
    setItem: jest.fn(),
}));

import React from 'react';
import { shallow } from 'enzyme';
import MockDate from 'mockdate';

import ls from 'auto-core/react/lib/localstorage';

import type { Props } from './OfferSearchPositionAbstract';
import OfferSearchPositionAbstract from './OfferSearchPositionAbstract';

const getLsItem = ls.getItem as jest.MockedFunction<typeof ls.getItem>;

const OFFER_ID = 'offer-id';

let props: Props;

beforeEach(() => {
    props = {
        isFeatured: false,
        offerId: OFFER_ID,
        position: 420,
    };

    jest.useFakeTimers();

    MockDate.set('2020-05-20');
});

it('плавно меняет позицию вверх', () => {
    const page = shallowRenderComponent({ props });
    page.setProps({ isFeatured: true });

    jest.advanceTimersByTime(150);
    expect(page.state().value).toBe(420);

    jest.advanceTimersByTime(200);
    expect(page.state().value).toBe(315.25);

    jest.advanceTimersByTime(600);
    expect(page.state().value).toBe(1);
});

it('плавно меняет позицию вниз', () => {
    const page = shallowRenderComponent({ props });
    page.setProps({ isFeatured: true });
    jest.advanceTimersByTime(150 + 800);
    page.setProps({ isFeatured: false });

    jest.advanceTimersByTime(150);
    expect(page.state().value).toBe(1);

    jest.advanceTimersByTime(200);
    expect(page.state().value).toBe(105.75);

    jest.advanceTimersByTime(600);
    expect(page.state().value).toBe(420);
});

it('если есть данные о предыдущей позиции в поиске, будет плавно менять и ее', () => {
    getLsItem.mockReturnValueOnce(JSON.stringify([ { offerId: OFFER_ID, ts: Date.now(), position: 400 } ]));
    const page = shallowRenderComponent({ props });
    page.setProps({ isFeatured: true });

    jest.advanceTimersByTime(150);
    expect(page.state().diffValue).toBe(-20);

    jest.advanceTimersByTime(200);
    expect(page.state().diffValue).toBe(89.75);

    jest.advanceTimersByTime(600);
    expect(page.state().diffValue).toBe(419);
});

it('корректно отработает быструю смену пропа isFeatured', () => {
    getLsItem.mockReturnValueOnce(JSON.stringify([ { offerId: OFFER_ID, ts: Date.now(), position: 400 } ]));
    const page = shallowRenderComponent({ props });

    page.setProps({ isFeatured: true });
    jest.advanceTimersByTime(5);

    page.setProps({ isFeatured: false });
    jest.advanceTimersByTime(200);

    expect(page.state().value).toBe(420);
    expect(page.state().diffValue).toBe(-20);
});

function shallowRenderComponent({ props }: { props: Props }) {
    class ComponentMock extends OfferSearchPositionAbstract<Props> {
        renderContent() {
            return <div>Buy</div>;
        }
    }

    const page = shallow<ComponentMock>(
        <ComponentMock { ...props }/>,
    );

    return page;
}
