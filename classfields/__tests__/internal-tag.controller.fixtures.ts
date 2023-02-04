import { TagModel } from '../tag.model';

const DEFAULT_TAG: Partial<TagModel> = {
    name: 'whatsnew',
};

export const fixtures = {
    'Internal tag controller GET /internal/tags Возвращает теги': {
        TAG_ATTRIBUTES_1: {
            ...DEFAULT_TAG,
            name: 'whatsnew-1',
        },
        TAG_ATTRIBUTES_2: {
            ...DEFAULT_TAG,
            name: 'whatsnew-2',
        },
        TAG_ATTRIBUTES_3: {
            ...DEFAULT_TAG,
            name: 'whatsnew-3',
        },
    },

    'Internal tag controller GET /internal/tags Возвращает теги отсортированные по имени': {
        TAG_ATTRIBUTES_1: {
            ...DEFAULT_TAG,
            name: 'whatsnew-1',
        },
        TAG_ATTRIBUTES_2: {
            ...DEFAULT_TAG,
            name: 'whatsnew-2',
        },
        TAG_ATTRIBUTES_3: {
            ...DEFAULT_TAG,
            name: 'whatsnew-3',
        },
    },
};
