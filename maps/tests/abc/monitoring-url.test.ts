import abcProvider from '../../providers/abc';
import assert from '../../utils/assert';
import {config} from '../../config';
import {fixMonitoringLinkExists} from './monitoring-url.fix';

const JUGGLER_TYPE_CODE = 'url_monitoring';
const PANEL_TYPE_CODE = 'url_monitoring_panels';

config.runForServices('abc/monitoring-url', ({slug, check}) => {
    /**
     * @description
     * Requires a link to the juggler to be present in the ABC service page.
     *
     * ## Rationale
     * Used by the automatic processes for providing information during an incident
     * Also it helps when you find a project in ABC to quickly jump to the project's alerts.
     *
     * ## Solution
     * You have to follow the following instructions:
     * 1. Go to your ABC service page - `https://abc.yandex-team.ru/services/${service-slug}/`, just replace service-slug with you service's slug.
     * 1. Hover your pointer over the "Contacts" section in the right side panel. An pencil icon should appear. Click on it.
     * 1. In the pop-up window choose "Monitorings. Main service panels/alerts" for the type. Give it a name like "Juggler", and enter the URL.
     */
    check('ABC_JUGGLER_LINK_EXISTS', async () => {
        const contacts = await abcProvider.getContacts({service__slug: slug, fields: 'type.code,content,title'});
        const alertContact = contacts.results.find((contact) => contact.type.code === JUGGLER_TYPE_CODE);
        const jugglerUrl = alertContact?.content;
        const expectedUrl = `https://juggler.yandex-team.ru/aggregate_checks/?query=host%3D${slug}_production.app`;

        if (config.autofix && jugglerUrl !== expectedUrl) {
            await fixMonitoringLinkExists({
                slug,
                contacts: contacts.results,
                updatedContact: {
                    title: 'Juggler dashboard',
                    content: expectedUrl,
                    contact_type: JUGGLER_TYPE_CODE
                }
            });

            return;
        }

        assert(
            jugglerUrl === expectedUrl,
            `Juggler url should be specified in ABC service. Expected: ${expectedUrl}, actual: ${jugglerUrl}`
        );
    });

    /**
     * @description
     * Requires a link to the panel to be present in the ABC service page.
     *
     * ## Rationale
     * Used by the automatic processes for providing information during an incident
     * Also it helps when you find a project in ABC to quickly jump to the project's infrastructure dashboard.
     *
     * ## Solution
     * You have to follow the following instructions:
     * 1. Go to your ABC service page - `https://abc.yandex-team.ru/services/${service-slug}/`, just replace service-slug with you service's slug.
     * 1. Hover your pointer over the "Contacts" section in the right side panel. An pencil icon should appear. Click on it.
     * 1. In the pop-up window choose "Monitorings. Panels" for the type. Give it a name like "Main dashboard", and enter the URL of the dashboard.
     */
    check('ABC_PANEL_LINK_EXISTS', async () => {
        const contacts = await abcProvider.getContacts({service__slug: slug, fields: 'type.code,content,title'});
        const panelContact = contacts.results.find((contact) => contact.type.code === PANEL_TYPE_CODE);
        const yasmUrl = panelContact?.content;
        const expectedUrl = `https://yasm.yandex-team.ru/template/panel/maps-front-common/full_env_name=${slug}_production`;
        const expectedUrlPattern = new RegExp(`^${expectedUrl}(;mode=full)?$`);

        if (config.autofix && !yasmUrl?.match(expectedUrlPattern)) {
            await fixMonitoringLinkExists({
                slug,
                contacts: contacts.results,
                updatedContact: {
                    title: 'Yasm panel',
                    content: expectedUrl,
                    contact_type: PANEL_TYPE_CODE
                }
            });

            return;
        }

        assert(
            yasmUrl?.match(expectedUrlPattern),
            `Panel url should be specified in ABC service. Expected: ${expectedUrl}, actual: ${yasmUrl}`
        );
    });
});
