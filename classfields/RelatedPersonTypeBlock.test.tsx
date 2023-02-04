import React from 'react';
import { render, screen } from '@testing-library/react';
import { renderHook, act } from '@testing-library/react-hooks/dom';
import { Provider, useSelector, useDispatch } from 'react-redux';

import mockStore from 'autoru-frontend/mocks/mockStore';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import creditApplicationMock from 'auto-core/react/dataDomain/credit/mocks/creditApplication.mockchain';
import card from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import { CreditFormProvider } from 'auto-core/react/components/common/CreditForm/contexts/CreditFormContext';
import useCreditForm from 'auto-core/react/components/common/CreditForm/hooks/useCreditForm';
import type { CreditFormProps } from 'auto-core/react/components/common/CreditForm/types';
import { WizardProvider } from 'auto-core/react/components/common/Wizard/contexts/WizardContext';
import { useWizardContextMock } from 'auto-core/react/components/common/Wizard/contexts/wizardContext.mock';
import { BlockName } from 'auto-core/react/components/mobile/CreditWizard/CreditWizardBlocks/types/blockName';

import RelatedPersonTypeBlock from './RelatedPersonTypeBlock';

const creditApplication = creditApplicationMock();

jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
        useDispatch: jest.fn(),
    };
});

function mockRedux(state = {}) {
    const store = mockStore(state);

    (useDispatch as jest.MockedFunction<typeof useDispatch>).mockReturnValue(
        (...args) => store.dispatch(...args),
    );

    (useSelector as jest.MockedFunction<typeof useSelector>).mockImplementation(
        (selector) => selector(store.getState()),
    );

    return store;
}

describe('RelatedPersonTypeBlock', () => {
    it('рендерит три типа контактов для eкредит заявки', () => {
        const offer = cloneOfferWithHelpers(card)
            .withIsOwner(false)
            .withCreditPrecondition()
            .withEcreditPrecondition()
            .value();

        const application = creditApplication.withOffer(offer).value();
        const state = {
            credit: {
                application: {
                    data: {
                        offers: [ offer ],
                    },
                },
            },
        };

        const {
            result: creditFormResult,
        } = renderHook(() => useCreditForm({
            creditApplication: application,
            offers: [ offer ],
        }, {
            publishCreditApplicationAction: jest.fn(),
            updateCreditApplicationAction: jest.fn(),
        }));

        const creditForm = creditFormResult.current as CreditFormProps;

        act(() => {
            render(
                <Provider store={ mockRedux(state) }>
                    <WizardProvider value={ useWizardContextMock }>
                        <CreditFormProvider value={ creditForm }>
                            <RelatedPersonTypeBlock
                                name={ BlockName.RELATED_PERSON }
                                order={ 0 }
                                maxPath={ 0 }
                            />
                        </CreditFormProvider>
                    </WizardProvider>
                </Provider>,
            );
        });

        expect(screen.queryByText('Мой номер')).toBeNull();
    });

    it('рендерит два типа контактов для обычной заявки', () => {
        const offer = cloneOfferWithHelpers(card)
            .withIsOwner(false)
            .withCreditPrecondition()
            .value();

        const application = creditApplication.withOffer(offer).value();
        const state = {
            credit: {
                application: {
                    data: {
                        offers: [ offer ],
                    },
                },
            },
        };

        const {
            result: creditFormResult,
        } = renderHook(() => useCreditForm({
            creditApplication: application,
            offers: [ offer ],
        }, {
            publishCreditApplicationAction: jest.fn(),
            updateCreditApplicationAction: jest.fn(),
        }));

        const creditForm = creditFormResult.current as CreditFormProps;

        act(() => {
            render(
                <Provider store={ mockRedux(state) }>
                    <WizardProvider value={ useWizardContextMock }>
                        <CreditFormProvider value={ creditForm }>
                            <RelatedPersonTypeBlock
                                name={ BlockName.RELATED_PERSON }
                                order={ 0 }
                                maxPath={ 0 }
                            />
                        </CreditFormProvider>
                    </WizardProvider>
                </Provider>,
            );
        });

        expect(screen.queryByText('Мой номер')).toBeDefined();
    });
});
