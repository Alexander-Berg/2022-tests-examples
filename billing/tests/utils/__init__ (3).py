# Import project module in only one place to avoid code duplication.
try:
    from billing.dcsaap.nirvana.vh3.operations.src import utils as project_utils  # noqa
except ImportError:
    from operations.src import utils as project_utils  # noqa
