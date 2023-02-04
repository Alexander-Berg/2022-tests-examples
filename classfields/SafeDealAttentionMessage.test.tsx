import React from 'react';
import { shallow } from 'enzyme';

import type { Props } from './SafeDealAttentionMessage';
import SafeDealAttentionMessage from './SafeDealAttentionMessage';

const attention = {
    deal_id: 'b7eeaa74-41ec-48c5-903a-d1106a71ff59',
    created: '2021-11-01T11:28:15.512531Z',
    template: 'seller-introducing-passport-details-seller',
    title: 'Безопасная сделка',
    // eslint-disable-next-line max-len
    body: 'Запрос на сделку принят. Теперь вы можете приступить к заполнению паспортных данных и данных о своем автомобиле. Они понадобятся для создания договора купли-продажи.',
    url: 'https://test.avto.ru/deal/b7eeaa74-41ec-48c5-903a-d1106a71ff59/',
};

const defaultProps = {
    onClose: jest.fn(),
    attention,
};

it('должен закрывать сообщение при клике на кнопку', () => {
    const closeMock = jest.fn();
    const wrapper = renderWrapper({ ...defaultProps, onClose: closeMock });

    wrapper.find('.SafeDealAttentionMessage__closeButton').simulate('click');

    expect(closeMock).toHaveBeenCalledWith(attention);
});

function renderWrapper(props: Props) {
    return shallow(<SafeDealAttentionMessage { ...props }/>);
}
