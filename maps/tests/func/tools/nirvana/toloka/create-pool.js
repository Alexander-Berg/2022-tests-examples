const got = require('got');

function createPool(projectId, version) {
    return got.post('https://sandbox.toloka.yandex.ru/api/v1/pools/', {
        headers: {
            Authorization: `OAuth ${process.env.TOLOKA_TOKEN}`
        },
        json: true,
        body: {
            project_id: projectId,
            private_name: `Пул для тестирования версии ${version}`,
            may_contain_adult_content: false,
            will_expire: new Date(Date.now() + 1000 * 60 * 60 * 24 * 14).toISOString(),
            reward_per_assignment: 0.01,
            assignment_max_duration_seconds: 60 * 60 * 4,
            auto_accept_solutions: true,
            auto_accept_period_day: 1,
            auto_close_after_complete_delay_seconds: 60,
            assignments_issuing_config: {
                issue_task_suites_in_creation_order: false
            },
            defaults: {
                default_overlap_for_new_task_suites: 999,
                default_overlap_for_new_tasks: 999
            },
            priority: 10
        }
    }).then(({body}) => body);
}

module.exports = createPool;
