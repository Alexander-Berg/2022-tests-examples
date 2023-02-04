import React from 'react';
import { mount } from 'enzyme';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SeoSiteLinks } from '../index';

import { defaultTitleProps } from './mocks';

// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
global.BUNDLE_LANG = 'ru';

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
            <SeoSiteLinks {...defaultTitleProps} placeName={placeName} placeType={placeType} />
        </AppProvider>
    );

    return wrapper.find('[className="SwipeableBlock__title"]').text();
}

test('Отрисовка заголовка по умолчанию', () => {
    expect(getTitle('Московская')).toEqual('Новостройки – Московская');
});

test('Отрисовка заголовка метро', () => {
    expect(getTitle('Павелецкая', 'metroStation')).toEqual('Новостройки у метро Павелецкая');
});

test('Отрисовка заголовка района', () => {
    expect(getTitle('Академический', 'district')).toEqual('Новостройки в районе Академический');
});
