import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';
import { shallowToJson } from 'enzyme-to-json';

import Whitelist from './WhitelistDumb';

it('должен написать правильный текст в Confirm, если выбран 1 номер телефона', () => {
    const tree = shallowRenderComponent([]);
    tree.instance().setState({ selectedPhones: [ '+79111111111' ], isShowingConfirm: true });
    tree.find('.Whitelist__selectAllCheckbox').simulate('click');

    expect(shallowToJson(tree.find('Confirm').children())).toMatchSnapshot();
});

it('должен написать правильный текст в Confirm, если пользователь выбрал несколько номеров телефонов', () => {
    const tree = shallowRenderComponent([]);
    tree.instance().setState({ selectedPhones: [ '+79111111111', '+79222222222' ], isShowingConfirm: true });
    tree.find('.Whitelist__selectAllCheckbox').simulate('click');

    expect(shallowToJson(tree.find('Confirm').children())).toMatchSnapshot();
});

it('должен написать правильный текст в Confirm, если пользователь нажал кнопку удалить в строке таблицы', () => {
    const tree = shallowRenderComponent([ '+79111111111' ]);
    tree.find('.Whitelist__item Button').simulate('click');

    expect(shallowToJson(tree.find('Confirm').children())).toMatchSnapshot();
});

function shallowRenderComponent(whitelistPhones: Array<string>) {
    return shallow(
        <Whitelist
            showModal={ _.noop }
            downloadCsv={ _.noop }
            whitelistPhones={ whitelistPhones }
            deletePhones={ () => Promise.resolve(true) }
            newPhoneNumbers={ [] }
            isWhitelistUpdating={ false }
            isPhonesModalVisible={ false }
            isPhoneNumberDeleting={ false }
            deleteFromNewPhoneNumbers={ _.noop }
        />);
}
