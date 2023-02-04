# -*- coding: utf-8 -*-

import infra.dctl.src.consts as consts
import os
import paramiko
import requests
import socket
import sys
import time
import yaml
import yatest.common
import yp.client
import yp.common
import yp.data_model as data_model
import yt.yson as yson
import yt_yson_bindings
from importlib import reload
from library.python import resource as rs
from tqdm import tqdm
from yp_proto.yp.client.api.proto import object_service_pb2
import logging
from io import StringIO
import datetime

reload(sys)

YP_TOKEN_ENV = "YP_TOKEN"
YP_TOKEN_FILE = os.path.expanduser("~/.yp/token")
YP_XDC_ADDRESS = "xdc"
YP_SAS_ADDRESS = "sas"
YAML_LOADER = getattr(yaml, "CSafeLoader", yaml.SafeLoader)
IDM_TOKEN_ENV = "IDM_TOKEN"
IDM_TOKEN_FILE = os.path.expanduser("~/.idm/token")
IDM_ADDRESS = "https://idm.yandex-team.ru/"
IDM_API_ROLEREQUESTS_PATH = "https://idm-api.yandex-team.ru/api/v1/rolerequests/"
IDM_API_ROLES_PATH = "https://idm-api.yandex-team.ru/api/v1/roles/"
SSH_PRIVATE_KEY_ENV = "SSH_PRIVATE_KEY"
DEPLOY_UNIT_ID = "DeployUnit1"  # default for all specs
PROJECT_YML_PATH = "project_spec.yaml"
DEPLOY_TICKET_YML_PATH = "deploy_ticket_spec.yaml"
RELEASE_YML_PATH = "release_spec.yaml"
RELEASE_RULE_YML_PATH = "release_rule_spec.yaml"
APPROVAL_POLICY_YML_PATH = "approval_policy_spec.yaml"
CLUSTER_TO_SPEC_FILE = {
    "xdc": "stage_spec_xdc.yaml",
    "sas-test": "stage_spec_sas_test.yaml",
    "man-pre": "stage_spec_man_pre.yaml",
}
CLUSTER_TO_IDM_SYSTEM = {
    "xdc": "deploy-prod",
    "sas-test": "deploy-test",
    "man-pre": "deploy-pre"
}
TIME_TO_WAIT_ROLE_REMOVING = 20
STAGE_CONTROLLER_SYNC_PERIOD = 15
PORTO_SHELL_SYNC_PERIOD = 10
COUNT_OF_PORTO_SHELL_ITERATIONS_FOR_WAIT = 3
POD_DEPLOY_MAX_WAIT_TIME = 900
POD_DEPLOY_FIRST_STEP_WAIT_TIME = POD_DEPLOY_MAX_WAIT_TIME // 3
POD_DEPLOY_AFTER_FIRST_STEP_WAIT_TIME = 20
SSH_PORT = 22
DEBUG_MODE = False
AUTO_TESTS_FACTOR = 3
BOX_READY_STATE = 6
DEFAULT_ID = "authctl-it-tests-{:d}"
REMOVE_MAX_TRIES = 3


def extract_location_master_address(crosslocation_cluster):
    object_yaml = rs.find(CLUSTER_TO_SPEC_FILE[crosslocation_cluster]).decode("utf-8")
    dict = yaml.load(object_yaml, Loader=YAML_LOADER)
    return list(dict["spec"]["deploy_units"][DEPLOY_UNIT_ID]["replica_set"]["per_cluster_settings"].keys())[0]


def run_with_catch_exception(function, description, *args, **kwargs):
    was_catched = False
    try:
        function(args, kwargs)
    except:
        was_catched = True
        logging.info("Access denied for {}".format(description))
    if not was_catched:
        raise Exception("Exception was not catched while {}!".format(description))


def sleep_with_progress(time_to_sleep_seconds, count_of_segments, wait_description):
    if DEBUG_MODE:
        step_sleep = int(time_to_sleep_seconds / count_of_segments)
        segments = [step for step in range(0, time_to_sleep_seconds, step_sleep)]
        for _ in tqdm(segments, desc="Wait {}s for {}".format(time_to_sleep_seconds, wait_description)):
            time.sleep(step_sleep)
    else:
        time_to_sleep_seconds = time_to_sleep_seconds * AUTO_TESTS_FACTOR
        logging.info("Wait {}s for {}".format(time_to_sleep_seconds, wait_description))
        time.sleep(time_to_sleep_seconds)


def add_role_subject(path, subject, token, cluster):
    json_req = {
        "system": CLUSTER_TO_IDM_SYSTEM[cluster],
        "path": path,
    }
    if isinstance(subject, int):
        json_req["group"] = subject
    else:
        json_req["user"] = subject
    rsp = requests.post(
        IDM_API_ROLEREQUESTS_PATH,
        json=json_req,
        headers={
            "Authorization": "OAuth {}".format(token)
        })

    if rsp.status_code == 201:
        sleep_with_progress(90, 90, "activating role {} for {}".format(path, subject))
        return rsp.json()["id"]
    else:
        raise Exception("Bad request for role adding, code {}, msg: {}".format(rsp.status_code, rsp.json()["message"]))


