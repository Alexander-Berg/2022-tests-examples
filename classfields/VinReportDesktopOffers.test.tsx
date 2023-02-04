import _ from 'lodash';
import React from 'react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { render } from '@testing-library/react';
import '@testing-library/jest-dom';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import mockStore from 'autoru-frontend/mocks/mockStore';

import offersData from 'auto-core/react/dataDomain/vinReport/mocks/autoRuOffers';
import photo from 'auto-core/react/dataDomain/vinReport/mocks/photo';

import VinReportDesktopOffers from './VinReportDesktopOffers';
import type { Props } from './VinReportDesktopOffers';

const Context = createContextProvider(contextMock);
const store = mockStore();

async function renderComponent(offersData: Props['offersData']) {
    const wrapper = await render(
        <Context>
            <Provider store={ store }>
                <VinReportDesktopOffers offersData={ offersData }/>
            </Provider>
        </Context>,
    );

    return wrapper;
}

const photoWithOtherImage = {
    ...photo,
    sizes: {
        ...photo.sizes,
        '1200x900n': 'OTHER_IMAGE_URL',
        small: 'OTHER_IMAGE_URL',
    },
};

it('десктоп: при клике на превью откроет fullscreen-галерею оффера', async() => {
    const { container } = await renderComponent(offersData);
    const gallery = container.querySelector('.VinReportDesktopOffers__gallery');

    expect(gallery).toBeInTheDocument();

    gallery && userEvent.click(gallery);

    expect(document.querySelector('.ImageGalleryFullscreenVertical')).toBeInTheDocument();
});

it('десктоп: прокинет в fullscreen-галерею правильные фото оффера', async() => {
    const LAST_INDEX = offersData.offers.length - 1;
    const offersDataWithOtherImageInLastOffer = _.cloneDeep(offersData);

    offersDataWithOtherImageInLastOffer.offers[LAST_INDEX].photo = photoWithOtherImage;
    offersDataWithOtherImageInLastOffer.offers[LAST_INDEX].photos = [ photoWithOtherImage, ...offersData.offers[LAST_INDEX].photos ];

    const { container } = await renderComponent(offersDataWithOtherImageInLastOffer);

    const offers = container.querySelectorAll('.VinReportOffer');
    const galleryForLastOffer = offers[offers.length - 1].querySelector('.VinReportDesktopOffers__gallery');

    galleryForLastOffer && userEvent.click(galleryForLastOffer);

    expect((document.querySelector('.ImageGalleryFullscreenVertical__thumb') as HTMLImageElement)?.src).toEqual('http://localhost/OTHER_IMAGE_URL');
});
