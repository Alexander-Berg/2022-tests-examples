package db

import (
	"context"
	_ "embed"
	"io/ioutil"
	"os"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/heetch/confita"
	"github.com/heetch/confita/backend/file"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/billing/hot/accounts/mock/storagemock"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core"
	"a.yandex-team.ru/billing/hot/accounts/pkg/storage"
	libmock "a.yandex-team.ru/billing/library/go/billingo/mock"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql/backends/pg"
)

//go:embed gotest/testdata/shard_config.yaml
var testShardConfig string

func TestShardConfig(t *testing.T) {
	tmpFile, err := ioutil.TempFile("", "accounts*.yaml")
	if err != nil {
		t.Error("Failed to create temporary tmpFile", err)
	}

	//goland:noinspection GoUnhandledErrorResult
	defer os.Remove(tmpFile.Name())

	if _, err = tmpFile.WriteString(testShardConfig); err != nil {
		t.Error("Failed to write temporary tmpFile", err)
	}

	if err = tmpFile.Close(); err != nil {
		t.Error("Failed to close temporary tmpFile", err)
	}

	loader := confita.NewLoader(file.NewBackend(tmpFile.Name()))
	config := core.ShardStorageConfig{}
	if err = loader.Load(context.Background(), &config); err != nil {
		t.Error("Failed to load from config", err)
	}

	assert.Equal(t, 1, len(config.Shards))
	assert.Equal(t, int64(1), config.Shards[0].ID)
	assert.Equal(t, int64(5), config.Shards[0].IDPrefix)
	assert.Equal(t, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaab", config.Shards[0].MinKey)
	assert.Equal(t, "ffffffffffffffffffffffffffffffffffffffff", config.Shards[0].MaxKey)
	assert.Equal(t, "accountsdb", config.Shards[0].Storage.Name)
	assert.Equal(t, "accounts", config.Shards[0].Storage.User)
}

func TestShardsRegister(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	shard := storagemock.NewMockShard(ctrl)
	shards := map[int64]storage.Shard{
		0: shard,
	}
	shardProvider := NewShardStorage(shards)

	connector := libmock.NewMockConnector(ctrl)
	connector.EXPECT().
		Register(gomock.Eq(shard))

	shardProvider.Register(connector)
}

func TestMultipleShards(t *testing.T) {
	shardsMap := make(map[int64]storage.Shard)
	shardsMap[1] = NewShard(pg.NewCluster(pg.PgBackendConfig{}),
		1, 0, "a", "g")
	shardsMap[2] = NewShard(pg.NewCluster(pg.PgBackendConfig{}),
		2, 2, "h", "v")
	shardStorage := NewShardStorage(shardsMap)

	assert.Equal(t, int64(0), shardsMap[1].GetIDPrefix())
	assert.Equal(t, int64(2000000000000000), shardsMap[2].GetIDPrefix())
	assert.Equal(t, int64(2999999999999999), shardsMap[2].GetMaxSeqID())

	for i := 0; i < 2; i++ {
		actualShard, err := shardStorage.GetLastShard()
		if err != nil {
			t.Fatal(err)
		}
		assert.Equal(t, shardsMap[2], actualShard)
	}

	shardIDs, err := shardStorage.GetShardIDs()
	if err != nil {
		t.Fatal(err)
	}
	assert.ElementsMatch(t, shardIDs, []int64{1, 2})

	shard, err := shardStorage.GetShard("a")
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, shardsMap[1], shard)

	shard, err = shardStorage.GetShard("g")
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, shardsMap[1], shard)

	shard, err = shardStorage.GetShard("c")
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, shardsMap[1], shard)

	shard, err = shardStorage.GetShard("h")
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, shardsMap[2], shard)

	shard, err = shardStorage.GetShard("m")
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, shardsMap[2], shard)

	shard, err = shardStorage.GetShard("x")
	assert.Nil(t, shard)
	if assert.Error(t, err) {
		assert.Equal(t, err.Error(), "Shard for key \"x\" not found")
	}
}
