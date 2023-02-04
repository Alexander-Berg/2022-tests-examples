/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow, mount } from 'enzyme';

import { Curtain, CurtainContent, CurtainHeader } from './Curtain';

let onClose: () => void;

beforeEach(() => {
    onClose = jest.fn();
});

it('передача isOpen при маунте сразу рендерит шторку', () => {
    return new Promise<void>((done) => {
        const wrapper = shallow(
            <Curtain
                isOpen
                onClose={ onClose }
            >
                <CurtainContent>
                    content
                </CurtainContent>
            </Curtain>,
        );

        expect(wrapper.find('CurtainContent')).toHaveLength(1);
        expect(wrapper.state('isOpen')).toBe(false);

        setTimeout(() => {
            expect(wrapper.state('isOpen')).toBe(true);

            done();
        }, 20);
    });

});

it('рендерит контент шторки, если children функция', () => {
    return new Promise<void>((done) => {
        const wrapper = shallow(
            <Curtain
                isOpen
                onClose={ onClose }
            >
                { () => (
                    <CurtainContent>
                        content
                    </CurtainContent>
                ) }
            </Curtain>,
        );

        expect(wrapper.find('CurtainContent')).toHaveLength(1);

        done();
    });
});

it('рендерит шторку при изменении isOpen', () => {
    return new Promise<void>((done) => {
        const wrapper = shallow(
            <Curtain
                onClose={ onClose }
            >
                { () => (
                    <CurtainContent>
                        content
                    </CurtainContent>
                ) }
            </Curtain>,
        );

        expect(wrapper.find('CurtainContent')).toHaveLength(0);

        wrapper.setProps({
            isOpen: true,
        });

        expect(wrapper.find('CurtainContent')).toHaveLength(1);
        expect(wrapper.state('isOpen')).toBe(false);

        setTimeout(() => {
            wrapper.update();
            expect(wrapper.state('isOpen')).toBe(true);

            done();
        }, 20);
    });
});

it('при закрытии не стирает контент сразу', () => {
    return new Promise<void>((done) => {
        const wrapper = shallow(
            <Curtain
                isOpen
                onClose={ onClose }
            >
                { () => (
                    <CurtainContent>
                        content
                    </CurtainContent>
                ) }
            </Curtain>,
        );

        expect(wrapper.find('CurtainContent')).toHaveLength(1);

        setTimeout(() => {
            expect(wrapper.state('isOpen')).toBe(true);

            wrapper.setProps({
                isOpen: false,
            });

            wrapper.update();

            expect(wrapper.state('isOpen')).toBe(false);
            expect(wrapper.find('CurtainContent')).toHaveLength(1);

            // на самом деле поменяется после анимашек
            (wrapper.instance() as Curtain).handleToggleTransitionEnd(Curtain.TOGGLE_PHASES.CLOSE);
            wrapper.update();
            expect(wrapper.find('CurtainContent')).toHaveLength(0);

            done();
        }, 20);
    });
});

describe('крестик', () => {
    it('должен отрендерить крестик', () => {
        const wrapper = shallow(
            <Curtain
                showCloser={ true }
                onClose={ onClose }
            >
                { () => (
                    <CurtainContent>
                        content
                    </CurtainContent>
                ) }
            </Curtain>,
        );

        expect(wrapper.find('.Curtain__closer')).toHaveLength(2);
    });

    it('прокидывает в CurtainHeader проп withPlaceForCloser, если крестик включен', () => {
        const wrapper = mount(
            <Curtain
                isOpen={ true }
                showCloser={ true }
                onClose={ onClose }
            >
                { () => (
                    <>
                        <CurtainHeader>
                            Header
                        </CurtainHeader>
                        <CurtainContent>
                            content
                        </CurtainContent>
                    </>
                ) }
            </Curtain>,
        );

        expect(wrapper.find('CurtainHeader').prop('withPlaceForCloser')).toBe(true);
    });

    it('не должен отрендерить крестик', () => {
        const wrapper = shallow(
            <Curtain
                showCloser={ false }
                onClose={ onClose }
            >
                { () => (
                    <CurtainContent>
                        content
                    </CurtainContent>
                ) }
            </Curtain>,
        );

        expect(wrapper.find('.Curtain__closer')).toHaveLength(0);
    });

    it('не прокидывает в CurtainHeader проп withPlaceForCloser, если крестик выключен', () => {
        const wrapper = shallow(
            <Curtain
                isOpen={ true }
                showCloser={ false }
                onClose={ onClose }
            >
                { () => (
                    <>
                        <CurtainHeader>
                            Header
                        </CurtainHeader>
                        <CurtainContent>
                            content
                        </CurtainContent>
                    </>
                ) }
            </Curtain>,
        );

        expect(wrapper.find('CurtainHeader').prop('withPlaceForCloser')).toBe(false);
    });
});

