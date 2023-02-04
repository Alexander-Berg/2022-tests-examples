import React from 'react';
import { render, fireEvent } from '@testing-library/react';

import imageMock from 'auto-core/react/dataDomain/mag/imageMock';

import type { ImageGalleryProps } from './ImageGalleryMag';
import ImageGalleryMag from './ImageGalleryMag';

import '@testing-library/jest-dom';

const BASE_ITEMS_MOCK: ImageGalleryProps['items'] = [
    { image: imageMock.withRandomMetaData().value(), description: 'тестовое описание' },
    { image: imageMock.withRandomMetaData().value(), title: 'Ah Shit, Here We Go Again', sourceUrl: 'https://auto.ru',
        description: 'немного тестового описания' },
    { image: imageMock.withRandomMetaData().value(), sourceUrl: 'https://t.me/', description: 'ещё тестовое описание' },
];

it('при скролле должен менять каунтер и описание у фотографии', () => {
    render(
        <ImageGalleryMag items={ BASE_ITEMS_MOCK }/>,
    );

    const scroller = document.querySelector('.ImageGalleryMag__scroller') as Element;
    const counterText = document.querySelector('.HighlightedText') as Element;
    const description = document.querySelector('.ImageGalleryDescription__markup') as Element;

    expect(counterText.textContent).toBe('Фото 1 из 3');
    expect(description.textContent).toBe(BASE_ITEMS_MOCK[0].description);

    jest
        .spyOn(scroller, 'scrollWidth', 'get')
        .mockImplementation(() => 300);

    fireEvent.scroll(scroller, { target: { scrollLeft: 100 } });

    expect(counterText.textContent).toBe('Фото 2 из 3');
    expect(description.textContent).toBe(BASE_ITEMS_MOCK[1].description);
});
