import React from 'react';
import Enzyme, { mount } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import withIntlProvider from '../../../../utils/test-utils/with-intl-provider';

import { PersonList } from 'common/components/PersonList';
import { listParams } from './partner-link.data';
import { initializeDesktopRegistry } from '../../../../__tests__/registry';

jest.mock('common/utils/old-fetch');

Enzyme.configure({ adapter: new Adapter() });

describe('admin', () => {
    beforeAll(initializeDesktopRegistry);

    describe('persons', () => {
        test('проверяет ссылку на договор для РСЯ/нет', async () => {
            expect.assertions(2);

            const Container = withIntlProvider(() => (
                <PersonList
                    {...listParams}
                    onFetchPage={() => {}}
                    onPageSizeChange={() => {}}
                    onShowPerson={() => {}}
                    onSelect={() => {}}
                />
            ));

            const wrapper = mount(<Container />);

            expect(
                wrapper.find('a[href="/partner-contracts.xml?person_id=12092318"]')
            ).toHaveLength(1);
            expect(wrapper.find('a[href="/contracts.xml?person_id=12092319"]')).toHaveLength(1);
        });
    });
});
