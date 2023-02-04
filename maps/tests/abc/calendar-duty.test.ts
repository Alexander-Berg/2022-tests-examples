import {config} from '../../config';
import assert from '../../utils/assert';
import {URLSearchParams} from 'url';
import abcProvider from '../../providers/abc';

config.runForServices('abc/calendar-duty', ({slug, check}) => {
    let schedules;

    before(async () => {
        const params = new URLSearchParams({
            service__slug: slug
        });
        const response = await abcProvider.getDutySchedules(params);
        schedules = response.results;
    });

    /**
     * @description
     * Requires duty calendar in ABC service.
     *
     * ## Rationale
     * Sometimes when service faces with a problem, only maintainer can fix this.
     * So, the specified duty schedule in the ABC service helps to find a necessary colleague for the issue.
     *
     * ## Solution
     * You have to follow the following instructions:
     * 1. Go to your ABC service page - `https://abc.yandex-team.ru/services/${service-slug}/duty/`, just replace service-slug with you service's slug.
     * 1. Click "Add duty scheduler" button in ABC interface and set up the schedule.
     */
    check('ABC_CALENDAR_DUTY_EXISTS', async () => {
        assert(schedules.length > 0, 'Duty calendar should exist');
    });

    /**
     * @description
     * Requires duty calendar with slug `on-call`.
     *
     * ## Rationale
     * This duty schedule slug is specified in our alerts notifications and is used for phone escalation.
     *
     * ## Solution
     * Make sure that one of your duty calendars in the ABC service has the slug named `on-call`.
     * If `qtools` tool is used, just run `qtools alerts push` command which will create a duty schedule with slug `on-call`.
     *
     * If you don't use `qtools` and your duty schedule has different slug, then remove the current schedule and create a new one with approprriate slug, named `on-call`.
     */
    check('ABC_CALENDAR_DUTY_HAS_ON_CALL', async () => {
        const scheduleSlugs = schedules.map((result) => result.slug);
        assert(
            scheduleSlugs.some((slug) => slug === 'on-call'),
            'Duty calendar with on-call slug should exist'
        );
    });
});