def remove_role(role_id, token):
    try_counter = 0
    max_tries = 5
    while try_counter < max_tries:
        try_counter += 1
        logging.info("Try {} for remove role {} from idm".format(try_counter, role_id))
        rsp = requests.delete(
            IDM_API_ROLES_PATH + str(role_id) + "/",
            headers={
                "Authorization": "OAuth {}".format(token)
            })
        if rsp.status_code == 204:
            sleep_with_progress(
                time_to_sleep_seconds=TIME_TO_WAIT_ROLE_REMOVING,
                count_of_segments=TIME_TO_WAIT_ROLE_REMOVING,
                wait_description="removing role {}".format(role_id))
            return
        elif rsp.status_code == 403:
            logging.info("Role hasn`t been activated in IDM yet. Let`s try remove again after 5s")
            time.sleep(5)
        else:
            raise Exception(
                "Bad request for role removing, code {}, msg: {}".format(rsp.status_code, rsp.json()["message"]))
    if try_counter == max_tries:
        raise Exception("Max tries exceeded")


def get_roles_for_path(path, token, cluster):
    return requests.get(
        IDM_API_ROLES_PATH,
        params={
            "path": path,
            "system": CLUSTER_TO_IDM_SYSTEM[cluster]
        },
        headers={
            "Authorization": "OAuth {}".format(token)
        })


def get_id_of_owner_role(project_id, token, cluster):
    role_path = "/{}/".format(project_id)

    max_tries = 10
    try_counter = 0
    while try_counter < max_tries:
        try_counter += 1
        logging.info("Try {} for get id of OWNER role".format(try_counter))
        rsp = get_roles_for_path(role_path, token, cluster)
        if rsp.status_code == 200:
            logging.info("OWNER role for project {} was detected".format(project_id))
            return rsp.json()["objects"][0]["id"]

        logging.info("Role doesn`t exist in IDM. Let`s try find again after 5s")
        time.sleep(5)

    raise Exception("OWNER role for project {} doesn`t exist in IDM, msg: {}".format(project_id, rsp.json()["message"]))


def get_ssh_private_key():
    key = os.getenv(SSH_PRIVATE_KEY_ENV)
    if key:
        key = key.replace("_", "\n")
        print(key)
        keyfile = StringIO(key)
        return paramiko.RSAKey.from_private_key(keyfile)

    agent = paramiko.Agent()
    agent_keys = agent.get_keys()
    if agent_keys and len(agent_keys) > 0:
        return agent_keys[0]
    raise Exception("No ssh private key provided")


def agent_auth(transport, username):
    key = get_ssh_private_key()
    try:
        transport.auth_publickey(username, key)
        logging.info("... SUCCESS!")
        return True
    except paramiko.SSHException:
        logging.info("... NOPE.")
        return False


def get_token(token_env, token_path, api_name):
    token = os.getenv(token_env)
    if token:
        logging.info("Use {} token from env {}".format(api_name, token_env))
        return token
    if os.path.isfile(token_path):
        logging.info("Use {} token from file {}".format(api_name, token_path))
        with open(token_path, "r") as f:
            return f.read().strip()
    raise Exception("No {} token provided".format(api_name))


def get_yp_client(yp_address):
    yp_token = get_token(YP_TOKEN_ENV, YP_TOKEN_FILE, "yp")
    cluster_config = consts.CLUSTER_CONFIGS[yp_address]

    client = yp.client.YpClient(
        address=cluster_config.address,
        config={
            "token": yp_token
        }
    )
    return client.create_grpc_object_stub()


def dict_to_yp_object(dict, object_type):
    return yt_yson_bindings.loads_proto(
        yson.dumps(dict),
        proto_class=object_type,
        skip_unknown_fields=False
    )


def create_object(yp_client, object_type, object, object_description, test_mode):
    if object.meta.id == "":
        object.meta.id = DEFAULT_ID.format(int(time.time() * 10e9))
    attr = object.labels.attributes.add()
    attr.key = "deploy_it_test_mode"
    attr.value = yson.dumps(test_mode)
    r = object_service_pb2.TReqCreateObject(
        object_type=object_type,
        attributes=yt_yson_bindings.dumps_proto(object)
    )
    rsp = yp_client.CreateObject(r)

    logging.info("{} {} was created".format(object_description, rsp.object_id))
    return rsp.object_id


def remove_object(yp_client, object_type, object_id, object_description):
    req = object_service_pb2.TReqRemoveObject(
        object_type=object_type,
        object_id=object_id
    )

    for i in range(REMOVE_MAX_TRIES):
        try:
            yp_client.RemoveObject(req)
            logging.info("{} {} was removed".format(object_description, object_id))
            return
        except Exception as e:
            if i == REMOVE_MAX_TRIES - 1:
                logging.info("{} {} WAS NOT REMOVED. Error: {}".format(object_description, object_id, str(e)))


def remove_objects(yp_client, object_type, object_ids, object_description):
    req = object_service_pb2.TReqRemoveObjects(
        ignore_nonexistent=True,
    )
    for id in object_ids:
        subreq = req.subrequests.add()
        subreq.object_id = id
        subreq.object_type = object_type

    try:
        yp_client.RemoveObjects(req)
        logging.info("{} {} were removed".format(object_description, object_ids))
        return
    except Exception as e:
        logging.info("{} {} WERE NOT REMOVED. Error: {}".format(object_description, object_ids, str(e)))


def create_project(yp_client, test_mode):
    project_yaml = rs.find(PROJECT_YML_PATH).decode("utf-8")
    dict = yaml.load(project_yaml, Loader=YAML_LOADER)
    project = dict_to_yp_object(dict, data_model.TProject)
    return create_object(yp_client, data_model.OT_PROJECT, project, "Project", test_mode)


