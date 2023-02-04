package app_test

import (
	"context"
	"database/sql"
	"fmt"
	"testing"
	"time"

	"github.com/YandexClassifieds/vtail/api/query"
	"github.com/YandexClassifieds/vtail/cmd/query/app"
	"github.com/YandexClassifieds/vtail/internal/field"
	"github.com/YandexClassifieds/vtail/test"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/types/known/timestamppb"
)

func TestAutocompleteService_Values(t *testing.T) {
	test.InitConfig(t)
	db := test.InitDb(t)
	loadFixtures(t, db)
	logger := test.NewTestLogger()
	config := &app.QueryServiceConfig{QueryTimeout: 10 * time.Second}
	service := app.NewAutocompleteService(config, db, logger)

	timeStart := timestamppb.New(time.Now().Add(-time.Minute))
	timeEnd := timestamppb.New(time.Now().Add(time.Minute))

	testCases := []struct {
		field   string
		filters string
		want    []string
	}{
		{field.Service, "", []string{"mysvc", "othersvc", "thread-null-svc"}},
		{field.Service, "level=error", []string{"mysvc"}},
		{field.Version, "service=mysvc or service=othersvc", []string{"v1"}},
		{field.Version, "service=mysvc", []string{"v1"}},
		{field.Branch, "", []string{"master"}},
		{field.Host, "", []string{"docker-01-myt", "docker-02-myt"}},
		{field.AllocationId, "", []string{"alloc1", "alloc2"}},
		{field.RequestId, "", []string{"reqid1"}},
		{field.Context, "", []string{"ctx"}},
		{field.Thread, "", []string{"thread1"}},
		{field.Thread, "service=thread-null-svc", []string{}},
	}
	for _, tc := range testCases {
		t.Run(
			fmt.Sprintf("field=%s; filters=%s", tc.field, tc.filters), func(t *testing.T) {
				response, err := service.Values(
					context.Background(), &query.ValuesListRequest{
						FieldName: tc.field,
						Start:     timeStart,
						End:       timeEnd,
						Filters:   tc.filters,
						Limit:     10,
					},
				)
				require.NoError(t, err)
				assert.ElementsMatch(t, tc.want, response.Value)
			},
		)
	}
}

func TestSkipFields(t *testing.T) {
	test.InitConfig(t)
	db := test.InitDb(t)
	loadFixtures(t, db)
	logger := test.NewTestLogger()
	config := &app.QueryServiceConfig{QueryTimeout: 10 * time.Second}
	service := app.NewAutocompleteService(config, db, logger)

	timeStart := timestamppb.New(time.Now().Add(-time.Minute))
	timeEnd := timestamppb.New(time.Now().Add(time.Minute))

	testCases := []string{field.Layer, field.Time, field.Level, field.Message, field.Dc, field.Canary}
	for _, tc := range testCases {
		t.Run(tc, func(t *testing.T) {
			response, err := service.Values(context.Background(), &query.ValuesListRequest{
				FieldName: tc,
				Start:     timeStart,
				End:       timeEnd,
				Filters:   "",
				Limit:     10,
			})
			require.Error(t, err)
			require.Equal(t, codes.InvalidArgument, status.Code(err))
			assert.Nil(t, response)
		})
	}
}

func TestTimeFrame(t *testing.T) {
	test.InitConfig(t)
	db := test.InitDb(t)
	loadFixtures(t, db)
	logger := test.NewTestLogger()
	config := &app.QueryServiceConfig{QueryTimeout: 10 * time.Second}
	service := app.NewAutocompleteService(config, db, logger)

	timeStart := timestamppb.New(time.Now().Add(-time.Second))
	timeEnd := timestamppb.New(time.Now().Add(time.Minute))

	response, err := service.Values(context.Background(), &query.ValuesListRequest{
		FieldName: field.Service,
		Start:     timeStart,
		End:       timeEnd,
		Filters:   "",
		Limit:     10,
	})
	require.NoError(t, err)
	assert.Len(t, response.Value, 1)
}

