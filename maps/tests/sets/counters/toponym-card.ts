import cssSelectors from '../../common/css-selectors';
import counterGenerator from '../../lib/counter-generator';

counterGenerator({
    name: 'Карточка топонима.',
    specs: [
        {
            name: 'maps_www.whatshere_panel.preview_card.providers_snippet.providers.item',
            description: 'Рекламный интернет-провайдер',
            url: '/213/moscow/house/ulitsa_ramenki_12/Z04Ycg5hSUIFQFtvfXp4cHlmYw==/',
            selector: cssSelectors.search.toponymCard.providers.bigItem,
            events: [
                {
                    type: 'click',
                    state: {
                        advertId: '3056307',
                        title: 'Домашний интернет билайн'
                    }
                }
            ]
        }
    ]
});
