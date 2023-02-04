package app_test

import (
	"context"
	"testing"
	"time"

	"github.com/YandexClassifieds/vtail/api/core"
	"github.com/YandexClassifieds/vtail/api/query"
	"github.com/YandexClassifieds/vtail/cmd/query/app"
	"github.com/YandexClassifieds/vtail/cmd/query/snapshot"
	"github.com/YandexClassifieds/vtail/test"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/metadata"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/types/known/timestamppb"
)

func TestQueryService_FetchLogs(t *testing.T) {
	test.InitConfig(t)

	db := test.InitDb(t)
	cfg := &app.QueryServiceConfig{
		QueryTimeout: time.Second * 10,
	}
	sp := snapshot.NewLruProvider()
	svc := app.NewQueryService(cfg, db, sp, test.NewTestLogger())
	ts := time.Now()
	timeStart := timestamppb.New(ts.Add(-time.Minute))
	timeEnd := timestamppb.New(ts.Add(time.Minute))

	ctx, cancel := context.WithTimeout(context.Background(), time.Second*3)
	defer cancel()
	// populate test data
	tx, err := db.BeginTx(ctx, nil)
	require.NoError(t, err, "tx begin fail")
	stmt, err := tx.PrepareContext(ctx, `INSERT INTO logs.logs
(_time, _time_nano, _context, _level, _service, _version, _canary, _message, _rest)
VALUES (?, ?, ?, ?, ?, ?, ?, ?)`)
	require.NoError(t, err, "tx prepare fail")

	_, err = stmt.ExecContext(ctx, ts, 123, "", "ERROR", "mysvc", "v1", false, "msg1", `{"foo":{"bar":"baz","bar2":42,"fv":42.2,"bv":true,"nonbv":"false"'}}`)
	require.NoError(t, err, "stmt exec err")
	_, err = stmt.ExecContext(ctx, ts, 1234, "ctx", "INFO", "othersvc", "23", true, "x", `{"data":"stuff","lb":"x\ny"}`)
	require.NoError(t, err, "stmt exec err")
	require.NoError(t, tx.Commit())

	t.Run("empty", func(t *testing.T) {
		ms := new(mockSrv)
		err := svc.FetchLogs(&query.FetchLogsRequest{
			Query:   " ",
			Start:   timeStart,
			End:     timeEnd,
			Limit:   10,
			SortDir: query.Sort_DESC,
		}, ms)
		require.Error(t, err)
		s, _ := status.FromError(err)
		assert.Equal(t, codes.InvalidArgument, s.Code())
	})

	t.Run("check-snapshot", func(t *testing.T) {
		ms := new(mockSrv)
		err := svc.FetchLogs(&query.FetchLogsRequest{
			Query:        "message=x",
			Start:        timeStart,
			End:          timeEnd,
			Limit:        10,
			SortDir:      query.Sort_DESC,
			SaveSnapshot: true,
		}, ms)
		require.NoError(t, err)
		require.Len(t, ms.md.Get("x-snapshot-id"), 1)
		_, err = sp.Get(context.Background(), ms.md.Get("x-snapshot-id")[0])
		require.NoError(t, err)
	})

	testCases := []struct {
		name  string
		query string
		cnt   int
	}{
		{name: "basic", query: "message=x", cnt: 1},
		{name: "message_glob", query: "message=x*", cnt: 1},
		{name: "canary", query: "canary=true", cnt: 1},
		{name: "level", query: "level=info", cnt: 1},
		{name: "rest_number", query: "rest.foo.fv=42.2", cnt: 1},
		{name: "rest_gt_number", query: "rest.foo.fv>42", cnt: 1},
		{name: "rest_between_number", query: "rest.foo.fv > 42 rest.foo.fv < 43", cnt: 1},
		{name: "rest_empty_number", query: "rest.foo.fv < 50", cnt: 1},
		{name: "rest_bool", query: "rest.foo.bv=true", cnt: 1},
		{name: "rest_bool_string", query: "rest.foo.nonbv=false", cnt: 1},
		{name: "rest_glob", query: "rest.foo.bar=ba*", cnt: 1},
		{name: "rest_like", query: "rest=*stuff*", cnt: 1},
		{name: "rest_implicit", query: "foo.fv=42.2", cnt: 1},
		{name: "not_query", query: "context!=ctx", cnt: 1},
		{name: "bool_check", query: "service=true", cnt: 0},
		{name: "line-break", query: `rest.lb="x\ny"`, cnt: 1},
	}
	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			ms := new(mockSrv)
			err := svc.FetchLogs(&query.FetchLogsRequest{
				Query:   tc.query,
				Start:   timeStart,
				End:     timeEnd,
				Limit:   10,
				SortDir: query.Sort_DESC,
			}, ms)
			require.NoError(t, err)
			assert.Len(t, ms.result, tc.cnt)
		})
	}
}

