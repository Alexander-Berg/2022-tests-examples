package clickhousespanstore

import (
	"context"
	"database/sql"
	"database/sql/driver"
	"encoding/json"
	"fmt"
	"math/rand"
	"strconv"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"github.com/stretchr/testify/assert"

	"github.com/hashicorp/go-hclog"

	"github.com/DATA-DOG/go-sqlmock"
	"github.com/gogo/protobuf/proto"
	"github.com/jaegertracing/jaeger/model"

	"github.com/jaegertracing/jaeger-clickhouse/storage/clickhousespanstore/mocks"
)

const (
	testTagCount      = 10
	testLogCount      = 5
	testLogFieldCount = 5
	testIndexTable    = "test_index_table"
	testSpansTable    = "test_spans_table"
)

type expectation struct {
	preparation string
	execArgs    [][]driver.Value
}

var (
	errorMock = fmt.Errorf("error mock")
	process   = model.NewProcess("test_service", []model.KeyValue{model.String("test_process_key", "test_process_value")})
	testSpan  = model.Span{
		TraceID:       model.NewTraceID(1, 2),
		SpanID:        model.NewSpanID(3),
		OperationName: "GET /unit_test",
		StartTime:     testStartTime,
		Process:       process,
		Tags:          []model.KeyValue{model.String("test_string_key", "test_string_value"), model.Int64("test_int64_key", 4)},
		Logs:          []model.Log{{Timestamp: testStartTime, Fields: []model.KeyValue{model.String("test_log_key", "test_log_value")}}},
		Duration:      time.Minute,
	}
	testSpans             = []*model.Span{&testSpan}
	keys, values          = uniqueTagsForSpan(&testSpan)
	indexWriteExpectation = expectation{
		preparation: fmt.Sprintf("INSERT INTO %s (timestamp, traceID, service, operation, durationUs, tags.key, tags.value) VALUES (?, ?, ?, ?, ?, ?, ?)", testIndexTable),
		execArgs: [][]driver.Value{{
			testSpan.StartTime,
			testSpan.TraceID.String(),
			testSpan.Process.GetServiceName(),
			testSpan.OperationName,
			testSpan.Duration.Microseconds(),
			keys,
			values,
		}}}
	writeBatchLogs = []mocks.LogMock{{Msg: "Writing spans", Args: []interface{}{"size", len(testSpans)}}}
)

func TestSpanWriter_TagString(t *testing.T) {
	tests := map[string]struct {
		kv       model.KeyValue
		expected string
	}{
		"string value":       {kv: model.String("tag_key", "tag_string_value"), expected: "tag_key=tag_string_value"},
		"true value":         {kv: model.Bool("tag_key", true), expected: "tag_key=true"},
		"false value":        {kv: model.Bool("tag_key", false), expected: "tag_key=false"},
		"positive int value": {kv: model.Int64("tag_key", 1203912), expected: "tag_key=1203912"},
		"negative int value": {kv: model.Int64("tag_key", -1203912), expected: "tag_key=-1203912"},
		"float value":        {kv: model.Float64("tag_key", 0.005009), expected: "tag_key=0.005009"},
	}
	for name, test := range tests {
		t.Run(name, func(t *testing.T) {
			assert.Equal(t, test.expected, tagString(&test.kv), "Incorrect tag string")
		})
	}
}

func TestSpanWriter_UniqueTagsForSpan(t *testing.T) {
	tests := map[string]struct {
		tags           []model.KeyValue
		processTags    []model.KeyValue
		logs           []model.Log
		expectedKeys   []string
		expectedValues []string
	}{
		"default": {
			tags:           []model.KeyValue{model.String("key2", "value")},
			processTags:    []model.KeyValue{model.Int64("key3", 412)},
			logs:           []model.Log{{Fields: []model.KeyValue{model.Float64("key1", .5)}}},
			expectedKeys:   []string{"key1", "key2", "key3"},
			expectedValues: []string{"0.5", "value", "412"},
		},
		"repeating tags": {
			tags:           []model.KeyValue{model.String("key2", "value"), model.String("key2", "value")},
			processTags:    []model.KeyValue{model.Int64("key3", 412)},
			logs:           []model.Log{{Fields: []model.KeyValue{model.Float64("key1", .5)}}},
			expectedKeys:   []string{"key1", "key2", "key3"},
			expectedValues: []string{"0.5", "value", "412"},
		},
		"repeating keys": {
			tags:           []model.KeyValue{model.String("key2", "value_a"), model.String("key2", "value_b")},
			processTags:    []model.KeyValue{model.Int64("key3", 412)},
			logs:           []model.Log{{Fields: []model.KeyValue{model.Float64("key1", .5)}}},
			expectedKeys:   []string{"key1", "key2", "key2", "key3"},
			expectedValues: []string{"0.5", "value_a", "value_b", "412"},
		},
		"repeating values": {
			tags:           []model.KeyValue{model.String("key2", "value"), model.Int64("key4", 412)},
			processTags:    []model.KeyValue{model.Int64("key3", 412)},
			logs:           []model.Log{{Fields: []model.KeyValue{model.Float64("key1", .5)}}},
			expectedKeys:   []string{"key1", "key2", "key3", "key4"},
			expectedValues: []string{"0.5", "value", "412", "412"},
		},
	}
	for name, test := range tests {
		t.Run(name, func(t *testing.T) {
			process := model.Process{Tags: test.processTags}
			span := model.Span{Tags: test.tags, Process: &process, Logs: test.logs}
			actualKeys, actualValues := uniqueTagsForSpan(&span)
			assert.Equal(t, test.expectedKeys, actualKeys)
			assert.Equal(t, test.expectedValues, actualValues)
		})
	}
}

