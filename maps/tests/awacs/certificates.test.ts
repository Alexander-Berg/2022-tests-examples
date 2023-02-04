import {config, externalSlb} from '../../config';
import awacsProvider from '../../providers/awacs';
import assert from '../../utils/assert';
import {getNamespaceId} from '../../utils/awacs';

config.runForServices('awacs/certificates', ({slug, check}) => {
    /**
     * @description
     * Awacs namespace must have "Yandex CA" certificate.
     *
     * ## Reason
     * It is needed to be able to use your balancer via secured HTTPS connection.
     *
     * {% note info %}
     *
     * See more documentation on https://wiki.yandex-team.ru/awacs/certs/
     *
     * {% endnote %}
     *
     * ## Solution
     * 1) Go to the your awacs namespace.
     * 2) Go to the certificates list through "Show Certificates" button in 'Certificates' section.
     * 3) Add new certificate by "Order Certificate" button.
     */
    check('AWACS_EXTERNAL_CERTIFICATES', async () => {
        const namespaceId = getNamespaceId(slug);
        const certificates = (await awacsProvider.listCertificates({namespaceId})).certificates;
        const internalCertificatesIds = certificates
            .filter((certificate) => {
                const activeStatusesPerUpstreams = Object.values(certificate.statuses[0].active);
                return activeStatusesPerUpstreams.length > 0 &&
                    activeStatusesPerUpstreams.every(({status}) => status === 'True') &&
                    certificate.spec.fields.issuerCommonName !== 'Yandex CA' &&
                    !certificate.spec.fields.issuerCommonName.startsWith('GlobalSign RSA');
            })
            .map((certificate) => certificate.meta.id);
        assert(
            !externalSlb.includes(namespaceId) || internalCertificatesIds.length === 0,
            `Some of certificates for ${namespaceId} has issuerCommonName different from 'Yandex CA'`
        );
    });
});
