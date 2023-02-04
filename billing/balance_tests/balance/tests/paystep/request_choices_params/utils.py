def get_test_case_name(prefix, context, with_agency, with_contract, pay_method, **extra):
    extra_suffix = ""
    if extra:
        extra_suffix = "_" + "_".join(
            "{}:{}".format(k, v)
            for k, v in extra.items()
        )

    return "{prefix}_{person_category}_{agency}_{contract}_{pay_method}".format(
        prefix=prefix,
        person_category=context.person_type.code,
        agency="agent" if with_agency else "not_agent",
        contract="contract" if with_contract else "not_contract",
        pay_method=pay_method,
    ) + extra_suffix
