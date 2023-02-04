import logging
import os
import json

import tornado.ioloop
import tornado.web

import handlers
import push_worker

log = logging.getLogger(__name__)

ADDRESSES = ("::", "0.0.0.0")
ISS_PORT = 25536


class Application(object):
    def __init__(self, port, second_unistat_port, third_unistat_port, push_port, emulate_iss, additional_tags=''):
        self._port = port
        self._second_unistat_port = second_unistat_port
        self._third_unistat_port = third_unistat_port
        self._push_port = push_port
        self._apps_with_ports = []
        self._push_worker = None
        self._emulate_iss = emulate_iss
        self._additional_tags = additional_tags

    def start(self):
        log.info('Starting...')
        dump_json_contents = self.load_dump_json()

        if self._additional_tags:
            tags_string = dump_json_contents.setdefault('properties', {}).get('tags', '')
            dump_json_contents['properties']['tags'] = tags_string + ' ' + self._additional_tags

        main_handlers = [
            (r'/status', handlers.StatusHandler),
            (r'/unistat', handlers.UnistatHandler),
        ]
        second_app_handlers = [
            (r'/unistat_secondary', handlers.UnistatHandler),
        ]
        third_app_handlers = [
            (r'/unistat_third', handlers.UnistatHandler),
        ]

        self._apps_with_ports = [
            (tornado.web.Application(main_handlers), self._port),
            (tornado.web.Application(second_app_handlers), self._second_unistat_port),
            (tornado.web.Application(third_app_handlers), self._third_unistat_port),
        ]

        if self._emulate_iss:
            instance_name = self.make_instance_name()
            instance_info = self.make_instance_info(dump_json_contents)
            iss_handler = [
                (r'/instances/active', handlers.IssHandler, dict(
                    instance_name=instance_name,
                    instance_info=instance_info))
            ]
            self._apps_with_ports.append(
                (tornado.web.Application(iss_handler), ISS_PORT)
            )
        for app, port in self._apps_with_ports:
            for address in ADDRESSES:
                log.info("Bind to %s:%d", address, port)
                app.listen(port=port, address=address)

        ioloop = tornado.ioloop.IOLoop.current()

        all_tags = self.extract_tags(dump_json_contents)
        self._push_worker = push_worker.PushWorker(ioloop, self._push_port, all_tags)
        self._push_worker.schedule_first_push()

        ioloop.start()

    @classmethod
    def get_instance_dir(cls, default=None):
        return os.environ.get("BSCONFIG_IDIR", default)

    @classmethod
    def load_dump_json(cls):
        path = cls.get_instance_dir()
        dump_contents = {}
        if path:
            path = os.path.join(path, 'dump.json')
            try:
                with open(path) as f:
                    dump_contents = json.load(f)
            except:
                log.exception('Could not open {}'.format(path))
        else:
            log.error("Can not find instance directory")
        return dump_contents

    @classmethod
    def make_instance_name(cls):
        node_name = os.environ.get("NODE_NAME", "localhost")
        pod_id = os.environ.get("YP_POD_ID", "unknownpod")
        return "{}:{}".format(node_name, pod_id)

    @classmethod
    def make_instance_info(cls, dump_json_contents):
        def copy_field_if_exist(to_dict, from_dict, field):
            value = from_dict.get(field)
            if value is not None:
                to_dict[field] = value

        properties = {}
        instance_info = {
            "volumes": [],
            "instanceDir": cls.get_instance_dir(os.getcwd()),
            "properties": properties
        }
        copy_field_if_exist(instance_info, dump_json_contents, "configurationId")

        dump_properties = dump_json_contents.get("properties", {})
        copy_field_if_exist(properties, dump_properties, "tags")
        copy_field_if_exist(properties, dump_properties, "yasmUnistatUrl")
        copy_field_if_exist(properties, dump_properties, "monitoringYasmagentEndpoint")
        copy_field_if_exist(properties, dump_properties, "BACKBONE_IP_ADDRESS")
        copy_field_if_exist(properties, dump_properties, "HOSTNAME")
        copy_field_if_exist(properties, dump_properties, "NANNY_SERVICE_ID")
        copy_field_if_exist(properties, dump_properties, "yasmUnistatFallbackPort")
        copy_field_if_exist(properties, dump_properties, "yasmInstanceFallbackPort")

        pod_id = os.environ.get("YP_POD_ID", None)
        if pod_id:
            properties["port"] = pod_id

        return instance_info

    @classmethod
    def extract_tags(cls, dump_json_contents):
        def parse_a_tag(raw_a_tag_value):
            tag_name, tag_value = None, None
            try:
                a_mark, _sep, tag_body = raw_a_tag_value.partition("_")
                if a_mark == "a":
                    tag_name, _sep, tag_value = tag_body.partition("_")
            except:
                pass
            return tag_name, tag_value

        tags_string = dump_json_contents.get("properties", {}).get("tags", "")
        tags = {}
        for tag in tags_string.split():
            tag_name, tag_value = parse_a_tag(tag)
            if tag_name is not None and tag_value is not None and tag_name not in tags:
                tags[tag_name] = tag_value
        return tags
