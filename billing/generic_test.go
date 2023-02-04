package ytreferences

import (
	"context"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/processor/pkg/storage/ytreferences/mocks"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	"a.yandex-team.ru/library/go/core/metrics/solomon"
)

type testGenericReferenceSuite struct {
	btesting.BaseSuite
	ctrl      *gomock.Controller
	ctx       context.Context
	client    *mocks.MockClient
	table     YtDynamicTable
	keyColumn string
	ref       GenericReference
}

func (s *testGenericReferenceSuite) SetupTest() {
	s.ctrl = gomock.NewController(s.T())
	registry := solomon.NewRegistry(nil)

	s.ctx = context.Background()
	s.client = mocks.NewMockClient(s.ctrl)
	s.keyColumn = "client_id"

	s.table = YtDynamicTable{
		path: "//test/table",
		config: YtDynamicTableConfig{
			TablePath: "//test/table",
			KeyColumn: s.keyColumn,
		},
		clients: []*Client{
			{
				cluster:  "hahn",
				client:   s.client,
				registry: registry,
			},
		},
	}

	s.ref = GenericReference{YtDynamicTable: s.table}
}

func (s *testGenericReferenceSuite) TestGenericReference_Get() {
	tableReader := mocks.NewMockTableReader(s.ctrl)
	keys := []string{"1", "5", "7"}
	n := len(keys)
	// проверим что ключи формируются правильно
	wantKeys := make([]any, n)
	for i, key := range keys {
		wantKeys[i] = &map[string]any{s.keyColumn: key}
	}

	s.client.EXPECT().LookupRows(gomock.Any(), gomock.Eq(s.table.path), gomock.Eq(wantKeys), gomock.Any()).Return(tableReader, nil)
	// 3 элемента + 1 false
	tableReader.EXPECT().Next().Times(n + 1).DoAndReturn(
		func() bool {
			n--
			return n >= 0
		})
	// на каждый элемент зовем scan
	tableReader.EXPECT().Scan(gomock.Any()).Times(n)
	// по итогам - читатель должен быть закрыт
	tableReader.EXPECT().Close()
	tableReader.EXPECT().Err().Return(nil)
	rows, err := s.ref.Get(s.ctx, keys)
	require.NoError(s.T(), err)
	// проверим что пустые мапы добавились в список
	assert.Len(s.T(), rows, len(keys))
}

func (s *testGenericReferenceSuite) TestGenericReference_Select() {
	tableReader := mocks.NewMockTableReader(s.ctrl)
	keyName := "client_id"
	keyValue := int64(10)

	wantQuery := " * FROM [//test/table] WHERE client_id = 10"
	s.client.EXPECT().SelectRows(gomock.Any(), gomock.Eq(wantQuery), gomock.Any()).Return(tableReader, nil)
	n := 1
	// 1 элемент + 1 false
	tableReader.EXPECT().Next().Times(n + 1).DoAndReturn(
		func() bool {
			n--
			return n >= 0
		})
	// на каждый элемент зовем scan
	tableReader.EXPECT().Scan(gomock.Any()).Times(n)
	// по итогам - читатель должен быть закрыт
	tableReader.EXPECT().Close()
	tableReader.EXPECT().Err().Return(nil)
	rows, err := s.ref.Select(s.ctx, keyName, keyValue)
	require.NoError(s.T(), err)
	assert.Len(s.T(), rows, 1)
}

func TestGetAccounterLockForSuite(t *testing.T) {
	suite.Run(t, &testGenericReferenceSuite{})
}

func TestParam_getValue(t *testing.T) {
	tests := []struct {
		testname  string
		types     map[string]string
		rows      []map[string]any
		want      []map[string]any
		withError bool
	}{
		{
			testname: "type fields",
			types: map[string]string{
				"a": "string",
				"b": "int64",
				"c": "bool",
			},
			rows: []map[string]any{
				{
					"a": []byte("hello"),
					"b": "1000",
					"c": "true",
				},
				{
					"a": []byte("world"),
					"b": "2000",
					"c": "F",
				},
			},
			want: []map[string]any{
				{
					"a": "hello",
					"b": int64(1000),
					"c": true,
				},
				{
					"a": "world",
					"b": int64(2000),
					"c": false,
				},
			},
		},
		{
			testname: "unknown type",
			types: map[string]string{
				"a": "string!",
			},
			rows: []map[string]any{
				{
					"a": []byte("hello"),
				},
			},
			withError: true,
		},
		{
			testname: "unknown field",
			types: map[string]string{
				"a": "string",
			},
			rows: []map[string]any{
				{
					"b": []byte("hello"),
				},
			},
			withError: true,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			table := YtDynamicTable{
				config: YtDynamicTableConfig{Types: test.types},
			}

			ref := GenericReference{YtDynamicTable: table}

			actual, err := ref.typeRows(test.rows)
			if test.withError {
				require.Error(t, err)
			} else {
				require.NoError(t, err)
				assert.Equal(t, test.rows, actual)
			}
		})
	}
}
