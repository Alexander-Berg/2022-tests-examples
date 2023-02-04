import { ISelectedFilterDeclaration } from 'realty-core/view/react/modules/filters/types/filter-declaration';

export const emptyFunctionMock = () => null;

export const selectedFiltersDeclaration: ISelectedFilterDeclaration[] = [
    {
        id: '1',
        getLabel: (i18n) => i18n('hasPark'),
        onResetFilterValue: emptyFunctionMock,
    },
    {
        id: '2',
        getLabel: (i18n) => i18n('hasPond'),
        onResetFilterValue: emptyFunctionMock,
    },
    {
        id: '3',
        getLabel: (i18n) => i18n('balcony+ANY'),
        onResetFilterValue: emptyFunctionMock,
    },
    {
        id: '4',
        getLabel: (i18n) => i18n('floorCommercial+first'),
        onResetFilterValue: emptyFunctionMock,
    },
    {
        id: '5',
        getLabel: (i18n) => i18n('bathroomUnit+TWO_AND_MORE'),

        onResetFilterValue: emptyFunctionMock,
    },
    {
        id: '6',
        getLabel: (i18n) => i18n('apartmentType+YES'),
        onResetFilterValue: emptyFunctionMock,
    },
];
