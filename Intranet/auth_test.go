package user

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"

	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/library/go/yandex/blackbox"
	"a.yandex-team.ru/library/go/yandex/blackbox/mocks"
	"a.yandex-team.ru/library/go/yandex/tvm"
	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
)

const (
	testServiceID tvm.ClientID = 123
	testUserID    tvm.UID      = 321
)

type mockedTVMClient struct {
	tvm.Client
}

func (t *mockedTVMClient) CheckServiceTicket(ctx context.Context, ticket string) (*tvm.CheckedServiceTicket, error) {
	return &tvm.CheckedServiceTicket{SrcID: tvm.ClientID(testServiceID)}, nil
}

func (t *mockedTVMClient) CheckUserTicket(ctx context.Context, ticket string) (*tvm.CheckedUserTicket, error) {
	return &tvm.CheckedUserTicket{DefaultUID: tvm.UID(testUserID)}, nil
}

func TestAuthuser(t *testing.T) {
	ctrl := gomock.NewController(t)

	logger, _ := zap.NewQloudLogger(log.FatalLevel)
	bbClient := mocks.NewMockClient(ctrl)
	tvmClient := &mockedTVMClient{}

	user := SetUserAndServiceID(logger, bbClient, tvmClient)

	for _, testCase := range []struct {
		name          string
		headers       map[string]string
		hasUserID     bool
		hasServiceID  bool
		statusCode    int
		timesToCookie int
		timesToOAuth  int
	}{
		{
			name:         "no_auth",
			headers:      nil,
			hasUserID:    false,
			hasServiceID: false,
			statusCode:   401,
		},
		{
			name:          "cookie",
			headers:       map[string]string{"cookie": "Session_id=session_id_cookie"},
			hasUserID:     true,
			hasServiceID:  false,
			statusCode:    200,
			timesToCookie: 1,
		},
		{
			name:         "oauth",
			headers:      map[string]string{"authorization": "OAuth oauth_token"},
			hasUserID:    true,
			hasServiceID: false,
			statusCode:   200,
			timesToOAuth: 1,
		},
		{
			name:         "sevice_ticket",
			headers:      map[string]string{"x-ya-service-ticket": "service_ticket"},
			hasUserID:    false,
			hasServiceID: true,
			statusCode:   200,
		},
		{
			name:         "both_service_user_tickets",
			headers:      map[string]string{"x-ya-service-ticket": "service_ticket", "x-ya-user-ticket": "user_ticket"},
			hasUserID:    true,
			hasServiceID: true,
			statusCode:   200,
		},
		{
			name:         "user_ticket",
			headers:      map[string]string{"x-ya-user-ticket": "user_ticket"},
			hasUserID:    false,
			hasServiceID: false,
			statusCode:   401,
		},
	} {
		t.Run(testCase.name, func(t *testing.T) {
			r := httptest.NewRequest("GET", "/test", nil)
			w := httptest.NewRecorder()
			for name, value := range testCase.headers {
				r.Header.Set(name, value)
			}

			bbClient.
				EXPECT().
				SessionID(gomock.Any(), blackbox.SessionIDRequest{SessionID: "session_id_cookie", Host: r.Host}).
				Return(&blackbox.SessionIDResponse{User: blackbox.User{ID: blackbox.ID(testUserID)}}, nil).
				Times(testCase.timesToCookie)

			bbClient.
				EXPECT().
				OAuth(gomock.Any(), blackbox.OAuthRequest{OAuthToken: "oauth_token"}).
				Return(&blackbox.OAuthResponse{User: blackbox.User{ID: blackbox.ID(testUserID)}}, nil).
				Times(testCase.timesToOAuth)

			handler := user(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				userID, ok := r.Context().Value(&userIDKey).(tvm.UID)
				assert.Equal(t, testCase.hasUserID, ok)
				if ok {
					assert.Equal(t, testUserID, userID)
				}

				serviceID, ok := r.Context().Value(&serviceIDKey).(tvm.ClientID)
				assert.Equal(t, testCase.hasServiceID, ok)
				if ok {
					assert.Equal(t, testServiceID, serviceID)
				}

				assert.Equal(t, testCase.statusCode, http.StatusOK)
				w.WriteHeader(http.StatusOK)
			}))

			handler.ServeHTTP(w, r)

			assert.Equal(t, testCase.statusCode, w.Result().StatusCode)
		})
	}
}
