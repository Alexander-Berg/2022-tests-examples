import React from 'react';
import { shallow } from 'enzyme';

import type { PhoneError } from 'www-cabinet/react/dataDomain/settingsWhitelist/types';

import WhitelistPhonesModalDumb from './WhitelistPhonesModalDumb';

const whiteListModalProps = {
    isVisible: true,
    hideModal: () => {},
    postPhones: () => {},
    isAddPhonesButtonActive: true,
    quantityOfAddedPhones: 122,
    invalidPhoneNumbers: [
        { phone: 'восемь-девятсотшестнадцать-три-семерки-две-пятерки-сорок-три', error: 'PARSE' as PhoneError },
        { phone: '+791111122333', error: 'DUPLICATE' as PhoneError }, { phone: '+79167775543', error: 'DUPLICATE' as PhoneError },
    ],
    quantityOfNewPhoneNumbers: 0,
};

it('должен показать текст с проблемными телефонами', async() => {
    const tree = shallowRenderComponent();
    tree.setState({ showAdditionalInfo: true });

    expect(tree.find('.WhitelistPhonesModal__subtitle').at(1)).toMatchSnapshot();
});

function shallowRenderComponent(props = whiteListModalProps) {
    return shallow(
        <WhitelistPhonesModalDumb { ...props }/>,
    );
}
