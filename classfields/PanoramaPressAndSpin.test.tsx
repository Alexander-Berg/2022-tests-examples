import React from 'react';
import { shallow } from 'enzyme';

import type { Props } from './PanoramaPressAndSpin';
import PanoramaPressAndSpin from './PanoramaPressAndSpin';

const touchEventMock = {
    stopPropagation: jest.fn(),
};

let props: Props;

beforeEach(() => {
    props = {
        onMouseDown: jest.fn(),
        onTouchStart: jest.fn(),
        onTouchMove: jest.fn(),
        onHide: jest.fn(),
    };
});

describe('при опускании пальца', () => {
    it('не пропустит ивент дальше', () => {
        const page = shallowRenderComponent({ props });
        page.simulate('touchStart', touchEventMock);

        expect(touchEventMock.stopPropagation).toHaveBeenCalledTimes(1);
    });

    it('скроет блок', () => {
        const page = shallowRenderComponent({ props });
        const hiddenBlock = page.find('.PanoramaPressAndSpin_hidden');

        expect(hiddenBlock.isEmptyRender()).toBe(true);

        page.simulate('touchStart', touchEventMock);
        const updatedHiddenBlock = page.find('.PanoramaPressAndSpin_hidden');

        expect(updatedHiddenBlock.isEmptyRender()).toBe(false);
    });

    it('вызовет проп', () => {
        const page = shallowRenderComponent({ props });
        page.simulate('touchStart', touchEventMock);

        expect(props.onTouchStart).toHaveBeenCalledTimes(1);
        expect(props.onTouchStart).toHaveBeenCalledWith(touchEventMock);
    });
});

function shallowRenderComponent({ props }: { props: Props }) {
    const page = shallow(
        <PanoramaPressAndSpin { ...props }/>,
    );

    return page;
}
