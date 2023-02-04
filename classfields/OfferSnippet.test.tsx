import React from 'react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import configMock from 'auto-core/react/dataDomain/config/mock';
import type { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';

import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import type { AppState } from 'www-poffer/react/store/AppState';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';
import { renderComponent } from 'www-poffer/react/utils/testUtils';
import offerDraftMock from 'www-poffer/react/dataDomain/offerDraft/mock';
import { OfferAccordionExtraSectionId } from 'www-poffer/react/components/desktop/OfferAccordion/types';

import OfferSnippet from './OfferSnippet';

type State = Partial<AppState>;

let defaultState: State;

beforeEach(() => {
    defaultState = {
        config: configMock.value(),
        offerDraft: offerDraftMock.value(),
    };

    (offerFormPageContextMock.sendFormLog as jest.MockedFunction<typeof offerFormPageContextMock.sendFormLog>).mockClear();
});

it('при клике выбирает на сниппет отправляет лог', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const { container } = await renderComponent(<OfferSnippet/>, { state: defaultState, formApi });

    const snippet = container.querySelector('.OfferSnippet');
    snippet && userEvent.click(snippet);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledWith({ block: OfferAccordionExtraSectionId.PREVIEW, event: 'click' });
});
