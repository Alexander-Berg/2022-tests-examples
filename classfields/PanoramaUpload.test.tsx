import React from 'react';
import { shallow } from 'enzyme';

import type { InteriorPanorama } from '@vertis/schema-registry/ts-types-snake/auto/panoramas/interior_model';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import type { Props } from './PanoramaUpload';
import PanoramaUpload from './PanoramaUpload';

let props: Props;

beforeEach(() => {
    props = {
        category: 'cars',
        isEdit: false,
        panoramaExterior: null,
        panoramaInterior: null,
        isDealer: true,
        showMessage: jest.fn(),
        setExteriorPanoramaData: jest.fn(),
        setInteriorPanoramaData: jest.fn(),
    };
});

it('для частника не покажет ссылку "как снять панораму"', () => {
    props.isDealer = false;
    const page = shallowRenderComponent({ props });
    const howToLink = page.find('Link[children="Как снять панораму?"]');

    expect(howToLink.isEmptyRender()).toBe(true);
});

it('для частника покажет ссылку "как снять панораму"', () => {
    const page = shallowRenderComponent({ props });
    const howToLink = page.find('Link[children="Как снять панораму?"]');

    expect(howToLink.isEmptyRender()).toBe(false);
});

describe('отображение', () => {
    it('для дилера область загрузки всегда видна', () => {
        const page = shallowRenderComponent({ props });
        const panoramaExteriorUploadBlock = page.find('PanoramaUploadExterior');
        const panoramaInteriorUploadBlock = page.find('PanoramaUploadInterior');

        expect(panoramaExteriorUploadBlock.isEmptyRender()).toBe(false);
        expect(panoramaInteriorUploadBlock.isEmptyRender()).toBe(false);
    });

    it('для частника область загрузки никогда не видна', () => {
        props.isDealer = false;
        const page = shallowRenderComponent({ props });

        expect(page.isEmptyRender()).toBe(true);
    });

    it('частник может просматривать уже загруженные панорамы', () => {
        props.isDealer = false;
        props.panoramaInterior = { status: 'COMPLETED' } as InteriorPanorama;
        const page = shallowRenderComponent({ props });
        const panoramaExteriorUploadBlock = page.find('PanoramaUploadExterior');
        const panoramaInteriorUploadBlock = page.find('PanoramaUploadInterior');

        expect(panoramaExteriorUploadBlock.isEmptyRender()).toBe(true);
        expect(panoramaInteriorUploadBlock.isEmptyRender()).toBe(false);
    });
});

function shallowRenderComponent({ props }: { props: Props }) {
    const ContextProvider = createContextProvider(contextMock);

    const page = shallow(
        <ContextProvider>
            <PanoramaUpload { ...props }/>
        </ContextProvider>,
    ).dive();

    return page;
}
