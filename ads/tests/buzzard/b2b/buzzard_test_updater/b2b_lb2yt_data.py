import os
import struct
from subprocess import check_call

from hashlib import md5
import yabs.logger as logger
from .bindings import grep_requests
from yt.wrapper import YtClient, yson


def create_local_tables(chunks, output_f, requests_dir):
    check_call(["rm", "-rf", requests_dir])
    check_call(["mkdir", "-p", requests_dir])

    grep_requests(chunks, requests_dir.encode())

    logger.info("Get data from YT")
    check_call(["rm", "-rf", output_f])
    check_call(["mkdir", "-p", output_f])

    yt_client = YtClient(proxy="seneca-sas", token=os.environ.get("YT_TOKEN"))

    def ids2lookup(field, to_type=int):
        def do_lookup(table_path, ids):
            return yt_client.lookup_rows(table_path, [{field: to_type(_id)} for _id in ids])

        return do_lookup

    def str_arg(value):
        return '"' + str(value).replace('"', '\\"') + '"'

    def int_arg(value):
        return str(int(value))

    def uint_arg(value):
        return str(int(value)) + "u"

    def uint_yabs_md5(value):
        m = md5(value.encode())

        arr = struct.unpack(">4L", m.digest())
        hi = arr[1] ^ arr[3]
        lo = arr[0] ^ arr[2]
        return str((hi << 32) | lo) + "u"

    def ids2select(field, to_arg=str_arg):
        def do_select(table_path, ids):
            return yt_client.select_rows(
                "* FROM [{}] WHERE {} IN ({})".format(table_path, field, ",".join(to_arg(_id) for _id in ids))
            )

        return do_select

    def resource_postprocess(response_record):
        # BANNER_APPLICATION_RESOURCE
        # https://a.yandex-team.ru/arc/trunk/arcadia/ads/bsyeti/libs/yt_fetcher/banner_application.h?rev=3450071#L10
        # 'ID=>123,TMPL_ID=>777'
        if int(response_record["ResourceNo"]) == 61 and response_record["Attr"] != "":
            with open("{}/App".format(requests_dir), "a") as new_requests:
                for app_id in [s.split("=>")[1] for s in response_record["Attr"].split(",") if s.startswith("ID=>")]:
                    new_requests.write(app_id + "\n")

    TABLES = [
        {
            "name": "CaesarBanners",
            "path": "//home/bigb/caesar/stable/Banners",
            "keys2get": ids2lookup("BannerID"),
        },
        {
            "name": "Phrase",
            "path": "//yabs/PhraseDict",
            "keys2get": ids2lookup("PhraseID"),
        },
        {
            "name": "CaesarPhrases",
            "ids_file_name": "Phrase",
            "path": "//home/bigb/caesar/stable/Phrases",
            "keys2get": ids2lookup("PhraseID"),
        },
        {
            "name": "CaesarBroadPhrases",
            "ids_file_name": "Phrase",
            "path": "//home/bigb/caesar/stable/BroadPhrases",
            "keys2get": ids2lookup("BroadPhraseID"),
        },
        {
            "name": "StoreApp",
            "path": "//home/bigb/caesar/stable/MobileAppsDict",
            "keys2get": ids2select("BundleId", str_arg),
        },
        {
            "name": "CaesarOffer",
            "path": "//home/bigb/caesar/stable/OfferUrlDict",
            "keys2get": ids2select("NormalizeUrlMD5", uint_yabs_md5),
        },
    ]

    class RequestsProcessor:
        def __init__(self):
            self.requests_buffer = []
            self.requests_buffer_size = 100
            self.total_req = 0
            self.total_resp = 0

        def flush(self, table_desc):
            self.total_req += len(self.requests_buffer)
            for response in table_desc["keys2get"](table_desc["path"], self.requests_buffer):
                self.total_resp += 1
                for hash_field in table_desc["expr_fields"]:
                    if hash_field in response:
                        del response[hash_field]
                if "postprocess" in table_desc:
                    table_desc["postprocess"](response)
                yield response
            self.requests_buffer = []

        def process(self, requests_file, table_desc):
            with open(requests_file, "r") as requests:
                for request in requests:
                    self.requests_buffer.append(request.strip())
                    if len(self.requests_buffer) >= self.requests_buffer_size:
                        for response in self.flush(table_desc):
                            yield response
                if len(self.requests_buffer) > 0:
                    for response in self.flush(table_desc):
                        yield response

            logger.info("{} / {}".format(self.total_resp, self.total_req))
            self.total_req = 0
            self.total_resp = 0

    req_processor = RequestsProcessor()

    for table_desc in TABLES:
        table_name = table_desc["name"]
        ids_file_name = table_desc.get("ids_file_name", table_name)

        check_call(
            "sort -u {dir}/{ids_file_name} > {dir}/{file}.uniq && mv {dir}/{file}.uniq {dir}/{file}".format(
                dir=requests_dir, ids_file_name=ids_file_name, file=table_name
            ),
            shell=True,
        )

        logger.info(table_name)
        with open("{}/{}.yson".format(output_f, table_name), "wb") as table_file:
            table_schema = yt_client.get_attribute(table_desc["path"], "schema")
            table = {
                "path": table_desc["path"],
                "attributes": {
                    "schema": table_schema,
                    "dynamic": True,
                },
            }
            table_desc["expr_fields"] = []
            for field in table_schema:
                if field["required"]:
                    field["required"] = "true"
                else:
                    field["required"] = "false"
                if "expression" in field:
                    table_desc["expr_fields"].append(field["name"])
            yson.dump(table, table_file, yson_format="text")

        with open("{}/{}.data".format(output_f, table_name), "wb") as table_data_file:
            table_data_file.write(b"[")
            for record in req_processor.process("{}/{}".format(requests_dir, table_name), table_desc):
                table_data_file.write(yson.dumps(record, yson_format="text"))
                table_data_file.write(b";\n")
            table_data_file.write(b"]")

    logger.info("YT_REQUESTS DONE\n")
