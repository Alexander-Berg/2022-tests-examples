import re
import json
import os
import sys
import requests

import yabs.logger as logger
from sandbox.common import rest as SandboxApi


OAUTH_TOKEN_ENV_VAR = "SANDBOX_TOKEN"


def get_sandbox_oauth_token():
    if OAUTH_TOKEN_ENV_VAR in os.environ:
        return os.environ[OAUTH_TOKEN_ENV_VAR]
    else:
        logger.error("You shoud specify your sandbox token in envvar", OAUTH_TOKEN_ENV_VAR)
        raise AssertionError


def get_resource_info(client, resource_id):
    resp = client.resource[int(resource_id)].read(limit=100)
    return resp["type"], resp.get("attributes", {}).get("released") == "stable"


def get_last_resource_number(client, resource_type, was_released):
    resp = client.resource.read(type=resource_type, limit=100)
    for resource in resp["items"]:
        if was_released and resource.get("attributes", {}).get("released") != "stable":
            continue
        return str(resource["id"])


def set_ttl_for_resource(resource_id):
    token = get_sandbox_oauth_token()
    payload = {"name": "ttl", "value": "inf"}
    post_r = requests.post(
        "http://sandbox.yandex-team.ru/api/v1.0/resource/{id}/attribute".format(id=resource_id),
        data=json.dumps(payload),
        headers={
            "Authorization": "OAuth {token}".format(token=token),
            "Content-Type": "application/json; charset=utf-8",
            "accept": "application/json; charset=utf-8",
        },
    )
    put_r = requests.put(
        "http://sandbox.yandex-team.ru/api/v1.0/resource/{id}/attribute/{name}".format(id=resource_id, name="ttl"),
        data=json.dumps(payload),
        headers={
            "Authorization": "OAuth {token}".format(token=token),
            "Content-Type": "application/json; charset=utf-8",
            "accept": "application/json; charset=utf-8",
        },
    )
    if not put_r.ok and not post_r.ok:
        logger.error("Resource {} can not be update with your rights {}".format(resource_id, put_r.content))


def get_updated_resource_list(file_content):
    client = SandboxApi.Client()
    new_file_content = ""
    ignore_rows = set(["SET(EAGLE_RESOURCES", "TAG(sb:~intel_e5645)", "DISABLE_DATA_VALIDATION(", "DATA(", ")", ""])
    # pattern = r'^\s+?sbr://(?P<res_id>\d+?)(=\w*)?\s+?#\s+?(?P<res_name>\w+?)(\s+?#.*)?$'
    pattern = r"^\s+?sbr://(?P<res_id>\d+)(?P<exact_file>(=\w*)?)(\Z|\s+(?P<residue>.*))$"
    for row in file_content.split("\n"):
        if row not in ignore_rows:
            mobj = re.match(pattern, row)
            if mobj:
                old_resource_id = mobj.group("res_id")
                suffix = (mobj.groupdict()["residue"] or "").strip()
                resource_type, was_released = get_resource_info(client, old_resource_id)
                if resource_type == "OTHER_RESOURCE":
                    new_file_content += row + "\n"
                    continue
                new_resource_id = get_last_resource_number(client, resource_type, was_released)
                set_ttl_for_resource(new_resource_id)

                auto_filled_prefix = "  # AUTO UPDATED RESOURCE"
                resource_type_prefix = "  # " + resource_type
                common_prefix = resource_type_prefix + auto_filled_prefix
                if suffix.startswith(common_prefix.strip()):
                    suffix = "  " + suffix
                elif suffix.startswith(resource_type_prefix.strip()):
                    leng = len(resource_type_prefix.strip())
                    suffix = common_prefix + "  " + suffix[leng:].strip()
                else:
                    suffix = common_prefix + "  " + suffix
                new_row = "    sbr://" + new_resource_id + mobj.group("exact_file") + suffix.rstrip()
                released_row = " (released)" if was_released else ""
                logger.info(
                    "Resource %s: was %s, become %s%s",
                    resource_type,
                    old_resource_id,
                    new_resource_id,
                    released_row,
                )
            else:
                new_row = row
        else:
            new_row = row
        new_file_content += new_row + "\n"
    return new_file_content[:-1]


def update_yamakeinc(arc_path):
    binary_path = sys.executable
    yamakeinc_path = binary_path.split("/arcadia/")[0] + "/" + arc_path.lstrip("/")
    with open(yamakeinc_path, "r") as f_pointer:
        content = f_pointer.read()
    logger.info("updating resources for %s", arc_path)
    new_content = get_updated_resource_list(content)
    with open(yamakeinc_path, "w") as f_pointer:
        f_pointer.write(new_content)
    return


if __name__ == "__main__":
    update_yamakeinc("arcadia/ads/bsyeti/tests/eagle/canonize_ut/ya.make.inc")
