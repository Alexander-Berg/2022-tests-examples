package blackbox

import (
	"fmt"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/YandexClassifieds/shiva/test"
	"github.com/gorilla/mux"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"golang.org/x/oauth2"
)

func TestSessionIdMiddleWare(t *testing.T) {
	bb := &MockChecker{}
	host := "hprof.vertis.yandex-team.ru"
	bb.On("CheckSessionId", "sess-id", "sess-id", host, mock.Anything).
		Return(&UserInfo{Login: "dev-user"}, nil)
	bb.On("CheckSessionId", "error-cause", "error-cause", host, mock.Anything).
		Return(nil, fmt.Errorf(""))

	testCases := []struct {
		name          string
		sessionId     string
		sslSessionId  string
		expectedLogin string
	}{
		{"empty context", "", "", ""},
		{"missing ssl", "sess-id", "", ""},
		{"missing cookie", "", "sess-id", ""},
		{"ok", "sess-id", "sess-id", "dev-user"},
		{"blackbox error", "error-cause", "error-cause", ""},
	}

	for _, tt := range testCases {
		t.Run(tt.name, func(t *testing.T) {
			recorder := httptest.NewRecorder()
			req, err := http.NewRequest("GET", "/", nil)
			require.NoError(t, err)
			req.AddCookie(&http.Cookie{
				Name:  "Session_id",
				Value: tt.sessionId,
			})
			req.AddCookie(&http.Cookie{
				Name:  "sessionid2",
				Value: tt.sslSessionId,
			})

			middleWare := SessionIdMiddleWare(bb, host, test.NewLogger(t))
			router := mux.NewRouter()
			router.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
				if tt.expectedLogin != "" {
					get, exists := r.Context().Value(LoginKey).(string)
					require.True(t, exists)
					require.Equal(t, tt.expectedLogin, get)
				}
			})
			router.Use(middleWare)
			router.ServeHTTP(recorder, req)
		})
	}
}

func TestOAuthMiddleware(t *testing.T) {
	bb := &MockChecker{}
	bb.On("CheckOAuthToken", &oauth2.Token{AccessToken: "oauth-token"}, mock.Anything).
		Return(&UserInfo{Login: "dev-user"}, nil)
	bb.On("CheckOAuthToken", &oauth2.Token{AccessToken: "abc"}, mock.Anything).
		Return(nil, ErrInvalidToken)
	bb.On("CheckOAuthToken", &oauth2.Token{AccessToken: "error-cause"}, mock.Anything).
		Return(nil, fmt.Errorf(""))

	testCases := []struct {
		name          string
		token         string
		expectedLogin string
	}{
		{"empty context", "", ""},
		{"ok", "oauth-token", "dev-user"},
		{"wrong token", "abc", ""},
		{"blackbox error", "error-cause", ""},
	}

	for _, tt := range testCases {
		t.Run(tt.name, func(t *testing.T) {
			recorder := httptest.NewRecorder()
			req, err := http.NewRequest("GET", "/", nil)
			require.NoError(t, err)
			req.Header.Set("Authorization", "Bearer "+tt.token)

			middleWare := OAuthMiddleware(bb, test.NewLogger(t))
			router := mux.NewRouter()
			router.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
				if tt.expectedLogin != "" {
					get, exists := r.Context().Value(LoginKey).(string)
					require.True(t, exists)
					require.Equal(t, tt.expectedLogin, get)
				}
			})
			router.Use(middleWare)
			router.ServeHTTP(recorder, req)
		})
	}
}
