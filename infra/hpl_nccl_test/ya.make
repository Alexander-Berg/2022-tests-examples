PY3_PROGRAM()

PEERDIR(
    yt/python/client
    library/python/vault_client
    infra/yt/lib/yt_ssh_swarm/sshd
    infra/yt/lib/yt_ssh_swarm/cypress_synchronizer
    infra/yt/lib/yt_ssh_swarm/hpl_tools
)

PY_SRCS(
    __main__.py
    layers.py
)

END()
