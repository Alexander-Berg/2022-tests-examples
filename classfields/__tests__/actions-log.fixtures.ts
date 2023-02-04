import { ActionLogAction, ActionLogEntity } from '../../../types/actions-log';

export const fixtures = {
    'Actions log service create Создает и возвращает модель лога': {
        ACTIONS_LOG_ATTRIBUTES: {
            author: 'rbuslov',
            action: ActionLogAction.publish,
            entity: ActionLogEntity.post,
            urlPart: 'avtovaz-lishil-lady-mediasistemy-izza-nehvatki-mikroshem',
        },
    },
    'Actions log service createMany Создает и возвращает массив моделей логов': {
        ACTIONS_LOG_ATTRIBUTES_1: {
            author: 'rbuslov',
            action: ActionLogAction.publish,
            entity: ActionLogEntity.post,
            urlPart: 'avtovaz-lishil-lady-mediasistemy-izza-nehvatki-mikroshem',
        },
        ACTIONS_LOG_ATTRIBUTES_2: {
            author: 'mtsymbal',
            action: ActionLogAction.publish,
            entity: ActionLogEntity.post,
            urlPart: 'renaultsafrane',
        },
        ACTIONS_LOG_ATTRIBUTES_3: {
            author: 'swapster',
            action: ActionLogAction.publish,
            entity: ActionLogEntity.post,
            urlPart: 'electromustang',
        },
    },
    'Actions log service updatePostKey Возвращает 0, если логи поста не найдены': {
        POST_ACTIONS_LOG_ATTRIBUTES_1: {
            author: 'aandreev',
            action: ActionLogAction.create,
            entity: ActionLogEntity.post,
            urlPart: 'avtovaz-lishil-lady-mediasistemy-izza-nehvatki-mikroshem',
        },
        POST_ACTIONS_LOG_ATTRIBUTES_2: {
            author: 'rbuslov',
            action: ActionLogAction.publish,
            entity: ActionLogEntity.post,
            urlPart: 'avtovaz-lishil-lady-mediasistemy-izza-nehvatki-mikroshem',
        },
    },
    'Actions log service updatePostKey Обновляет urlPart и возвращает количество обновленных записей': {
        POST_ACTIONS_LOG_ATTRIBUTES_1: {
            author: 'aandreev',
            action: ActionLogAction.create,
            entity: ActionLogEntity.post,
            urlPart: 'avtovaz-lishil-lady-mediasistemy-izza-nehvatki-mikroshem',
        },
        POST_ACTIONS_LOG_ATTRIBUTES_2: {
            author: 'rbuslov',
            action: ActionLogAction.publish,
            entity: ActionLogEntity.post,
            urlPart: 'avtovaz-lishil-lady-mediasistemy-izza-nehvatki-mikroshem',
        },
        POST_ACTIONS_LOG_ATTRIBUTES_3: {
            author: 'rbuslov',
            action: ActionLogAction.update,
            entity: ActionLogEntity.post,
            urlPart: 'bespilotniki-vstali-na-vooruzhenie-gibdd-v-17-regionah-rossii',
        },
        TAG_ACTIONS_LOG_ATTRIBUTES_1: {
            author: 'rbuslov',
            action: ActionLogAction.create,
            entity: ActionLogEntity.tag,
            urlPart: 'bmw',
        },
    },
    'Actions log service updatePostKey Обновляет urlPart только у постов': {
        POST_ACTIONS_LOG_ATTRIBUTES_1: {
            author: 'aandreev',
            action: ActionLogAction.create,
            entity: ActionLogEntity.post,
            urlPart: 'avtovaz-lishil-lady-mediasistemy-izza-nehvatki-mikroshem',
        },
        POST_ACTIONS_LOG_ATTRIBUTES_2: {
            author: 'rbuslov',
            action: ActionLogAction.publish,
            entity: ActionLogEntity.post,
            urlPart: 'avtovaz-lishil-lady-mediasistemy-izza-nehvatki-mikroshem',
        },
        TAG_ACTIONS_LOG_ATTRIBUTES_1: {
            author: 'rbuslov',
            action: ActionLogAction.create,
            entity: ActionLogEntity.tag,
            urlPart: 'bmw',
        },
    },
};
