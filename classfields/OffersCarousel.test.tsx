/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { render } from 'react-dom';
import { shallow } from 'enzyme';
import { act } from 'react-dom/test-utils';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import gateApi from 'auto-core/react/lib/gateApi';

import { SectionItems, CategoryItems } from 'auto-core/types/TSearchParameters';

import OffersCarousel from './OffersCarousel';

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const getResource = gateApi.getResource as jest.MockedFunction<typeof gateApi.getResource>;

const defaultProps = {
    isMobile: false,
    filters: {
        breadcrumbs: {
            mark: 'BMW',
            model: 'X5',
            superGeneration: [ '21307931' ],
        },
    },
};

it('не рендерит на фазе монтирования', () => {
    const wrapper = shallow(<OffersCarousel { ...defaultProps }/>);

    expect(wrapper.find('OffersCarousel').exists()).toBe(false);
});

it('не рендерит, если ручка вернула 0 офферов', async() => {
    getResource.mockImplementation(jest.fn(() => {
        return Promise.resolve({
            offers: [],
        });
    }));

    const container = await renderComponent();

    expect(
        container.querySelector(`.OffersCarousel`),
    ).toBeNull();
});

it('рендерит правильную ссылку у оффера', async() => {
    getResource.mockImplementation(jest.fn(() => {
        return Promise.resolve({
            offers: [
                { id: 'my-cool-offer-1' },
                { id: 'my-cool-offer-2' },
                { id: 'my-cool-offer-3' },
                { id: 'my-cool-offer-4' },
            ],
            searchParams: {
                section: SectionItems.ALL,
                category: CategoryItems.CARS,
            },
        });
    }));

    const container = await renderComponent();

    expect(
        // eslint-disable-next-line max-len
        container.querySelector(`.OffersCarouselItem[href="link/card/?category=&section=false&mark=&model=&sale_id=my-cool-offer-1&sale_hash=&from=mag.ad-carousel"]`),
    ).not.toBeNull();
});

it('построит правильную ссылку на листинг', async() => {
    getResource.mockImplementation(jest.fn(() => {
        return Promise.resolve({
            offers: [ {} ],
            searchParameters: {
                section: SectionItems.ALL,
                category: CategoryItems.MOTO,
                moto_category: 'motorcycle',
                catalog_filter: [
                    { mark: 'MINI' },
                ],
            },
        });
    }));

    const container = await renderComponent();

    expect(container.querySelector('.CarouselUniversal__title')?.getAttribute('href'))
        .toBe('link/listing/?section=all&from=mag.ad-carousel&category=moto&moto_category=motorcycle');
});

async function renderComponent(props = defaultProps) {
    const container = document.createElement('div');

    const ContextProvider = createContextProvider(contextMock);

    await act(async() => {
        render(
            <ContextProvider>
                <OffersCarousel { ...props }/>
            </ContextProvider>,
            container,
        );
    });

    return container;
}