describe('onClose', () => {
    it('должен вызвать onClose при клике на крестик', () => {
        const onClose = jest.fn();

        const wrapper = shallow(
            <Curtain
                showCloser={ true }
                onClose={ onClose }
            >
                { () => (
                    <CurtainContent>
                        content
                    </CurtainContent>
                ) }
            </Curtain>,
        );

        wrapper.find('.Curtain__closer').first().simulate('click');
        wrapper.find('.Curtain__closer').last().simulate('click');

        expect(onClose).toHaveBeenCalledTimes(2);
    });

    it('должен вызвать onClose при нажатии ESC', () => {
        return new Promise<void>((done) => {
            const onClose = jest.fn();

            const wrapper = mount(
                <Curtain
                    isOpen
                    showCloser={ true }
                    onClose={ onClose }
                >
                    { () => (
                        <CurtainContent>
                            content
                        </CurtainContent>
                    ) }
                </Curtain>,
            );

            setTimeout(() => {
                wrapper.update();

                window.document.dispatchEvent(new KeyboardEvent('keydown', { bubbles: true, keyCode: 27 } as KeyboardEventInit));

                expect(onClose).toHaveBeenCalledTimes(1);

                done();
            }, 20);
        });
    });

    it('должен вызвать onClose при клике на документ', () => {
        return new Promise<void>((done) => {
            const onClose = jest.fn();

            const wrapper = mount(
                <div className="document">
                    бла-бла

                    <Curtain
                        isOpen
                        showCloser={ true }
                        onClose={ onClose }
                    >
                        { () => (
                            <CurtainContent>
                                content
                            </CurtainContent>
                        ) }
                    </Curtain>
                </div>,
            );

            setTimeout(() => {
                wrapper.update();

                window.document.dispatchEvent(new Event('mousedown', { bubbles: true }));

                expect(onClose).toHaveBeenCalledTimes(1);

                done();
            }, 20);
        });
    });

    it('не должен вызвать onClose при клике внутри', () => {
        return new Promise<void>((done) => {
            const onClose = jest.fn();

            const wrapper = mount(
                <div className="document">
                    бла-бла

                    <Curtain
                        isOpen
                        showCloser={ true }
                        onClose={ onClose }
                    >
                        { () => (
                            <CurtainContent>
                                content
                            </CurtainContent>
                        ) }
                    </Curtain>
                </div>,
            );

            setTimeout(() => {
                wrapper.update();

                wrapper.find('.CurtainContent').getDOMNode().dispatchEvent(new Event('click', { bubbles: true }));

                expect(onClose).toHaveBeenCalledTimes(0);

                done();
            }, 20);
        });
    });

    it('должен вызвать onClose при клике на оверлей', () => {
        const onClose = jest.fn();

        const wrapper = shallow(
            <Curtain
                showCloser={ true }
                onClose={ onClose }
            >
                { () => (
                    <CurtainContent>
                        content
                    </CurtainContent>
                ) }
            </Curtain>,
        );

        wrapper.find('.Curtain__overlay').simulate('click');

        expect(onClose).toHaveBeenCalledTimes(1);
    });
});
