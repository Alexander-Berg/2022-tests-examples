/**
 * @jest-environment node
 */
/* eslint-disable max-len */
import { mockStore } from 'core/mocks/store.mock';
import { ROUTER_SERVICE_MOCK_1 } from 'core/services/router/mocks/routerService.mock';
import { ServiceId } from 'core/services/ServiceId';

import { useHomeSeo } from './useHomeSeo';

it('title, description и canonicalUrl', () => {
    mockStore({ [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1 });

    const result = useHomeSeo();

    expect(result?.title).toBe('Исследования на рынке недвижимости, интервью экспертов и статьи про квартиры, новостройки и дома в Журнале Недвижимости');
    expect(result?.description).toBe('Лучшие экспертные статьи про покупку и продажу квартир и домов, основные тенденции рынка недвижимости, документы для сделок и другое в Журнале Яндекс.Недвижимости');
    expect(result?.canonicalUrl).toBe('https://realty.yandex.ru/journal/');
});