def update_project(yp_client, project_id):
    req = object_service_pb2.TReqUpdateObject()
    req.object_type = data_model.OT_PROJECT
    req.object_id = project_id

    upd = req.set_updates.add()
    upd.path = "/spec/user_specific_box_types/end"
    upd.value = yson.dumps("new_type")

    yp_client.UpdateObject(req)
    logging.info("Project {} was updated".format(project_id))


def remove_project(yp_client, project_id):
    remove_object(yp_client, data_model.OT_PROJECT, project_id, "Project")


def create_stage(yp_client, yp_address, project_id, test_mode):
    stage_yaml = rs.find(CLUSTER_TO_SPEC_FILE[yp_address]).decode("utf-8")
    dict = yaml.load(stage_yaml, Loader=YAML_LOADER)
    stage = dict_to_yp_object(dict, data_model.TStage)
    stage.meta.project_id = project_id
    return create_object(yp_client, data_model.OT_STAGE, stage, "Stage", test_mode)


def update_stage(yp_client, yp_cross_address, stage_id):
    req = object_service_pb2.TReqUpdateObject()
    req.object_type = data_model.OT_STAGE
    req.object_id = stage_id

    location_address = extract_location_master_address(yp_cross_address)
    upd = req.set_updates.add()
    upd.path = "/spec/deploy_units/DeployUnit1/replica_set/per_cluster_settings/{}/pod_count".format(location_address)
    upd.value = yson.dumps(2)

    yp_client.UpdateObject(req)
    logging.info("Stage {} was updated".format(stage_id))


def remove_stage(yp_client, stage_id):
    remove_object(yp_client, data_model.OT_STAGE, stage_id, "Stage")


def create_release(yp_client, test_mode):
    release_yaml = rs.find(RELEASE_YML_PATH).decode("utf-8")
    dict = yaml.load(release_yaml, Loader=YAML_LOADER)
    release = dict_to_yp_object(dict, data_model.TRelease)
    return create_object(yp_client, data_model.OT_RELEASE, release, "Release", test_mode)


def remove_release(yp_client, release_id):
    remove_object(yp_client, data_model.OT_RELEASE, release_id, "Release")


def create_release_rule(yp_client, stage_id, test_mode):
    release_rule_yaml = rs.find(RELEASE_RULE_YML_PATH).decode("utf-8")
    dict = yaml.load(release_rule_yaml, Loader=YAML_LOADER)
    release_rule = dict_to_yp_object(dict, data_model.TReleaseRule)
    release_rule.meta.stage_id = stage_id
    return create_object(yp_client, data_model.OT_RELEASE_RULE, release_rule, "Release rule", test_mode)


def create_deploy_ticket(yp_client, stage_id, release_id, release_rule_id, test_mode):
    deploy_ticket_yaml = rs.find(DEPLOY_TICKET_YML_PATH).decode("utf-8")
    dict = yaml.load(deploy_ticket_yaml, Loader=YAML_LOADER)
    deploy_ticket = dict_to_yp_object(dict, data_model.TDeployTicket)
    deploy_ticket.meta.stage_id = stage_id
    deploy_ticket.spec.release_id = release_id
    deploy_ticket.spec.release_rule_id = release_rule_id
    return create_object(yp_client, data_model.OT_DEPLOY_TICKET, deploy_ticket, "Deploy ticket", test_mode)


def control_action_deploy_ticket(yp_client, deploy_ticket_id, control):
    req = object_service_pb2.TReqUpdateObject()
    req.object_type = data_model.OT_DEPLOY_TICKET
    req.object_id = deploy_ticket_id

    upd = req.set_updates.add()
    upd.path = "/control/{}".format(control)
    upd.value = yson.dumps({
        "options": {
            "message": "msg"
        }
    })

    yp_client.UpdateObject(req)
    logging.info("Deploy ticket {} was updated by {} control".format(deploy_ticket_id, control))


def create_approval_policy(yp_client, stage_id, test_mode):
    approval_policy_yaml = rs.find(APPROVAL_POLICY_YML_PATH).decode("utf-8")
    dict = yaml.load(approval_policy_yaml, Loader=YAML_LOADER)
    approval_policy = dict_to_yp_object(dict, data_model.TApprovalPolicy)
    approval_policy.meta.stage_id = stage_id
    approval_policy.meta.id = stage_id
    return create_object(yp_client, data_model.OT_APPROVAL_POLICY, approval_policy, "Approval policy", test_mode)


def get_pods_with_needed_pod_set_id(yp_client, pod_set_id):
    req = object_service_pb2.TReqSelectObjects()
    req.object_type = data_model.OT_POD
    req.selector.paths.append("")

    query = '[/meta/pod_set_id]="{}"'.format(pod_set_id)
    req.filter.query = query
    objects = []
    resp = yp_client.SelectObjects(req)
    for res in resp.results:
        obj = yt_yson_bindings.loads_proto(
            res.values[0],
            data_model.TPod,
            skip_unknown_fields=True
        )
        objects.append(obj)
    return objects


def get_default_box_ipv6(pod):
    for box in pod.status.agent.pod_agent_payload.status.boxes:
        if box.id != "logbroker_tools_box":
            return box.ip6_address


def get_logbroker_box_ipv6(pod):
    for box in pod.status.agent.pod_agent_payload.status.boxes:
        if box.id == "logbroker_tools_box":
            return box.ip6_address


