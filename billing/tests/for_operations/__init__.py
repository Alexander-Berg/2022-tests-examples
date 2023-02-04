# Import project module in only one place to avoid code duplication.
try:
    import billing.dcsaap.nirvana.vh3.operations as project_operations  # noqa
except ImportError:
    import operations as project_operations  # noqa
