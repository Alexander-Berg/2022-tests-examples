package ytreferences

import (
	"context"
	"fmt"
	"os"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/stretchr/testify/suite"

	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	"a.yandex-team.ru/yt/go/migrate"
	"a.yandex-team.ru/yt/go/schema"
	"a.yandex-team.ru/yt/go/yson"
	"a.yandex-team.ru/yt/go/yttest"
)

type testSuite struct {
	btesting.BaseSuite
}

type ContractRow struct {
	ID      uint64         `yson:"id,key"`
	Version uint64         `yson:"version"`
	Obj     map[string]any `yson:"obj"`
}

func (s *testSuite) TestContractReference_Get() {
	exmpCon1 := `{
    client_id = 1349012089;
    collaterals = {
        "0" = { "attribute_batch_id" = 14339965 }
    };
    external_id = "ОФ-104447666";
    id = 666;
    passport_id = 16571028;
    person_id = 13158511;
    type = "SPENDABLE";
    update_dt = "2020-11-24T02:48:24";
    version_id = 1
}`
	exmpCon2 := `{
    client_id = 1349008667;
    collaterals = {
        "0" = { "attribute_batch_id" = 143399667 }
    };
    external_id = "ОФ-104447667";
    id = 667;
    passport_id = "16571028666666666666666666666666666666666666";
    person_id = 13158511;
    type = "SPENDABLE";
    update_dt = "2020-11-24T02:48:24";
    version_id = 2
}`
	s.T().Parallel()

	env, cancel := yttest.NewEnv(s.T())
	defer cancel()

	ctx, cancel := context.WithTimeout(context.Background(), time.Second*15)
	defer cancel()

	testTable := env.TmpPath().Child("_balance_contract")
	require.NoError(s.T(), migrate.Create(env.Ctx, env.YT, testTable, schema.MustInfer(&ContractRow{})))
	require.NoError(s.T(), migrate.MountAndWait(env.Ctx, env.YT, testTable))

	var con1 map[string]any
	err := yson.Unmarshal([]byte(exmpCon1), &con1)
	require.NoError(s.T(), err)

	var con2 map[string]any
	err = yson.Unmarshal([]byte(exmpCon2), &con2)
	require.NoError(s.T(), err)

	rows := []any{
		&ContractRow{uint64((con1["id"]).(int64)), uint64((con1["version_id"]).(int64)), con1},
		&ContractRow{uint64((con2["id"]).(int64)), uint64((con2["version_id"]).(int64)), con2},
	}

	tx, err := env.YT.BeginTabletTx(ctx, nil)
	require.NoError(s.T(), err)

	err = tx.InsertRows(env.Ctx, testTable, rows, nil)
	require.NoError(s.T(), err)

	err = tx.Commit()
	require.NoError(s.T(), err)

	contractRefs, err := NewContractReference(
		YtDynamicTableConfig{
			Clusters:       []string{os.Getenv("YT_PROXY")},
			AuthToken:      os.Getenv("YT_TOKEN"),
			TablePath:      testTable.String(),
			KeyColumn:      DefaultKeyColumn,
			ClientType:     DefaultClientType,
			ClusterTimeout: DefaultClusterTimeout,
		},
		s.Registry(),
	)
	require.NoError(s.T(), err)

	actualContracts, err := contractRefs.Get(ctx, []uint64{666, 667})
	require.NoError(s.T(), err)
	var contract1, contract2 Contract
	err = decodeToStruct(con1, &contract1)
	require.NoError(s.T(), err)
	err = decodeToStruct(con2, &contract2)
	require.NoError(s.T(), err)
	assert.Equal(s.T(), []Contract{contract1, contract2}, actualContracts)

	for _, contract := range actualContracts {
		fmt.Printf("contract: %v\n", contract)
	}

	genericContractRefs, err := NewGenericReference(
		YtDynamicTableConfig{
			Clusters:       []string{os.Getenv("YT_PROXY")},
			AuthToken:      os.Getenv("YT_TOKEN"),
			TablePath:      testTable.String(),
			KeyColumn:      DefaultKeyColumn,
			ClientType:     DefaultClientType,
			ClusterTimeout: DefaultClusterTimeout,
		},
		s.Registry(),
	)
	require.NoError(s.T(), err)

	actualContractsGenerics, err := genericContractRefs.Get(ctx, []uint64{666, 667})
	require.NoError(s.T(), err)
	for _, contract := range actualContractsGenerics {
		fmt.Printf("contract as generic: %v\n", contract)
	}
}

func TestContractReference(t *testing.T) {
	suite.Run(t, &testSuite{})
}
