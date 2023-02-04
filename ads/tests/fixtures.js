import {APIStatusEnum, APIStatusMessage, getApiDraftStatus, WindowsEnum} from '../src/actions/utils';

// These consts are functions for immutability

export const STATUS_DRAFT_STATE = () => ({
        status: APIStatusEnum.DRAFT,
        message: APIStatusMessage.DRAFT,
        errors: [],
        visible: false,
        code: 0
});


export const STATUS_LOADING_STATE = () => ({
        status: APIStatusEnum.LOADING,
        message: APIStatusMessage.LOADING,
        errors: [],
        visible: false,
        code: 0
});


export const STATUS_FAILED_4xx_STATE = () => ({
    status: APIStatusEnum.FAILED,
    message: "Ошибка",
    errors: ["Все плохо"],
    visible: false,
    code: expect.any(Number)
});


export const STATUS_FAILED_5xx_STATE = () => ({
    status: APIStatusEnum.FAILED,
    message: "Внутренняя ошибка сервера(500)",
    errors: [],
    visible: false,
    code: expect.any(Number)
});


export const STATUS_SUCCESS_STATE = () => ({
    status: APIStatusEnum.SUCCESS,
    message: APIStatusMessage.SUCCESS,
    errors: [],
    visible: false,
    code: expect.any(Number)
});


export const ROUTER_INITIAL_STATE = () => ({
    location: null
});

export const INITIAL_STATE = () => ({
    allUpdates: ALL_UPDATES_INITIAL_STATE(),
    lastUpdates: LAST_UPDATES_INITIAL_STATE(),
    initializationStatus: STATUS_DRAFT_STATE(),
    model: MODEL_INITIAL_STATE(),
    window: WindowsEnum.LOADING,
    router: ROUTER_INITIAL_STATE()
});


export const ALL_UPDATES_INITIAL_STATE = () => ({
    date: null,
    models: null,
    status: STATUS_DRAFT_STATE()
});


export const LAST_UPDATES_INITIAL_STATE = () => ({
    models: null,
    status: STATUS_DRAFT_STATE()
});


export const MODEL_INITIAL_STATE = () => ({
    allUpdates: {
        date: null,
        models: null,
        status: STATUS_DRAFT_STATE()
    },
    activeUpdate: {
        id: null,
        model: null,
        status: STATUS_DRAFT_STATE()
    },
    meta: {
        data: {
            name: null,
            launchers: null
        },
        status: STATUS_DRAFT_STATE()
    }
});


export const API_ALL_UPDATES = () => ({
    events: [
        {
            id: 1,
            launcher: {
                released: "2017-11-13T15:07:34"
            },
            lm: {
                shards_num: 18,
                id: 160,
                location: "stat"
            },
            dump: {
                name: "task1",
                last_log_date: "2017-11-10T00:00:00",
                type: "vw"
            }
        },
        {
            id: 2,
            launcher: {
                released: "2017-11-12T15:07:34"
            },
            lm: {
                shards_num: 1,
                id: 161,
                location: "meta"
            },
            dump: {
                name: "task2",
                last_log_date: "2017-11-11T00:00:00",
                type: "vw"
            }
        }
    ]

});

export const STATE_ALL_UPDATES_MODELS = () => {
    return API_ALL_UPDATES()['events'];
};


export const API_4xx_ERROR = () => ({
    obj: {
        message: "Ошибка",
        errors: ["Все плохо"]
    },
    status: 400
});


export const API_403_ERROR = () => ({
    obj: {
        message: "Ошибка",
        errors: ["Все плохо"]
    },
    status: 403
});


export const API_5xx_ERROR = () => ({
    obj: {
        message: "Ошибка",
        errors: ["Все плохо"]
    },
    status: 500
});


export default class APIMock {
    constructor(api_type) {
        this.getAllUpdates = jest.fn();
        this.getLastUpdates = jest.fn();
        this.getModelUpdates = jest.fn();
    }

    setResolveGetAllUpdates(lms) {
        this.getAllUpdates.mockReturnValue(
            new Promise((resolve, reject) => resolve(lms))
        );
    }

    setRejectGetAllUpdates(error) {
        this.getAllUpdates.mockReturnValue(
            new Promise((resolve, reject) => reject(error))
        );
    }
}

