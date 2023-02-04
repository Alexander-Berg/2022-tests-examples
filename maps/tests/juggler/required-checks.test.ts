import JugglerProvider from '@yandex-int/maps._juggler';
import {config, DEPLOY_UNITS_BY_SLUG} from '../../config';
import assert from '../../utils/assert';

const jugglerProvider = new JugglerProvider({
    token: config.tokens.JUGGLER_OAUTH_TOKEN,
    logger: {
        info: () => {},
        error: (msg) => console.error(msg)
    }
});

function getHost(slug: string, deployUnit: string): string {
    return `${slug}_production.${deployUnit}`;
}

config.runForServices('juggler/required-checks', ({slug, check}) => {
    const DEPLOY_UNITS = DEPLOY_UNITS_BY_SLUG[slug] || DEPLOY_UNITS_BY_SLUG.default;

    let checks: Partial<Record<string, Record<string, null>>> = {};

    before(async () => {
        const results = await Promise.all(DEPLOY_UNITS.map(async (deployUnit) => {
            return jugglerProvider.getListChecks(getHost(slug, deployUnit)).catch(() => {});
        }));
        checks = results.reduce((result, items) => ({
            ...result,
            ...items
        }), {});
    });

    /**
     * @description
     * Requires `http-ping` juggler check.
     *
     * ## Rationale
     * The `/ping` endpoint of each service is used for determining the service's liveliness.
     * If the `/ping` endpoint doesn't return 200 status code, the service can be considered dead.
     * This endpoint is used by balancers (to turn off traffic payload to dead instances) and deploy (`readiness` status means that instance is ready for payload).
     * Also this check is used as a flag that active alerts are pushed and work properly.
     *
     * ## Solution
     * 1. Go to the your repository in terminal.
     * 1. Check you have latest version of `@yandex-int/qtools` package.
     * 1. Run the following command: `npx qtools alerts push`
     * 1. If there are changes to be applied to the check configuration, the command will display the diff and will ask you to run it again with the --force flag to confirm. If asked - confirm your changes.
     */
    check('JUGGLER_CHECK_HTTP_PING_EXISTS', async () => {
        DEPLOY_UNITS.forEach((deployUnit) => {
            const host = getHost(slug, deployUnit);
            assert(
                checks[host]?.hasOwnProperty('http-ping'),
                `Juggler check "http-ping" should be exist in host="${host}"`
            );
        });
    });

    /**
     * @description
     * Requires "cpu_usage" check.
     *
     * ## Rationale
     * CPU usages is a vital indicator for the service health. So, this check is obligatory for all services.
     *
     * Also this check is used as a flag that passive alerts are pushed and work properly.
     *
     * ## Solution
     * 1. Go to the your repository in terminal.
     * 1. Check you have latest version of `@yandex-int/qtools` package.
     * 1. Run the following command: `npx qtools alerts push`
     * 1. If there are changes to be applied to the check configuration, the command will display the diff and will ask you to run it again with the --force flag to confirm. If asked - confirm your changes.
     */
    check('JUGGLER_CHECK_CPU_USAGE_EXISTS', async () => {
        DEPLOY_UNITS.forEach((deployUnit) => {
            const host = getHost(slug, deployUnit);
            assert(
                checks[host]?.hasOwnProperty('cpu_usage'),
                `Juggler check "cpu_usage" should be exist in host="${host}"`
            );
        });
    });
});
