import cssSelectors from '../../common/css-selectors';
import counterGenerator from '../../lib/counter-generator';
import getSelectorByText from '../../lib/func/get-selector-by-text';

const ZOOM = 19;
const CENTER = '30.272909,59.943380';
const CLIENT_ID = 'web';
const FORM_CONTEXT_ID = 'ugc_profile.entrances_assignment.edit';
const CLIENT_CONTEXT_ID = 'analytics-client-context';

const ASSIGNMENTS = [
    {
        testName: 'Задание на добавление адреса',
        taskId: 'address_add:v17wh36pb',
        type: 'addressAddAssignment',
        formId: 'toponym',
        feedbackType: 'assignment/address/add',
        closeSelector: {
            desktop: cssSelectors.backButton,
            mobile: cssSelectors.sidebar.miniCardCloseButton
        }
    },
    {
        testName: 'Задание на статус организации',
        taskId: 'os:1001716309',
        type: 'organizationEditStatusAssignment',
        formId: 'organization',
        feedbackType: 'assignment/organization/status',
        closeSelector: {
            desktop: cssSelectors.backButton,
            mobile: cssSelectors.sidebar.panelCloseButton
        }
    },
    {
        testName: 'Задание на шлагбаум',
        taskId: 'barrier:1611396979',
        type: 'barrierEditAssignment',
        formId: 'toponym',
        feedbackType: 'assignment/barrier/edit',
        closeSelector: {
            desktop: cssSelectors.backButton,
            mobile: cssSelectors.sidebar.miniCardCloseButton
        }
    },
    {
        testName: 'Задание на калитку',
        taskId: 'gate:3207319857',
        type: 'gateEditAssignment',
        formId: 'toponym',
        feedbackType: 'assignment/gate/edit',
        closeSelector: {
            desktop: cssSelectors.backButton,
            mobile: cssSelectors.sidebar.miniCardCloseButton
        }
    },
    {
        testName: 'Задание на подъезды',
        taskId: 'entrance:1611396979',
        type: 'entrancesEditAssignment',
        formId: 'toponym',
        feedbackType: 'assignment/entrance/edit',
        closeSelector: {
            desktop: cssSelectors.backButton,
            mobile: cssSelectors.sidebar.miniCardCloseButton
        }
    },
    {
        testName: 'Задание на схему СНТ',
        taskId: 'settlement:1611396979',
        type: 'settlementSchemeAssignment',
        formId: 'toponym',
        feedbackType: 'assignment/settlement/scheme/add',
        closeSelector: {
            desktop: cssSelectors.backButton,
            mobile: cssSelectors.sidebar.miniCardCloseButton
        }
    }
] as const;

describe('Личный кабинет', () => {
    describe('Аналитика', () => {
        ASSIGNMENTS.forEach((params) => {
            const COMMON_SPECS_PARAMS = {
                url: `/profile/ugc/assignments?assignment_id=${params.taskId}&ll=${CENTER}&z=${ZOOM}&client_id=${CLIENT_ID}&feedback-context=${FORM_CONTEXT_ID}&feedback-client-context=${CLIENT_CONTEXT_ID}&edit=true`,
                login: true,
                openPageOptions: {
                    userId: 'ugcProfile'
                }
            } as const;
            const COMMON_BEBR_PARAMS = {
                assignment_id: params.taskId,
                assignment_type: params.type,
                formType: params.feedbackType,
                formId: params.formId,
                clientId: CLIENT_ID,
                formContextId: FORM_CONTEXT_ID,
                clientContextId: CLIENT_CONTEXT_ID
            };
            const SUBMIT_BEBR_PARAMS = {
                ...COMMON_BEBR_PARAMS,
                type: 'submit_button'
            };
            const FORM_BEBR_PARAMS = {
                ...COMMON_BEBR_PARAMS,
                type: 'form'
            };

            counterGenerator({
                name: params.testName,
                specs: [
                    {
                        ...COMMON_SPECS_PARAMS,
                        name: 'maps_www.ugc_profile.assignment_form',
                        description: 'Форма',
                        events: [
                            {
                                type: 'show',
                                state: FORM_BEBR_PARAMS
                            },
                            {
                                type: 'hide',
                                state: FORM_BEBR_PARAMS,
                                setup: async (browser) => {
                                    const platform = browser.isPhone ? 'mobile' : 'desktop';

                                    await browser.waitAndClick(params.closeSelector[platform]);
                                }
                            }
                        ]
                    },
                    {
                        ...COMMON_SPECS_PARAMS,
                        name: new RegExp(
                            'maps_www\\.ugc_profile\\.assignment_form\\.sidebar\\.(carousel\\.delete_entrance|submit)'
                        ),
                        description: 'Кнопка отправить',
                        setup: async (browser) => {
                            switch (params.type) {
                                case 'addressAddAssignment':
                                    await browser.setValueToInput(
                                        cssSelectors.ugc.assignments.common.inputs.house,
                                        'Дом'
                                    );
                                    break;

                                case 'barrierEditAssignment':
                                case 'gateEditAssignment':
                                    await browser.waitAndClick(
                                        cssSelectors.ugc.assignments.common.buttons.firstButton.enabled
                                    );
                                    break;

                                case 'entrancesEditAssignment':
                                    if (browser.isPhone) {
                                        await browser.waitAndClick(getSelectorByText('Да, но с ошибками'));
                                    }
                                    break;

                                case 'settlementSchemeAssignment':
                                    await browser.uploadImage(cssSelectors.feedback.form.fileInput, '300x300');
                            }
                        },
                        events: [
                            {
                                type: 'show',
                                options: {
                                    multiple: true
                                },
                                state: SUBMIT_BEBR_PARAMS
                            },
                            {
                                type: 'click',
                                options: {
                                    multiple: true
                                },
                                state: SUBMIT_BEBR_PARAMS,
                                setup: async (browser) => {
                                    switch (params.type) {
                                        case 'organizationEditStatusAssignment':
                                            await browser.waitAndClick(getSelectorByText('Закрыта навсегда'));
                                            break;

                                        case 'entrancesEditAssignment':
                                            if (browser.isPhone) {
                                                await browser.waitAndClick(
                                                    cssSelectors.ugc.assignments.entrancesAssignment.mobile.buttons
                                                        .deleteEntrance
                                                );
                                            } else {
                                                await browser.waitAndClick(
                                                    cssSelectors.ugc.assignments.common.buttons.firstButton.enabled
                                                );
                                            }
                                            break;

                                        default:
                                            await browser.waitAndClick(
                                                cssSelectors.ugc.assignments.common.buttons.firstButton.enabled
                                            );
                                    }
                                }
                            },
                            {
                                type: 'hide',
                                options: {
                                    multiple: true
                                },
                                state: SUBMIT_BEBR_PARAMS,
                                setup: async (browser) => {
                                    const platform = browser.isPhone ? 'mobile' : 'desktop';

                                    await browser.waitAndClick(params.closeSelector[platform]);
                                }
                            }
                        ]
                    }
                ]
            });
        });
    });
});
