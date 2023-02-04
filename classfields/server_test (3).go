package app

import (
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/pb/shiva/api/hprof"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/golang/protobuf/ptypes"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

func TestServer_Get(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	require.NoError(t, db.GormDb.AutoMigrate(Dump{}))
	fixtures := fixture(time.Now())
	for _, dump := range fixtures {
		db.GormDb.Create(&dump)
	}

	hprofStore := NewHprofStore(NewDumpStorage(db, test.NewLogger(t)), NewConf(), test.NewLogger(t))

	expected := fixtures[0]
	resp, err := hprofStore.Get(nil, &hprof.GetRequest{Id: 1})
	require.NoError(t, err)
	require.Equal(t, expected.Id, resp.Dump.Id)
	require.Equal(t, expected.ServiceName, resp.Dump.ServiceName)
	require.Equal(t, expected.Version, resp.Dump.Version)
	require.Equal(t, expected.Canary, resp.Dump.Canary)
	require.Equal(t, expected.Branch, resp.Dump.Branch)
	require.Equal(t, "http://127.0.0.1/hprof/1", resp.Dump.DumpUrl)
	require.Equal(t, expected.AllocationId, resp.Dump.AllocationId)
	require.Equal(t, expected.Host, resp.Dump.Host)
	require.Equal(t, layer.Layer_PROD, resp.Dump.Layer)
	require.NotNil(t, resp.Dump.Created)
}

func TestServer_List(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	require.NoError(t, db.GormDb.AutoMigrate(Dump{}))
	now := time.Now()
	for _, dump := range fixture(now) {
		db.GormDb.Create(&dump)
	}

	hprofStore := NewHprofStore(NewDumpStorage(db, test.NewLogger(t)), NewConf(), test.NewLogger(t))

	nowTimestamp, err := ptypes.TimestampProto(now)
	afterSecondTimestamp, err := ptypes.TimestampProto(now.Add(time.Second))
	require.NoError(t, err)
	testCases := []struct {
		name        string
		req         *hprof.ListRequest
		responseIds []int64
	}{
		{"empty filter, order by created desc", &hprof.ListRequest{}, []int64{3, 1}},
		{"idFrom", &hprof.ListRequest{IdFrom: 3}, []int64{3}},
		{"idTo", &hprof.ListRequest{IdTo: 1}, []int64{1}},
		{"limit0", &hprof.ListRequest{Limit: 0}, []int64{3, 1}},
		{"limit-1", &hprof.ListRequest{Limit: -1}, []int64{3, 1}},
		{"limit1", &hprof.ListRequest{Limit: 1}, []int64{3}},
		{"createdFrom", &hprof.ListRequest{CreatedFrom: nowTimestamp}, []int64{3, 1}},
		{"createdTo", &hprof.ListRequest{CreatedTo: nowTimestamp}, []int64{1}},
		{"createdFrom2", &hprof.ListRequest{CreatedFrom: afterSecondTimestamp}, []int64{3}},
		{"service-name", &hprof.ListRequest{ServiceName: "service-name"}, []int64{1}},
		{"layer", &hprof.ListRequest{Layer: layer.Layer_TEST}, []int64{3}},
		{"service,version,layer", &hprof.ListRequest{Layer: layer.Layer_PROD, ServiceName: "service-name", Version: "1.0.0"}, []int64{1}},
	}

	for _, tt := range testCases {
		t.Run(tt.name, func(t *testing.T) {
			resp, err := hprofStore.List(nil, tt.req)
			require.NoError(t, err)
			require.Len(t, resp.List, len(tt.responseIds), "wrong response length")
			var have []int64
			for _, dump := range resp.List {
				have = append(have, dump.Id)
			}
			require.Equal(t, tt.responseIds, have, "response mismatch")
		})
	}
}

func TestServer_Put(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(Dump{}))

	conf := NewConf()
	hprofStore := NewHprofStore(NewDumpStorage(db, test.NewLogger(t)), conf, test.NewLogger(t))

	createdTime := time.Now().Add(-1 * time.Second)
	timestamp, err := ptypes.TimestampProto(createdTime)
	require.NoError(t, err)
	_, err = hprofStore.Put(nil, &hprof.PutRequest{Dump: &hprof.Dump{
		Id:           10,
		ServiceName:  "service-name",
		Layer:        layer.Layer_PROD,
		Version:      "1.0.0",
		Canary:       false,
		Branch:       "br",
		DumpUrl:      "http://ya.ru",
		AllocationId: "alloc-id",
		Host:         "docker-01-prod.yandex-team.ru",
		Created:      timestamp,
	}})
	require.NoError(t, err)

	var dump Dump
	db.GormDb.First(&dump)
	require.NoError(t, db.GormDb.Error)
	require.NotEqual(t, 10, dump.Id, "id field is not ignored")
	require.NotEqual(t, createdTime, dump.CreatedAt, "created at field is not ignored")
	require.Equal(t, "service-name", dump.ServiceName)
	require.Equal(t, common.Prod, dump.Layer)
	require.Equal(t, "1.0.0", dump.Version)
	require.Equal(t, false, dump.Canary)
	require.Equal(t, "br", dump.Branch)
	require.Equal(t, "http://ya.ru", dump.DumpUrl)
	require.Equal(t, "alloc-id", dump.AllocationId)
	require.Equal(t, "docker-01-prod.yandex-team.ru", dump.Host)
	require.True(t, dump.ExpireAt.After(time.Now().AddDate(0, 0, 29)), "wrong expire field")
}

