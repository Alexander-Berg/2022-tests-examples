import React from 'react';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';

import offersData from 'auto-core/react/dataDomain/vinReport/mocks/autoRuOffers';

import VinReportOfferDescription from './VinReportOfferDescription';

it('развернет/свернет длинный дескрипшн при клике на "Читать дальше"', () => {
    const wrapper = shallow(
        <VinReportOfferDescription
            offer={ offersData.offers[1] }
            preset="default"
        />,
    );

    // Запоминаем начальное состояние (потом сравним в конце)
    const snapStart = shallowToJson(wrapper.find('.VinReportOfferDescription'));
    expect(snapStart).toMatchSnapshot();

    // Раскрываем дескрипшн
    wrapper.find('Link').at(0).simulate('click');
    expect(shallowToJson(wrapper.find('.VinReportOfferDescription'))).toMatchSnapshot();

    // Скрываем и сравниваем с начальным
    wrapper.find('Link').at(0).simulate('click');
    const snapEnd = shallowToJson(wrapper.find('.VinReportOfferDescription'));
    expect(snapStart).toEqual(snapEnd);
});