def check_ssh_availability(ip_address, ip_description, username):
    logging.info("Start checking ssh access for {} by {}".format(ip_description, username))
    try:
        sock = socket.socket(socket.AF_INET6, socket.SOCK_STREAM)
        sock.connect((ip_address, SSH_PORT))
    except Exception as e:
        logging.info("*** Connect failed: " + str(e))
        sys.exit(1)

    try:
        transport = paramiko.Transport(sock)
        try:
            transport.start_client()
        except paramiko.SSHException:
            logging.info("*** SSH negotiation failed.")
            sys.exit(1)

        if agent_auth(transport, username):
            transport.close()
            return True
        else:
            transport.close()
            return False

    except Exception as e:
        logging.info("*** Caught exception: {} : {}".format(str(e.__class__), str(e)))
        try:
            transport.close()
        except:
            pass
        sys.exit(1)


def check_no_access(default_box_ip, system_box_ip, pod_fqdn):
    logging.info("Start checking no access")
    return not check_ssh_availability(default_box_ip, "default box", "nobody") \
           and not check_ssh_availability(default_box_ip, "default box", "root") \
           and not check_ssh_availability(system_box_ip, "system box", "nobody") \
           and not check_ssh_availability(system_box_ip, "system box", "root") \
           and not check_ssh_availability(pod_fqdn, "pod", "nobody") \
           and not check_ssh_availability(pod_fqdn, "pod", "root")


def check_developer_access(default_box_ip, system_box_ip, pod_fqdn):
    logging.info("Start checking developer access")
    return check_ssh_availability(default_box_ip, "default box", "nobody") \
           and not check_ssh_availability(default_box_ip, "default box", "root") \
           and not check_ssh_availability(system_box_ip, "system box", "nobody") \
           and not check_ssh_availability(system_box_ip, "system box", "root") \
           and not check_ssh_availability(pod_fqdn, "pod", "nobody") \
           and not check_ssh_availability(pod_fqdn, "pod", "root")


def check_root_developer_access(default_box_ip, system_box_ip, pod_fqdn):
    logging.info("Start checking root developer access")
    return check_ssh_availability(default_box_ip, "default box", "nobody") \
           and check_ssh_availability(default_box_ip, "default box", "root") \
           and not check_ssh_availability(system_box_ip, "system box", "nobody") \
           and not check_ssh_availability(system_box_ip, "system box", "root") \
           and not check_ssh_availability(pod_fqdn, "pod", "nobody") \
           and not check_ssh_availability(pod_fqdn, "pod", "root")


def check_system_developer_access(default_box_ip, system_box_ip, pod_fqdn):
    logging.info("Start checking system developer access")
    return not check_ssh_availability(default_box_ip, "default box", "nobody") \
           and not check_ssh_availability(default_box_ip, "default box", "root") \
           and check_ssh_availability(system_box_ip, "system box", "nobody") \
           and check_ssh_availability(system_box_ip, "system box", "root") \
           and check_ssh_availability(pod_fqdn, "pod", "nobody") \
           and check_ssh_availability(pod_fqdn, "pod", "root")


def get_boxes_ip_with_pod_fqdn(yp_location_client, stage_id):
    pod_set_id = "{}.{}".format(stage_id, DEPLOY_UNIT_ID)
    start = time.time()
    end = time.time()
    first_sleep = True
    try_counter = 0
    while end - start < POD_DEPLOY_MAX_WAIT_TIME:
        try_counter += 1
        logging.info("Try {} for find ips of boxes".format(try_counter))
        pods = get_pods_with_needed_pod_set_id(yp_location_client, pod_set_id)
        if len(pods) == 0 \
                or not pods[0].status.agent.pod_agent_payload.HasField("status") \
                or pods[0].status.agent.pod_agent_payload.status.boxes[0].ip6_address == "" \
                or pods[0].status.agent.pod_agent_payload.status.boxes[0].state != BOX_READY_STATE \
                or pods[0].status.agent.pod_agent_payload.status.boxes[1].ip6_address == "" \
                or pods[0].status.agent.pod_agent_payload.status.boxes[1].state != BOX_READY_STATE:

            if first_sleep:
                first_sleep = False
                sleep_time = POD_DEPLOY_FIRST_STEP_WAIT_TIME
                if not DEBUG_MODE:
                    # sleep_time will be multiplied in sleep_with_progress func in auto tests mode,
                    # but we dont`t need it in this loop.
                    sleep_time = sleep_time // AUTO_TESTS_FACTOR

                sleep_with_progress(
                    time_to_sleep_seconds=sleep_time,
                    count_of_segments=sleep_time,
                    wait_description="full deploying of pod")
            else:
                sleep_with_progress(
                    time_to_sleep_seconds=POD_DEPLOY_AFTER_FIRST_STEP_WAIT_TIME,
                    count_of_segments=POD_DEPLOY_AFTER_FIRST_STEP_WAIT_TIME,
                    wait_description="full deploying of pod")
            end = time.time()
        else:
            logging.info("Pod has full deployed. Ips of boxes have been detected after {}s".format(end - start))
            break

    if end - start >= POD_DEPLOY_MAX_WAIT_TIME:
        raise Exception("Pod hasn`t fully deploy after {}s".format(POD_DEPLOY_MAX_WAIT_TIME))

    if end - start > 5:
        # If we spent so long time on resolving ip, we need to wait portoshell syncing.
        time_to_sleep = PORTO_SHELL_SYNC_PERIOD * COUNT_OF_PORTO_SHELL_ITERATIONS_FOR_WAIT
        sleep_with_progress(
            time_to_sleep_seconds=time_to_sleep,
            count_of_segments=time_to_sleep,
            wait_description="portoshell sync"
        )

    default_box_ipv6 = get_default_box_ipv6(pods[0])
    logbroker_box_ipv6 = get_logbroker_box_ipv6(pods[0])
    pod_fqdn = pods[0].status.dns.persistent_fqdn
    return default_box_ipv6, logbroker_box_ipv6, pod_fqdn


