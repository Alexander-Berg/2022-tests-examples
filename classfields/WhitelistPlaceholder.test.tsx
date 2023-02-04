import React from 'react';
import { shallow } from 'enzyme';

import WhitelistPlaceholder from './WhitelistPlaceholder';

it('не должен показывать информацию о менеджере, если ее нет', () => {
    const tree = shallow(<WhitelistPlaceholder/>);

    expect(tree.find('WhitelistPlaceholder__contactPanel')).not.toExist();
});
