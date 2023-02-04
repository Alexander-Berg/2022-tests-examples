import React from 'react';
import { shallow } from 'enzyme';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import BanReason from './BanReason';

it('если оффер неактивный, покажет соответствующий заголовок', () => {
    const offer = {
        human_reasons_ban: [ { text: 'текст' } ],
        status: 'INACTIVE',
    };
    const tree = shallow(
        <BanReason offer={ offer as Offer }/>,
    );

    expect(tree.find('.BanReason__title').find('span').text()).toBe('Снято с продажи');
});

it('если оффер забанен, покажет соответствующий заголовок', () => {
    const offer = {
        human_reasons_ban: [ { text: 'текст' } ],
        status: 'BANNED',
    };
    const tree = shallow(
        <BanReason offer={ offer as Offer }/>,
    );

    expect(tree.find('.BanReason__title').find('span').text()).toBe('Причины блокировки');
});
