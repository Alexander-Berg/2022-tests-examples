import React from 'react';
import { shallow } from 'enzyme';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import { panoramaHotSpotMock } from 'auto-core/react/dataDomain/panoramaHotSpots/mocks';

import PanoramaHotSpotsPreviewPopup from './PanoramaHotSpotsPreviewPopup';
import type { Props } from './PanoramaHotSpotsPreviewPopup';

let props: Props;

beforeEach(() => {
    props = {
        isOpened: true,
        isOwner: false,
        onClick: jest.fn(),
        onEditClick: jest.fn(),
        onMouseEnter: jest.fn(),
        onMouseLeave: jest.fn(),
        onRequestHide: jest.fn(),
        onSpotRemove: jest.fn(),
        spot: panoramaHotSpotMock.value(),
        zIndexLevel: 1,
        anchor: {} as HTMLElement,
    };
});

describe('при клике на попап', () => {
    describe('если есть картинка', () => {
        beforeEach(() => {
            props.spot = panoramaHotSpotMock.withImage('image-of-cat').value();
        });

        it('вызовет проп', () => {
            const page = shallowRenderComponent({ props: props as Props });
            const container = page.find('.PanoramaHotSpotsPreviewPopup__container');
            container.simulate('click', { stopPropagation: () => {} });

            expect(props.onClick).toHaveBeenCalledTimes(1);
        });

        it('в журнале не будет вызывать проп', () => {
            props.theme = 'mag';
            const page = shallowRenderComponent({ props: props as Props });
            const container = page.find('.PanoramaHotSpotsPreviewPopup__container');
            container.simulate('click', { stopPropagation: () => {} });

            expect(props.onClick).toHaveBeenCalledTimes(0);
        });

        it('для не-владельца отправит метрику', () => {
            const page = shallowRenderComponent({ props: props as Props });
            const container = page.find('.PanoramaHotSpotsPreviewPopup__container');
            container.simulate('click', { stopPropagation: () => {} });

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        });

        it('для владельца не отправит метрику', () => {
            props.isOwner = true;
            const page = shallowRenderComponent({ props: props as Props });
            const container = page.find('.PanoramaHotSpotsPreviewPopup__container');
            container.simulate('click', { stopPropagation: () => {} });

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
        });
    });

    it('если нет картинки не вызовет проп', () => {
        const page = shallowRenderComponent({ props: props as Props });
        const container = page.find('.PanoramaHotSpotsPreviewPopup__container');
        container.simulate('click', { stopPropagation: () => {} });

        expect(props.onClick).toHaveBeenCalledTimes(0);
    });
});

function shallowRenderComponent({ props }: { props: Props }) {
    const ContextProvider = createContextProvider(contextMock);

    const page = shallow(
        <ContextProvider>
            <PanoramaHotSpotsPreviewPopup { ...props }/>
        </ContextProvider>,
    );

    return page.dive();
}
