from collections import defaultdict
from itertools import combinations
from random import shuffle

import yabs.logger as logger
from ads.bsyeti.tests.test_lib.data_collector.config import (
    ADS_SERVER_CLIENTS,
    QUERY_IDS_PARAMS,
    QUERY_IDS_PARAMS_PRIORITY,
    YABS_MAIN_CLIENTS,
)


def filter_global_requests(rows):
    new_queries = []
    queries_list = rows.values()
    shuffle(queries_list)

    logger.info("Creating tuples for common requests")
    forbidden_requests = set()  # because we dont want to duplicate requests

    arg_combs = defaultdict(int)
    unused_combinations = set()

    for queries in queries_list:  # step one: getting existing combinations
        for query in queries:
            filtered_names_gen = (arg[0] for arg in query if arg[0] in QUERY_IDS_PARAMS)
            sorted_names = sorted(filtered_names_gen, key=QUERY_IDS_PARAMS_PRIORITY.get)
            arg_combs[tuple(sorted_names)] += 1

    for keys in arg_combs.keys():  # step two: creating new combinations
        for length in range(1, len(keys)):
            for current_args in combinations(keys, length):
                target = tuple(current_args)
                if arg_combs.get(tuple(current_args)) is None:
                    unused_combinations.add(target)

    logger.debug(
        "Common requests, total %d existing and %d created arglists",
        len(arg_combs),
        len(unused_combinations),
    )

    for queries in queries_list:  # step three: adding requests with created combinations
        for query in queries:
            filtered_names_set = set(arg[0] for arg in query if arg[0] in QUERY_IDS_PARAMS)
            to_discard = set()
            for comb in unused_combinations:
                target_names = set(comb)
                if target_names.issubset(filtered_names_set):  # we found this one
                    filtered_args_gen = (arg for arg in query if arg[0] in target_names)
                    sorted_args = sorted(filtered_args_gen, key=QUERY_IDS_PARAMS_PRIORITY.get)
                    new_queries.append(sorted_args)
                    to_discard.add(comb)
            unused_combinations -= to_discard
            if not unused_combinations:
                break

    logger.debug("Common requests, total found %d requests with created arglists", len(new_queries))

    for key, val in arg_combs.items():
        arg_combs[key] = min(val, 3)

    for queries in queries_list:  # step four: adding requests with existing combinations
        for query in queries:
            filtered_names_gen = (arg[0] for arg in query if arg[0] in QUERY_IDS_PARAMS)
            sorted_names = sorted(filtered_names_gen, key=QUERY_IDS_PARAMS_PRIORITY.get)
            if arg_combs[tuple(sorted_names)] > 0:
                filtered_args_gen = (arg for arg in query if arg[0] in target_names)
                sorted_args = sorted(filtered_args_gen, key=QUERY_IDS_PARAMS_PRIORITY.get)
                if tuple(sorted_args) not in forbidden_requests:
                    forbidden_requests.add(tuple(sorted_args))
                    new_queries.append(sorted_args)
                    arg_combs[tuple(sorted_names)] -= 1

    logger.info("Filtered %s queries for common requests", len(new_queries))
    return new_queries


def filter_per_client_requests(rows):
    result = {}
    for client, queries in rows.items():
        logger.debug("Creating tuples for %s client", client)
        limit = 15
        if client in YABS_MAIN_CLIENTS:
            limit = 500
        elif client in ADS_SERVER_CLIENTS:
            limit = 120

        new_queries = []

        if limit >= len(queries):  # if there is little amount of requests
            for query in queries:
                filtered_args_gen = (arg for arg in query if arg[0] in QUERY_IDS_PARAMS)
                sorted_args = sorted(filtered_args_gen, key=QUERY_IDS_PARAMS_PRIORITY.get)
                new_queries.append(sorted_args)
            result[client] = new_queries
            logger.info("Getting all %s queries for %s client", len(new_queries), client)
            continue

        shuffle(queries)
        forbidden_requests = set()  # because we dont want to duplicate requests
        arg_combs = defaultdict(int)

        for query in queries:  # calc stats for queryargs
            filtered_names_gen = (arg[0] for arg in query if arg[0] in QUERY_IDS_PARAMS)
            sorted_names = sorted(filtered_names_gen, key=QUERY_IDS_PARAMS_PRIORITY.get)
            arg_combs[tuple(sorted_names)] += 1

        logger.debug(
            "Client %s, total %d existing request type and %d requests total",
            client,
            len(arg_combs),
            len(queries),
        )

        new_arg_combs = defaultdict(int)
        top_sum = float(sum(arg_combs.values()))
        assert top_sum == len(queries)

        for key, val in arg_combs.items():
            new_arg_combs[key] = max(int((val / top_sum) * limit), 1)
        new_top_sum = sum(new_arg_combs.values())
        logger.debug("Client %s: limit is %s, candidates is %s", client, limit, new_top_sum)

        for query in queries:
            filtered_names_gen = (arg[0] for arg in query if arg[0] in QUERY_IDS_PARAMS)
            sorted_names = sorted(filtered_names_gen, key=QUERY_IDS_PARAMS_PRIORITY.get)
            if new_arg_combs[tuple(sorted_names)] > 0 or new_top_sum < limit:
                filtered_args_gen = (arg for arg in query if arg[0] in QUERY_IDS_PARAMS)
                sorted_args = sorted(filtered_args_gen, key=QUERY_IDS_PARAMS_PRIORITY.get)
                if tuple(sorted_args) not in forbidden_requests:
                    forbidden_requests.add(tuple(sorted_args))
                    new_queries.append(sorted_args)
                    if new_arg_combs[tuple(sorted_names)] > 0:
                        new_arg_combs[tuple(sorted_names)] -= 1
                    else:
                        new_top_sum += 1

        result[client] = new_queries
        logger.info(
            "Filtered %d queries for %s client (total %d requests)",
            len(new_queries),
            client,
            len(queries),
        )
    return result
