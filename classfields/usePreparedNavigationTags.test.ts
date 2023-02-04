/**
 * @jest-environment node
 */
import { mockStore } from 'core/mocks/store.mock';
import { ROUTER_SERVICE_MOCK_1 } from 'core/services/router/mocks/routerService.mock';
import { HOME_SERVICE_MOCK_1, HOME_SERVICE_MOCK_2 } from 'core/services/home/mocks/homeService.mock';
import { ServiceId } from 'core/services/ServiceId';

import { usePreparedNavigationTags } from './usePreparedNavigationTags';

it('подготавливает теги и возвращает каждый тег как заголовок и href', () => {
    mockStore({
        [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1,
        [ServiceId.HOME]: HOME_SERVICE_MOCK_2,
    });

    const { tags } = usePreparedNavigationTags();

    expect(tags).toEqual([
        {
            href: '/journal/tag/research/',
            title: 'Исследования',
        },
        {
            href: '/journal/tag/kak-sdat/',
            title: 'Как сдать',
        },
        {
            href: '/journal/tag/kak-snyat/',
            title: 'Как снять',
        },
    ]);
});

it('возвращает пустой массив тегов, если они отсутствуют в сторе', () => {
    mockStore({
        [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1,
        [ServiceId.HOME]: HOME_SERVICE_MOCK_1,
    });

    const { tags } = usePreparedNavigationTags();

    expect(tags).toEqual([]);
});
