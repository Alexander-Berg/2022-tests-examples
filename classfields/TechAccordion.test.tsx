jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
        useDispatch: jest.fn(),
    };
});
const AccordionMock = ({ children }: { children: ReactElement }) => <div>{ children }</div>;
jest.mock('auto-core/react/components/common/Accordion/Accordion', () => AccordionMock);
jest.mock('auto-core/react/components/common/TechAccordion/hooks/useTechAccordion');
jest.mock('auto-core/react/dataDomain/catalogSuggest/actions/fetch');
jest.mock('auto-core/react/dataDomain/notifier/actions/notifier');
jest.mock('auto-core/react/components/common/Form/fields/hooks/useFetchCatalogSuggest');
jest.mock('auto-core/react/components/common/Form/fields/hooks/useResetDependedTechFields');

import type { ReactElement } from 'react';
import React from 'react';
import { act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import type { CarSuggest } from '@vertis/schema-registry/ts-types-snake/auto/api/cars_helper_model';
import { Car_BodyType, Car_EngineType, Car_GearType, Car_Transmission } from '@vertis/schema-registry/ts-types-snake/auto/api/cars_model';

import { findByChildrenText } from 'autoru-frontend/jest/unit/queryHelpers';
import flushPromises from 'autoru-frontend/jest/unit/flushPromises';

import catalogSuggestStateMock from 'auto-core/react/dataDomain/catalogSuggest/mock';
import type { FormContext, FormValues } from 'auto-core/react/components/common/Form/types';
import { AccordionContextMock } from 'auto-core/react/components/common/Accordion/AccordionContext.mock';
import type { FieldErrors, Fields } from 'auto-core/react/components/common/Form/fields/types';
import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';
import useFetchCatalogSuggest from 'auto-core/react/components/common/Form/fields/hooks/useFetchCatalogSuggest';
import useResetDependedTechFields from 'auto-core/react/components/common/Form/fields/hooks/useResetDependedTechFields';
import type { AppStateCore } from 'auto-core/react/AppState';
import { renderComponent } from 'auto-core/react/components/common/Form/utils/testUtils';

import superGenMock from 'auto-core/models/catalogSuggest/mocks/super_gen.mock';
import catalogSuggestMock from 'auto-core/models/catalogSuggest/mocks';

import useTechAccordion from './hooks/useTechAccordion';
import { TechAccordionSectionId } from './types';
import type { Props } from './TechAccordion';
import TechAccordion from './TechAccordion';
import { sections } from './sections/sections';

const useTechAccordionMock = useTechAccordion as jest.MockedFunction<typeof useTechAccordion>;
useTechAccordionMock.mockReturnValue({
    accordionApi: {
        current: AccordionContextMock,
    },
});
const useFetchCatalogSuggestMock = useFetchCatalogSuggest as jest.MockedFunction<typeof useFetchCatalogSuggest>;
const useResetDependedTechFieldsMock = useResetDependedTechFields as jest.MockedFunction<typeof useResetDependedTechFields>;

const fetchCatalogSuggestMock = jest.fn(() => Promise.resolve({} as CarSuggest));
const resetDependedTechFieldsMock = jest.fn(() => {});

type State = Partial<AppStateCore>

let defaultState: State;
let defaultProps: Props;
let defaultInitialValues: FormValues<FieldNames, Fields>;

beforeEach(() => {
    const catalogSuggest = catalogSuggestMock.value();

    defaultProps = {
        catalogSuggest,
        isHidden: false,
        isLoading: false,
        isSuggestError: false,
        isSuggestFetching: false,
        sections: sections,
        onFieldFilled: jest.fn(),
    };

    defaultState = {
        catalogSuggest: catalogSuggestStateMock.withData(catalogSuggest).value(),
    };

    defaultInitialValues = {
        [FieldNames.YEAR]: 2020,
        [FieldNames.BODY_TYPE]: Car_BodyType.WAGON_5_DOORS,
        [FieldNames.SUPER_GEN]: {
            data: '1',
            text: 'IX (X110)',
        },
        [FieldNames.ENGINE_TYPE]: Car_EngineType.LPG,
        [FieldNames.GEAR_TYPE]: Car_GearType.REAR_DRIVE,
        [FieldNames.TRANSMISSION]: Car_Transmission.AUTOMATIC,
        [FieldNames.TECH_PARAM]: {
            data: '1',
            text: '100 л.с. (1.4 MT)',
        },
        [FieldNames.COLOR]: 'FAFBFB',
        [FieldNames.MILEAGE]: 20000,
        [FieldNames.RIGHT_STEERING_WHEEL]: true,
        [FieldNames.EQUIPMENT]: {
            gbo: true,
        },
    };

    fetchCatalogSuggestMock.mockClear();
    resetDependedTechFieldsMock.mockClear();
    useFetchCatalogSuggestMock.mockReturnValue(fetchCatalogSuggestMock);
    useResetDependedTechFieldsMock.mockReturnValue(resetDependedTechFieldsMock);
});

describe('при маунте', () => {
    it('раскроет первую незаполненную секцию, но не будет скроллить к ней', async() => {
        const initialValues = {
            [FieldNames.YEAR]: 2020,
        };
        await renderComponent(<TechAccordion { ...defaultProps }/>, { initialValues, state: defaultState });
        await flushPromises();

        expect(AccordionContextMock.expandSection).toHaveBeenCalledTimes(1);
        expect(AccordionContextMock.expandSection).toHaveBeenCalledWith(TechAccordionSectionId.BODY_TYPE);
        expect(AccordionContextMock.scrollToSection).not.toHaveBeenCalled();
    });

    it('если все обязательные секции заполнены, то ничего не будет делать', async() => {
        await renderComponent(<TechAccordion { ...defaultProps }/>, { initialValues: defaultInitialValues, state: defaultState });
        await flushPromises();

        expect(AccordionContextMock.expandSection).not.toHaveBeenCalled();
    });
});

describe('лог при изменения поля', () => {
    it('с ограниченным набором значений', async() => {
        expect.assertions(2);
        const { findAllByRole } = await renderComponent(<TechAccordion { ...defaultProps }/>, { initialValues: defaultInitialValues, state: defaultState });

        const item = (await findAllByRole('button')).find(findByChildrenText('Седан'));
        if (!item) {
            return;
        }
        userEvent.click(item);

        expect(defaultProps.onFieldFilled).toHaveBeenCalledTimes(1);
        expect(defaultProps.onFieldFilled).toHaveBeenCalledWith(FieldNames.BODY_TYPE, Car_BodyType.SEDAN, 'success');
    });

    it('с неограниченным набором значений', async() => {
        const initialValues = {
            ...defaultInitialValues,
            [FieldNames.YEAR]: undefined,
        };
        const { findByRole } = await renderComponent(<TechAccordion { ...defaultProps }/>, { initialValues, state: defaultState });

        const item = await findByRole('button', { name: '2020' });
        userEvent.click(item);

        expect(defaultProps.onFieldFilled).toHaveBeenCalledTimes(1);
        expect(defaultProps.onFieldFilled).toHaveBeenCalledWith(FieldNames.YEAR, 2020, 'success');
    });
});

describe('правильно формирует список видимых секций', () => {
    it('если какие-то секции пропущены и незаполнены', async() => {
        const initialValues = {
            ...defaultInitialValues,
            [FieldNames.ENGINE_TYPE]: undefined,
        };
        await renderComponent(<TechAccordion { ...defaultProps }/>, { initialValues, state: defaultState });

        const sections = document.querySelectorAll('.TechAccordion__section');
        const sectionsVisibility = Array.from(sections).map((section) => ({
            id: section.id,
            isVisible: section.className.includes('visible'),
        }));

        expect(sectionsVisibility).toEqual([
            { id: TechAccordionSectionId.YEAR, isVisible: true },
            { id: TechAccordionSectionId.BODY_TYPE, isVisible: true },
            { id: TechAccordionSectionId.SUPER_GEN, isVisible: true },
            { id: TechAccordionSectionId.ENGINE_TYPE, isVisible: true },
            { id: TechAccordionSectionId.GEAR_TYPE, isVisible: false },
            { id: TechAccordionSectionId.TRANSMISSION, isVisible: false },
            { id: TechAccordionSectionId.TECH_PARAM, isVisible: false },
            { id: TechAccordionSectionId.MILEAGE, isVisible: false },
        ]);
    });

    it('если загружаем саджест для секции', async() => {
        const initialValues = {
            ...defaultInitialValues,
            [FieldNames.ENGINE_TYPE]: undefined,
        };
        const props = {
            ...defaultProps,
            isLoading: true,
        };
        await renderComponent(<TechAccordion { ...props }/>, { initialValues, state: defaultState });

        const sections = document.querySelectorAll<HTMLElement>('.TechAccordion__section');
        const engineSection = Array.from(sections).find(findByChildrenText('Двигатель'));

        expect(engineSection?.className).not.toContain('visible');
    });

    it('если заполнено все до пробега', async() => {
        const initialValues = {
            ...defaultInitialValues,
            [FieldNames.MILEAGE]: undefined,
        };
        await renderComponent(<TechAccordion { ...defaultProps }/>, { initialValues, state: defaultState });

        const sections = document.querySelectorAll('.TechAccordion__section');
        const sectionsVisibility = Array.from(sections).map((section) => ({
            id: section.id,
            isVisible: section.className.includes('visible'),
        }));

        expect(sectionsVisibility).toEqual([
            { id: TechAccordionSectionId.YEAR, isVisible: true },
            { id: TechAccordionSectionId.BODY_TYPE, isVisible: true },
            { id: TechAccordionSectionId.SUPER_GEN, isVisible: true },
            { id: TechAccordionSectionId.ENGINE_TYPE, isVisible: true },
            { id: TechAccordionSectionId.GEAR_TYPE, isVisible: true },
            { id: TechAccordionSectionId.TRANSMISSION, isVisible: true },
            { id: TechAccordionSectionId.TECH_PARAM, isVisible: true },
            { id: TechAccordionSectionId.MILEAGE, isVisible: true },
        ]);
    });
});

describe('при расхлопывании секции', () => {
    it('если для нее не пришел саджест, пойдем за ним снова', async() => {
        expect.assertions(2);

        const props = {
            ...defaultProps,
            catalogSuggest: catalogSuggestMock.withEngineTypes([]).value(),
        };
        await renderComponent(<TechAccordion { ...props }/>, { initialValues: defaultInitialValues, state: defaultState });

        const sections = document.querySelectorAll<HTMLElement>('.TechAccordion__section');
        const engineSection = Array.from(sections).find(findByChildrenText('Двигатель'));
        const engineSectionHeader = engineSection?.querySelector('.TechAccordionSectionHeader');
        if (!engineSectionHeader) {
            return;
        }
        await act(async() => {
            await userEvent.click(engineSectionHeader);
        });

        expect(fetchCatalogSuggestMock).toHaveBeenCalledTimes(1);
        expect(fetchCatalogSuggestMock).toHaveBeenCalledWith();
    });
});

describe('при выборе значения в секции', () => {
    it('сначала схлопнет текущую секцию и все последующие секции', async() => {
        expect.assertions(2);
        const initialValues = {
            [FieldNames.YEAR]: 2020,
        };
        const { findAllByRole } = await renderComponent(<TechAccordion { ...defaultProps }/>, { initialValues, state: defaultState });
        const item = (await findAllByRole('button')).find(findByChildrenText('Седан'));
        if (!item) {
            return;
        }
        userEvent.click(item);

        expect(AccordionContextMock.collapseSection).toHaveBeenCalledTimes(10);
        expect(AccordionContextMock.collapseSection.mock.calls).toEqual([
            [ TechAccordionSectionId.BODY_TYPE ],
            [ TechAccordionSectionId.BODY_TYPE ],
            [ TechAccordionSectionId.SUPER_GEN ],
            [ TechAccordionSectionId.ENGINE_TYPE ],
            [ TechAccordionSectionId.GEAR_TYPE ],
            [ TechAccordionSectionId.TRANSMISSION ],
            [ TechAccordionSectionId.TECH_PARAM ],
            [ TechAccordionSectionId.COLOR ],
            [ TechAccordionSectionId.MILEAGE ],
            [ TechAccordionSectionId.EXTRA ],
        ]);
    });

    it('сбросит зависимые поляи и запросит саджест каталога', async() => {
        expect.assertions(4);
        const initialValues = {
            [FieldNames.YEAR]: 2020,
        };
        const { findAllByRole } = await renderComponent(<TechAccordion { ...defaultProps }/>, { initialValues, state: defaultState });
        const item = (await findAllByRole('button')).find(findByChildrenText('Седан'));
        if (!item) {
            return;
        }
        userEvent.click(item);

        expect(resetDependedTechFieldsMock).toHaveBeenCalledTimes(1);
        expect(resetDependedTechFieldsMock).toHaveBeenCalledWith(FieldNames.BODY_TYPE);

        expect(fetchCatalogSuggestMock).toHaveBeenCalledTimes(1);
        expect(fetchCatalogSuggestMock).toHaveBeenCalledWith(FieldNames.BODY_TYPE, Car_BodyType.SEDAN);
    });

    it('если ничего нет для автоселекта, откроет следующую секцию', async() => {
        expect.assertions(2);

        const initialValues = {
            [FieldNames.YEAR]: 2020,
        };

        const { findAllByRole } = await renderComponent(<TechAccordion { ...defaultProps }/>, { initialValues, state: defaultState });

        const item = (await findAllByRole('button')).find(findByChildrenText('Седан'));
        if (!item) {
            return;
        }
        userEvent.click(item);

        await flushPromises();
        expect(AccordionContextMock.expandSection).toHaveBeenCalledTimes(2);
        expect(AccordionContextMock.expandSection).toHaveBeenNthCalledWith(2, TechAccordionSectionId.SUPER_GEN);
    });

    describe('если есть поля для авто селекта', () => {
        let formApi: React.RefObject<FormContext<FieldNames, Fields, FieldErrors>>;
        const fetchPromise = Promise.resolve(
            catalogSuggestMock
                .withSuperGens([
                    superGenMock.value(),
                ])
                .withEngineTypes([ Car_EngineType.DIESEL ])
                .value(),
        );
        const fetchCatalogSuggestMock = jest.fn(() => fetchPromise);

        beforeEach(async() => {
            const initialValues = {
                [FieldNames.YEAR]: 2020,
            };
            useFetchCatalogSuggestMock.mockReturnValue(fetchCatalogSuggestMock);

            formApi = React.createRef<FormContext<FieldNames, Fields, FieldErrors>>();
            const { findAllByRole } = await renderComponent(<TechAccordion { ...defaultProps }/>, { initialValues, state: defaultState, formApi });

            const item = (await findAllByRole('button')).find(findByChildrenText('Седан'));
            if (!item) {
                return;
            }

            await act(async() => {
                userEvent.click(item);

                await flushPromises();
            });
        });

        it('выберет их', async() => {
            const superGen = formApi.current?.getFieldValue(FieldNames.SUPER_GEN);
            expect(superGen).toEqual({
                data: '21028015',
                text: 'IV',
            });

            const engineType = formApi.current?.getFieldValue(FieldNames.ENGINE_TYPE);
            expect(engineType).toBe(Car_EngineType.DIESEL);
        });

        it('отправит лог', async() => {
            expect(defaultProps.onFieldFilled).toHaveBeenCalledTimes(3);
            expect(defaultProps.onFieldFilled).toHaveBeenNthCalledWith(2, FieldNames.SUPER_GEN, { data: '21028015', text: 'IV' }, 'autofill');
            expect(defaultProps.onFieldFilled).toHaveBeenNthCalledWith(3, FieldNames.ENGINE_TYPE, Car_EngineType.DIESEL, 'autofill');
        });

        it('сделает запрос за саджестом с новыми параметрами', async() => {
            expect(fetchCatalogSuggestMock).toHaveBeenCalledTimes(2);
            expect(fetchCatalogSuggestMock).toHaveBeenCalledWith(FieldNames.ENGINE_TYPE, Car_EngineType.DIESEL);
        });

        it('после запроса перейдет к следующей пустой секции', async() => {
            await flushPromises();

            expect(AccordionContextMock.expandSection).toHaveBeenCalledTimes(2);
            expect(AccordionContextMock.expandSection).toHaveBeenNthCalledWith(2, TechAccordionSectionId.GEAR_TYPE);
            expect(AccordionContextMock.scrollToSection).toHaveBeenCalledTimes(1);
            expect(AccordionContextMock.scrollToSection).toHaveBeenNthCalledWith(1, TechAccordionSectionId.GEAR_TYPE, { block: 'center' });
        });
    });
});
