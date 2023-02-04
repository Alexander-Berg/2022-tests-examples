jest.mock('www-poffer/react/components/desktop/OfferAccordion/utils/sections', () => []);
jest.mock('www-poffer/react/components/common/OfferForm/utils/scrollToField', () => jest.fn());

jest.mock('auto-core/react/components/common/Wizard/contexts/WizardContext', () => {
    const originalModule = jest.requireActual('auto-core/react/components/common/Wizard/contexts/WizardContext');
    const goToNextStep = jest.fn();

    return {
        __esModule: true,
        ...originalModule,
        goToNextStep,
        useWizardContext: () => {
            return {
                ...originalModule.useWizardContext(),
                goToNextStep,
            };
        },
    };
});

import React from 'react';
import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { simulateControlClick } from 'jest/unit/eventSimulators';

import mockStore from 'autoru-frontend/mocks/mockStore';
import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';
import flushPromises from 'autoru-frontend/jest/unit/flushPromises';

import type { FormFieldRegistryField } from 'auto-core/react/components/common/Form/types';
import * as mockWizardContext from 'auto-core/react/components/common/Wizard/contexts/WizardContext';
import FormInputControl from 'auto-core/react/components/common/Form/controls/FormInputControl';
import { FieldNames, FieldErrors } from 'auto-core/react/components/common/Form/fields/types';

import scrollToField from 'www-poffer/react/components/common/OfferForm/utils/scrollToField';
import mockSections from 'www-poffer/react/components/desktop/OfferAccordion/utils/sections';
import { OfferAccordionSectionId } from 'www-poffer/react/components/desktop/OfferAccordion/types';
import type { OfferAccordionSectionProps } from 'www-poffer/react/components/desktop/OfferAccordion/types';
import {
    OfferWizard,
    OfferWizardControls,
    OfferWizardStep,
    OfferWizardContent,
} from 'www-poffer/react/components/desktop/OfferWizard/OfferWizard';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';
import { renderComponent } from 'www-poffer/react/utils/testUtils';

import '@testing-library/jest-dom';

const validator = jest.fn(async() => {});

afterEach(() => {
    validator.mockReset();
});

const TestSection: (props: OfferAccordionSectionProps) => JSX.Element | null = () => {
    return (
        <div id={ FieldNames.TECH_PARAM }>
            <FormInputControl<FieldNames.TECH_PARAM, OfferFormFields, FieldErrors>
                name={ FieldNames.TECH_PARAM }
                validator={ validator }
            />
        </div>
    );
};

afterEach(() => {
    mockSections.splice(0, mockSections.length);
});

it('кнопки не показываются, если shouldShowWizardButtons вернул false', async() => {
    mockSections.push({
        id: OfferAccordionSectionId.TECH,
        component: TestSection,
        isRequired: true,
        isVisible: true,

        shouldShowWizardButtons: () => false,
    });

    await componentRenderer();

    expect(screen.queryByText('Пропустить')).not.toBeInTheDocument();
    expect(screen.queryByText('Продолжить')).not.toBeInTheDocument();
});

it('кнопки показываются, если shouldShowWizardButtons вернул true', async() => {
    mockSections.push({
        id: OfferAccordionSectionId.TECH,
        component: TestSection,
        isRequired: true,
        isVisible: true,

        shouldShowWizardButtons: () => true,
    });

    await componentRenderer();

    expect(screen.queryByText('Пропустить')).not.toBeInTheDocument();
    expect(screen.queryByText('Продолжить')).toBeInTheDocument();
});

it('кнопки показываются, если shouldShowWizardButtons нет', async() => {
    mockSections.push({
        id: OfferAccordionSectionId.TECH,
        component: TestSection,
        isRequired: true,
        isVisible: true,
    });

    await componentRenderer();

    expect(screen.queryByText('Пропустить')).not.toBeInTheDocument();
    expect(screen.queryByText('Продолжить')).toBeInTheDocument();
});

it('для не required не заполненного блока показывается кнопка Пропустить', async() => {
    mockSections.push({
        id: OfferAccordionSectionId.TECH,
        component: TestSection,
        isRequired: false,
        isVisible: true,
        isFilled: () => false,
    });

    await componentRenderer({
        canBeSkipped: true,
    });

    expect(screen.queryByText('Пропустить')).toBeInTheDocument();
    expect(screen.queryByText('Продолжить')).not.toBeInTheDocument();
});

