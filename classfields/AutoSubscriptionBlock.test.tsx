import React from 'react';
import { shallow } from 'enzyme';

import postMock from 'auto-core/react/dataDomain/mag/articleMock';

import AutoSubscriptionBlock from './AutoSubscriptionBlock';

const POST = postMock.value();

describe('не отрендерит если', () => {
    it('не передать пост', () => {
        const tree = shallow(
            <AutoSubscriptionBlock/>,
        );

        expect(tree).toEqual({});
    });

    it('позиция баннера не "block"', () => {
        const tree = shallow(
            <AutoSubscriptionBlock post={{
                ...POST,
                banners: { telegramOrSubscriptionForm: { position: 'footer', type: 'form', subscribed: false } },
                uniqBlockTypes: [],
            }}/>,
        );

        expect(tree).toEqual({});
    });

    it('тип баннера не "form"', () => {
        const tree = shallow(
            <AutoSubscriptionBlock post={{
                ...POST,
                banners: { telegramOrSubscriptionForm: { position: 'block', type: 'telegram', subscribed: false } },
                uniqBlockTypes: [],
            }}/>,
        );

        expect(tree).toEqual({});
    });
});

it('рендерит блок', () => {
    const tree = shallow(
        <AutoSubscriptionBlock post={{
            ...POST,
            banners: { telegramOrSubscriptionForm: { position: 'block', type: 'form', subscribed: false } },
            uniqBlockTypes: [],
        }}/>,
    );

    expect(tree.find('Memo(SubscriptionForm)').exists()).toBe(true);
});
