# Import project module in only one place to avoid code duplication.
try:
    import billing.dcsaap.nirvana.vh3.graphs as project_graphs  # noqa
except ImportError:
    import graphs as project_graphs  # noqa
