import { ISamoletPlansFilters } from 'realty-core/types/samolet/filters';

export const plansSiteFilter = [
    { value: '202457', label: 'Пригород Лесное' },
    { value: '1680614', label: 'Большое Путилково' },
    { value: '1839196', label: 'Люберцы' },
    { value: '1645991', label: 'Остафьево' },
];

export const searchQuery = {};

export const fullSearchQuery = {
    roomsTotal: ['STUDIO', '3', 'PLUS_4'],
    priceMin: 1234567,
    priceMax: 9876543,
    siteId: ['1680614'],
} as ISamoletPlansFilters & Record<string, string | number>;
