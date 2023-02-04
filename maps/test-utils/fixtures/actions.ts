import {
    ActionData,
    IntroscreenEnrichedData,
    ActionSimpleParams,
    ActionWebviewParams
} from '../../@types/navi';
import {ActionType} from '../../common/navi/constants';

export const testBlancIntroscreenEnrichedData: IntroscreenEnrichedData = {
    id: 'test',
    canBeAction: false,
    name: 'test',
    isActive: true,
    yqlCancelClicks: '',
    yqlConfirmationClicks: '',
    yqlViews: '',
    headerText: '',
    headerColor: '',
    descriptionColor: '',
    descriptionText: '',
    pictureUrl: '',
    bgColor: '',
    primaryButtonActiveColor: '',
    primaryButtonDefaultColor: '',
    primaryButtonText: '',
    primaryButtonTextColor: '',
    primaryButtonActionId: '',
    secondaryButtonActionId: '',
    secondaryButtonActiveColor: '',
    secondaryButtonDefaultColor: '',
    secondaryButtonText: '',
    secondaryButtonTextColor: '',
    cancelButtonText: '',
    buttonsQuantity: '',
    primaryActionData: null,
    secondaryActionData: null
};
export const testIntentAction: ActionData = {
    id: 'test-intent-action-id',
    type: ActionType.INTENT,
    params: {
        target: 'test'
    } as ActionSimpleParams
};
export const testExtUrlAction: ActionData = {
    id: 'test-url-action-id',
    type: ActionType.LINK,
    params: {
        target: 'test'
    } as ActionSimpleParams
};
export const testSoundAction: ActionData = {
    id: 'test-sound-action-id',
    type: ActionType.SOUND,
    params: {
        target: 'test'
    } as ActionSimpleParams
};
export const testWebViewAction: ActionData = {
    id: 'test-webview-action-id',
    type: ActionType.WEBVIEW,
    params: {
        target: 'test',
        navbarTitle: 'test',
        authType: 'uuid'
    } as ActionWebviewParams
};

export const testUnknownAction = {
    id: 'test-other-type-id',
    type: 'SOME OTHER TYPE',
    someParam: {}
};
