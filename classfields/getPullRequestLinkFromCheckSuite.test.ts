import type { EventPayloads } from '@octokit/webhooks';
import { getPullRequestLinkFromCheckSuite } from './getPullRequestLinkFromCheckSuite';

it('должен собирать ссылку на пулл-реквест', () => {
    const result = getPullRequestLinkFromCheckSuite({
        repository: {
            html_url: 'https://github.com/YandexClassifieds/test-vertis-frontend-gh-app',
        },
        check_suite: {
            pull_requests: [
                { number: 2 },
            ],
        },
    } as EventPayloads.WebhookPayloadCheckSuite);

    expect(result).toBe('https://github.com/YandexClassifieds/test-vertis-frontend-gh-app/pull/2');
});
