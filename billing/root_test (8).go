package response

import (
	"net/http/httptest"
	"testing"

	"github.com/stretchr/testify/assert"

	coreerrors "a.yandex-team.ru/billing/hot/accounts/pkg/core/errors"
	"a.yandex-team.ru/library/go/core/xerrors"
)

func TestAPIOk(t *testing.T) {
	w := httptest.NewRecorder()
	err := APIOK(w, map[string]string{"some": "data"})
	if err != nil {
		t.Fatal(err)
	}

	assert.Equal(t, w.Code, 200)
	assert.Equal(t, w.Header().Get("Content-Type"), "application/json; charset=utf-8")
	assert.Equal(t, w.Body.String(), `{"status":"ok","data":{"some":"data"}}`)
}

func TestAPIErrorGeneric(t *testing.T) {
	w := httptest.NewRecorder()
	err := APIError(w, xerrors.New("epic fail"))
	if err != nil {
		t.Fatal(err)
	}

	assert.Equal(t, w.Code, 500)
	assert.Equal(t, w.Header().Get("Content-Type"), "application/json; charset=utf-8")
	assert.Equal(t, w.Body.String(), `{"status":"error","data":{"code":"INTERNAL_ERROR","description":"epic fail"}}`)
}

func TestAPIErrorCoded(t *testing.T) {
	w := httptest.NewRecorder()
	err := APIError(w, coreerrors.NewCodedError(132, "321", "akqwe"))
	if err != nil {
		t.Fatal(err)
	}

	assert.Equal(t, w.Code, 132)
	assert.Equal(t, w.Header().Get("Content-Type"), "application/json; charset=utf-8")
	assert.Equal(t, w.Body.String(), `{"status":"error","data":{"code":"321","description":"akqwe"}}`)
}

func TestAPIErrorCodedWrapping(t *testing.T) {
	w := httptest.NewRecorder()
	codedErr := coreerrors.NewCodedError(499, "WWW", "base").Wrap(xerrors.New("wrapped"))
	err := APIError(w, xerrors.Errorf("wrapper: %w", codedErr))
	if err != nil {
		t.Fatal(err)
	}

	assert.Equal(t, w.Code, 499)
	assert.Equal(t, w.Header().Get("Content-Type"), "application/json; charset=utf-8")
	assert.Equal(t, w.Body.String(), `{"status":"error","data":{"code":"WWW","description":"wrapper: base: wrapped"}}`)
}
