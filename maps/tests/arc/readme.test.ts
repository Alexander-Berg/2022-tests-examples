import {config} from '../../config';
import assert from '../../utils/assert';
import {getRepoPath} from '../../providers/abc';
import arcProvider from '../../providers/arc';

config.runForServices('arc/readme', ({slug, check}) => {
    let arcRepoPath;
    before(async () => {
        arcRepoPath = await getRepoPath(slug);
    });

    /**
     * @description
     * Requires `README.md` file.
     *
     * ## Rationale
     * The `README.md` file in the root of the project, filled out using our template helps a lot during incidents.
     *
     * ## Solution
     * Create a `README.md` file in the root of your service and fill it in using [maps-front-infra's template](https://a.yandex-team.ru/arc_vcs/maps/front/docs/templates/service-deploy.md).
     */
    check('README_EXISTS', async () => {
        const file = await arcProvider.getFile(arcRepoPath + '/README.md');
        assert(file !== null, 'The project should include a README.md file');
    });

    /**
     * @description
     * Requires `CONTRIBUTING.md` file.
     *
     * ## Rationale
     * The `CONTRIBUTING.md` file in the root of the project is used when trying to run the service for the first time.
     *
     * ## Solution
     * Create a `CONTRIBUTING.md` file in the root of your service and fill in the following details:
     * 1. Dependencies needed for the project to be ran in dev mode and for running tests
     * 1. How to start the service in dev mode
     * 1. How to run service's tests
     * 1. How release a new version of the service in testing/production
     * 1. Everything else that can be helpful when getting to know the dev/release infrastructure of the service
     */
    check('CONTRIBUTING_EXISTS', async () => {
        const file = await arcProvider.getFile(arcRepoPath + '/CONTRIBUTING.md');
        assert(file !== null, 'The project should include a CONTRIBUTING.md file');
    });
});
