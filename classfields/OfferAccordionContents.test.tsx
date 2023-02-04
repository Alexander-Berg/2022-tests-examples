jest.mock('www-poffer/react/components/common/OfferForm/utils/scrollToField', () => jest.fn());
// eslint-disable-next-line import-helpers/order-imports
import mockSections from './sections.mock';
// eslint-disable-next-line import-helpers/order-imports
import * as mockUseWizardContext from 'auto-core/react/components/common/Wizard/contexts/wizardContext.mock';
jest.mock('auto-core/react/components/common/Wizard/contexts/WizardContext', () => mockUseWizardContext);
jest.mock('www-poffer/react/components/desktop/OfferAccordion/utils/sections', () => mockSections);

import { act, screen } from '@testing-library/react';
import React from 'react';

import mockStore from 'autoru-frontend/mocks/mockStore';
import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';
import flushPromises from 'autoru-frontend/jest/unit/flushPromises';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import { FieldNames, FieldErrors } from 'auto-core/react/components/common/Form/fields/types';

import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import { OfferAccordionSectionId } from 'www-poffer/react/components/desktop/OfferAccordion/types';
import OfferAccordionContents
    from 'www-poffer/react/components/desktop/OfferAccordion/OfferAccordionContents/OfferAccordionContents';
import '@testing-library/jest-dom';
import { renderComponent } from 'www-poffer/react/utils/testUtils';

const {
    useWizardContext,
    useWizardContextMock,
} = mockUseWizardContext;

beforeEach(async() => {
    useWizardContext.mockClear();

    useWizardContextMock.currentStepOrder = 1;
});

const storeMock = mockStore({});

it('не отображает секции без тайтла и невидимые', async() => {
    await componentRenderer();

    expect(screen.queryByTestId(`menu-section-${ OfferAccordionSectionId.TECH }`)).toBeInTheDocument();
    expect(screen.queryByTestId(`menu-section-${ OfferAccordionSectionId.COMPLECTATION }`)).toBeInTheDocument();
    expect(screen.queryByTestId(`menu-section-${ OfferAccordionSectionId.CONTACTS }`)).not.toBeInTheDocument();
    expect(screen.queryByTestId(`menu-section-${ OfferAccordionSectionId.STS }`)).not.toBeInTheDocument();
    expect(screen.queryByTestId(`menu-section-${ OfferAccordionSectionId.ADDRESS }`)).toBeInTheDocument();
});

describe('wizard', () => {
    beforeEach(() => {
        offerFormPageContextMock.showWizard = true;
    });

    it('подсвечиваются все шаги до текущего (включительно)', async() => {
        await componentRenderer();

        expect(isGroupAccessible(OfferAccordionSectionId.TECH)).toEqual(true);

        expect(isGroupAccessible(OfferAccordionSectionId.COMPLECTATION)).toEqual(true);

        expect(isGroupAccessible(OfferAccordionSectionId.ADDRESS)).toEqual(false);
    });
});

describe('не wizard', () => {
    beforeEach(() => {
        offerFormPageContextMock.showWizard = false;
    });

    it('подсвечиваются все шаги', async() => {
        await componentRenderer();

        expect(isGroupAccessible(OfferAccordionSectionId.TECH)).toEqual(true);

        expect(isGroupAccessible(OfferAccordionSectionId.COMPLECTATION)).toEqual(true);

        expect(isGroupAccessible(OfferAccordionSectionId.ADDRESS)).toEqual(true);
    });
});

