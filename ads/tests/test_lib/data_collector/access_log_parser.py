from collections import defaultdict
from multiprocessing import Pool
from six.moves.urllib.parse import unquote

import yabs.logger as logger
from ads.bsyeti.tests.test_lib.data_collector.config import ALLOWED_QUERY_KEYS


def filter_row(parts):
    if len(parts) < 5:
        logger.debug("filtered by parts count: %s", parts)
        return
    if parts[4] != "200":
        logger.debug("filtered by http code: %s : %s", parts[3], parts[4])
        return False
    if not parts[3].startswith("/bigb?"):
        logger.debug("filtered by path: %s : %s", parts[3], parts[4])
        return False
    return True


def parse_params(parts):
    _, query_part = parts[3].split("?", 1)
    parts_generator = (tuple(pair.split("=", 1)) for pair in query_part.split("&"))
    filter_generator = ((pair[0], unquote(pair[1])) for pair in parts_generator if pair[0] in ALLOWED_QUERY_KEYS)
    return list(filter_generator)


def parser_generator(f_p):
    for row in f_p.readlines():
        parts = row.split("\t")
        if filter_row(parts):
            params = parse_params(parts)
            clients_params = [i for i in params if i[0] in ("client", "keyword-set")]
            if len(clients_params) < 1:
                logger.debug("strange no client, %s : %s", parts[3], parts[4])
                clients_params = [["keyword-set", "1"]]
            if len(clients_params) > 1:
                ks_value = [i[1] for i in clients_params if i[0] == "keyword-set"][0]
                if ks_value.isdigit() and 0 <= int(ks_value) <= 30:
                    logger.warning(
                        "skipped by too much client params: %s : %s : %s",
                        clients_params,
                        parts[3],
                        parts[4],
                    )
            elif len(clients_params) == 1:
                client_name = clients_params[0][1]
                if clients_params[0][0] == "keyword-set":
                    client_name = "ks_" + client_name
                yield client_name, params


def parse_file(filename):
    with open(filename, "rb") as f_p:
        logger.info("Started parsing %s", filename)
        rows = defaultdict(list)
        for client, params in parser_generator(f_p):
            rows[client].append(params)
        logger.info("File parsed %s, clients count %d", filename, len(rows))
    return rows


def parse_access_logs(filenames):
    proc_pool = Pool(len(filenames))
    rows = defaultdict(list)
    for rows_part in proc_pool.imap_unordered(parse_file, filenames):
        for client, params in rows_part.items():
            rows[client] += params
    logger.info("All files parsed, total clients = %d", len(rows))
    return rows