def check_ssh_access_core(yp_location_client, stages_id, check_func, role_name, access_description):
    for stage_id in stages_id:
        logging.info("Stage {}:".format(stage_id))
        default_box_ipv6, logbroker_box_ipv6, pod_fqdn = get_boxes_ip_with_pod_fqdn(yp_location_client, stage_id)

        # check ssh access in boxes
        if not check_func(default_box_ipv6, logbroker_box_ipv6, pod_fqdn):
            raise Exception("{} role error: {} access is WRONG for stage {}".format(
                role_name, access_description, stage_id)
            )
        logging.info("Ssh access for stage {} is RIGHT!!!".format(stage_id))


def check_project_maintenance_roles(yp_cross_client, yp_cross_address, yp_location_client, subject, idm_token,
                                    role_name, project_id, stage_id1, stage_id2, test_mode, need_to_add_role=False):
    logging.info("*" * 20)
    logging.info("Start checking {} role for project".format(role_name))

    try:
        if need_to_add_role:
            add_role_subject("/{}/{}/".format(project_id, role_name), subject, idm_token, yp_cross_address)
    except Exception as err:
        remove_project_with_stages(yp_cross_client, project_id, [stage_id1, stage_id2])
        raise err
    try:
        check_ssh_access_core(
            yp_location_client,
            [stage_id1, stage_id2],
            check_root_developer_access,
            role_name,
            "root developer"
        )

        stage_id = create_stage(yp_cross_client, yp_cross_address, project_id, test_mode)
        sleep_with_progress(30, 30, wait_description="acl inheritance")
        try:
            update_stage(yp_cross_client, yp_cross_address, stage_id)
        except Exception as err:
            raise err
        finally:
            remove_stage(yp_cross_client, stage_id)

        update_project(yp_cross_client, project_id)
    except Exception as err:
        raise err
    finally:
        remove_project_with_stages(yp_cross_client, project_id, [stage_id1, stage_id2])

    logging.info("{} role for project is RIGHT!!!".format(role_name))
    logging.info("*" * 20)


def check_project_owner_role(yp_cross_client, yp_cross_address, yp_location_client, subject, idm_token, project_id,
                             stage_id1, stage_id2,
                             need_to_add_role=False):
    check_project_maintenance_roles(yp_cross_client, yp_cross_address, yp_location_client, subject, idm_token, "OWNER",
                                    project_id, stage_id1, stage_id2, "project_owner", need_to_add_role)


def check_project_maintainer_role(yp_cross_client, yp_cross_address, yp_location_client, subject, idm_token, project_id,
                                  stage_id1, stage_id2):
    check_project_maintenance_roles(yp_cross_client, yp_cross_address, yp_location_client, subject, idm_token,
                                    "MAINTAINER", project_id, stage_id1, stage_id2, "project_maintainer", need_to_add_role=True)


def check_project_developer_role(yp_cross_client, yp_cross_address, yp_location_client, subject, idm_token, role_name,
                                 check_function, project_id, stage_id1, stage_id2):
    logging.info("*" * 20)
    logging.info("Start checking {} role for project".format(role_name))

    role_id = add_role_subject("/{}/{}/".format(project_id, role_name), subject, idm_token, yp_cross_address)
    check_ssh_access_core(
        yp_location_client,
        [stage_id1, stage_id2],
        check_function,
        role_name,
        role_name
    )

    run_with_catch_exception(create_stage, "creating new stage as {} for project".format(role_name),
                             yp_cross_client, yp_cross_address, project_id, "all_developers")
    run_with_catch_exception(update_stage, "updating stage {} as {} for project".format(stage_id1, role_name),
                             yp_cross_client, yp_cross_address, stage_id1)
    run_with_catch_exception(remove_stage, "removing stage {} as {} for project".format(stage_id1, role_name),
                             yp_cross_client, stage_id1)

    remove_role(role_id, idm_token)

    logging.info("{} role for project is RIGHT!!!".format(role_name))


def check_stage_maintainer_role(yp_cross_client, yp_cross_address, yp_location_client, subject, idm_token, project_id,
                                stage_id1, stage_id2):
    logging.info("*" * 20)
    logging.info("Start checking MAINTAINER role for stage")

    add_role_subject("/{}/{}/{}/".format(project_id, stage_id1, "MAINTAINER"), subject, idm_token, yp_cross_address)
    try:
        check_ssh_access_core(
            yp_location_client,
            [stage_id1],
            check_root_developer_access,
            "MAINTAINER",
            "root developer"
        )
        check_ssh_access_core(
            yp_location_client,
            [stage_id2],
            check_no_access,
            "MAINTAINER",
            "no"
        )

        update_stage(yp_cross_client, yp_cross_address, stage_id1)
    except Exception as err:
        raise err
    finally:
        remove_stage(yp_cross_client, stage_id1)

    run_with_catch_exception(create_stage, "creating new stage without permissions for project",
                             yp_cross_client, yp_cross_address, project_id, "stage_maintainer")
    run_with_catch_exception(update_stage, "updating stage {} as MAINTAINER for stage {}".format(stage_id2, stage_id1),
                             yp_cross_client, yp_cross_address, stage_id2)
    run_with_catch_exception(remove_stage, "removing stage {} as MAINTAINER for stage {}".format(stage_id2, stage_id1),
                             yp_cross_client, stage_id2)

    logging.info("MAINTAINER role for stage is RIGHT!!!")


