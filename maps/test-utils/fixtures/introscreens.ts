import {
    IntroscreenData,
    IntroscreenEnrichedData
} from '../../@types/navi';
import {testIntentAction, testExtUrlAction} from './actions';

export const testIntroscreenOne: IntroscreenData = {
    id: '01-test-introscreen-id',
    canBeAction: true,
    name: 'test introscreen 01',
    isActive: true,
    yqlConfirmationClicks: 'test yqlConfirmationClicks',
    yqlCancelClicks: 'test yqlCancelClicks',
    yqlViews: 'test yqlViews',
    headerText: 'test introscreen header text',
    headerColor: 'fff',
    descriptionText: 'test introscreen description',
    descriptionColor: 'fff',
    bgColor: 'fff',
    pictureUrl: 'https://test.ru',
    primaryButtonText: 'test primary Button Text',
    primaryButtonDefaultColor: '000',
    primaryButtonActiveColor: '0ff',
    primaryButtonTextColor: '00f',
    primaryButtonActionId: 'primary-action-test-id',
    secondaryButtonText: 'test secondary Button Text',
    secondaryButtonDefaultColor: '000',
    secondaryButtonActiveColor: '111',
    secondaryButtonTextColor: '222',
    secondaryButtonActionId: 'secondary-action-test-id',
    cancelButtonText: 'test cancel Button Text',
    buttonsQuantity: '3'
};

export const testIntroscreenTwo: IntroscreenData = {
    id: '02-test-introscreen-id',
    canBeAction: true,
    name: 'test introscreen 02',
    isActive: true,
    yqlConfirmationClicks: 'test yqlConfirmationClicks',
    yqlCancelClicks: 'test yqlCancelClicks',
    yqlViews: 'test yqlViews',
    headerText: 'test introscreen header text',
    headerColor: 'fff',
    descriptionText: 'test introscreen description',
    descriptionColor: 'fff',
    bgColor: 'fff',
    pictureUrl: 'https://test.ru',
    primaryButtonText: 'test primary Button Text',
    primaryButtonDefaultColor: '000',
    primaryButtonActiveColor: '0ff',
    primaryButtonTextColor: '00f',
    primaryButtonActionId: 'primary-action-test-id',
    secondaryButtonText: '',
    secondaryButtonDefaultColor: '',
    secondaryButtonActiveColor: '',
    secondaryButtonTextColor: '',
    secondaryButtonActionId: '',
    cancelButtonText: 'test cancel Button Text',
    buttonsQuantity: '2'
};

export const enrichedTestIntroscreenOne: IntroscreenEnrichedData = {
    ...testIntroscreenOne,
    primaryActionData: {
        ...testIntentAction
    },
    secondaryActionData: {
        ...testExtUrlAction
    }
};

export const enrichedTestIntroscreenTwo: IntroscreenEnrichedData = {
    ...testIntroscreenTwo,
    primaryActionData: {
        ...enrichedTestIntroscreenOne
    },
    secondaryActionData: null
};

export const jsonStructureWithSimpleActions = {
    reporting_id: 'sp-introscreen_test-project_01-test-introscreen-id',
    title: 'test introscreen header text',
    description: 'test introscreen description',
    image: 'test.ru',
    close_button_text: 'test cancel Button Text',
    title_color: 'ffffffff',
    description_color: 'ffffffff',
    background_color: 'ffffffff',
    primary_button: {
        style: {
            normal_color: 'ff000000',
            pressed_color: 'ff00ffff',
            text_color: 'ff0000ff'
        },
        text: 'test primary Button Text',
        actions: [{
            type: 'intent',
            url: 'test',
            on_completed_message: ''
        }]
    },
    secondary_button: {
        style: {
            normal_color: 'ff000000',
            pressed_color: 'ff111111',
            text_color: 'ff222222'
        },
        text: 'test secondary Button Text',
        actions: [{
            type: 'external_uri',
            uri: 'test'
        }]
    }
};

export const jsonStructureWithIntroscreenAction = {
    reporting_id: 'sp-introscreen_test-project_02-test-introscreen-id',
    title: 'test introscreen header text',
    description: 'test introscreen description',
    image: 'test.ru',
    close_button_text: 'test cancel Button Text',
    title_color: 'ffffffff',
    description_color: 'ffffffff',
    background_color: 'ffffffff',
    primary_button: {
        style: {
            normal_color: 'ff000000',
            pressed_color: 'ff00ffff',
            text_color: 'ff0000ff'
        },
        text: 'test primary Button Text',
        actions: [{
            type: 'intro',
            reporting_id: 'sp-introscreen_test-project_01-test-introscreen-id',
            title: 'test introscreen header text',
            description: 'test introscreen description',
            image: 'test.ru',
            close_button_text: 'test cancel Button Text',
            title_color: 'ffffffff',
            description_color: 'ffffffff',
            background_color: 'ffffffff',
            primary_button: {
                style: {
                    normal_color: 'ff000000',
                    pressed_color: 'ff00ffff',
                    text_color: 'ff0000ff'
                },
                text: 'test primary Button Text',
                actions: [{
                    type: 'intent',
                    url: 'test',
                    on_completed_message: ''
                }]
            },
            secondary_button: {
                style: {
                    normal_color: 'ff000000',
                    pressed_color: 'ff111111',
                    text_color: 'ff222222'
                },
                text: 'test secondary Button Text',
                actions: [{
                    type: 'external_uri',
                    uri: 'test'
                }]
            }
        }]
    }
};
