jest.mock('../utils/loader');
jest.mock('auto-core/react/lib/onDomContentLoaded', () => {
    return (callback: () => void) => callback();
});

import _ from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';
import type ymaps from 'yandex-maps';
import { InView } from 'react-intersection-observer';

import panoramaInteriorMock from 'auto-core/models/panoramaInterior/mocks';

import loader from '../utils/loader';

import type { BaseProps, BaseState } from './PanoramaInteriorBase';
import PanoramaInteriorBase from './PanoramaInteriorBase';

type YMaps = typeof ymaps;

const lib = {} as unknown as YMaps;
const loadPromise = Promise.resolve(lib);
type Loader = {
    load: () => Promise<YMaps>;
}
const loaderMock = loader as unknown as jest.MockedFunction<() => Loader>;
const loadMock = jest.fn(() => loadPromise);
loaderMock.mockReturnValue({
    load: loadMock,
});

class ComponentMock extends PanoramaInteriorBase<BaseProps, BaseState> {
    renderZoomControls() {
        return null;
    }

    renderPullToSlideAnimation() {
        return null;
    }

    getComponentClassNames() {
        return '';
    }

    getPainterClassName() {
        return '';
    }

    hidePressAndSpin = jest.fn()
}

let props: BaseProps;

beforeEach(() => {
    props = {
        data: panoramaInteriorMock.value(),
        isCurrentSlide: true,
        isPressAndSpinHidden: false,
        isTouch: false,
        loadStrategy: 'mount',
        place: 'card_page',
        onDragStateChange: jest.fn(),
        onTap: jest.fn(),
    };
});

it('если панорама грузится по скроллу, то начнет загрузку только после попадания блока во вьюпорт', () => {
    props.loadStrategy = 'intersection';
    const { page } = shallowRenderComponent({ props });

    expect(loadMock).toHaveBeenCalledTimes(0);

    const observer = page.find(InView);
    observer.simulate('change', true);

    expect(loadMock).toHaveBeenCalledTimes(1);
});

describe('промо "нажми и вращай"', () => {
    beforeEach(() => {
        props.isPressAndSpinHidden = false;
    });

    it('показывается если все условия соблюдены', () => {
        const { page } = shallowRenderComponent({ props });

        return loadPromise
            .then(() => {
                const promo = page.find('PanoramaPressAndSpin');
                expect(promo.isEmptyRender()).toBe(false);
            });
    });

    describe('не показывается', () => {
        it('если скрыли до этого', () => {
            props.isPressAndSpinHidden = true;
            const { page } = shallowRenderComponent({ props });

            return loadPromise
                .then(() => {
                    const promo = page.find('PanoramaPressAndSpin');
                    expect(promo.isEmptyRender()).toBe(true);
                });
        });

        it('если панорама еще не загрузилась', () => {
            const { page } = shallowRenderComponent({ props });

            const promo = page.find('PanoramaPressAndSpin');
            expect(promo.isEmptyRender()).toBe(true);
        });
    });

    describe('при опускании мыши на панораму', () => {
        it('вызовет метод если клик был внутри контейнера', () => {
            const { componentInstance, page } = shallowRenderComponent({ props });

            return loadPromise
                .then(() => {
                    page.simulate('mouseDown', {
                        stopPropagation: _.noop,
                        target: { closest: () => true },
                    });

                    expect(componentInstance.hidePressAndSpin).toHaveBeenCalledTimes(1);
                });
        });

        it('не вызовет метод если клик был снаружи контейнера', () => {
            const { componentInstance, page } = shallowRenderComponent({ props });

            return loadPromise
                .then(() => {
                    page.simulate('mouseDown', {
                        stopPropagation: _.noop,
                        target: { closest: () => false },
                    });

                    expect(componentInstance.hidePressAndSpin).toHaveBeenCalledTimes(0);
                });
        });
    });

    describe('при опускании пальца на панораму', () => {
        it('вызовет метод если тап был внутри контейнера', () => {
            const { componentInstance, page } = shallowRenderComponent({ props });

            return loadPromise
                .then(() => {
                    page.simulate('touchStart', {
                        stopPropagation: _.noop,
                        target: { closest: () => true },
                    });

                    expect(componentInstance.hidePressAndSpin).toHaveBeenCalledTimes(1);
                });
        });

        it('не вызовет метод если тап был снаружи контейнера', () => {
            const { componentInstance, page } = shallowRenderComponent({ props });

            return loadPromise
                .then(() => {
                    page.simulate('touchStart', {
                        stopPropagation: _.noop,
                        target: { closest: () => false },
                    });

                    expect(componentInstance.hidePressAndSpin).toHaveBeenCalledTimes(0);
                });
        });
    });
});

function shallowRenderComponent({ props }: { props: BaseProps }) {
    const page = shallow(
        <ComponentMock { ...props }/>
        ,
    );

    const componentInstance = page.instance() as ComponentMock;

    return {
        page,
        componentInstance,
    };
}
