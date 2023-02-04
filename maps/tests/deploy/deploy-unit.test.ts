import {config} from '../../config';
import assert from '../../utils/assert';
import deploy, {NYP} from '../../providers/deploy';
import {Mb, Gb} from '../../utils/units';

config.runForServices('deploy/deploy-unit', ({slug, check}) => {
    let deployUnits: Record<string, NYP.NClient.NApi.NProto.ITDeployUnitSpec>;

    before(async () => {
        deployUnits = await deploy.getDeployUnits(slug).catch(() => ({}));
        assert(Object.keys(deployUnits).length > 0, 'There is no deploy units');
    });

    /**
     * @description
     * Requires prefix `app` for every deploy unit.
     *
     * ## Rationale
     * This rule is required for unification reasons to make all deploy unit names consistent.
     *
     * ## Solution
     * 1. Go to your deploy configuration in repository.
     * 1. Change the deploy unit names so that it can match the pattern `app(-\d)?`:
     * ```yml
     * spec:
     *     deploy_units:
     *         app:
     *             ...
     * ```
     */
    check('DEPLOY_UNIT_NAME', async () => {
        const regexp = /^app(-\d+)?$/;
        Object.keys(deployUnits).forEach((name) => {
            assert(
                regexp.test(name),
                `Name of deploy unit "${name}" should match regexp "${regexp.toString()}"`
            );
        });
    });

    /**
     * @description
     * Require postfix `_box` for every box.
     *
     * ## Rationale
     * This rule is required for unification reasons to make all box names consistent.
     *
     * ## Solution
     * 1. Go to your deploy configuration in repository.
     * 1. Change the box names so that it can match the pattern `.*_box`:
     * - for multi cluster replica set
     * ```yml
     * spec:
     *     deploy_units:
     *         app:
     *             multi_cluster_replica_set:
     *                 replica_set:
     *                     pod_template_spec:
     *                         spec:
     *                             pod_agent_payload:
     *                                 spec:
     *                                     boxes:
     *                                         - id: app_box
     *                                         ...
     *                                      workloads:
     *                                          - id: app_workload
     *                                            box_ref: app_box
     *                                          ...
     * ```
     * - for replica set
     * ```yml
     * spec:
     *     deploy_units:
     *         app:
     *             replica_set:
     *                 replica_set_template:
     *                     pod_template_spec:
     *                         spec:
     *                             boxes:
     *                                 - id: app_box
     *                                 ...
     *                              workloads:
     *                                  - id: app_workload
     *                                    box_ref: app_box
     *                                  ...
     * ```
     */
    check('DEPLOY_BOX_NAME', async () => {
        Object.entries(deployUnits).forEach(([name, deployUnit]) => {
            const podSpec = deployUnit.multi_cluster_replica_set?.replica_set?.pod_template_spec ||
                deployUnit.replica_set.replica_set_template.pod_template_spec;
            const box = podSpec.spec.pod_agent_payload?.spec?.boxes?.[0];
            const expected = `${name}_box`;
            assert(
                box?.id === expected,
                `Name of main box in DU "${name}" should be equal "${expected}" (current value "${box?.id}")`
            );
        });
    });

    /**
     * @description
     * Requires postfix `_workload` for every workload.
     *
     * ## Rationale
     * This rule is required for unification reasons to make all workload names consistent
     *
     * ## Solution
     * 1. Go to your deploy configuration in repository.
     * 1. Change the workload names so that it can match the pattern `.*_workload`:
     * - for multi cluster replica set
     * ```yml
     * spec:
     *     deploy_units:
     *         app:
     *             multi_cluster_replica_set:
     *                 replica_set:
     *                     pod_template_spec:
     *                         spec:
     *                             pod_agent_payload:
     *                                 spec:
     *                                      workloads:
     *                                          - id: app_workload
     *                                            box_ref: app_box
     *                                          ...
     * ```
     * - for replica set
     * ```yml
     * spec:
     *     deploy_units:
     *         app:
     *             replica_set:
     *                 replica_set_template:
     *                     pod_template_spec:
     *                         spec:
     *                              workloads:
     *                                  - id: app_workload
     *                                    box_ref: app_box
     *                                  ...
     * ```
     */
    check('DEPLOY_WORKLOAD_NAME', async () => {
        Object.entries(deployUnits).forEach(([name, deployUnit]) => {
            const podSpec = deployUnit.multi_cluster_replica_set?.replica_set?.pod_template_spec ||
                deployUnit.replica_set.replica_set_template.pod_template_spec;
            const workload = podSpec.spec.pod_agent_payload?.spec?.workloads?.[0];
            const expected = `${name}_workload`;
            assert(
                workload?.id === expected,
                `Name of main workload in DU "${name}" should be equal "${expected}" (current value "${workload?.id}")`
            );
        });
    });

    /**
     * @description
     * Requires multi-cluster replica set (MCRS) for deploy unit.
     *
     * ## Rationale
     * A multi-cluster replica set defines a "virtual" data center which can be distributed among different physical data centers. The virtual DC can have it's own configuration for the number of maximum unavailable instances.
     * The other kind of replica set, called Single Replica Set (SRS) requires multiple instances in each data center, and also requires a separate configuration for the maximum unavailable instances for each data center.
     * Most of our services are small and run on 1 instance in 3 data centers. That's why it is required for the majority of services to use MCRS for their deploy unit configuration. If you think your service should be excluded from this group contact a member of the maps-front-infra DevOps team.
     *
     * ## Solution
     * 1. Go to your deploy configuration in the code repository.
     * 1. Check that your configuration corresponds to the following scheme:
     * ```yml
     * spec:
     *     deploy_units:
     *         app:
     *             multi_cluster_replica_set:
     *                 replica_set:
     *                     ...
     * ```
     */
    check('DEPLOY_UNIT_TYPE', async () => {
        Object.entries(deployUnits).forEach(([name, deployUnit]) => {
            assert(
                Boolean(deployUnit.multi_cluster_replica_set),
                `Type of deploy unit "${name}" should be multi-cluster replica set (MCRS)`
            );
        });
    });

    /**
     * @description
     * Requires the disruption budget of a deploy unit to be less or equal to 33% of the instance count.
     *
     * ## Rationale
     * The disruption budget determines how many instances can be unavailable (restarting) during the deployment process.  If the disruption budget is more than a third of the total instance count, a spike in traffic or a incident occurring during the deployment ca seriously affect the service SLA.
     *
     * ## Solution
     * 1. Go to your deploy configuration in repository.
     * 1. Set the `max_unavailable` value to be less or equal to 0.3 of the total replica count:
     * ```yml
     * spec:
     *     deploy_units:
     *         app:
     *             multi_cluster_replica_set:
     *                 replica_set:
     *                     clusters:
     *                         - cluster: man
     *                           spec:
     *                               replica_count: 1
     *                         - cluster: sas
     *                           spec:
     *                               replica_count: 1
     *                         - cluster: vla
     *                           spec:
     *                               replica_count: 1
     *                     deployment_strategy:
     *                         max_unavailable: 1
     * ```
     */
    check('DEPLOY_DISRUPTION_BUDGET', async () => {
        Object.entries(deployUnits).forEach(([name, deployUnit]) => {
            // TODO: replica_set. Не забыть про полакационную выкладку.
            const replicaSet = deployUnit.multi_cluster_replica_set?.replica_set;
            const count = replicaSet?.clusters.reduce((acc, {spec}) => acc + spec.replica_count, 0);
            const budget = replicaSet?.deployment_strategy.max_unavailable;
            assert(
                budget <= Math.round(count * 0.3),
                `Disruption budget of deploy unit "${name}" should be less or equal than 30% of instances count (current value "${(budget * 100 / count).toFixed(2)}%")`
            );
        });
    });

    /**
     * @description
     * Requires TVM deploy unit.
     *
     * ## Rationale
     * All services should interact with each other only using [TVM](https://wiki.yandex-team.ru/passport/tvm2) as authorization mechanism.
     *
     * ## Solution
     * 1. Go to your deploy configuration in repository.
     * 1. Check that your configuration corresponds to the following scheme:
     * ```yml
     * spec:
     *     deploy_units:
     *         app:
     *             tvm_config:
     *                 mode: "enabled"
     * ```
     */
    check('DEPLOY_TVM', async () => {
        Object.entries(deployUnits).forEach(([name, deployUnit]) => {
            const mode = deployUnit.tvm_config?.mode as unknown as string;
            assert(
                mode === 'enabled',
                `TVM of deploy unit "${name}" should be enabled (current value "${mode}")`
            );
        });
    });

    /**
     * @description
     * Requires minimum disk size greater than 15Gb.
     *
     * ## Rationale
     * After several incidents involving multi-gigabyte log files we're requiring to have at least 15Gb on each instance.
     *
     * ## Solution
     * 1. Go to your deploy configuration in repository.
     * 1. Check that your configuration corresponds to the following scheme:
     * ```yml
     * spec:
     *     deploy_units:
     *         app:
     *             multi_cluster_replica_set:
     *                 replica_set:
     *                     pod_template_spec:
     *                         spec:
     *                             disk_volume_requests:
     *                                 - id: 'DISK_NAME'
     *                                   quota_policy:
     *                                       # 15 Gb
     *                                       capacity: 16106127360
     * ```
     */
    check('DEPLOY_DISK_SIZE', async () => {
        Object.entries(deployUnits).forEach(([name, deployUnit]) => {
            const podSpec = deployUnit.multi_cluster_replica_set?.replica_set?.pod_template_spec ||
                deployUnit.replica_set.replica_set_template.pod_template_spec;
            const capacity = podSpec.spec.disk_volume_requests[0]?.quota_policy.capacity;
            assert(
                capacity >= 15 * Gb,
                `Disk size of deploy unit "${name}" should be greater than 15Gb (current value ${Math.round(capacity / Gb)}Gb)`
            );
        });
    });

    /**
     * @description
     * Requires disk bandwidth guarantee greater than 15Mb.
     *
     * ## Rationale
     * It's a deploy platform's recommendation. See ((https://clubs.at.yandex-team.ru/infra-cloud/1099 post in infra-cloud club)).
     *
     * ## Solution
     * 1. Go to your deploy configuration in repository.
     * 1. Check that your configuration corresponds to the following scheme:
     * ```yml
     * spec:
     *     deploy_units:
     *         app:
     *             multi_cluster_replica_set:
     *                 replica_set:
     *                     pod_template_spec:
     *                         spec:
     *                             disk_volume_requests:
     *                                 - id: 'DISK_NAME'
     *                                   storage_class: hdd
     *                                   quota_policy:
     *                                       bandwidth_guarantee: 15728640
     *                                       bandwidth_limit: 31457280
     * ```
     * {% note info %}
     *
     * Disk bandwidth limit of deploy unit is calculated as follows:
     * - for SSD storage class: `bandwidth_limit = bandwidth_guarantee`
     * - for HDD storage class: `bandwidth_limit = 2 * bandwidth_guarantee`
     *
     * {% endnote %}
     *
     */
    check('DEPLOY_DISK_BANDWIDTH', async () => {
        Object.entries(deployUnits).forEach(([name, deployUnit]) => {
            const podSpec = deployUnit.multi_cluster_replica_set?.replica_set?.pod_template_spec ||
                deployUnit.replica_set.replica_set_template.pod_template_spec;
            const disk = podSpec.spec.disk_volume_requests[0];
            const storage = disk?.storage_class || 'hdd';
            const {
                bandwidth_guarantee: guarantee = 0,
                bandwidth_limit: limit = 0
            } = disk?.quota_policy || {};
            assert(
                guarantee >= 15 * Mb,
                `Disk bandwidth guarantee of deploy unit "${name}" should be greater than 15Mb (current value ${Math.round(guarantee / Mb)}Mb)`
            );

            const ratio = storage === 'ssd' ? 1 : 2;
            const expectedLimit = ratio * guarantee;
            assert(
                limit >= expectedLimit,
                `Disk bandwidth limit of deploy unit "${name}" should be great than ${Math.round(expectedLimit / Mb)}Mb (current value ${Math.round(limit / Mb)}Mb)`
            );
        });
    });

    /**
     * @description
     * Requires monitorings itype of deploy equals to `maps-front`.
     *
     * ## Rationale
     * Our own itype makes it possible to use our own Golovan quota.
     *
     * ## Solution
     * 1. Go to your deploy configuration in repository.
     * 1. Check that your configuration corresponds to the following scheme:
     * ```yml
     * spec:
     *     deploy_units:
     *         app:
     *             multi_cluster_replica_set:
     *                 replica_set:
     *                     pod_template_spec:
     *                         spec:
     *                             host_infra:
     *                                   monitoring:
     *                                       labels:
     *                                           itype: maps-front
     * ```
     */
    check('DEPLOY_YASM_ITYPE', async () => {
        Object.entries(deployUnits).forEach(([name, deployUnit]) => {
            const podSpec = deployUnit.multi_cluster_replica_set?.replica_set?.pod_template_spec ||
                deployUnit.replica_set.replica_set_template.pod_template_spec;
            const itype = podSpec.spec.host_infra?.monitoring?.labels?.itype;
            assert(
                itype === 'maps-front',
                `Monitorings itype of deploy unit "${name}" should be equal "maps-front" (current value "${itype}")`
            );
        });
    });

    /**
     * @description
     * Requires liveness limit ratio equal to 1.
     *
     * ## Rationale
     * Part of instances are removed from endpoint_set during deployment process. So Juggler removes them from its own group and call an alert because of more than one third instances are unavailable. See ((https://st.yandex-team.ru/RTCSUPPORT-8660#60257e32140d331ea1ee9313 RTCSUPPORT-8660)).
     *
     * ## Solution
     * 1. Go to your deploy configuration in repository.
     * 1. Check that your configuration corresponds to the following scheme:
     * ```yml
     * spec:
     *     deploy_units:
     *         endpoint_sets:
     *             - id: http
     *               liveness_limit_ratio: 1
     * ```
     */
    check('DEPLOY_LIVENESS_LIMIT_RATIO', async () => {
        Object.entries(deployUnits).forEach(([name, deployUnit]) => {
            const limitRatio = deployUnit.endpoint_sets?.[0]?.liveness_limit_ratio;
            assert(
                limitRatio === 1,
                `Liveness limit ratio of deploy unit "${name}" should be equal "1" (current value "${limitRatio}")`
            );
        });
    });

    /**
     * @description
     * Requires the presence of a unistat endpoint with a specific name.
     *
     * ## Rationale
     * By default Golovan uses `/unistat` as endpoint. We chose not change this value and so it is the required name for unistat.
     *
     * ## Solution
     * 1) Go to your deploy configuration in repository.
     * 2) Check that your configuration corresponds to the following scheme:
     * ```yml
     * spec:
     *     deploy_units:
     *         app:
     *             multi_cluster_replica_set:
     *                 replica_set:
     *                     pod_template_spec:
     *                         spec:
     *                             host_infra:
     *                                   monitoring:
     *                                       - workload_id: app_workload
     *                                         port: 7032
     *                                         path: /unistat
     * ```
     */
    check('DEPLOY_WORKLOAD_UNISTAT', async () => {
        Object.entries(deployUnits).forEach(([name, deployUnit]) => {
            const podSpec = deployUnit.multi_cluster_replica_set?.replica_set?.pod_template_spec ||
                deployUnit.replica_set.replica_set_template.pod_template_spec;
            const unistat = podSpec.spec.host_infra?.monitoring?.unistats?.[0];
            assert(
                Boolean(unistat),
                `Unistat of deploy unit ${name} should be exist.`
            );
            assert(
                unistat.path === '/unistat',
                `Unistat path of deploy unit "${name}" should be equal "/unistat" (current value "${unistat.path}")`
            );
            assert(
                unistat.port === 7032,
                `Unistat port of deploy unit "${name}" should be equal "7032" (current value "${unistat.port}")`
            );
            assert(
                unistat.workload_id === `${name}_workload`,
                `Unistat of deploy unit "${name}" should be linked with workload "${name}_workload" (current value "${unistat.workload_id}")`
            );
        });
    });

    /**
     * @description
     * Requires that liveness check for each workload is enabled.
     *
     * ## Rationale
     * The liveness of an instance is defined as "the container started normally and basic services are ready". Usually it means that there is web server listening and responding on TCP 80.
     *
     * ## Solution
     * 1. Go to your deploy configuration in repository.
     * 1. Check that your configuration corresponds to the following scheme:
     * ```yml
     * spec:
     *     deploy_units:
     *         app:
     *             multi_cluster_replica_set:
     *                 replica_set:
     *                     pod_template_spec:
     *                         spec:
     *                             pod_agent_payload:
     *                                   spec:
     *                                       workloads:
     *                                           - id: app_workload
     *                                             liveness_check:
     *                                                 tcp_check:
     *                                                     port: 80
     * ```
     */
    check('DEPLOY_WORKLOAD_LIVENESS', async () => {
        Object.entries(deployUnits).forEach(([name, deployUnit]) => {
            const podSpec = deployUnit.multi_cluster_replica_set?.replica_set?.pod_template_spec ||
                deployUnit.replica_set.replica_set_template.pod_template_spec;
            const workload = podSpec.spec.pod_agent_payload?.spec?.workloads?.find(({id}) => id === `${name}_workload`);
            const check = workload?.liveness_check?.tcp_check;
            assert(
                check?.port === 80,
                `Liveness check of workload "${name}_workload" should be set as TCP check on 80 port`
            );
        });
    });

    /**
     * @description
     * Requires readiness check to be set for each workload.
     *
     * ## Rationale
     * The readiness of an instance can be defined as "the service is up and ready to accept traffic". Usually that means that the application inside the container responds to HTTP GET `/ping` requests. This check is used by Yandex.Deploy during deploy time.
     * Note that the same endpoint is used as our `http-ping` monitoring, which tries to determine the status of application instance.
     *
     * ## Solution
     * 1. Go to your deploy configuration in repository.
     * 1. Check that your configuration corresponds to the following scheme:
     * ```yml
     * spec:
     *     deploy_units:
     *         app:
     *             multi_cluster_replica_set:
     *                 replica_set:
     *                     pod_template_spec:
     *                         spec:
     *                             pod_agent_payload:
     *                                   spec:
     *                                       workloads:
     *                                           - id: app_workload
     *                                             readiness_check:
     *                                                  http_get:
     *                                                      expected_answer: ""
     *                                                      path: "/ping"
     *                                                      port: 80
     *                                                      time_limit:
     *                                                          max_execution_time_ms: 1000
     * ```
     */
    check('DEPLOY_WORKLOAD_READINESS', async () => {
        Object.entries(deployUnits).forEach(([name, deployUnit]) => {
            const podSpec = deployUnit.multi_cluster_replica_set?.replica_set?.pod_template_spec ||
                deployUnit.replica_set.replica_set_template.pod_template_spec;
            const workload = podSpec.spec.pod_agent_payload?.spec?.workloads?.find(({id}) => id === `${name}_workload`);
            const check = workload?.readiness_check?.http_get;
            assert(
                check?.port === 80 && check.path === '/ping',
                `Readiness check of workload "${name}_workload" should be set as HTTP check with port 80, path "/ping"`
            );
        });
    });

    /**
     * @description
     * Requires the stop policy of each workload set with a certain template.
     *
     * ## Rationale
     * It's used during deploy time to seamlessly switch traffic between the old and new version: the current instance will be closed, Yandex.Deploy will wait for the current requests to be processed and only then the instance will be destroyed.
     *
     * ## Solution
     * 1. Go to your deploy configuration in repository.
     * 1. Check that your configuration corresponds to the following scheme:
     * ```yml
     * spec:
     *     deploy_units:
     *         app:
     *             multi_cluster_replica_set:
     *                 replica_set:
     *                     pod_template_spec:
     *                         spec:
     *                             pod_agent_payload:
     *                                   spec:
     *                                       workloads:
     *                                           stop_policy:
     *                                               container:
     *                                                   command_line: bash -c "/detach.sh"
     *                                                   time_limit:
     *                                                       max_execution_time_ms: 20000
     *                                                       max_restart_period_ms: 30000
     *                                                       min_restart_period_ms: 30000
     *                                               max_tries: 2
     * ```
     */
    check('DEPLOY_WORKLOAD_STOP_POLICY', async () => {
        Object.entries(deployUnits).forEach(([name, deployUnit]) => {
            const podSpec = deployUnit.multi_cluster_replica_set?.replica_set?.pod_template_spec ||
                deployUnit.replica_set.replica_set_template.pod_template_spec;
            const workload = podSpec.spec.pod_agent_payload?.spec?.workloads?.find(({id}) => id === `${name}_workload`);
            const check = workload?.stop_policy?.container;
            const time = check?.time_limit;

            assert(
                check?.command_line === 'bash -c "/detach.sh"',
                `Stop policy of workload "${name}_workload" should be set as command "bash -c "/detach.sh""`
            );
            assert(
                workload?.stop_policy?.max_tries <= 2,
                `Stop policy retries of workload "${name}_workload" should be less or equal then 2`
            );

            const maxExecTime = 20000;
            const maxRestartPeriod = 30000;
            const minRestartPeriod = 30000;
            const messageExpected = [
                `time.max_execution_time_ms=${maxExecTime}`,
                `time.max_restart_period_ms=${maxRestartPeriod}`,
                `time.min_restart_period_ms=${minRestartPeriod}`
            ].join(' ');
            assert(
                time?.max_execution_time_ms === maxExecTime &&
                time?.max_restart_period_ms === maxRestartPeriod &&
                time?.min_restart_period_ms === minRestartPeriod,
                `Stop policy timings of workload "${name}_workload" should be set as next config "${messageExpected}"`
            );
        });
    });

    /**
     * @description
     * Requires transmitting logs.
     *
     * ## Rationale
     * All our services should send all logs to YT.
     *
     * ## Solution
     * 1. Go to your deploy configuration in repository.
     * 1. Check that your configuration corresponds to the following scheme:
     * ```yml
     * spec:
     *     deploy_units:
     *         app:
     *             multi_cluster_replica_set:
     *                 replica_set:
     *                     pod_template_spec:
     *                         spec:
     *                             pod_agent_payload:
     *                                   spec:
     *                                       workloads:
     *                                           transmit_logs: true
     * ```
     */
    check('DEPLOY_WORKLOAD_LOGS', async () => {
        Object.entries(deployUnits).forEach(([name, deployUnit]) => {
            const podSpec = deployUnit.multi_cluster_replica_set?.replica_set?.pod_template_spec ||
                deployUnit.replica_set.replica_set_template.pod_template_spec;
            const workload = podSpec.spec.pod_agent_payload?.spec?.workloads?.find(({id}) => id === `${name}_workload`);
            assert(
                Boolean(workload?.transmit_logs),
                `Workload "${name}_workload" should transmit logs`
            );
        });
    });
});
