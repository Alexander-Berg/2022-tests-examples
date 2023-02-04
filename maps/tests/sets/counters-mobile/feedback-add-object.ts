import cssSelectors from '../../common/css-selectors';
import counterGenerator from '../../lib/counter-generator';

const CLIENT_ID = 'web';
const CONTEXT = 'map.context';
const ADD_OBJECT_MENU_TYPE = 'object/add';
const CLIENT_CONTEXT_ID = 'analytics-client-context';

counterGenerator({
    name: 'Карточка добавления фидбека.',
    specs: [
        {
            name: 'maps_www.feedback.form',
            description: 'Меню.',
            url: `/?feedback=${ADD_OBJECT_MENU_TYPE}&feedback-metadata[client_id]=${CLIENT_ID}&feedback-context=${CONTEXT}&feedback-client-context=${CLIENT_CONTEXT_ID}`,
            selector: cssSelectors.addObjectFeedback.view,
            events: [
                {
                    type: 'hide',
                    state: {
                        type: 'form',
                        formId: 'toponym',
                        formType: ADD_OBJECT_MENU_TYPE,
                        formContextId: CONTEXT,
                        clientContextId: CLIENT_CONTEXT_ID,
                        clientId: CLIENT_ID
                    },
                    setup: async (browser) => {
                        await browser.waitAndClick(cssSelectors.feedback.header.close);
                    }
                }
            ]
        }
    ]
});
