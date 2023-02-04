B2BGEO Generic Load Testing
===========================

Low-RPS load testing tool.
It's generally recommended to test service instances rather than a balancer: please read
`this post <https://neverov.at.yandex-team.ru/986>`_ for more info.

USAGE
-----
This can be used either as a standalone CLI program or as a library, e.g. one can create a stress test as a part of
a service's system/integration tests using classes from this library.

Requests template can be described either in json format or directly in Python code. The format follows
"Python requests" parameters, here is an example of a ``PATCH`` request::

    {
        "method": "patch",
        "url": "https://test.courier.yandex.ru/api/v1/companies/7/orders/123",
        "data": {
            "phone": null,
            "status": "confirmed"
        },
        "timeout": 10,
        "verify": false
    }

But that was an example of a complete request, rather than a template. There are several modules that enable
parametrization of requests. You can see several basic blocks described below.

OneOf
~~~~~
Random choice from a list. For more complicated distribution specify ``weights`` parameter. Possible values could be both simple types as well as sub-templates.

Python example::

    OneOf(["driving", "walking", "transit"], weights=[0.3, 0.2, 0.5])

JSON representation::

    "mode": {
        "_type_": "OneOf",
        "values": ["driving", "walking", "transit"],
        "weights": [0.3, 0.2, 0.5]
    }

Linspace
~~~~~~~~
A random value within [``min_value``: ``max_value``] with some ``step``. ``weights`` parameter is optional.

Python example::

   Linspace(min_value=1, max_value=4, step=1, weights=[0.3, 0.2, 0.4, 0.1])  # possible values: {1, 2, 3, 4}

JSON representation::

    {
        "_type_": "Linspace",
        "min_value": 1,
        "max_value": 10,
        "step": 1,
        "weights": [0.3, 0.2, 0.4, 0.1]
    }

Settings
~~~~~~~~
Format a string with values provided in a dictionary passed at the runtime.

Python Example::

    Settings("{backend}/distancematrix")

JSON representation::

    "apikey": {
        "_type_": "Settings",
        "str_template": "{apikey}"
    }

Misc.
~~~~~
Further parametrization modules could be found in ``load_testing/lib/parametrization.py``.
Usage examples: here ``load_testing/tests/test_utils.py`` or ``load_testing/scenarios/distancematrix.json``.

Example of a ping request
~~~~~~~~~~~~~~~~~~~~~~~~~

json template::

    {
        "method": "GET",
        "url": {
            "_type_": "Settings",
            "str_template": "{backend}/api/v1/ping"
        },
        "timeout": 5,
        "verify": false
    }


CLI
---
CLI tool tests a service provided as URL at a given RPS rate and then prints statistics on status code, query timings
and exceptions (if any).
One has to either explicitly specify number of threads to be used or provide an expected average response time (then
the number of threads will be deduced automatically). In either case the specified RPS is not guaranteed to be reached,
since this also depends a lot on a service itself.
It is important that *request template* contains a timeout, default value is 3 times the provided
*mean expected response time* or 10 seconds if no expected response time was provided.

Build::

    arcadia/maps/b2bgeo/tools/load_testing$ ya make -rA

Usage example::

    $ ./load_testing -i scenarios/distancematrix.json  --rps 30 \
            --url 'routing-public-api-stable-1.iva.yp-c.yandex.net' \
            --apikey XXX --duration 25 --response-time 1.5

Output example::

    2019-12-04 18:36:16,832 - root - INFO - Preparing data
    2019-12-04 18:36:16,832 - root - INFO - Num threads: 54, target rps per thread: 0.56. Num processes: 1, max threads per process: 200. Total RPS: 30.00
    2019-12-04 18:36:16,884 - root - INFO - Preparation complete.
    2019-12-04 18:36:16,900 - root - INFO - Working...
    2019-12-04 18:36:41,908 - root - INFO - Waiting for workers being stopped...
    2019-12-04 18:36:43,098 - root - INFO - Finished.
    2019-12-04 18:36:43,100 - root - INFO - ====
    Load testing results:

    2019-12-04 18:36:43,100 - root - INFO - Status codes: {
      "200": {
        "amount": 756,
        "msg": "<omitted>"
      }
    };
    2019-12-04 18:36:43,100 - root - INFO - Response time traits [seconds]: {
      "min": 0.145,
      "max": 0.566,
      "mean": 0.284,
      "median": 0.27,
      "p99": 0.495
    };
    2019-12-04 18:36:43,100 - root - INFO - Response time histogram [seconds]: {
      "[0.00 - 0.15)": 1,
      "[0.15 - 0.30)": 468,
      "[0.30 - 0.45)": 263,
      "[0.45 - 0.59)": 24
    };
    2019-12-04 18:36:43,100 - root - INFO - Target RPS: 30.0, actually observed RPS: 30.85


To generate Lunapark ammo --generate-ammo argument should be specified. Lunapark config will be saved to ``load.yaml``.
The ammo will be stored in ``ammofile.txt``.

Usage example::

    $ ./load_testing -i scenarios/route.json --rps 50 --url 'api.routing.yandex.net' \
            --apikey XXX --duration 10 --generate-ammo


Python module
-------------
Classes in ``load_testing/lib/*.py`` can be reused. The CLI tool itself is a good usage example.


Directions of further development
---------------------------------

* [Core] Create "Sequence" parametrization module to make chained queries possible.
* [CLI] Make the cli tool capable to read python-described scenarios.
* [Core] Make ``LoadTester`` class capable to accept response validator (via its ``Config``).
* [CLI] Automatically determine max RPS a service is capable to withstand without hitting timeouts.
* [CLI] Introduce true multiprocessing or try out async/await. Alternatively, develop a high-performing backend, e.g. written
  in C++. Either way, it's preferred to keep the Python frontend around because it allows more flexible requests
  preparation process as well as statistics tools (e.g. in Python it's very simple to extend make this tool by drawing
  plots.
