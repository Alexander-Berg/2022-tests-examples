import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';

import offer from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import { ClaimState } from 'auto-core/types/TCreditBroker';

import CreditClaimSnippetCar from './CreditClaimSnippetCar';

it('не должен рисовать кредитный сниппет с машиной, если статус не поддерживается', () => {

    const tree = shallow(
        <CreditClaimSnippetCar
            status={ ClaimState.PREAPPROVED }
            offer={ offer }
            amount={ 123456 }
            successCount={ 3 }
        />,
        { context: contextMock },
    );

    expect(tree).toBeEmptyRender();
});
