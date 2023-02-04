package filter

import (
	"testing"

	"github.com/YandexClassifieds/vtail/api/core"
	"github.com/YandexClassifieds/vtail/cmd/backend/parser"
	"github.com/YandexClassifieds/vtail/cmd/streamer/filter/compiled"
	"github.com/YandexClassifieds/vtail/cmd/streamer/source"
	"github.com/YandexClassifieds/vtail/test"
	"github.com/stretchr/testify/require"
)

func TestRestMatch(t *testing.T) {
	testCases := []struct {
		json   string
		filter string
	}{
		{`{"level":"200"}`, "rest.level=200"},
		{`{"foo":"bar"}`, "foo=bar"},
		{`{"_message":"test","message":"other"}`, "message=test"},
		{`{"level":"200.3"}`, "rest.level=200*"},
		{`{"level":" 200"}`, "rest.level=*200"},
		{`{"level":"-200"}`, "rest.level=*200"},
		{`{"level":200}`, "rest.level=200"},
		{`{"level":200.2}`, "rest.level=200.2"},
		{`{"level":200.2}`, "rest.level='200.2'"},
		{`{"level":200.3}`, "rest.level=200*"},
		{`{"level":2003}`, "rest.level=200*"},
		{`{"level":{"inner":"200"}}`, "rest.level.inner=200"},
		{`{"level":{"inner":{"deeper":"200"}}}`, "rest.level.inner.deeper=200"},
		{`{"validation_error":"unparsed tail: \"IndexerApp started\""}`, "rest.validation_error='*unparsed tail*'"},
	}

	queryParser := parser.NewQueryParser()
	for _, testCase := range testCases {
		m, err := source.DecodeMessage([]byte(testCase.json))
		require.NoError(t, err)
		message := compiled.NewMessage(&m.LogMessage)

		filters, err := queryParser.Parse(testCase.filter)
		require.NoError(t, err)
		matcher := NewMatcherWithLogger(filters, test.NewTestLogger())

		res := matcher.Match(message)
		require.Truef(t, res, "%+v", testCase)
	}
}

func TestEscape(t *testing.T) {
	testCases := []struct {
		json   string
		filter string
	}{
		{`{".le.vel.":200}`, `rest.\.le\.vel\.=200`},
		{`{".le.vel.":{".inn.er.":200}}`, `rest.\.le\.vel\..\.inn\.er\.=200`},
		{`{"m":"ab"}`, `rest.m=a*b`},
		{`{"m":"a*b"}`, `rest.m=a\*b`},
		{`{"m":"ab"}`, `rest.m=*b`},
		{`{"m":"*b"}`, `rest.m=\*b`},
		{`{"m":"b"}`, `rest.m=b*`},
		{`{"m":"b*"}`, `rest.m=b\*`},
		{`{"m":"b"}`, `rest.m=*b*`},
		{`{"m":"*b*"}`, `rest.m=\*b\*`},
		{`{"m":""}`, `rest.m=*`},
		{`{"m":"*"}`, `rest.m=\*`},
		{`{"m":"b*"}`, `rest.m=*b\*`},
	}

	queryParser := parser.NewQueryParser()
	for _, testCase := range testCases {
		m, err := source.DecodeMessage([]byte(testCase.json))
		require.NoError(t, err)
		message := compiled.NewMessage(&m.LogMessage)

		filters, err := queryParser.Parse(testCase.filter)
		require.NoError(t, err)
		matcher := NewMatcherWithLogger(filters, test.NewTestLogger())

		res := matcher.Match(message)
		require.Truef(t, res, "%+v", testCase)
	}
}

func TestRestMultiMatch(t *testing.T) {
	testCases := []struct {
		json   string
		filter string
	}{
		{`{"level":"200", "field":"msg"}`, "rest.level=200 rest.field=msg"},
		{`{"level":"200", "field":"msg"}`, "rest.level=200 field=msg"},
		{`{"level":"200", "field":{"inner":"msg"}}`, "rest.level=200 rest.field.inner=msg"},
		{`{"level":{"inner":"200"}, "field":"msg"}`, "rest.level.inner=200 rest.field=msg"},
		{`{"level":{"inner":"200"}, "field":"msg"}`, "rest.level.inner=201 or rest.field=msg"},
		{`{"level":{"inner":"200"}, "field":"msg"}`, "rest.level.missing=200 or rest.field=msg"},
		{`{"level":{"inner":"200"}, "field":"msg"}`, "rest.level.inner=200 or rest.missing=msg"},
		{`{"level":{"inner":"200"}, "field":"msg"}`, "rest.level.inner=200 or rest.field=msg2"},
		{`{"level":{"inner":"200"}, "field":"msg"}`, "rest.level.inner=201 rest.field=msg2 or rest.field=msg"},
		{`{"level":{"inner":"200"}, "field":"msg"}`, "rest.level.inner=200 (rest.field=msg2 or rest.field=msg)"},
	}

	queryParser := parser.NewQueryParser()
	for _, testCase := range testCases {
		m, err := source.DecodeMessage([]byte(testCase.json))
		require.NoError(t, err)
		message := compiled.NewMessage(&m.LogMessage)

		parenthesis, err := queryParser.Parse(testCase.filter)
		require.NoError(t, err)
		matcher := NewMatcherWithLogger(parenthesis, test.NewTestLogger())

		res := matcher.Match(message)
		require.Truef(t, res, "%+v", testCase)
	}
}

