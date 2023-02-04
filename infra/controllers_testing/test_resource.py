import infra.callisto.controllers.sdk.tier as tiers
import infra.callisto.controllers.sdk.resource as resource
import infra.callisto.controllers.utils.entities as entities


def test_shard_resource():
    for prefix in [
        '/web/prod/',
        '/test/',
        '/test/x/y/z/',
    ]:
        for tier in tiers.TIERS.values():
            shard = tier.make_shard((0, 0), 12345678)
            res = resource.shard_to_resource(prefix, shard)
            assert prefix in res.namespace
            assert res.name == shard.fullname
            assert resource.resource_to_shard(prefix, res) == shard


def test_chunk_resource():
    for prefix in [
        '/web/prod/',
        '/test/',
        '/test/x/y/z/'
    ]:
        for tier in tiers.TIERS.values():
            # no more than 3 components in path
            for path in [
                'path_in/shard',
                'path/in/shard',
                'path_in_shard',
            ]:
                chunk = entities.Chunk(tier.make_shard((0, 0), 12345678), path)
                res = resource.chunk_to_resource(prefix, chunk, '')
                assert prefix in res.namespace
                assert resource.resource_to_chunk(prefix, res) == chunk
