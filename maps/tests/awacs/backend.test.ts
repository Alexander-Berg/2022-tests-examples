import {awacs} from '@yandex-int/awacs-api';
import {config} from '../../config';
import assert from '../../utils/assert';
import {getAwacsListBackends} from '../../providers/awacs';

config.runForServices('awacs/backends', ({slug, check}) => {
    const backendsByIds: Record<string, awacs.model.IBackend> = {};

    before(async () => {
        await getAwacsListBackends(slug).then(({backends}) => {
            backends.forEach((backend) => {
                backendsByIds[backend.meta.id] = backend;
            });
        });
    });

    /**
     * @description
     * Awacs backends should have SD endpoint sets type.
     *
     * ## Solution
     * 1) Go to your awacs namespace.
     * 2) Go to backends list and choose target backend.
     * 3) Tap "Edit" button.
     * 4) In tab "Spec" choose "YP endpoint sets SD (fast)" type for target DC.
     * 5) Save changes with comment.
     */
    check('AWACS_BACKENDS_TYPE_CHECK', async () => {
        const backendsNoSDType = Object.values(backendsByIds).filter(
            (backend) => backend.spec.selector.type.toString() !== 'YP_ENDPOINT_SETS_SD'
        );

        assert(
            backendsNoSDType.length === 0,
            `Backends should have SD endpoint sets type: ${backendsNoSDType.map((backend) => backend.meta.id).join()}`
        );
    });

    /**
     * @description
     * Awacs backends should be in each DC.
     *
     * ## Solution
     * 1) Go to your awacs namespace.
     * 2) Go to backends list.
     * 3) Tap "Create Backend" button or "Copy Backends" button and create backends for missing DC.
     *
     * {% note info %}
     *
     * Target DC you can select in tab "Spec" in block "YP endpoint sets".
     *
     * {% endnote %}
     */
    check('AWACS_BACKENDS_IN_EACH_DC', async () => {
        const backendsIds = Object.keys(backendsByIds);

        assert(
            backendsIds.length >= 3,
            'Backends should be in each DC. ' + (
                backendsIds.length === 0 ?
                    'Backends were not found.' :
                    `Was found only: ${backendsIds.join()}`
            )
        );
    });
});
