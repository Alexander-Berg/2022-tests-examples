def configure_nameservers(dns_client_mock, nameserver_data):
    nameserver_data = nameserver_data or {}
    for record_type in ["A", "AAAA", "PTR"]:
        if record_type not in nameserver_data:
            nameserver_data[record_type] = {}

    def lookup_a(left):
        return nameserver_data["A"].get(left, None)

    def lookup_aaaa(left):
        return nameserver_data["AAAA"].get(left, None)

    def lookup_ptr(left):
        return nameserver_data["PTR"].get(left, None)

    dns_client_mock().get_a.side_effect = lookup_a
    dns_client_mock().get_aaaa.side_effect = lookup_aaaa
    dns_client_mock().get_ptr.side_effect = lookup_ptr