it('для не required не заполненного блока c ошибками показывается кнопка Пропустить', async() => {
    mockSections.push({
        id: OfferAccordionSectionId.TECH,
        component: TestSection,
        isRequired: false,
        isVisible: true,
    });

    (
        validator as unknown as jest.MockedFunction<
        Exclude<FormFieldRegistryField<OfferFormFieldNamesType, OfferFormFields, FieldErrors>['validator'], undefined>
        >
    ).mockImplementation(async() => ({
        type: FieldErrors.REQUIRED,
        text: 'error',
    }));

    await componentRenderer({
        canBeSkipped: true,
    });

    expect(validator).toHaveBeenCalled();
    expect(screen.queryByText('Пропустить')).toBeInTheDocument();
    expect(screen.queryByText('Продолжить')).not.toBeInTheDocument();
});

it('для не required потроганного блока кнопка Продолжить', async() => {
    mockSections.push({
        id: OfferAccordionSectionId.TECH,
        component: TestSection,
        isRequired: false,
        isVisible: true,
        isFilled: () => false,
    });

    await componentRenderer({
        canBeSkipped: true,
    });

    userEvent.type(screen.getByRole('textbox'), 'asd');

    expect(screen.queryByText('Пропустить')).not.toBeInTheDocument();
    expect(screen.queryByText('Продолжить')).toBeInTheDocument();
});

it('для required блока кнопка Продолжить', async() => {
    mockSections.push({
        id: OfferAccordionSectionId.TECH,
        component: TestSection,
        isRequired: true,
        isVisible: true,
    });

    await componentRenderer();

    expect(screen.queryByText('Пропустить')).not.toBeInTheDocument();
    expect(screen.queryByText('Продолжить')).toBeInTheDocument();
});

it('для последнего блока кнопки не показываются', async() => {
    mockSections.push({
        id: OfferAccordionSectionId.TECH,
        component: TestSection,
        isRequired: false,
        isVisible: true,
    });

    await componentRenderer({
        showLast: false,
    });

    expect(screen.queryByText('Пропустить')).not.toBeInTheDocument();
    expect(screen.queryByText('Продолжить')).not.toBeInTheDocument();
});

it('тык в Продолжить валидирует секцию и скроллит к ошибке', async() => {
    mockSections.push({
        id: OfferAccordionSectionId.TECH,
        component: TestSection,
        isRequired: true,
        isVisible: true,
    });

    (
        validator as unknown as jest.MockedFunction<
        Exclude<FormFieldRegistryField<OfferFormFieldNamesType, OfferFormFields, FieldErrors>['validator'], undefined>
        >
    ).mockImplementation(async() => ({
        type: FieldErrors.REQUIRED,
        text: 'error1',
    }));

    await componentRenderer();

    await act(async() => {
        await simulateControlClick(screen.getByText('Продолжить'));
    });

    await flushPromises();

    expect(validator).toHaveBeenCalled();
    expect(scrollToField).toHaveBeenCalledTimes(1);
    expect(scrollToField).toHaveBeenCalledWith(FieldNames.TECH_PARAM, 0);
});

it('тык в Продолжить валидирует секцию и переключает шаг', async() => {
    mockSections.push({
        id: OfferAccordionSectionId.TECH,
        component: TestSection,
        isRequired: true,
        isVisible: true,
    });

    await componentRenderer();

    await act(async() => {
        await simulateControlClick(screen.getByText('Продолжить'));
    });

    await flushPromises();

    expect(validator).toHaveBeenCalledTimes(2);
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    expect(mockWizardContext.goToNextStep).toHaveBeenCalledTimes(1);
});

it('тык в Пропустить переключает шаг', async() => {
    mockSections.push({
        id: OfferAccordionSectionId.TECH,
        component: TestSection,
        isRequired: false,
        isVisible: true,

        isFilled: () => false,
    });

    await componentRenderer({
        canBeSkipped: true,
    });

    userEvent.click(screen.getByText('Пропустить'));

    await flushPromises();

    expect(validator).toHaveBeenCalledTimes(1);
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    expect(mockWizardContext.goToNextStep).toHaveBeenCalledTimes(1);
});

type RenderOptions = {
    canBeSkipped?: boolean;
    showLast?: boolean;
}

async function componentRenderer(options: RenderOptions = {}) {
    const {
        canBeSkipped = false,
        showLast = true,
    } = options;

    const { mockUseDispatch, mockUseSelector } = applyUseSelectorMock();
    const storeMock = mockStore({});
    mockUseSelector({});
    mockUseDispatch(storeMock);

    await renderComponent(
        <OfferWizard
            stepScrollOffset={ 116 }
            initialStepOrder={ 0 }
        >
            <OfferWizardContent>
                <OfferWizardStep
                    order={ 0 }
                    canBeSkipped={ canBeSkipped }
                >
                    <TestSection
                        id={ OfferAccordionSectionId.TECH }
                        isVisible
                        initialIsCollapsed
                    />
                </OfferWizardStep>

                { showLast && (
                    <OfferWizardStep order={ 1 }>последний шаг</OfferWizardStep>
                ) }

                <OfferWizardControls/>
            </OfferWizardContent>
        </OfferWizard>,
    );
}
