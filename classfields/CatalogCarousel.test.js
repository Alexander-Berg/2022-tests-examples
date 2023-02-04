require('jest-enzyme');
const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const CatalogCarousel = require('./CatalogCarousel');

const images = [
    '//avatars.mds.yandex.net/get-verba/1030388/2a0000017080b874c59ece700722941db579/cattouchret',
    '//avatars.mds.yandex.net/get-verba/1030388/2a0000016d6821d53128ece9662756e05d55/cattouchret',
    '//avatars.mds.yandex.net/get-verba/997355/2a0000016efea787dd46c599a439feb532c5/cattouchret',
    '//avatars.mds.yandex.net/get-verba/787013/2a000001608e6ad6fd310f858594cb686875/cattouchret',
    '//avatars.mds.yandex.net/get-verba/1030388/2a000001723b7af52d8ac094e193cb63d36e/cattouchret',
    '//avatars.mds.yandex.net/get-verba/1030388/2a0000016be1a153059d0bb72110064caad8/cattouchret',
];

const slides = images.map((image, index) => ({ text: index, image }));

it('должен отрендерить карусель', () => {
    const tree = shallow(
        <CatalogCarousel
            showCounter={ true }
            slides={ slides }
        />,
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен отрендерить карусель с пагинацией в виде точек', () => {
    const wrapper = shallow(
        <CatalogCarousel
            showDots={ true }
            showCounter={ false }
            slides={ slides }
        />,
    );

    expect(wrapper.find('.CatalogCarousel__dots')).toExist();
});

it('должен отрендерить карусель со счетчиком фоток', () => {
    const wrapper = shallow(
        <CatalogCarousel
            showDots={ false }
            showCounter={ true }
            slides={ slides }
        />,
    );

    expect(wrapper.find('.CatalogCarousel__counter')).toExist();
});