func TestLimit(t *testing.T) {
	test.InitConfig(t)
	db := test.InitDb(t)
	loadFixtures(t, db)
	logger := test.NewTestLogger()
	config := &app.QueryServiceConfig{QueryTimeout: 10 * time.Second}
	service := app.NewAutocompleteService(config, db, logger)

	timeStart := timestamppb.New(time.Now().Add(-time.Minute))
	timeEnd := timestamppb.New(time.Now().Add(time.Minute))

	response, err := service.Values(context.Background(), &query.ValuesListRequest{
		FieldName: field.Service,
		Start:     timeStart,
		End:       timeEnd,
		Filters:   "",
		Limit:     1,
	})
	require.NoError(t, err)
	assert.Len(t, response.Value, 1)
	// wrong filter
}

func TestWrongFilter(t *testing.T) {
	test.InitConfig(t)
	db := test.InitDb(t)
	loadFixtures(t, db)
	logger := test.NewTestLogger()
	config := &app.QueryServiceConfig{QueryTimeout: 10 * time.Second}
	service := app.NewAutocompleteService(config, db, logger)

	timeStart := timestamppb.New(time.Now().Add(-time.Minute))
	timeEnd := timestamppb.New(time.Now().Add(time.Minute))

	response, err := service.Values(context.Background(), &query.ValuesListRequest{
		FieldName: field.Service,
		Start:     timeStart,
		End:       timeEnd,
		Filters:   "abc",
		Limit:     1,
	})
	require.Error(t, err)
	require.Equal(t, codes.InvalidArgument, status.Code(err))
	assert.Nil(t, response)
}

func loadFixtures(t *testing.T, db *sql.DB) {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second*300)
	defer cancel()

	tx, err := db.BeginTx(ctx, nil)
	require.NoError(t, err, "tx begin fail")
	stmt, err := tx.PrepareContext(ctx, `INSERT INTO logs.logs
(_time, _time_nano, _context, _level, _service, _version, _canary, _message, _rest, _branch, _host, _allocation_id, _request_id, _thread)
VALUES`)
	require.NoError(t, err, "tx prepare fail")

	ts := time.Now()
	_, err = stmt.ExecContext(ctx, ts.Add(-2*time.Second), 123, "", "ERROR", "mysvc", "v1", false,
		"msg1", `{"foo":{"bar":"baz","bar2":42,"fv":42.2,"bv":true}}`, ``, `docker-01-myt`, `alloc1`, ``, `thread1`)
	require.NoError(t, err, "stmt exec err")
	_, err = stmt.ExecContext(ctx, ts, 1234, "ctx", "INFO", "othersvc", "v1", true,
		"x", `{"data":"stuff"}`, `master`, `docker-02-myt`, `alloc2`, `reqid1`, ``)
	require.NoError(t, err, "stmt exec err")
	_, err = stmt.ExecContext(ctx, ts, 1235, "", "INFO", "shiva-ci-Test", "", true, "", ``, ``, ``, ``, ``, ``)
	require.NoError(t, err, "stmt exec err")
	_, err = stmt.ExecContext(ctx, ts, 1236, "", "INFO", "autoru-api/periodic-123", "", true, "", ``, ``, ``, ``, ``, ``)
	require.NoError(t, err, "stmt exec err")
	require.NoError(t, tx.Commit())

	tx, err = db.BeginTx(ctx, nil)
	require.NoError(t, err)
	stmt, err = tx.PrepareContext(ctx, `INSERT INTO logs.logs
(_time, _time_nano, _context, _level, _service, _version, _canary, _message, _rest, _branch, _host, _allocation_id, _request_id)
VALUES`)
	require.NoError(t, err)
	_, err = stmt.ExecContext(ctx,
		ts.Add(-3*time.Second), 123, "", "INFO", "thread-null-svc", "v1", true, "x", `{"data":"stuff"}`, `master`, `docker-02-myt`, `alloc2`, `reqid1`)
	require.NoError(t, err)
	require.NoError(t, tx.Commit())

	require.NoError(t, err)
}