def check_stage_developer_role(yp_cross_client, yp_cross_address, yp_location_client, subject, idm_token, role_name,
                               check_function, project_id, stage_id1, stage_id2):
    logging.info("*" * 20)
    logging.info("Start checking {} role for stage".format(role_name))

    role_id = add_role_subject("/{}/{}/{}/".format(project_id, stage_id1, role_name), subject, idm_token,
                               yp_cross_address)
    check_ssh_access_core(
        yp_location_client,
        [stage_id1],
        check_function,
        role_name,
        role_name
    )
    check_ssh_access_core(
        yp_location_client,
        [stage_id2],
        check_no_access,
        role_name,
        role_name
    )

    run_with_catch_exception(create_stage, "creating new stage without permissions for project",
                             yp_cross_client, yp_cross_address, project_id, "all_developers")
    run_with_catch_exception(update_stage,
                             "updating stage {} as {} for stage {}".format(stage_id1, role_name, stage_id1),
                             yp_cross_client, yp_cross_address, stage_id1)
    run_with_catch_exception(remove_stage,
                             "removing stage {} as {} for stage {}".format(stage_id1, role_name, stage_id1),
                             yp_cross_client, stage_id1)
    run_with_catch_exception(update_stage,
                             "updating stage {} as {} for stage {}".format(stage_id2, role_name, stage_id1),
                             yp_cross_client, yp_cross_address, stage_id2)
    run_with_catch_exception(remove_stage,
                             "removing stage {} as {} for stage {}".format(stage_id2, role_name, stage_id1),
                             yp_cross_client, stage_id2)

    remove_role(role_id, idm_token)

    logging.info("{} role for stage is RIGHT!!!".format(role_name))


def check_deployer_role(yp_cross_client, yp_cross_address, yp_location_client, subject, idm_token,
                        project_id, stage_id1, stage_id2):
    logging.info("*" * 20)
    logging.info("Start checking DEPLOYER role for project")

    role_id = add_role_subject("/{}/DEPLOYER/".format(project_id), subject, idm_token, yp_cross_address)
    check_ssh_access_core(
        yp_location_client,
        [stage_id1, stage_id2],
        check_no_access,
        "DEPLOYER",
        "no"
    )

    run_with_catch_exception(create_stage, "creating new stage as DEPLOYER for project",
                             yp_cross_client, yp_cross_address, project_id, "deployer")
    run_with_catch_exception(remove_stage, "removing stage {} as DEPLOYER for project".format(stage_id1),
                             yp_cross_client, stage_id1)
    try:
        update_stage(yp_cross_client, yp_cross_address, stage_id1)
        update_stage(yp_cross_client, yp_cross_address, stage_id2)
        update_project(yp_cross_client, project_id)
    except Exception as err:
        raise err
    finally:
        remove_role(role_id, idm_token)

    logging.info("DEPLOYER role for project is RIGHT!!!")


def prepare_objects(yp_cross_client, yp_cross_address, test_mode):
    project_id = create_project(yp_cross_client, test_mode)
    sleep_with_progress(60, 60, "OWNER activating")
    try:
        stage_id1 = create_stage(yp_cross_client, yp_cross_address, project_id, test_mode)
    except Exception as err:
        remove_project(yp_cross_client, project_id)
        raise Exception("Error while first stage creating: " + str(err))

    try:
        stage_id2 = create_stage(yp_cross_client, yp_cross_address, project_id, test_mode)
    except Exception as err:
        remove_stage(yp_cross_client, stage_id1)
        remove_project(yp_cross_client, project_id)
        raise Exception("Error while second stage creating: " + str(err))

    sleep_with_progress(60, 60, "authctl and stagectl sync")
    return project_id, stage_id1, stage_id2


def remove_project_with_stages(yp_cross_client, project, stages):
    for stage in stages:
        remove_stage(yp_cross_client, stage)
    remove_project(yp_cross_client, project)


def prepare_objects_with_owner_role_removing(yp_cross_client, yp_cross_address, idm_token, test_mode):
    project_id, stage_id1, stage_id2 = prepare_objects(yp_cross_client, yp_cross_address, test_mode)

    try:
        remove_role(get_id_of_owner_role(project_id, idm_token, yp_cross_address), idm_token)
        return project_id, stage_id1, stage_id2
    except Exception as err:
        remove_project_with_stages(yp_cross_client, project_id, [stage_id1, stage_id2])
        raise err


