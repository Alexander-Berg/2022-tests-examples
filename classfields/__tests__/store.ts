import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { UserPersonalActivity } from 'types/user';

import { IUniversalStore } from 'view/modules/types';
import { Fields } from 'view/modules/questionnaireForm/types';
import { initialState as fieldsInitialState } from 'view/modules/questionnaireForm/reducers/fields';
import { initialState as networkInitialState } from 'view/modules/questionnaireForm/reducers/network';

export const filledQuestionnaireStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    questionnaireForm: {
        fields: {
            [Fields.REASON_FOR_RELOCATION]: {
                id: Fields.REASON_FOR_RELOCATION,
                value: 'Уголовное преследование',
            },
            [Fields.ADDITIONAL_TENANT]: {
                id: Fields.ADDITIONAL_TENANT,
                value: 'Один',
            },
            [Fields.HAS_CHILDREN]: {
                id: Fields.HAS_CHILDREN,
                value: true,
            },
            [Fields.HAS_PETS]: {
                id: Fields.HAS_PETS,
                value: true,
            },
            [Fields.NUMBER_OF_TEENAGERS]: {
                id: Fields.NUMBER_OF_TEENAGERS,
                value: 1,
            },
            [Fields.NUMBER_OF_SCHOOLCHILDREN]: {
                id: Fields.NUMBER_OF_SCHOOLCHILDREN,
                value: 2,
            },
            [Fields.NUMBER_OF_PRESCHOOLERS]: {
                id: Fields.NUMBER_OF_PRESCHOOLERS,
                value: 3,
            },
            [Fields.NUMBER_OF_BABIES]: {
                id: Fields.NUMBER_OF_BABIES,
                value: 4,
            },
            [Fields.PETS_INFO]: {
                id: Fields.PETS_INFO,
                value: 'Беспредельщики',
            },
            [Fields.TELL_ABOUT_YOURSELF]: {
                id: Fields.TELL_ABOUT_YOURSELF,
                value: 'Беспредельщик',
            },
            [Fields.PERSONAL_ACTIVITY_TYPE]: {
                id: Fields.PERSONAL_ACTIVITY_TYPE,
                value: UserPersonalActivity.WORK_AND_STUDY,
            },
            [Fields.EDUCATIONAL_INSTITUTION]: {
                id: Fields.EDUCATIONAL_INSTITUTION,
                value: 'СПБГЭТУ "ЛЭТИ"',
            },
            [Fields.ABOUT_WORK_AND_POSITION]: {
                id: Fields.ABOUT_WORK_AND_POSITION,
                value: 'ООО Яндекс Вертикали',
            },
            [Fields.ABOUT_BUSINESS]: {
                id: Fields.ABOUT_BUSINESS,
                value: '',
            },
        },
        network: networkInitialState,
    },
    modal: {
        isOpen: false,
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    page: { params: {} },
    spa: {
        status: RequestStatus.PENDING,
    },
    questionnaireForm: {
        fields: fieldsInitialState,
        network: networkInitialState,
    },
    modal: {
        isOpen: false,
    },
};
