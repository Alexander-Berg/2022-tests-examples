const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const AppStoreBanner = require('./AppStoreBanner');

it('должен правильно отрендерить заглушку промо-баннера для главной страницы', () => {
    const tree = shallow(
        <AppStoreBanner isMobile={ false } isPromoApp={ false }/>, { disableLifecycleMethods: true });

    expect(shallowToJson(tree)).toMatchSnapshot();

    const instance = tree.instance();
    instance.componentDidMount();
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен сразу отрендерить промо-баннер для промо-страницы', () => {
    const tree = shallow(
        <AppStoreBanner isMobile={ false } isPromoApp={ true }/>, { disableLifecycleMethods: true });

    expect(shallowToJson(tree)).toMatchSnapshot();
});
