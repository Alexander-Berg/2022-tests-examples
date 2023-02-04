package mwtracing

import (
	"bytes"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/opentracing/opentracing-go"
	"github.com/opentracing/opentracing-go/mocktracer"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/library/go/billingo/pkg/extracontext"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/web"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/web/mw"
	mwtracker "a.yandex-team.ru/billing/library/go/billingo/pkg/web/mw/tracker"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xtrace"
)

func handlerOK(http.ResponseWriter, *http.Request) {}

func handlerExtraContext(w http.ResponseWriter, r *http.Request) {
	if ctx := web.GetCtx(r); ctx == nil {
		w.WriteHeader(http.StatusInternalServerError)
	}
}

func handler404(w http.ResponseWriter, _ *http.Request) {
	w.WriteHeader(404)
	_, _ = w.Write(bytes.Repeat([]byte("1"), 2*xtrace.MaxLogSize))
}

func handler500(w http.ResponseWriter, _ *http.Request) {
	w.WriteHeader(500)
	_, _ = w.Write(bytes.Repeat([]byte("1"), xtrace.MaxLogSize/2))

}

func createTest(t *testing.T, handlerFunc http.HandlerFunc, opts ...Option) (http.Handler, *mocktracer.MockTracer, *httptest.ResponseRecorder, *http.Request) {
	tracer := mocktracer.New()
	opentracing.SetGlobalTracer(tracer)

	handler := ServerSpans(opts...)(handlerFunc)

	w := httptest.NewRecorder()
	r, err := http.NewRequest("POST", "dunno?key=value", nil)
	require.NoError(t, err)
	return handler, tracer, w, r
}

func TestWithTracker(t *testing.T) {
	handler, tracer, w, r := createTest(t, handlerOK)
	handler = mwtracker.AddTracker()(handler)

	handler.ServeHTTP(w, r)

	spans := tracer.FinishedSpans()
	require.Len(t, spans, 1)

	span := spans[0]
	assert.NotEmpty(t, span.Tag("http.request_id"))
	assert.NotEmpty(t, span.Tag("http.method"))
	assert.NotEmpty(t, span.Tag("http.path"))
	assert.Empty(t, span.Tag("http.url.params"))
	assert.Empty(t, span.Tag("error"))

	assert.NotEmpty(t, w.Header().Get(mw.RequestIDHeader))
}

func TestNoTracker(t *testing.T) {
	handler, tracer, w, r := createTest(t, handlerOK)

	handler.ServeHTTP(w, r)

	spans := tracer.FinishedSpans()
	require.Len(t, spans, 1)

	span := spans[0]
	assert.Empty(t, span.Tag("http.request_id"))
	assert.Empty(t, w.Header().Get(mw.RequestIDHeader))
}

func TestWithResponseBig(t *testing.T) {
	handler, tracer, w, r := createTest(t, handler404)
	handler = mwtracker.AddTracker()(handler)

	handler.ServeHTTP(w, r)

	spans := tracer.FinishedSpans()
	require.Len(t, spans, 1)

	span := spans[0]
	assert.NotEmpty(t, span.Tag("http.request_id"))
	assert.Len(t, span.Logs(), 1)
	assert.Len(t, span.Logs()[0].Fields, 1)
	assert.Equal(t, span.Logs()[0].Fields[0].Key, "http.response")
	assert.Len(t, span.Logs()[0].Fields[0].ValueString, xtrace.MaxLogSize)
	assert.Empty(t, span.Tag("error"))
}

func TestWithResponseSmall(t *testing.T) {
	handler, tracer, w, r := createTest(t, handler500, WithResponseTagOnErrorDisabled(false))
	handler = mwtracker.AddTracker()(handler)

	handler.ServeHTTP(w, r)

	spans := tracer.FinishedSpans()
	require.Len(t, spans, 1)

	span := spans[0]
	assert.NotEmpty(t, span.Tag("http.request_id"))
	assert.NotEmpty(t, span.Logs())
	assert.Len(t, span.Logs()[0].Fields, 1)
	assert.Equal(t, span.Logs()[0].Fields[0].Key, "http.response")
	assert.NotEmpty(t, span.Tag("error"))
}

func TestWithResponseDisabledBig(t *testing.T) {
	handler, tracer, w, r := createTest(t, handler404, WithResponseTagOnErrorDisabled(true))
	handler = mwtracker.AddTracker()(handler)

	handler.ServeHTTP(w, r)

	spans := tracer.FinishedSpans()
	require.Len(t, spans, 1)

	span := spans[0]
	assert.NotEmpty(t, span.Tag("http.request_id"))
	assert.Empty(t, span.Logs())
}

func TestDisabled(t *testing.T) {
	handler, tracer, w, r := createTest(t, handlerOK, WithDisabled(true))
	handler = mwtracker.AddTracker()(handler)

	handler.ServeHTTP(w, r)

	spans := tracer.FinishedSpans()
	require.Empty(t, spans)
}

func TestExtraContext(t *testing.T) {
	handler, tracer, w, r := createTest(t, handlerExtraContext)
	r = web.SetCtx(extracontext.NewWithParent(r.Context()), r)

	handler.ServeHTTP(w, r)

	assert.Equal(t, http.StatusOK, w.Result().StatusCode)

	spans := tracer.FinishedSpans()
	assert.Len(t, spans, 1)
}

func TestURLParams(t *testing.T) {
	handler, tracer, w, r := createTest(t, handlerOK, WithURLParamsTag())

	handler.ServeHTTP(w, r)

	spans := tracer.FinishedSpans()
	assert.Len(t, spans, 1)

	span := spans[0]
	assert.NotEmpty(t, span.Tag("http.url.params"))
}
