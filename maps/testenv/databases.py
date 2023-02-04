from maps.pylibs.fixtures.sandbox.tasks import (
    BuildDockerAndRelease, GardenUploadModuleBinary
)
from maps.pylibs.fixtures.testenv.fixture import Database, TestenvJob, JobSentinels


BUILD_DATABASES = dict(
    maps_geoinfra_tests=Database(
        jobs=dict(
            BUILD_DOCKER_AND_RELEASE_MAPS_CORE_TEAPOT=TestenvJob(
                task=BuildDockerAndRelease,
                params=dict(
                    revision=JobSentinels.revision,
                    package_file='maps/infra/teapot/docker/pkg.json',
                    nanny_release_to=['unstable'],
                )
            ),
            BUILD_DOCKER_AND_RELEASE_MAPS_CORE_TEACUP=TestenvJob(
                task=BuildDockerAndRelease,
                params=dict(
                    revision=JobSentinels.revision,
                    package_file='maps/infra/teacup/docker/pkg.json',
                    nanny_release_to=['testing'],
                )
            ),
        ),
        is_started=True,
    ),
    maps_garden_modules=Database(
        jobs=dict(
            BUILD_GARDEN_MODULE_BINARY_BACKA_EXPORT=TestenvJob(
                task=GardenUploadModuleBinary,
                params=dict(
                    revision=JobSentinels.revision,
                    target='maps/garden/modules/backa_export/bin',
                    module_name='backa_export',
                )
            ),
        ),
        is_started=True,
    )
)
