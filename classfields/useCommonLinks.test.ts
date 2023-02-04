import { mockStore } from 'core/mocks/store.mock';
import { CONFIG_SERVICE_MOCK_1, CONFIG_SERVICE_MOCK_2 } from 'core/services/config/mocks/configService.mock';
import { ServiceId } from 'core/services/ServiceId';

import { useCommonLinks } from './useCommonLinks';

const BASE_COMMON_LINKS = [
    { href: 'https://yandex.ru/adv/products/display/realty', text: 'Реклама' },
    { href: 'https://yandex.ru/promo/autoru/careers/all?utm_source=ya-realty&utm_campaign=footer', text: 'Стань частью команды' },
    { href: 'https://yandex.ru/legal/realty_termsofuse/', text: 'Пользовательское соглашение' },
    { href: 'https://yandex.ru/support/realty/', text: 'Помощь' } ];

it('возвращает список ссылок с isWebView=false', () => {
    mockStore({
        [ServiceId.CONFIG]: CONFIG_SERVICE_MOCK_1,
    });

    const { commonLinks } = useCommonLinks();

    expect(commonLinks).toEqual([
        { href: '/', text: 'Вся недвижимость' },
        ...BASE_COMMON_LINKS,
    ]);
});

it('возвращает список ссылок с isWebView=true', () => {
    mockStore({
        [ServiceId.CONFIG]: CONFIG_SERVICE_MOCK_2,
    });

    const { commonLinks } = useCommonLinks();

    expect(commonLinks).toEqual(BASE_COMMON_LINKS);
});