def all_developer_roles_test(yp_cross_client, yp_cross_address, yp_sas_client, subject, idm_token):
    project_id, stage_id1, stage_id2 = prepare_objects_with_owner_role_removing(
        yp_cross_client, yp_cross_address, idm_token, "all_developers")

    try:
        check_stage_developer_role(yp_cross_client, yp_cross_address, yp_sas_client, subject, idm_token, "DEVELOPER",
                                   check_developer_access, project_id, stage_id1, stage_id2)
        check_stage_developer_role(yp_cross_client, yp_cross_address, yp_sas_client, subject, idm_token,
                                   "ROOT_DEVELOPER", check_root_developer_access, project_id, stage_id1, stage_id2)
        check_stage_developer_role(yp_cross_client, yp_cross_address, yp_sas_client, subject, idm_token,
                                   "SYSTEM_DEVELOPER", check_system_developer_access, project_id, stage_id1, stage_id2)
        check_project_developer_role(yp_cross_client, yp_cross_address, yp_sas_client, subject, idm_token, "DEVELOPER",
                                     check_developer_access, project_id, stage_id1, stage_id2)
        check_project_developer_role(yp_cross_client, yp_cross_address, yp_sas_client, subject, idm_token,
                                     "ROOT_DEVELOPER", check_root_developer_access, project_id, stage_id1, stage_id2)
        check_project_developer_role(yp_cross_client, yp_cross_address, yp_sas_client, subject, idm_token,
                                     "SYSTEM_DEVELOPER", check_system_developer_access, project_id, stage_id1,
                                     stage_id2)
    except Exception as err:
        raise err
    finally:
        add_role_subject("/{}/{}/".format(project_id, "OWNER"), subject, idm_token, yp_cross_address)
        remove_project_with_stages(yp_cross_client, project_id, [stage_id1, stage_id2])


def project_owner_role_test(yp_cross_client, yp_cross_address, yp_sas_client, subject, idm_token):
    project_id, stage_id1, stage_id2 = prepare_objects(yp_cross_client, yp_cross_address, "project_owner")
    check_project_owner_role(yp_cross_client, yp_cross_address, yp_sas_client, subject, idm_token, project_id,
                             stage_id1, stage_id2)


def project_maintainer_role_test(yp_cross_client, yp_cross_address, yp_sas_client, subject, idm_token):
    project_id, stage_id1, stage_id2 = prepare_objects_with_owner_role_removing(
        yp_cross_client, yp_cross_address, idm_token, "project_maintainer")

    check_project_maintainer_role(yp_cross_client, yp_cross_address, yp_sas_client, subject, idm_token, project_id,
                                  stage_id1, stage_id2)


def stage_maintainer_role_test(yp_cross_client, yp_cross_address, yp_sas_client, subject, idm_token):
    project_id, stage_id1, stage_id2 = prepare_objects_with_owner_role_removing(
        yp_cross_client, yp_cross_address, idm_token, "stage_maintainer")

    try:
        check_stage_maintainer_role(yp_cross_client, yp_cross_address, yp_sas_client, subject, idm_token, project_id,
                                    stage_id1, stage_id2)
    except Exception as err:
        raise err
    finally:
        # first stage was removed while stage MAINTAINER was checked
        add_role_subject("/{}/{}/".format(project_id, "OWNER"), subject, idm_token, yp_cross_address)
        remove_project_with_stages(yp_cross_client, project_id, [stage_id2])


def check_project_approver_role(yp_cross_client, yp_cross_address, project_id, deploy_ticket_id1, deploy_ticket_id2,
                                subject, idm_token, role_name):
    logging.info("*" * 20)
    logging.info("Start checking {} role for project".format(role_name))
    role_id = add_role_subject("/{}/{}/".format(project_id, role_name), subject, idm_token, yp_cross_address)
    control_action_deploy_ticket(yp_cross_client, deploy_ticket_id1, "approve")
    control_action_deploy_ticket(yp_cross_client, deploy_ticket_id2, "approve")
    control_action_deploy_ticket(yp_cross_client, deploy_ticket_id1, "disapprove")
    control_action_deploy_ticket(yp_cross_client, deploy_ticket_id2, "disapprove")
    remove_role(role_id, idm_token)
    logging.info("{} role for project is RIGHT!!!".format(role_name))


def check_stage_approver_role(yp_cross_client, yp_cross_address, project_id, stage_id, deploy_ticket_id_with_role,
                              deploy_ticket_id_without_role, subject, idm_token, role_name):
    logging.info("*" * 20)
    logging.info("Start checking {} role for stage".format(role_name))
    role_id = add_role_subject("/{}/{}/{}/".format(project_id, stage_id, role_name), subject, idm_token,
                               yp_cross_address)
    control_action_deploy_ticket(yp_cross_client, deploy_ticket_id_with_role, "approve")
    control_action_deploy_ticket(yp_cross_client, deploy_ticket_id_with_role, "disapprove")
    run_with_catch_exception(
        control_action_deploy_ticket,
        "approving deploy ticket {} without approver role".format(deploy_ticket_id_without_role),
        yp_cross_client, deploy_ticket_id_without_role, "approve")
    remove_role(role_id, idm_token)
    logging.info("{} role for stage is RIGHT!!!".format(role_name))