func TestQueryService_FetchLogs_Multi(t *testing.T) {
	test.InitConfig(t)

	db := test.InitDb(t)
	cfg := &app.QueryServiceConfig{
		QueryTimeout: time.Second * 10,
	}
	sp := snapshot.NewLruProvider()
	svc := app.NewQueryService(cfg, db, sp, test.NewTestLogger())
	now := time.Now()

	// populate test data
	ctx, cancel := context.WithTimeout(context.Background(), time.Second*3)
	defer cancel()

	tx, err := db.BeginTx(ctx, nil)
	require.NoError(t, err, "tx begin fail")
	stmt, err := tx.PrepareContext(ctx, `INSERT INTO logs.logs
(_time, _time_nano, _context, _level, _service, _version, _canary, _message)
VALUES (?, ?, ?, ?, ?, ?, ?)`)
	require.NoError(t, err, "tx prepare fail")

	dataSet := []struct {
		time time.Time
		msg  string
	}{
		{now, "part one: stuff"},
		{now.Add(-time.Minute), "part one: other stuff"},
		{now.Add(-time.Hour).Truncate(time.Hour), "stuff 2-0"},
		{now.Add(-time.Hour).Truncate(time.Hour).Add(-time.Second * 1), "stuff 2-1"},
		{now.Add(-time.Hour).Truncate(time.Hour).Add(-time.Second * 2), "stuff 2-2"},
		{now.Add(-time.Hour).Truncate(time.Hour).Add(-time.Second * 3), "stuff 2-3"},
		{now.Add(-time.Hour).Truncate(time.Hour).Add(-time.Second * 4), "stuff 2-4"},
		{now.Add(-time.Hour).Truncate(time.Hour).Add(-time.Second * 5), "stuff 2-5"},
		{now.Add(-time.Hour).Truncate(time.Hour).Add(-time.Second * 6), "stuff 2-7"},
		{now.Add(-time.Hour * 2).Add(-time.Minute * 5), "stuff 3"},
		{now.Add(-time.Hour * 6), "stuff 4"},
	}
	for i, r := range dataSet {
		_, err := stmt.ExecContext(ctx, r.time, 123, "multi", "INFO", "svc", "v1", false, r.msg)
		require.NoError(t, err, "stmt exec error", i)
	}
	require.NoError(t, tx.Commit())

	// actual test
	ms := new(mockSrv)
	err = svc.FetchLogs(&query.FetchLogsRequest{
		Query:   "service=svc",
		Start:   timestamppb.New(now.Add(-time.Hour * 2)),
		End:     timestamppb.New(now),
		Limit:   5,
		SortDir: query.Sort_DESC,
	}, ms)
	require.NoError(t, err)
	require.Len(t, ms.result, 5)

	expectedMsg := make([]string, 5)
	actualMsg := make([]string, 5)
	for i := 0; i < 5; i++ {
		expectedMsg[i] = dataSet[i].msg
		actualMsg[i] = ms.result[i].GetMsg().GetMessage()
	}
	assert.Equal(t, expectedMsg, actualMsg)
}

func TestQueryService_FetchSnapshot(t *testing.T) {
	test.InitConfig(t)

	db := test.InitDb(t)
	cfg := &app.QueryServiceConfig{
		QueryTimeout: time.Second * 10,
	}
	sc := snapshot.NewLruProvider()
	sc.Put(context.Background(), "abc", &core.LogList{
		Msg: []*core.LogMessage{
			{Message: "one"},
			{Message: "two"},
		},
	})
	svc := app.NewQueryService(cfg, db, sc, test.NewTestLogger())
	resp, err := svc.FetchSnapshot(context.TODO(), &query.FetchSnapshotRequest{SnapshotId: "abc"})
	require.NoError(t, err)
	require.Len(t, resp.Msg, 2)
	assert.Equal(t, "one", resp.Msg[0].Message)
	_, err = svc.FetchSnapshot(context.Background(), &query.FetchSnapshotRequest{SnapshotId: "not-found"})
	errStatus, _ := status.FromError(err)
	require.Equal(t, codes.NotFound, errStatus.Code())
}

func TestQueryService_GetYQLText(t *testing.T) {
	test.InitConfig(t)

	db := test.InitDb(t)
	cfg := &app.QueryServiceConfig{
		QueryTimeout: time.Second * 10,
	}
	sp := snapshot.NewLruProvider()
	svc := app.NewQueryService(cfg, db, sp, test.NewTestLogger())
	now := time.Now()

	t.Run("base", func(t *testing.T) {
		resp, err := svc.GetYQLText(context.Background(), &query.FetchLogsRequest{
			Start: timestamppb.New(now.Add(-time.Hour)),
			End:   timestamppb.New(now),
			Limit: 1000,
			Query: "layer=test service=some_service",
		})
		require.NoError(t, err)
		assert.Contains(t, resp.ClickhouseSql, "WHERE _time BETWEEN")
		assert.Contains(t, resp.ClickhouseSql, "some_service")
	})
	t.Run("failed_validation", func(t *testing.T) {
		_, err := svc.GetYQLText(context.Background(), &query.FetchLogsRequest{})
		s, _ := status.FromError(err)
		require.Equal(t, codes.InvalidArgument, s.Code())
	})
}

type mockSrv struct {
	mock.Mock
	grpc.ServerStream

	result []*query.FetchLogsRecord
	md     metadata.MD
}

func (m *mockSrv) Send(record *query.FetchLogsRecord) error {
	m.result = append(m.result, record)
	return nil
}

func (m *mockSrv) Context() context.Context {
	return context.Background()
}

func (m *mockSrv) SetTrailer(md metadata.MD) {
	m.md = md
}