func TestSpanWriter_General_New(t *testing.T) {
	spanJSON, err := json.Marshal(&testSpan)
	require.NoError(t, err)
	modelWriteExpectationJSON := getModelWriteExpectation(spanJSON)
	spanProto, err := proto.Marshal(&testSpan)
	require.NoError(t, err)
	modelWriteExpectationProto := getModelWriteExpectation(spanProto)

	ctx, cancel := context.WithTimeout(context.Background(), time.Second*10)
	defer cancel()

	tests := map[string]struct {
		encoding     Encoding
		indexTable   TableName
		spans        []*model.Span
		expectations []expectation
		action       func(writeWorker *batchWriter, spans []*model.Span) error
		expectedLogs []mocks.LogMock
	}{
		"write index batch": {
			encoding:     EncodingJSON,
			indexTable:   testIndexTable,
			spans:        testSpans,
			expectations: []expectation{indexWriteExpectation},
			action: func(writeWorker *batchWriter, spans []*model.Span) error {
				return writeWorker.writeIndexBatch(ctx, spans)
			},
		},
		"write model batch JSON": {
			encoding:     EncodingJSON,
			indexTable:   testIndexTable,
			spans:        testSpans,
			expectations: []expectation{modelWriteExpectationJSON},
			action:       func(w *batchWriter, spans []*model.Span) error { return w.writeModelBatch(ctx, spans) },
		},
		"write model batch Proto": {
			encoding:     EncodingProto,
			indexTable:   testIndexTable,
			spans:        testSpans,
			expectations: []expectation{modelWriteExpectationProto},
			action:       func(w *batchWriter, spans []*model.Span) error { return w.writeModelBatch(ctx, spans) },
		},
		"write batch no index JSON": {
			encoding:     EncodingJSON,
			indexTable:   "",
			spans:        testSpans,
			expectations: []expectation{modelWriteExpectationJSON},
			action:       func(writeWorker *batchWriter, spans []*model.Span) error { return writeWorker.writeBatch(spans) },
			expectedLogs: writeBatchLogs,
		},
		"write batch no index Proto": {
			encoding:     EncodingProto,
			indexTable:   "",
			spans:        testSpans,
			expectations: []expectation{modelWriteExpectationProto},
			action:       func(writeWorker *batchWriter, spans []*model.Span) error { return writeWorker.writeBatch(spans) },
			expectedLogs: writeBatchLogs,
		},
		"write batch JSON": {
			encoding:     EncodingJSON,
			indexTable:   testIndexTable,
			spans:        testSpans,
			expectations: []expectation{modelWriteExpectationJSON, indexWriteExpectation},
			action:       func(writeWorker *batchWriter, spans []*model.Span) error { return writeWorker.writeBatch(spans) },
			expectedLogs: writeBatchLogs,
		},
		"write batch Proto": {
			encoding:     EncodingProto,
			indexTable:   testIndexTable,
			spans:        testSpans,
			expectations: []expectation{modelWriteExpectationProto, indexWriteExpectation},
			action:       func(writeWorker *batchWriter, spans []*model.Span) error { return writeWorker.writeBatch(spans) },
			expectedLogs: writeBatchLogs,
		},
	}

	for name, test := range tests {
		t.Run(name, func(t *testing.T) {
			db, mock, err := mocks.GetDbMock()
			require.NoError(t, err, "an error was not expected when opening a stub database connection")
			defer db.Close()

			spyLogger := mocks.NewSpyLogger()
			worker := getBatchWriter(spyLogger, db, test.encoding, test.indexTable)

			for _, expectation := range test.expectations {
				mock.ExpectBegin()
				prep := mock.ExpectPrepare(expectation.preparation)
				for _, args := range expectation.execArgs {
					prep.ExpectExec().WithArgs(args...).WillReturnResult(sqlmock.NewResult(1, 1))
				}
				mock.ExpectCommit()
			}

			assert.NoError(t, test.action(worker, test.spans))
			assert.NoError(t, mock.ExpectationsWereMet())
			spyLogger.AssertLogsOfLevelEqual(t, hclog.Debug, test.expectedLogs)
		})
	}
}