func TestServer_Put_WrongDumpUrl(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(Dump{}))

	hprofStore := NewHprofStore(NewDumpStorage(db, test.NewLogger(t)), NewConf(), test.NewLogger(t))

	_, err := hprofStore.Put(nil, &hprof.PutRequest{Dump: &hprof.Dump{
		DumpUrl: "http://google.ru",
	}})
	require.Error(t, err)
	statusErr, ok := status.FromError(err)
	require.True(t, ok)
	require.Equal(t, codes.InvalidArgument, statusErr.Code())
}

func TestServer_Put_BadDumpUrl(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(Dump{}))

	hprofStore := NewHprofStore(NewDumpStorage(db, test.NewLogger(t)), NewConf(), test.NewLogger(t))

	_, err := hprofStore.Put(nil, &hprof.PutRequest{Dump: &hprof.Dump{
		DumpUrl: "abcdefg",
	}})
	require.Error(t, err)
	statusErr, ok := status.FromError(err)
	require.True(t, ok)
	require.Equal(t, codes.InvalidArgument, statusErr.Code())
}

func fixture(now time.Time) []*Dump {
	return []*Dump{
		{
			Id:           1,
			ServiceName:  "service-name",
			Layer:        common.Prod,
			Version:      "1.0.0",
			Canary:       false,
			Branch:       "br",
			DumpUrl:      "http://ya.ru",
			AllocationId: "alloc-id",
			Host:         "docker-01-prod.yandex-team.ru",
			CreatedAt:    now,
			ExpireAt:     now.Add(time.Minute),
		},
		{
			Id:           2,
			ServiceName:  "expired",
			Layer:        common.Prod,
			Version:      "1.0.0",
			Canary:       false,
			Branch:       "br",
			DumpUrl:      "http://ya.ru",
			AllocationId: "alloc-id",
			Host:         "docker-01-prod.yandex-team.ru",
			ExpireAt:     now.Add(-1 * time.Minute),
		},
		{
			Id:           3,
			ServiceName:  "service-name2",
			Layer:        common.Test,
			Version:      "1.0.0",
			Canary:       true,
			Branch:       "br",
			DumpUrl:      "http://ya.ru",
			AllocationId: "alloc-id",
			Host:         "docker-01-prod.yandex-team.ru",
			CreatedAt:    now.Add(time.Second),
			ExpireAt:     now.Add(time.Minute),
		},
	}
}