def all_approver_roles_test(yp_cross_client, yp_cross_address, subject, idm_token):
    test_mode = "all_approvers"

    project_id, stage_id1, stage_id2 = prepare_objects(yp_cross_client, yp_cross_address, test_mode)
    try:
        release_id = create_release(yp_cross_client, test_mode)
    except Exception as err:
        remove_project_with_stages(yp_cross_client, project_id, [stage_id1, stage_id2])
        raise err

    try:
        sleep_with_progress(30, 30, "acl inheritance")
        release_rule_id1 = create_release_rule(yp_cross_client, stage_id1, test_mode)
        release_rule_id2 = create_release_rule(yp_cross_client, stage_id2, test_mode)
        deploy_ticket_id1 = create_deploy_ticket(yp_cross_client, stage_id1, release_id, release_rule_id1, test_mode)
        deploy_ticket_id2 = create_deploy_ticket(yp_cross_client, stage_id2, release_id, release_rule_id2, test_mode)
        create_approval_policy(yp_cross_client, stage_id1, test_mode)
        create_approval_policy(yp_cross_client, stage_id2, test_mode)
        remove_role(get_id_of_owner_role(project_id, idm_token, yp_cross_address), idm_token)
    except Exception as err:
        remove_release(yp_cross_client, release_id)
        remove_project_with_stages(yp_cross_client, project_id, [stage_id1, stage_id2])
        raise err

    try:
        check_project_approver_role(yp_cross_client, yp_cross_address, project_id, deploy_ticket_id1,
                                    deploy_ticket_id2, subject, idm_token, "APPROVER")
        check_project_approver_role(yp_cross_client, yp_cross_address, project_id, deploy_ticket_id1,
                                    deploy_ticket_id2, subject, idm_token, "MANDATORY_APPROVER")

        check_stage_approver_role(yp_cross_client, yp_cross_address, project_id, stage_id1, deploy_ticket_id1,
                                  deploy_ticket_id2, subject, idm_token, "APPROVER")
        check_stage_approver_role(yp_cross_client, yp_cross_address, project_id, stage_id1, deploy_ticket_id1,
                                  deploy_ticket_id2, subject, idm_token, "MANDATORY_APPROVER")
    except Exception as err:
        raise err
    finally:
        add_role_subject("/{}/{}/".format(project_id, "OWNER"), subject, idm_token, yp_cross_address)
        remove_release(yp_cross_client, release_id)
        remove_project_with_stages(yp_cross_client, project_id, [stage_id1, stage_id2])


def deployer_role_test(yp_cross_client, yp_cross_address, yp_sas_client, subject, idm_token):
    project_id, stage_id1, stage_id2 = prepare_objects_with_owner_role_removing(
        yp_cross_client, yp_cross_address, idm_token, "deployer")

    try:
        check_deployer_role(yp_cross_client, yp_cross_address, yp_sas_client, subject, idm_token, project_id,
                            stage_id1, stage_id2)
    except Exception as err:
        raise err
    finally:
        add_role_subject("/{}/{}/".format(project_id, "OWNER"), subject, idm_token, yp_cross_address)
        remove_project_with_stages(yp_cross_client, project_id, [stage_id1, stage_id2])


def clean_old_objects(yp_client, object_type, object_description):
    start_timestamp_seconds = datetime.datetime.now().timestamp() - 7200  # objects can live only 2 hours
    query = f'[/labels/deploy_it_test_owner] = "auth_controller" and [/meta/creation_time] < {int(start_timestamp_seconds * 1e6)}u'

    req = object_service_pb2.TReqSelectObjects()
    req.object_type = object_type
    req.selector.paths.append("/meta/id")

    req.filter.query = query
    resp = yp_client.SelectObjects(req)

    ids = []
    for r in resp.results:
        id = yson.loads(r.values[0])
        if id.startswith("authctl-it-tests"):
            ids.append(id)

    remove_objects(yp_client, object_type, ids, object_description)


def get_params():
    global DEBUG_MODE
    DEBUG_MODE = True if yatest.common.get_param("debug_mode", "False") == "True" else False

    user = yatest.common.get_param("user", None)
    group_id = yatest.common.get_param("idm_group_id", None)
    if not user and not group_id:
        logging.info("No one of user or group id was filled. Tests will not start")
        return
    subjects = []
    if user:
        subjects.append(user)
    if group_id:
        subjects.append(int(group_id))

    cluster = yatest.common.get_param("cluster", None)
    if not cluster:
        raise Exception("Missing required parameter 'cluster'")

    modes = list(yatest.common.get_param("mode", "all_roles").split())
    return subjects, cluster, modes


def test_roles_behaviour():
    subjects, cluster, modes = get_params()
    yp_cross_client = get_yp_client(cluster)
    yp_location_client = get_yp_client(extract_location_master_address(cluster))
    idm_token = get_token(IDM_TOKEN_ENV, IDM_TOKEN_FILE, "idm")

    clean_old_objects(yp_cross_client, data_model.OT_STAGE, "Stages")
    clean_old_objects(yp_cross_client, data_model.OT_PROJECT, "Projects")

    for subject in subjects:
        if "all_roles" in modes:
            all_developer_roles_test(yp_cross_client, cluster, yp_location_client, subject, idm_token)
            project_owner_role_test(yp_cross_client, cluster, yp_location_client, subject, idm_token)
            project_maintainer_role_test(yp_cross_client, cluster, yp_location_client, subject, idm_token)
            stage_maintainer_role_test(yp_cross_client, cluster, yp_location_client, subject, idm_token)
            all_approver_roles_test(yp_cross_client, cluster, subject, idm_token)
            deployer_role_test(yp_cross_client, cluster, yp_location_client, subject, idm_token)
            return

        if "all_developers" in modes:
            all_developer_roles_test(yp_cross_client, cluster, yp_location_client, subject, idm_token)
        if "project_owner" in modes:
            project_owner_role_test(yp_cross_client, cluster, yp_location_client, subject, idm_token)
        if "project_maintainer" in modes:
            project_maintainer_role_test(yp_cross_client, cluster, yp_location_client, subject, idm_token)
        if "stage_maintainer" in modes:
            stage_maintainer_role_test(yp_cross_client, cluster, yp_location_client, subject, idm_token)
        if "all_approvers" in modes:
            all_approver_roles_test(yp_cross_client, cluster, subject, idm_token)
        if "deployer" in modes:
            deployer_role_test(yp_cross_client, cluster, yp_location_client, subject, idm_token)
