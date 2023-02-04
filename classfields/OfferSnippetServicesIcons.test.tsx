import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import type { MultipostingClassifiedServiceAutoru } from 'auto-core/types/Multiposting';

import OfferSnippetServicesIcons from './OfferSnippetServicesIcons';

const servicesMock: Array<MultipostingClassifiedServiceAutoru> = [
    { service: 'autostrategy', is_active: true } as MultipostingClassifiedServiceAutoru,
    { service: 'all_sale_special', is_active: true } as MultipostingClassifiedServiceAutoru,
    { service: 'all_sale_fresh', is_active: true } as MultipostingClassifiedServiceAutoru,
    { service: 'all_sale_badge', is_active: true } as MultipostingClassifiedServiceAutoru,
    { service: 'package_turbo', is_active: true } as MultipostingClassifiedServiceAutoru,
    { service: 'all_sale_premium', is_active: true } as MultipostingClassifiedServiceAutoru,
];

it('покажет прочерк, если нет активных услуг', () => {
    const tree = shallow(
        <OfferSnippetServicesIcons services={ [] }/>,
    );

    expect(tree.find('.OfferSnippetServicesIcons_empty').text()).toEqual('—');
});

it('покажет не больше иконок активных услуг, чем передано в пропсах', () => {
    const MAX_ICONS_SHOW_COUNT = 3;
    const tree = shallow(
        <OfferSnippetServicesIcons services={ servicesMock } maxIconsShowCount={ MAX_ICONS_SHOW_COUNT }/>,
    );

    expect(tree.find('.OfferSnippetServicesIcons__iconWrapper')).toHaveLength(MAX_ICONS_SHOW_COUNT);
});

it('покажет все иконки, если в пропсах не передано их максимальное количество', () => {
    const tree = shallow(
        <OfferSnippetServicesIcons services={ servicesMock }/>,
    );

    expect(tree.find('.OfferSnippetServicesIcons__iconWrapper')).toHaveLength(servicesMock.length);
});