func TestRestNoMatch(t *testing.T) {
	testCases := []struct {
		json   string
		filter string
	}{
		{`{"level":"205"}`, "rest.level=200"},
		{`{"level":205}`, "rest.level=200"},
		{`{"level":[200]}`, "rest.level=200"},
		{`{"level":200}`, "rest.level2=200"},
		{`{"level":{"inner":"200"}}`, "rest.level.missingField=200"},
		{`{"level":{"inner":"200"}}`, "rest.level.inner=203"},
		{`{"level":{"inner":["200"]}}`, "rest.level.inner=200"},
		{`{"level":{"inner":["200"]}}`, "rest.level.inner.field=200"},
		{`{"level":{"inner":"200"}}`, "rest.level.inner.field=200"},
		{`{"level":{"inner":{"deeper":"203"}}}`, "rest.level.inner.deeper=200"},
		{`{"level":{"inner":{"deeper":"200"}}}`, "rest.level.inner2.deeper=200"},
		{`{"level":{"inner":"200"}, "field":"msg"}`, "rest.level.inner=201 (rest.field=msg2 or rest.field=msg)"},
		{`{"level":{"inner":"200"}, "field":"msg"}`, "rest.level.inner=200 (rest.field=msg2 or rest.field=msg3)"},
	}

	queryParser := parser.NewQueryParser()
	for _, testCase := range testCases {
		m, err := source.DecodeMessage([]byte(testCase.json))
		require.NoError(t, err)
		message := compiled.NewMessage(&m.LogMessage)

		parenthesis, err := queryParser.Parse(testCase.filter)
		require.NoError(t, err)
		matcher := NewMatcherWithLogger(parenthesis, test.NewTestLogger())

		res := matcher.Match(message)
		require.Falsef(t, res, "%+v", testCase)
	}
}

func TestLevel(t *testing.T) {
	testCases := []struct {
		level  string
		filter string
	}{
		{"FATAL", "level=fatal"},
		{"ERROR", "level=error"},
		{"WARN", "level=warn"},
		{"INFO", "level=info"},
		{"DEBUG", "level=debug"},
		{"TRACE", "level=trace"},
	}

	queryParser := parser.NewQueryParser()
	for _, testCase := range testCases {
		parenthesis, err := queryParser.Parse(testCase.filter)
		require.NoError(t, err)
		matcher := NewMatcherWithLogger(parenthesis, test.NewTestLogger())
		message := compiled.NewMessage(&core.LogMessage{Level: testCase.level})

		res := matcher.Match(message)
		require.Truef(t, res, "%+v", testCase)
	}
}

func TestCoreField(t *testing.T) {
	testCases := []struct {
		message *core.LogMessage
		filter  string
	}{
		{&core.LogMessage{Service: "abc"}, "service=abc"},
		{&core.LogMessage{Layer: "test"}, "layer=test"},
		{&core.LogMessage{Version: "abc"}, "version=abc"},
		{&core.LogMessage{Thread: "abc"}, "thread=abc"},
		{&core.LogMessage{Dc: "abc"}, "dc=abc"},
		{&core.LogMessage{Canary: false}, "canary=false"},
		{&core.LogMessage{Canary: false}, "canary=f"},
		{&core.LogMessage{Canary: false}, "canary=0"},
		{&core.LogMessage{Branch: "abc"}, "branch=abc"},
		{&core.LogMessage{Host: "abc"}, "host=abc"},
		{&core.LogMessage{AllocationId: "abc"}, "allocation_id=abc"},
		{&core.LogMessage{RequestId: "abc"}, "request_id=abc"},
		{&core.LogMessage{Context: "abc"}, "context=abc"},
		{&core.LogMessage{Message: "abc"}, "message=abc"},
	}

	queryParser := parser.NewQueryParser()
	for _, testCase := range testCases {
		parenthesis, err := queryParser.Parse(testCase.filter)
		require.NoError(t, err)
		matcher := NewMatcherWithLogger(parenthesis, test.NewTestLogger())
		message := compiled.NewMessage(testCase.message)

		res := matcher.Match(message)
		require.Truef(t, res, testCase.filter)
	}
}

func TestNoMatch(t *testing.T) {
	testCases := []struct {
		message *core.LogMessage
		filter  string
	}{
		{&core.LogMessage{Service: "abc2"}, "service=abc"},
		{&core.LogMessage{Layer: "test"}, "layer=prod"},
		{&core.LogMessage{Version: ""}, "version=abc"},
		{&core.LogMessage{Canary: false}, "canary=true"},
		{&core.LogMessage{Canary: false}, "canary=t"},
		{&core.LogMessage{Canary: false}, "canary=1"},
		{&core.LogMessage{Service: "abc2"}, "service=abc2 rest.field=5"},
	}

	queryParser := parser.NewQueryParser()
	for _, testCase := range testCases {
		parenthesis, err := queryParser.Parse(testCase.filter)
		require.NoError(t, err)
		matcher := NewMatcherWithLogger(parenthesis, test.NewTestLogger())
		message := compiled.NewMessage(testCase.message)

		res := matcher.Match(message)
		require.Falsef(t, res, testCase.filter)
	}
}
