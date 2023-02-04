import React from 'react';
import * as enzyme from 'enzyme';
import { DeepPartial } from 'utility-types';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { MainMenuPromoBlockType } from 'realty-core/types/header';
import { LinkProp } from 'realty-core/view/react/common/enhancers/withLink';
import { ICoreStore } from 'realty-core/view/react/common/reducers/types';
import { ProfileTypes } from 'realty-core/types/profile/profileTypes';
import { AnyObject } from 'realty-core/types/utils';

import { MainMenuPromoBlockContainer } from '../container';
import { defaultStore } from '../../__tests__/store/defaultStore';

const Component: React.ComponentType<{ type: MainMenuPromoBlockType; store?: DeepPartial<ICoreStore> }> = ({
    type,
    store = defaultStore,
}) => (
    <AppProvider initialState={store} context={{ link: (...args: [string, AnyObject]) => JSON.stringify(args) }}>
        <MainMenuPromoBlockContainer type={type} />
    </AppProvider>
);

describe('MainMenuPromoBlock', () => {
    const mainMenuPromoBlockTypes: [MainMenuPromoBlockType, Parameters<LinkProp> | string][] = [
        [MainMenuPromoBlockType.CHECK_APARTMENT, ['egrn-address-purchase']],
        [MainMenuPromoBlockType.FIND_AGENT, ['profile-search', { rgid: 587795, profileUserType: ProfileTypes.AGENCY }]],
        [MainMenuPromoBlockType.DEVELOPERS_MAP, ['newbuilding-map', { rgid: 587795, type: 'SELL' }]],
        [MainMenuPromoBlockType.MORTGAGE_CALCULATOR, ['alfabank']],
        [
            MainMenuPromoBlockType.YANDEX_RENT_LANDING,
            'https://realty.yandex.ru/?from=main_menu&utm_source=header_dropdown_yarealty_banner',
        ],
        [
            MainMenuPromoBlockType.YANDEX_RENT_LISTING,
            [
                'commercial-search',
                {
                    rgid: 741964,
                    typeCode: 'snyat',
                    categoryCode: 'kvartira',
                    uid: 1260401477,
                },
            ],
        ],
        [MainMenuPromoBlockType.YANDEX_DEAL_VALUATION, ['ya-deal-valuation']],
    ];

    test.each(mainMenuPromoBlockTypes)('Ссылка у промоблока %s', (type, expected) => {
        const wrapper = enzyme.mount(<Component type={type} />);

        let href = wrapper.find(`a`).props().href;

        // почти всегда href набор параметров, но для аренды это строка
        try {
            href = href && JSON.parse(href);
            // eslint-disable-next-line no-empty
        } catch (e) {}

        expect(href).toEqual(expected);
    });
});
