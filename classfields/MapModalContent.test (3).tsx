import React from 'react';

import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';

import { GeoObject } from 'react-yandex-maps';
import MapModalContent, { MODE } from './MapModalContent';

const BASE_PROPS = {
    lat: 59.02,
    lng: 54.65,
};

it('должен отрендерить лоадер, если карта не загружена', () => {
    const tree = shallow(
        <MapModalContent
            { ...BASE_PROPS }
            mode={ MODE.SHOW }
        />,
    );
    expect(tree.find('Loader')).toHaveLength(1);
});

it('не должен отрендерить лоадер, если карта загружена', () => {
    const tree = shallow(
        <MapModalContent
            { ...BASE_PROPS }
            mode={ MODE.SHOW }
        />,
    );
    const instance = tree.instance();
    instance.setState({ is_loaded: true });
    expect(tree.find('Loader')).toHaveLength(0);
});

it('должен отрендерить компонент в режиме просмотра', () => {
    const tree = shallow(
        <MapModalContent
            { ...BASE_PROPS }
            mode={ MODE.SHOW }
        />,
    );
    const instance = tree.instance();
    instance.setState({ is_loaded: true });
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен отрендерить компонент в режиме редактирования', () => {
    const tree = shallow(
        <MapModalContent
            { ...BASE_PROPS }
            mode={ MODE.EDIT }
        />,
    );
    const instance = tree.instance();
    instance.setState({ is_loaded: true });
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен отрендерить маркер карты, если переданы широта и долгота', () => {
    const tree = shallow(
        <MapModalContent
            { ...BASE_PROPS }
            mode={ MODE.EDIT }
        />,
    );
    expect(tree.find(GeoObject)).toHaveLength(1);
});

it('не должен отрендерить маркер карты, если не переданы широта и долгота', () => {
    const tree = shallow(
        <MapModalContent
            mode={ MODE.EDIT }
        />,
    );
    expect(tree.find(GeoObject)).toHaveLength(0);
});
