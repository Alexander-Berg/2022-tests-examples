/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import type { ReactWrapper } from 'enzyme';
import React from 'react';
import { mount, shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';
import MockDate from 'mockdate';

import { SECOND, MINUTE } from 'auto-core/lib/consts';

import Timer from 'auto-core/react/components/common/Timer/Timer';

import type { Props } from './VasDiscountAbstract';
import VasDiscountAbstract from './VasDiscountAbstract';

declare var global: { location: Record<string, any> };
const { location } = global;

//@see https://github.com/facebook/jest/issues/11551
jest.useFakeTimers('legacy');

class TestComponent extends VasDiscountAbstract<Props> {
    renderContent() {
        return (
            <div>
                tiny test text
                { /* таймер тут, чтобы следить за показаниями сброшенного времени */ }
                <Timer onTimerFinish={ this.handleTimerFinish } expiresDate={ this.state.expirationTime } key={ this.state.timerKey }/>
            </div>
        );
    }
}

afterEach(() => {
    MockDate.reset();
});

describe('правильно рисует компонент:', () => {
    const testCases = [
        {
            name: 'если цены одинаковы то ничего не рисует',
            props: { originalPrice: 999, price: 999, updatePricesHandler: jest.fn() },
        },
        {
            name: 'если обычная цена меньше то ничего не рисует',
            props: { originalPrice: 777, price: 999, updatePricesHandler: jest.fn() },
        },
        {
            name: 'если обычной цены нет то ничего не рисует',
            props: { price: 999, updatePricesHandler: jest.fn() },
        },
        {
            name: 'если цена меньше обычной цены то рисует скидку и таймер',
            props: { originalPrice: 999, price: 777, updatePricesHandler: jest.fn() },
        },
    ];

    testCases.forEach(({ name, props }) => {
        it(name, () => {
            MockDate.set('2019-02-26T13:13:13.000+0300');
            const tree = shallow(<TestComponent { ...props }/>);
            expect(shallowToJson(tree)).toMatchSnapshot();
        });
    });
});

describe('тестирование таймера', () => {
    const props = {
        originalPrice: 999,
        price: 777,
    };

    describe('при начальном рендере', () => {
        let timer: ReactWrapper;

        beforeAll(() => {
            // в москве '2019-02-25 20:30:00', зона магадан +11:00 к utc, зона москвы +03:00 к utc
            MockDate.set('2019-02-26T04:30:00.000+1100');
            timer = mount(<TestComponent { ...props }/>).find('.Timer');
        });

        it('правильно рассчитывает оставшееся время скидки', () => {
            expect(timer).toHaveLength(1);
            expect(timer.text()).toBe('03:29:59');
        });

        it('запускает таймер', () => {
            MockDate.set('2019-02-26T04:30:01.000+1100');
            jest.advanceTimersByTime(SECOND);
            expect(timer.text()).toBe('03:29:58');
        });
    });

    describe('когда таймер выйдет', () => {
        it('если хэндлер не передан остановит таймер и не будет ничего делать', () => {
            MockDate.set('2019-02-26T23:59:49.000+0300');
            const timer = mount(<TestComponent { ...props }/>).find('.Timer');

            MockDate.set('2019-02-27T00:00:05.000+0300');
            jest.advanceTimersByTime(SECOND);
            expect(timer.text()).toBe('00:00:00');
        });

        describe('если хэндлер передан', () => {
            it('вызовет его через 2 секунды как закончится таймер', () => {
                MockDate.set('2019-02-26T23:59:49.000+0300');
                const handler = jest.fn();
                mount(<TestComponent { ...props } updatePricesHandler={ handler }/>).find('.Timer');

                MockDate.set('2019-02-26T23:59:59.000+0300');
                jest.advanceTimersByTime(SECOND);
                expect(handler).not.toHaveBeenCalled();

                jest.advanceTimersByTime(2 * SECOND);
                expect(handler).toHaveBeenCalledTimes(1);
            });

            it('перезапустит таймер когда зарезолвится хэндлер', () => {
                MockDate.set('2019-02-26T23:59:49.000+0300');
                const pr = Promise.resolve();
                const handler = jest.fn(() => pr);
                const wrapper = mount(<TestComponent { ...props } updatePricesHandler={ handler }/>);

                // таймер вышел, должен запустится коллбэк
                MockDate.set('2019-02-26T23:59:59.000+0300');
                jest.advanceTimersByTime(SECOND);

                // прошло какое-то время
                MockDate.set('2019-02-27T00:00:05.000+0300');
                jest.advanceTimersByTime(2 * SECOND);

                return pr.then(() => {
                    wrapper.update();
                    const timer = wrapper.find('.Timer');
                    expect(timer.text()).toBe('23:59:54');
                });
            });

            it('будет дергать хэндлер каждые 30 минут', () => {
                MockDate.set('2019-02-26T13:13:13.000+0300');
                const handler = jest.fn();
                mount(<TestComponent { ...props } updatePricesHandler={ handler }/>).find('.Timer');

                jest.advanceTimersByTime(30 * MINUTE + 2 * SECOND);
                expect(handler).toHaveBeenCalledTimes(1);

                jest.advanceTimersByTime(30 * MINUTE + SECOND);
                expect(handler).toHaveBeenCalledTimes(2);
            });

        });
    });

    describe('если хэндлер передан и при вызове он упал', () => {

        beforeAll(() => {
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-ignore
            delete global.location;
            global.location = {
                reload: jest.fn(),
            };
        });

        afterAll(() => {
            global.location = location;
        });

        it('страница будет перезагружена', () => {
            MockDate.set('2019-02-26T23:59:49.000+0300');
            const pr = Promise.reject();
            const handler = jest.fn(() => pr);

            mount(<TestComponent { ...props } updatePricesHandler={ handler }/>);

            // таймер вышел, должен запустится коллбэк
            MockDate.set('2019-02-26T23:59:59.000+0300');
            jest.advanceTimersByTime(3 * SECOND);

            return pr.then(
                () => Promise.reject('UNEXPECTED_RESOLVE'),
                async() => {
                    await new Promise((resolve) => process.nextTick(resolve));
                    expect(global.location.reload).toHaveBeenCalledTimes(1);
                },
            );
        });
    });
});
