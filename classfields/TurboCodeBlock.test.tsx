import React from 'react';
import { shallow } from 'enzyme';

import TurboCodeBlock from './TurboCodeBlock';
import { allowed, notAllowed } from './mocks/embeds.mock';

for (const embedKey in allowed) {
    it(`рендерит разрешённый эмбед ${ embedKey }`, () => {
        const wrapper = shallow(
            <TurboCodeBlock data={{ type: 'code', code: allowed[embedKey] }}/>,
        );

        expect(wrapper).toMatchSnapshot();
    });
}

for (const embedKey in notAllowed) {
    it(`не рендерит запрещённый эмбед ${ embedKey }`, () => {
        const wrapper = shallow(
            <TurboCodeBlock data={{ type: 'code', code: notAllowed[embedKey] }}/>,
        );

        expect(wrapper.exists('ForwardRef(SectionBlock)')).toBe(false);
    });
}
