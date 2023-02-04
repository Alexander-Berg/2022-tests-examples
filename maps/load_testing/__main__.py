import logging
import random
import json
from argparse import ArgumentParser
from .lib.ammo_generator import AmmoGenerator
from .lib.parametrization import RequestsProvider
from .lib.tester import LoadTester, Config

logging.basicConfig(format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger()


def parse_args():
    parser = ArgumentParser(description="Parametrized load testing tool")
    parser.add_argument('-i', '--scenario', required=True, help='JSON file with parametrized scenario data')
    parser.add_argument('-r', '--rps', type=float, required=True, help='Target rps')
    parser.add_argument('-u', '--url', required=True, help='Service endpoint prefix')
    parser.add_argument('-a', '--apikey', help='Valid OAuth token relevant for service in the scenario')
    parser.add_argument('-d', '--duration', default=10, type=int,
                        help='Prepare load for this amount of time in seconds')

    parser.add_argument('-s', '--response-time', type=float, default=1,
                        help='Give a pessimistic hint on expected response time. '
                             'It is used to compute number of threads.')
    parser.add_argument('-t', '--threads-per-process', type=int, default=200,
                        help='Number of threads per process. Too many sessions per one process cause read timeouts.')
    parser.add_argument('-g', '--generate-ammo', action='store_true', help='Generate shooting ammo for the Lunapark. Shooting will not be done.')
    parser.add_argument('-v', '--verbose', action='store_true', help='Print more info')
    return parser.parse_args()


def main():
    random.seed()
    args = parse_args()
    assert args.apikey is not None
    assert args.duration >= 5
    assert args.rps > 0.

    logger.setLevel(level=logging.DEBUG if args.verbose else logging.INFO)
    logging.getLogger("urllib3").setLevel(logging.DEBUG if args.verbose else logging.WARNING)

    logger.info("Preparing data")
    with open(args.scenario, 'r') as fin:
        requests_provider = RequestsProvider.load(json.load(fin))
    settings = {
        'backend': args.url,
        'apikey': args.apikey
    }
    config = Config(args.rps, requests_provider, settings, args.duration, args.response_time, args.threads_per_process)

    if args.generate_ammo:
        generator = AmmoGenerator(config)
        generator.create_ammo_file()
        generator.create_config_yaml()
        return 0

    tester = LoadTester(config)

    logger.info("Preparation complete.")

    results = tester.run()

    logging.info("====\nLoad testing results:\n")
    logging.info(f"Status codes: {json.dumps(results.status_code_hist, indent=2)};")
    logging.info(f"Response time traits [seconds]: {json.dumps(results.response_time_traits, indent=2)};")
    logging.info(f"Response time histogram [seconds]: {json.dumps(results.response_time_hist, indent=2)};")
    logging.info(f"Target RPS: {args.rps}, actually observed RPS: {results.observed_rps}")
    if results.exceptions_hist:
        logging.info(f"The following exceptions occurred: {json.dumps(results.exceptions_hist, indent=2)};")


if __name__ == "__main__":
    main()
