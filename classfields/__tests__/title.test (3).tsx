import React from 'react';
import { mount } from 'enzyme';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SeoOfferLinksBlock } from '../index';

import { defaultTitleProps } from './mocks';

Object.defineProperty(window, 'matchMedia', {
    value: () => ({
        matches: false,
        addListener: () => ({}),
        removeListener: () => ({}),
    }),
});

function getTitle(placeName: string, placeType?: 'metroStation' | 'district') {
    const wrapper = mount(
        <AppProvider>
            <SeoOfferLinksBlock {...defaultTitleProps} placeName={placeName} placeType={placeType} />
        </AppProvider>
    );

    return wrapper.find('[className="title"]').text();
}

test('Отрисовка заголовка по умолчанию', () => {
    expect(getTitle('Московская')).toEqual('Снять квартиру – Московская');
});

test('Отрисовка заголовка метро', () => {
    expect(getTitle('Павелецкая', 'metroStation')).toEqual('Снять квартиру у метро Павелецкая');
});

test('Отрисовка заголовка района', () => {
    expect(getTitle('Академический', 'district')).toEqual('Снять квартиру в районе Академический');
});
