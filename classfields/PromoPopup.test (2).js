const { shallow } = require('enzyme');
const PromoPopup = require('./PromoPopup');
const React = require('react');

it('должен передвать onClose в кнопку, если isRenderingButton = true, но buttonUrl=undefined', async() => {
    const onClose = () => 'onClose';

    const tree = shallow(
        <PromoPopup
            buttonText="Понятно"
            isRenderingBody={ false }
            isRenderingButton={ true }
            isRenderingImage={ false }
            isRenderingTitle={ false }
            isVisible={ true }
            onClose={ onClose }
        />);

    expect(tree.find('Button').props().url).toBeUndefined();
    expect(tree.find('Button').props().onClick).toEqual(onClose);
});