func TestSpanWriter_BeginError(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second/10)
	defer cancel()

	tests := map[string]struct {
		action       func(w *batchWriter) error
		expectedLogs []mocks.LogMock
	}{
		"write model batch": {action: func(w *batchWriter) error { return w.writeModelBatch(ctx, testSpans) }},
		"write index batch": {action: func(w *batchWriter) error { return w.writeIndexBatch(ctx, testSpans) }},
		"write batch": {
			action:       func(w *batchWriter) error { return w.writeBatch(testSpans) },
			expectedLogs: writeBatchLogs,
		},
	}

	for name, test := range tests {
		t.Run(name, func(t *testing.T) {
			db, mock, err := mocks.GetDbMock()
			require.NoError(t, err, "an error was not expected when opening a stub database connection")
			defer db.Close()

			spyLogger := mocks.NewSpyLogger()
			writeWorker := getBatchWriter(spyLogger, db, EncodingJSON, testIndexTable)

			mock.ExpectBegin().WillReturnError(errorMock)

			assert.ErrorIs(t, test.action(writeWorker), errorMock)
			assert.NoError(t, mock.ExpectationsWereMet())
			spyLogger.AssertLogsOfLevelEqual(t, hclog.Debug, test.expectedLogs)
		})
	}
}

func TestSpanWriter_PrepareError(t *testing.T) {
	spanJSON, err := json.Marshal(&testSpan)
	require.NoError(t, err)
	modelWriteExpectation := getModelWriteExpectation(spanJSON)

	ctx, cancel := context.WithTimeout(context.Background(), time.Second*10)
	defer cancel()

	tests := map[string]struct {
		action       func(w *batchWriter) error
		expectation  expectation
		expectedLogs []mocks.LogMock
	}{
		"write model batch": {
			action:      func(w *batchWriter) error { return w.writeModelBatch(ctx, testSpans) },
			expectation: modelWriteExpectation,
		},
		"write index batch": {
			action:      func(w *batchWriter) error { return w.writeIndexBatch(ctx, testSpans) },
			expectation: indexWriteExpectation,
		},
		"write batch": {
			action:       func(w *batchWriter) error { return w.writeBatch(testSpans) },
			expectation:  modelWriteExpectation,
			expectedLogs: writeBatchLogs,
		},
	}

	for name, test := range tests {
		t.Run(name, func(t *testing.T) {
			db, mock, err := mocks.GetDbMock()
			require.NoError(t, err, "an error was not expected when opening a stub database connection")
			defer db.Close()

			spyLogger := mocks.NewSpyLogger()
			spanWriter := getBatchWriter(spyLogger, db, EncodingJSON, testIndexTable)

			mock.ExpectBegin()
			mock.ExpectPrepare(test.expectation.preparation).WillReturnError(errorMock)
			mock.ExpectRollback()

			assert.ErrorIs(t, test.action(spanWriter), errorMock)
			assert.NoError(t, mock.ExpectationsWereMet())
			spyLogger.AssertLogsOfLevelEqual(t, hclog.Debug, test.expectedLogs)
		})
	}
}

