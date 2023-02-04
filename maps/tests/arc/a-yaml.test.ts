import jsYaml from 'js-yaml';
import {config} from '../../config';
import assert from '../../utils/assert';
import arc from '../../providers/arc';

config.runForServices('arc/a-yaml', ({slug, check}) => {
    /**
     * @description
     * Requires a real ABC service in the `a.yaml` of your repository.
     *
     * ## Rationale
     * The link to the repo is used by the DevOps automatic process to help the on-call engineer mitigate the problem.
     * Also it helps when you find a project in ABC to quickly jump to the code repository.
     *
     * ## Solution
     * Add ABC slug to the service's description:
     * 1. Go to the arc folder of your project.
     * 1. Add `service: {slug}` to `a.yaml`.
     * 1. Click "Commit".
     */
    check('ARC_A_YAML', async () => {
        const filePath = slug.replace('maps-front-', 'maps/front/services/') + '/a.yaml';

        const content = await arc.getFile(filePath).catch(() => '');
        assert(content, `There is no a.yaml in your service with path "${filePath}".`);

        const data = jsYaml.load(content) as {service: string};

        assert(data.service === slug, `Value of service in a.yaml does not equal "${slug}".`);
    });
});
