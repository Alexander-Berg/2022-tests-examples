/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';
import type { DebouncedFunc } from 'lodash';
import _ from 'lodash';

import { panoramaHotSpotMock } from 'auto-core/react/dataDomain/panoramaHotSpots/mocks';

import type { Props } from './PanoramaHotSpot';
import PanoramaHotSpot from './PanoramaHotSpot';

let props: Props;
let originalWindowPageOffsetX: number;
let originalWindowPageOffsetY: number;

beforeEach(() => {
    props = {
        index: 0,
        isPending: false,
        isPostError: false,
        isPortalOpened: false,
        isOwner: true,
        onRemove: jest.fn(),
        onClick: jest.fn(),
        onDragStart: jest.fn(),
        onDragMove: jest.fn(),
        onDragEnd: jest.fn(),
        onMouseEnter: jest.fn(),
        onMouseLeave: jest.fn(),
        setSpotRef: jest.fn(),
        spot: panoramaHotSpotMock.value(),
    };

    originalWindowPageOffsetX = global.pageXOffset;
    originalWindowPageOffsetY = global.pageYOffset;
    global.pageXOffset = 100;
    global.pageYOffset = 200;
});

afterEach(() => {
    global.pageXOffset = originalWindowPageOffsetX;
    global.pageYOffset = originalWindowPageOffsetY;
});

describe('перетаскивание точки', () => {
    it('при муве и дропе вызовет проп и передаст в него координаты', () => {
        const page = shallowRenderComponent({ props });

        page.simulate('mouseDown', { clientX: 300, clientY: 200, target: { closest: _.noop } });
        page.simulate('mouseMove', { clientX: 350, clientY: 220, stopPropagation: _.noop });

        expect(props.onDragStart).toHaveBeenCalledTimes(1);
        expect(props.onDragMove).toHaveBeenCalledTimes(1);
        expect(props.onDragMove).toHaveBeenCalledWith(props.spot, [ 50, 20 ]);

        page.simulate('mouseUp');

        expect(props.onDragEnd).toHaveBeenCalledTimes(1);
        expect(props.onDragEnd).toHaveBeenCalledWith(props.spot, { clientX: 225, clientY: 125 });
    });

    it('при последовательном муве вызовет onDragStart только один раз', () => {
        const page = shallowRenderComponent({ props });

        page.simulate('mouseDown', { clientX: 300, clientY: 200, target: { closest: _.noop } });
        page.simulate('mouseMove', { clientX: 350, clientY: 220, stopPropagation: _.noop });
        page.simulate('mouseMove', { clientX: 352, clientY: 222, stopPropagation: _.noop });

        expect(props.onDragStart).toHaveBeenCalledTimes(1);
        expect(props.onDragMove).toHaveBeenCalledTimes(2);
    });

    it('при незначительном смещении ничего не будет делать', () => {
        const page = shallowRenderComponent({ props });

        page.simulate('mouseDown', { clientX: 300, clientY: 200, target: { closest: _.noop } });
        page.simulate('mouseMove', { clientX: 302, clientY: 202, stopPropagation: _.noop });

        expect(props.onDragStart).toHaveBeenCalledTimes(0);
        expect(props.onDragMove).toHaveBeenCalledTimes(0);

        page.simulate('mouseUp');
        expect(props.onDragEnd).toHaveBeenCalledTimes(0);
    });

    it('для покупателя ничего не будет делать', () => {
        props.isOwner = false;
        const page = shallowRenderComponent({ props });

        page.simulate('mouseDown', { clientX: 300, clientY: 200, target: { closest: _.noop } });
        page.simulate('mouseMove', { clientX: 350, clientY: 250, stopPropagation: _.noop });

        expect(props.onDragStart).toHaveBeenCalledTimes(0);
        expect(props.onDragMove).toHaveBeenCalledTimes(0);

        page.simulate('mouseUp');
        expect(props.onDragEnd).toHaveBeenCalledTimes(0);
    });

    it('во время загрузки ничего не будет делать', () => {
        props.isPending = true;
        const page = shallowRenderComponent({ props });

        page.simulate('mouseDown', { clientX: 300, clientY: 200, target: { closest: _.noop } });
        page.simulate('mouseMove', { clientX: 350, clientY: 250, stopPropagation: _.noop });

        expect(props.onDragStart).toHaveBeenCalledTimes(0);
        expect(props.onDragMove).toHaveBeenCalledTimes(0);

        page.simulate('mouseUp');
        expect(props.onDragEnd).toHaveBeenCalledTimes(0);
    });
});

function shallowRenderComponent({ props }: { props: Props }) {
    const page = shallow(
        <PanoramaHotSpot { ...props }/>,
        { disableLifecycleMethods: true },
    );

    const componentInstance = page.instance() as PanoramaHotSpot;

    componentInstance.spotRef = {
        getBoundingClientRect: () => ({
            width: 50,
            height: 50,
            top: 100,
            bottom: 150,
            left: 200,
            right: 250,
            x: 200,
            y: 100,
            toJSON: () => {},
        }),
    } as HTMLDivElement;

    componentInstance.throttledHandleMouseMove =
        componentInstance.handleMouseMove as DebouncedFunc<(event: MouseEvent | React.MouseEvent<Element, MouseEvent>) => void>;

    if (typeof componentInstance.componentDidMount === 'function') {
        componentInstance.componentDidMount();
    }

    return page;
}
