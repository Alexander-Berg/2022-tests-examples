import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import SharkBlockIds from 'auto-core/lib/creditBroker/SharkBlockIds';

import creditApplicationMockchain from 'auto-core/react/dataDomain/credit/mocks/creditApplication.mockchain';

import CallmenCreditAccordion from './CallmenCreditAccordion';

it('убирает заполненные и ненужные секции', () => {
    const creditApplicationMock = creditApplicationMockchain({
        id: '5dc027b4-9671-43f1-86ef-246fb6e6f6c3',
    })
        .withProfile({
            block_types: [
                SharkBlockIds.BIRTH_PLACE,
                SharkBlockIds.BIRTH_DATE,
                SharkBlockIds.PASSPORT_RF,
                SharkBlockIds.OLD_NAME,
            ],
        })
        .value();

    const store = mockStore({
        credit: {
            application: {
                data: {
                    credit_application: creditApplicationMock,
                },
            },
        },
    });

    const wrapper = shallow(
        <CallmenCreditAccordion
            applicationId="5dc027b4-9671-43f1-86ef-246fb6e6f6c3"
            onFormDisplayed={ _.noop }
            onPublishSuccess={ _.noop }
            onBackButtonClick={ _.noop }
        />,
        {
            context: {
                ...contextMock,
                store,
            },
        },
    ).dive();

    const accordionDumb = wrapper.find('CreditAccordionDumb');

    expect(accordionDumb.prop('sections')).toMatchSnapshot();
});
