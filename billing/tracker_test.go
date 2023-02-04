package mwtracker

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/library/go/billingo/pkg/extracontext"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/web"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xtrace"
	"a.yandex-team.ru/library/go/core/log/ctxlog"
)

type handlerOK struct {
	t              *testing.T
	expectedFields int
}

func (h handlerOK) ServeHTTP(_ http.ResponseWriter, r *http.Request) {
	h.t.Helper()

	require.NotNil(h.t, web.ResponseTrackerFromRequest(r))
	require.Len(h.t, ctxlog.ContextFields(r.Context()), h.expectedFields)
	assert.NotEmpty(h.t, xtrace.RequestID(r.Context()))
}

var _ http.Handler = handlerOK{}

func createTest(t *testing.T, expectedFields int, opts ...Option) (http.Handler, *httptest.ResponseRecorder, *http.Request) {
	handler := AddTracker(opts...)(handlerOK{t: t, expectedFields: expectedFields})

	w := httptest.NewRecorder()
	r, err := http.NewRequest("POST", "dunno", nil)
	require.NoError(t, err)
	return handler, w, r
}

func TestTracker(t *testing.T) {
	handler, w, r := createTest(t, 1)

	handler.ServeHTTP(w, r)
}

func TestWithCallID(t *testing.T) {
	handler, w, r := createTest(t, 2, WithCallID())

	handler.ServeHTTP(w, r)
}

func TestExtraContext(t *testing.T) {
	handler, w, r := createTest(t, 1)
	r = r.WithContext(extracontext.NewWithParent(r.Context()))

	handler.ServeHTTP(w, r)
}
