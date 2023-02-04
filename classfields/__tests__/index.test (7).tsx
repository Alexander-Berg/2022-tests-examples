import React from 'react';
import * as enzyme from 'enzyme';
import noop from 'lodash/noop';
import merge from 'lodash/merge';
import { DeepPartial } from 'utility-types';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { MainMenuExpandedItemType, MainMenuItemType } from 'realty-core/types/header';
import { ICoreStore } from 'realty-core/view/react/common/reducers/types';

import { MainMenuExpandedMenuContainer } from '../container';
import { defaultStore } from '../../__tests__/store';

import { testCases } from './testCases';

const Component: React.ComponentType<{ expandedItem: MainMenuItemType; store?: DeepPartial<ICoreStore> }> = ({
    expandedItem,
    store = defaultStore,
}) => (
    <AppProvider
        initialState={store}
        context={{ link: (...args: [string, Record<string, unknown>]) => JSON.stringify(args) }}
    >
        <MainMenuExpandedMenuContainer
            expandedItem={expandedItem}
            expandedMenuRef={React.createRef()}
            handleMouseLeave={noop}
            handleClickOutside={noop}
        />
    </AppProvider>
);

describe('MainMenuExpandedMenu', () => {
    test.each(testCases)('Ссылки выпадающего меню у пункта %s', (type, expandedItems, promotionItems) => {
        const wrapper = enzyme.mount(<Component expandedItem={type} />);

        expandedItems.forEach(([expandedItem, expected]) => {
            let href = wrapper.find(`a[data-test='${expandedItem}']`).props().href || '';

            // почти всегда href набор параметров, но для аренды это строка
            try {
                href = href && JSON.parse(href);
                // eslint-disable-next-line no-empty
            } catch (e) {}

            expect(href).toEqual(expected);
        });

        promotionItems.forEach(([promotionItem, expected]) => {
            let href = wrapper.find(`a[data-test='${promotionItem}']`).props().href || '';

            // почти всегда href набор параметров, но для аренды это строка
            try {
                href = href && JSON.parse(href);
                // eslint-disable-next-line no-empty
            } catch (e) {}

            expect(href).toEqual(expected);
        });
    });

    it('Расширяет гео у ссылки VILLAGES в пункте SITES', () => {
        const store: DeepPartial<ICoreStore> = merge({}, defaultStore, {
            geo: {
                sitesRgids: {
                    district: 245,
                },
            },
        });

        const wrapper = enzyme.mount(<Component expandedItem={MainMenuItemType.SITES} store={store} />);

        const href = wrapper.find(`a[data-test='${MainMenuExpandedItemType.VILLAGES}']`).props().href;

        expect(JSON.parse(href!)).toEqual(['village-search', { rgid: 245, type: 'SELL' }]);
    });

    it('Расширяет гео у ссылки SITES в пункте SITES', () => {
        const store: DeepPartial<ICoreStore> = merge({}, defaultStore, {
            geo: {
                sitesRgids: {
                    district: 245,
                },
            },
        });

        const wrapper = enzyme.mount(<Component expandedItem={MainMenuItemType.SITES} store={store} />);

        const href = wrapper.find(`a[data-test='${MainMenuExpandedItemType.SITES}']`).props().href;

        expect(JSON.parse(href!)).toEqual(['newbuilding-search', { rgid: 245, type: 'SELL' }]);
    });
});
