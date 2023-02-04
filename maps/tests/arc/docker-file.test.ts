import {config} from '../../config';
import assert from '../../utils/assert';
import {getRepoPath} from '../../providers/abc';
import arcProvider from '../../providers/arc';

const BASE_IMAGE_VERSION_REGEXP = /^FROM registry\.yandex\.net\/.*:(\d*.\d*.\d*)/m;
const MONITORINGS_REGEXP = /^RUN maps_init/m;
const WORKDIR_REGEXP = /^WORKDIR \/usr\/local\/app/m;
const MIN_IMAGE_VERSION = '10.0.0';

config.runForServices('arc/docker-file', ({slug, check}) => {
    let dockerFile: string;

    before(async () => {
        const arcRepoPath = await getRepoPath(slug);
        dockerFile = arcRepoPath ? await arcProvider.getFile(arcRepoPath + '/Dockerfile') : undefined;
    });

    /**
     * @description
     * Requires a Dockerfile to be present in the service's repository.
     *
     * ## Rationale
     * To make our services infrastructure consistent, docker was chosen as a way to transport the code to the servers.
     * That's why a Dockerfile is required for all of our services.
     *
     * ## Solution
     * Add `Dockerfile` into project repository's root folder.
     */
    check('DOCKERFILE_EXISTS', async () => {
        assert(dockerFile !== undefined, 'Dockerfile should exist');
    });

    /**
     * @description
     * Requires the version of the baseimage used in the project to be at least equal to the enforced version (see below).
     *
     * ## Rationale
     * The base docker image is a foundation for the service and contains not only basic infrastructure for monitorings, logging, etc. It also contains a bunch of useful tools and implements our best-practices, which should be consistent across all our services.
     *
     * ## Solution
     * Update your base image in Dockerfile. You can find enforced versions [here](https://a.yandex-team.ru/arc_vcs/maps/front/tools/docker-baseimages/versions.json).
     */
    check('DOCKERFILE_BASEIMAGE_VERSION', async () => {
        const version = dockerFile.match(BASE_IMAGE_VERSION_REGEXP)?.[1];

        assert(
            isVersionHigher(version),
            `Base image version shouldn't be less then ${MIN_IMAGE_VERSION} (got ${version})`
        );
    });

    /**
     * @description
     * Requires `maps_init` to be invoked in the Dockerfile.
     *
     * ## Rationale
     * The special `maps_init` comand is used to bootstrap the infrastructure configs for your project on build-time. For example it prepares configs for monitorings, graphics and alerts.
     *
     * ## Solution
     * Add command `RUN maps_init` in your Dockerfile.
     */
    check('DOCKERFILE_RUNS_INIT', async () => {
        assert(
            MONITORINGS_REGEXP.test(dockerFile),
            'Dockerfile should contain "RUN maps_init" to enable monitorings'
        );
    });

    /**
     * @description
     * Requires `/usr/local/app` to be used as `WORKDIR`.
     *
     * ## Rationale
     * The DevOps team should have an uniform way to find the built application in a docker container. For that reason it is required to place your built application in `/usr/local/app`.
     *
     * ## Solution
     * Make sure that you use the `WORKDIR /usr/local/app` directive in your Dockerfile and that your built application is placed in `/usr/local/app`.
     */
    check('DOCKERFILE_WORKDIR', async () => {
        assert(
            WORKDIR_REGEXP.test(dockerFile),
            'Dockerfile should contain WORKDIR \/usr\/local\/app'
        );
    });
});

function isVersionHigher(version: string): boolean {
    const [major, minor, patch] = version.split('.').map(Number);
    const [minMajor, minMinor, minPatch] = MIN_IMAGE_VERSION.split('.').map(Number);

    if (isNaN(major) || isNaN(minor) || isNaN(patch)) {
        return false;
    }

    if (major !== minMajor) {
        return major > minMajor;
    }

    if (minor !== minMinor) {
        return minor > minMinor;
    }

    return patch >= minPatch;
}
