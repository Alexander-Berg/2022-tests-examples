import React from 'react';
import { shallow } from 'enzyme';

import MapModal from '../MapModal/MapModal';

import SendLocationButton from './SendLocationButton';

it('должен передать урл правильного формата при выборе локации на карте', () => {
    const on_select_location_mock = jest.fn();
    const tree = shallow(
        <SendLocationButton
            on_select_location={ on_select_location_mock }
        >
            { (on_click: () => void): JSX.Element => <div onClick={ on_click }>Я кнопка</div> }
        </SendLocationButton>,
    );
    const on_change_prop = tree.find(MapModal).prop('on_change') as (lat: number, lng: number) => void;
    on_change_prop(55.73991898188298, 37.64946593258509);
    expect(on_select_location_mock)
        .toHaveBeenCalledWith('https://yandex.ru/maps/?mode=whatshere&whatshere%5Bpoint%5D=37.64946593258509,55.73991898188298&whatshere');
});