func TestSpanWriter_ExecError(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second/10)
	defer cancel()

	spanJSON, err := json.Marshal(&testSpan)
	require.NoError(t, err)
	modelWriteExpectation := getModelWriteExpectation(spanJSON)
	tests := map[string]struct {
		indexTable   TableName
		expectations []expectation
		action       func(w *batchWriter) error
		expectedLogs []mocks.LogMock
	}{
		"write model batch": {
			indexTable:   testIndexTable,
			expectations: []expectation{modelWriteExpectation},
			action:       func(w *batchWriter) error { return w.writeModelBatch(ctx, testSpans) },
		},
		"write index batch": {
			indexTable:   testIndexTable,
			expectations: []expectation{indexWriteExpectation},
			action:       func(w *batchWriter) error { return w.writeIndexBatch(ctx, testSpans) },
		},
		"write batch no index": {
			indexTable:   "",
			expectations: []expectation{modelWriteExpectation},
			action:       func(w *batchWriter) error { return w.writeBatch(testSpans) },
			expectedLogs: writeBatchLogs,
		},
		"write batch": {
			indexTable:   testIndexTable,
			expectations: []expectation{modelWriteExpectation, indexWriteExpectation},
			action:       func(w *batchWriter) error { return w.writeBatch(testSpans) },
			expectedLogs: writeBatchLogs,
		},
	}

	for name, test := range tests {
		t.Run(name, func(t *testing.T) {
			db, mock, err := mocks.GetDbMock()
			require.NoError(t, err, "an error was not expected when opening a stub database connection")
			defer db.Close()

			spyLogger := mocks.NewSpyLogger()
			writeWorker := getBatchWriter(spyLogger, db, EncodingJSON, testIndexTable)

			for i, expectation := range test.expectations {
				mock.ExpectBegin()
				prep := mock.ExpectPrepare(expectation.preparation)
				if i < len(test.expectations)-1 {
					for _, args := range expectation.execArgs {
						prep.ExpectExec().WithArgs(args...).WillReturnResult(sqlmock.NewResult(1, 1))
					}
					mock.ExpectCommit()
				} else {
					prep.ExpectExec().WithArgs(expectation.execArgs[0]...).WillReturnError(errorMock)
					mock.ExpectRollback()
				}
			}

			assert.ErrorIs(t, test.action(writeWorker), errorMock)
			assert.NoError(t, mock.ExpectationsWereMet())
			spyLogger.AssertLogsOfLevelEqual(t, hclog.Debug, test.expectedLogs)
		})
	}
}

func getBatchWriter(spyLogger mocks.SpyLogger, db *sql.DB, encoding Encoding, indexTable TableName) *batchWriter {
	return &batchWriter{
		logger: spyLogger,
		opts: batchWriterOpts{
			BatchSize:     100,
			BatchWorkers:  2,
			FlushInterval: time.Second,
			Timeout:       time.Second * 10,
			Encoding:      encoding,
			spansTable:    testSpansTable,
			indexTable:    string(indexTable),
		},
		spanBuf: make(chan *model.Span, 100),
		batches: make(chan []*model.Span, 10),
	}
}

func generateRandomSpans(count int) []*model.Span {
	spans := make([]*model.Span, count)
	for i := 0; i < count; i++ {
		span := generateRandomSpan()
		spans[i] = &span
	}
	return spans
}

func generateRandomSpan() model.Span {
	processTags := generateRandomKeyValues(testTagCount)
	process := model.Process{
		ServiceName: "service" + strconv.FormatUint(rand.Uint64(), 10),
		Tags:        processTags,
	}
	span := model.Span{
		TraceID:       model.NewTraceID(rand.Uint64(), rand.Uint64()),
		SpanID:        model.NewSpanID(rand.Uint64()),
		OperationName: "operation" + strconv.FormatUint(rand.Uint64(), 10),
		StartTime:     getRandomTime(),
		Process:       &process,
		Tags:          generateRandomKeyValues(testTagCount),
		Logs:          generateRandomLogs(),
		Duration:      time.Unix(rand.Int63n(1<<32), 0).Sub(time.Unix(0, 0)),
	}
	return span
}

func generateRandomLogs() []model.Log {
	logs := make([]model.Log, 0, testLogCount)
	for i := 0; i < testLogCount; i++ {
		timestamp := getRandomTime()
		logs = append(logs, model.Log{Timestamp: timestamp, Fields: generateRandomKeyValues(testLogFieldCount)})
	}
	return logs
}

func getRandomTime() time.Time {
	return time.Unix(rand.Int63n(time.Now().Unix()), 0)
}

func generateRandomKeyValues(count int) []model.KeyValue {
	tags := make([]model.KeyValue, 0, count)
	for i := 0; i < count; i++ {
		key := "key" + strconv.FormatUint(rand.Uint64(), 16)
		value := "key" + strconv.FormatUint(rand.Uint64(), 16)
		kv := model.KeyValue{Key: key, VType: model.ValueType_STRING, VStr: value}
		tags = append(tags, kv)
	}

	return tags
}

func getModelWriteExpectation(spanJSON []byte) expectation {
	return expectation{
		preparation: fmt.Sprintf("INSERT INTO %s (timestamp, traceID, model) VALUES (?, ?, ?)", testSpansTable),
		execArgs: [][]driver.Value{{
			testSpan.StartTime,
			testSpan.TraceID.String(),
			spanJSON,
		}},
	}
}
