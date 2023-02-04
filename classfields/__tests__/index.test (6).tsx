import React from 'react';
import * as enzyme from 'enzyme';
import { DeepPartial } from 'utility-types';
import merge from 'lodash/merge';
import { advanceTo } from 'jest-date-mock';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { MainMenuItemType } from 'realty-core/types/header';
import { LinkProp } from 'realty-core/view/react/common/enhancers/withLink';
import { ICoreStore } from 'realty-core/view/react/common/reducers/types';
import { AnyObject } from 'realty-core/types/utils';

import { MainMenuContainer } from '../container';

import { defaultStore } from './store';

advanceTo(new Date('2020-12-31 23:59:59'));

const Component: React.ComponentType<{ store?: DeepPartial<ICoreStore> }> = ({ store = defaultStore }) => (
    <AppProvider initialState={store} context={{ link: (...args: [string, AnyObject]) => JSON.stringify(args) }}>
        <MainMenuContainer />
    </AppProvider>
);

describe('MainMenu', () => {
    const wrapper = enzyme.mount(<Component />);

    const mainMenuItemsTypes: [MainMenuItemType, Parameters<LinkProp> | string][] = [
        [MainMenuItemType.SELL, ['search', { rgid: 587795, category: 'APARTMENT', type: 'SELL' }]],
        [MainMenuItemType.RENT, ['search', { rgid: 587795, category: 'APARTMENT', type: 'RENT' }]],
        [MainMenuItemType.SITES, ['newbuilding-map', { rgid: 587795, type: 'SELL' }]],
        [MainMenuItemType.COMMERCIAL, ['search', { rgid: 587795, category: 'COMMERCIAL', type: 'RENT' }]],
        [MainMenuItemType.MORTGAGE, ['mortgage-search', { rgid: 587795, flatType: 'NEW_FLAT' }]],
        [MainMenuItemType.FOR_PROFESSIONAL, ['promotion']],
        [MainMenuItemType.YANDEX_ARENDA, 'https://realty.yandex.ru/?from=main_menu&utm_source=header_yarealty'],
        [MainMenuItemType.SPECIAL_PROJECT, '/pik/?from=main_menu'],
        [MainMenuItemType.JOURNAL, ['journal']],
    ];

    test.each(mainMenuItemsTypes)('Ссылка у пункта %s', (type, expected) => {
        let href = wrapper.find(`a[data-test='${type}']`).props().href;

        // почти всегда href набор параметров, но для спецпроектов это строка
        try {
            href = href && JSON.parse(href);
            // eslint-disable-next-line no-empty
        } catch (e) {}

        expect(href).toEqual(expected);
    });

    it('Расширяет гео у ссылки SITES', () => {
        const store: DeepPartial<ICoreStore> = merge({}, defaultStore, {
            geo: {
                sitesRgids: {
                    district: 245,
                },
            },
            config: {
                yaArendaUrl: 'https://realty.yandex.ru',
            },
        });

        const wrapper = enzyme.mount(<Component store={store} />);

        const href = wrapper.find(`a[data-test='${MainMenuItemType.SITES}']`).props().href;

        expect(JSON.parse(href!)).toEqual(['newbuilding-map', { rgid: 245, type: 'SELL' }]);
    });
});
