/**
 * @jest-environment node
 */
import { mockStore } from 'core/mocks/store.mock';
import { ROUTER_SERVICE_MOCK_1 } from 'core/services/router/mocks/routerService.mock';
import { ServiceId } from 'core/services/ServiceId';
import { POST_LISTING_SERVICE_MOCK_1, POST_LISTING_SERVICE_MOCK_4 } from 'core/services/postListing/mocks/postListingService.mock';

import { usePreparedNavigationTags } from './useNavigationTags';

it('подготавливает теги и возвращает каждый тег как заголовок и href', () => {
    mockStore({
        [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1,
        [ServiceId.POST_LISTING]: POST_LISTING_SERVICE_MOCK_4,
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
        [ServiceId.POST_LISTING]: POST_LISTING_SERVICE_MOCK_1,
    });

    const { tags } = usePreparedNavigationTags();

    expect(tags).toEqual([]);
});