describe('ошибки', () => {
    beforeEach(() => {
        offerFormPageContextMock.isPending = false;
    });

    it('для сабмитнутой формы подсвечиваются, если в группе показаны ошибки', async() => {
        offerFormPageContextMock.isPending = true;

        const {
            formApi,
        } = await componentRenderer();

        await act(async() => {
            formApi.current?.setFieldError(FieldNames.TECH_PARAM, {
                type: FieldErrors.REQUIRED,
                text: 'required',
            });

            await flushPromises();
        });

        expect(hasGroupError(OfferAccordionSectionId.TECH)).toEqual(true);

        expect(hasGroupError(OfferAccordionSectionId.COMPLECTATION)).toEqual(false);

        expect(hasGroupError(OfferAccordionSectionId.ADDRESS)).toEqual(false);
    });

    it('для не сабмитнутой формы подсвечиваются ошибки, если группа потрогана, есть показанные ошибки и шаг доступен', async() => {
        const {
            formApi,
        } = await componentRenderer();

        await act(async() => {
            formApi.current?.setFieldValue(FieldNames.TECH_PARAM, { text: '123' });
            formApi.current?.setFieldValue(OfferFormFieldNames.LOCATION, { cityName: '123' });

            formApi.current?.setFieldError(FieldNames.TECH_PARAM, {
                type: FieldErrors.REQUIRED,
                text: 'required',
            });

            formApi.current?.setFieldError(OfferFormFieldNames.LOCATION, {
                type: FieldErrors.REQUIRED,
                text: 'required',
            });

            await flushPromises();
        });

        expect(hasGroupError(OfferAccordionSectionId.TECH)).toEqual(true);

        expect(hasGroupError(OfferAccordionSectionId.COMPLECTATION)).toEqual(false);

        expect(hasGroupError(OfferAccordionSectionId.ADDRESS)).toEqual(true);
    });

    it('для не сабмитнутой формы НЕ подсвечиваются ошибки, если группа НЕ потрогана, есть показанные ошибки и шаг доступен', async() => {
        const {
            formApi,
        } = await componentRenderer();

        await act(async() => {
            formApi.current?.setFieldValue(OfferFormFieldNames.LOCATION, { cityName: '123' });

            formApi.current?.setFieldError(FieldNames.TECH_PARAM, {
                type: FieldErrors.REQUIRED,
                text: 'required',
            });

            formApi.current?.setFieldError(OfferFormFieldNames.LOCATION, {
                type: FieldErrors.REQUIRED,
                text: 'required',
            });

            await flushPromises();
        });

        expect(hasGroupError(OfferAccordionSectionId.TECH)).toEqual(false);

        expect(hasGroupError(OfferAccordionSectionId.COMPLECTATION)).toEqual(false);

        expect(hasGroupError(OfferAccordionSectionId.ADDRESS)).toEqual(true);
    });

    it('для не сабмитнутой формы НЕ подсвечиваются ошибки, если группа потрогана, НЕ показанных ошибок и шаг доступен', async() => {
        const {
            formApi,
        } = await componentRenderer();

        await act(async() => {
            formApi.current?.setFieldValue(FieldNames.TECH_PARAM, { text: '123' });
            formApi.current?.setFieldValue(OfferFormFieldNames.LOCATION, { cityName: '123' });

            formApi.current?.setFieldError(OfferFormFieldNames.LOCATION, {
                type: FieldErrors.REQUIRED,
                text: 'required',
            });

            await flushPromises();
        });

        expect(hasGroupError(OfferAccordionSectionId.TECH)).toEqual(false);

        expect(hasGroupError(OfferAccordionSectionId.COMPLECTATION)).toEqual(false);

        expect(hasGroupError(OfferAccordionSectionId.ADDRESS)).toEqual(true);
    });
});

describe('заполненность', () => {
    it('показывается, если нет показанных ошибок, заполненны все поля (тут варианты: !isFilled или !hasErrors) и шаг доступен', async() => {
        await componentRenderer();

        await flushPromises();

        expect(isGroupMarkedAsSuccess(OfferAccordionSectionId.TECH)).toEqual(true);

        expect(isGroupMarkedAsSuccess(OfferAccordionSectionId.COMPLECTATION)).toEqual(true);

        expect(isGroupMarkedAsSuccess(OfferAccordionSectionId.ADDRESS)).toEqual(true);
    });

    it('НЕ показывается, если показаны ошибки, НЕ заполненны все поля (тут варианты: !isFilled или !hasErrors) и шаг доступен', async() => {
        const {
            formApi,
        } = await componentRenderer();

        await act(async() => {
            formApi.current?.setFieldError(FieldNames.TECH_PARAM, {
                type: FieldErrors.REQUIRED,
                text: 'required',
            });

            formApi.current?.setFieldError(OfferFormFieldNames.LOCATION, {
                type: FieldErrors.REQUIRED,
                text: 'required',
            });

            formApi.current?.setFieldError(FieldNames.COMPLECTATION, {
                type: FieldErrors.REQUIRED,
                text: 'required',
            });

            await flushPromises();
        });

        expect(isGroupMarkedAsSuccess(OfferAccordionSectionId.TECH)).toEqual(false);

        expect(isGroupMarkedAsSuccess(OfferAccordionSectionId.COMPLECTATION)).toEqual(false);

        expect(isGroupMarkedAsSuccess(OfferAccordionSectionId.ADDRESS)).toEqual(false);
    });
});

async function componentRenderer() {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const { mockUseDispatch, mockUseSelector } = applyUseSelectorMock();

    mockUseSelector({});
    mockUseDispatch(storeMock);

    await renderComponent(
        <>
            <OfferAccordionContents
                sections={ mockSections }
                allSections={ mockSections }
            />

            { mockSections.map((section) => {
                const Component = section.component;

                return (
                    <Component
                        key={ section.id }
                        id={ section.id }
                        isVisible={ section.isVisible }
                        initialIsCollapsed={ false }
                    />
                );
            }) }
        </>,
        {
            formApi,
            offerFormContext: offerFormPageContextMock,
        },
    );

    return {
        formApi,
    };
}

function isGroupAccessible(groupId: OfferAccordionSectionId) {
    return screen.queryByTestId(`menu-section-${ groupId }`)?.classList.contains('OfferAccordionContents__listItem_accessible');
}

function hasGroupError(groupId: OfferAccordionSectionId) {
    return screen.queryByTestId(`menu-section-${ groupId }`)?.classList.contains('OfferAccordionContents__listItem_error');
}

function isGroupMarkedAsSuccess(groupId: OfferAccordionSectionId) {
    return screen.queryByTestId(`menu-section-${ groupId }`)?.classList.contains('OfferAccordionContents__listItem_success');
}
