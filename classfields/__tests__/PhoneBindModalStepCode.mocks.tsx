import noop from 'lodash/noop';

import { PhoneBindError } from 'view/models';

import { IPhoneBindModalStepCodeProps } from '../PhoneBindModalStepCode';

const successfulPromiseCreator = () => new Promise((resolve) => resolve()) as Promise<void>;
const failedPromiseCreatorCreator = (err?: unknown) => () =>
    new Promise((resolve, reject) => setTimeout(reject, undefined, err)) as Promise<void>;
const infinitePromiseCreator = () => new Promise(() => undefined) as Promise<void>;

export const defaultState: IPhoneBindModalStepCodeProps = {
    phoneBindResult: {
        trackId: '1',
        codeLength: 5,
    },
    phone: '+7 (999) 123-4567',
    onCodeResend: successfulPromiseCreator,
    onCodeSubmit: successfulPromiseCreator,
    onPhoneReset: noop,
    disableResendTimeout: true,
} as const;

export const defaultWithTimer = {
    ...defaultState,
    disableResendTimeout: false,
};

export const loadingSubmit = {
    ...defaultState,
    onCodeSubmit: infinitePromiseCreator,
};

export const loadingResend = {
    ...defaultState,
    onCodeResend: infinitePromiseCreator,
};

export const errorSubmitUnknown = {
    ...defaultState,
    onCodeSubmit: failedPromiseCreatorCreator(PhoneBindError.UNKNOWN_ERROR),
};

export const errorSubmitWrongCode = {
    ...defaultState,
    onCodeSubmit: failedPromiseCreatorCreator(PhoneBindError.PHONE_BAD_CONFIRMATION_CODE),
};

export const errorResendUnknown = {
    ...defaultState,
    onCodeResend: failedPromiseCreatorCreator(PhoneBindError.UNKNOWN_ERROR),
};

export const errorResendPhoneFormat = {
    ...defaultState,
    onCodeResend: failedPromiseCreatorCreator(PhoneBindError.PHONE_BAD_NUM_FORMAT),
};

export const errorResendPhoneBlocked = {
    ...defaultState,
    onCodeResend: failedPromiseCreatorCreator(PhoneBindError.PHONE_BLOCKED),
};

export const errorResendPhoneBound = {
    ...defaultState,
    onCodeResend: failedPromiseCreatorCreator(PhoneBindError.PHONE_ALREADY_BOUND),
};
