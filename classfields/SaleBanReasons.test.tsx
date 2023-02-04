import React from 'react';
import { shallow } from 'enzyme';

import SaleBanReasons from './SaleBanReasons';

it('не должен рендерить дропдаун, если массив причин бана пустой', () => {
    const tree = shallow(<SaleBanReasons bansReasons={ [] }/>);
    expect(tree.find('Dropdown')).not.toExist();
});

it('должен показать правильный заголовок, если массив причин бана пустой', () => {
    const tree = shallow(<SaleBanReasons bansReasons={ [] }/>);
    expect(tree.find('.SaleBanReasons__title').text()).toEqual('Объявление заблокировано');
});

it('должен показать правильный заголовок, если есть только одна причина бана', () => {
    const banTitle = 'заголовок бана 1';
    const tree = shallow(<SaleBanReasons
        bansReasons={ [
            { title: banTitle, text_lk_dealer: 'текст бана 1' },
        ] }
    />);

    expect(tree.find('.SaleBanReasons__title').text()).toEqual('Объявление заблокировано');
});

it('должен показать правильный заголовок, если причин бана несколько', () => {
    const tree = shallow(<SaleBanReasons
        bansReasons={ [
            { title: 'заголовок бана 1', text_lk_dealer: 'текст бана 1' },
            { title: 'заголовок бана 2', text_lk_dealer: 'текст бана 2' },
        ] }
    />);

    expect(tree.find('.SaleBanReasons__title').text()).toEqual('Объявление заблокировано по нескольким причинам');
});
