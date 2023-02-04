package access

import (
	"context"
	"errors"
	"sort"
	"testing"

	auth2 "github.com/YandexClassifieds/shiva/cmd/secret-service/store/auth"
	delegation2 "github.com/YandexClassifieds/shiva/cmd/secret-service/store/delegation"
	commonStorage "github.com/YandexClassifieds/shiva/common/storage"
	proto "github.com/YandexClassifieds/shiva/pb/ss/access"
	"github.com/YandexClassifieds/shiva/pkg/yav/client"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"gorm.io/gorm"
)

func TestGRPCHandler_NewToken(t *testing.T) {
	t.Run("success", testNewTokenSuccess)
	t.Run("errors", testNewTokenErrors)
}

func testNewTokenSuccess(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)

	h := &GRPCHandler{
		store: auth2.NewStorage(db, log),
		log:   log,
	}
	response, err := h.NewToken(context.TODO(), &proto.NewTokenRequest{
		ServiceName: "newtoken-svc",
		Env: []*proto.EnvSecret{
			{EnvKey: "k1", SecretId: "sec-one", VersionId: "ver-one", SecretKey: "sk1"},
			{EnvKey: "k2", SecretId: "sec-two", VersionId: "ver-two", SecretKey: "sk2"},
		},
	})
	require.NoError(t, err)
	require.NotEmpty(t, response.Token)
	at, err := h.store.Get(response.Token)
	require.NoError(t, err)
	require.Equal(t, "newtoken-svc", at.ServiceName)
	require.Len(t, at.Data, 2)
	expectedData := auth2.TokenData{
		{EnvKey: "k1", SecretId: "sec-one", VersionId: "ver-one", SecretKey: "sk1"},
		{EnvKey: "k2", SecretId: "sec-two", VersionId: "ver-two", SecretKey: "sk2"},
	}
	require.Equal(t, expectedData, at.Data)
}

func testNewTokenErrors(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	failDb := &(*db)
	failDb.GormDb = db.GormDb.Scopes(func(db *gorm.DB) *gorm.DB {
		_ = db.AddError(errors.New("stub error"))
		return db
	})
	log := test.NewLogger(t)
	testCases := []struct {
		name            string
		db              *commonStorage.Database
		req             *proto.NewTokenRequest
		expectedCode    codes.Code
		expectedMessage string
	}{
		{
			name:            "no_service",
			db:              db,
			req:             &proto.NewTokenRequest{},
			expectedCode:    codes.InvalidArgument,
			expectedMessage: "validate failed: service_name not set",
		},
		{
			name:            "no_secret_id",
			db:              db,
			req:             &proto.NewTokenRequest{ServiceName: "xyz"},
			expectedCode:    codes.InvalidArgument,
			expectedMessage: "validate failed: no envs",
		},
		{
			name: "missing_env_key",
			db:   db,
			req: &proto.NewTokenRequest{
				ServiceName: "xyz",
				Env: []*proto.EnvSecret{
					{EnvKey: "a", SecretId: "sec-b", VersionId: "b", SecretKey: "c"},
					{VersionId: "x", SecretKey: "x"},
				},
			},
			expectedCode:    codes.InvalidArgument,
			expectedMessage: "idx: 1, env_key/secret_id/version_id/secret_key not set",
		},
		{
			name: "missing_secret_id",
			db:   db,
			req: &proto.NewTokenRequest{
				ServiceName: "xyz",
				Env: []*proto.EnvSecret{
					{EnvKey: "zzz", SecretKey: "x"},
				},
			},
			expectedCode:    codes.InvalidArgument,
			expectedMessage: "idx: 0, env_key/secret_id/version_id/secret_key not set",
		},
		{
			name: "db_fail",
			db:   failDb,
			req: &proto.NewTokenRequest{
				ServiceName: "test-svc",
				Env: []*proto.EnvSecret{
					{EnvKey: "x", SecretId: "sec-y", VersionId: "y", SecretKey: "z"},
				},
			},
			expectedCode:    codes.Internal,
			expectedMessage: "failed to create access token",
		},
	}
	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			h := &GRPCHandler{
				store: auth2.NewStorage(db, log),
				log:   log,
			}
			_, err := h.NewToken(context.TODO(), tc.req)
			require.Error(t, err)
			s, ok := status.FromError(err)
			require.True(t, ok)
			require.Equal(t, tc.expectedCode, s.Code())
			require.Contains(t, s.Message(), tc.expectedMessage)
		})
	}
}

func TestGRPCHandler_CheckToken(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)
	h := &GRPCHandler{
		store:     auth2.NewStorage(db, log),
		ds:        delegation2.NewStorage(db, log),
		log:       log,
		tp:        new(mockTicket),
		selfTvmId: 42,
	}

	tokens := []*delegation2.Token{
		{ServiceName: "ct", SecretId: "sec-42", Token: "t42", TvmId: 1337},
	}
	for _, m := range tokens {
		require.NoError(t, db.GormDb.Create(m).Error)
	}

	testCases := []struct {
		name          string
		request       *proto.CheckTokenRequest
		expectedState bool
		expectedCode  codes.Code
		expectedMiss  []string
		dt            *delegation2.Token
		yavResponse   interface{}
	}{
		{
			name:          "delegated",
			request:       &proto.CheckTokenRequest{ServiceName: "ct", SecretId: "sec-42", TvmId: 1337},
			dt:            &delegation2.Token{ServiceName: "ct", SecretId: "sec-42", Token: "t42-self", TvmId: 42},
			yavResponse:   client.SecretValues{},
			expectedState: true,
			expectedCode:  codes.OK,
			expectedMiss:  []string{},
		},
		{
			name:          "not_delegated",
			request:       &proto.CheckTokenRequest{ServiceName: "ct", SecretId: "sec-nonexistent"},
			expectedState: false,
			expectedCode:  codes.OK,
		},
		{
			name:          "version_error",
			request:       &proto.CheckTokenRequest{ServiceName: "ct", SecretId: "sec-422", VersionId: "non-existent-version"},
			dt:            &delegation2.Token{ServiceName: "ct", SecretId: "sec-422", TokenId: "token_version_err"},
			yavResponse:   &client.TokenError{Code: client.ErrCodeValidation},
			expectedState: false,
			expectedCode:  codes.OK,
		},
		{
			name: "required_keys_missing",
			request: &proto.CheckTokenRequest{
				ServiceName:  "ct",
				SecretId:     "missing-keys",
				VersionId:    "v44",
				RequiredKeys: []string{"k1", "k2"},
			},
			dt:            &delegation2.Token{ServiceName: "ct", SecretId: "missing-keys", Token: "missing-keys-token", TvmId: 42},
			yavResponse:   client.SecretValues{"k1": "xyz"},
			expectedState: true,
			expectedCode:  codes.OK,
			expectedMiss:  []string{"k2"},
		},
		{
			name:         "no_svc",
			request:      &proto.CheckTokenRequest{ServiceName: "", SecretId: "sec"},
			expectedCode: codes.InvalidArgument,
		},
		{
			name:         "no_sid",
			request:      &proto.CheckTokenRequest{ServiceName: "cc", SecretId: ""},
			expectedCode: codes.InvalidArgument,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			yav := new(mock.YavService)
			h.yavSvc = yav
			if tc.dt != nil {
				err := h.ds.Save(tc.dt)
				require.NoError(t, err)
				var y1, y2 interface{}
				switch tc.yavResponse.(type) {
				case error:
					y2 = tc.yavResponse
				default:
					y1 = tc.yavResponse
				}
				yav.On("SecretFromToken", tc.dt.Token, tc.request.ServiceName, tc.request.VersionId, "mock").
					Return(y1, y2)
			}
			r, err := h.CheckToken(context.TODO(), tc.request)
			if tc.expectedCode == codes.OK {
				require.NoError(t, err)
				require.Equal(t, tc.expectedState, r.IsDelegated)
				sort.Strings(tc.expectedMiss)
				sort.Strings(r.MissingKeys)
				require.Equal(t, tc.expectedMiss, r.MissingKeys, "missing keys check failed")
			} else {
				require.Error(t, err)
				s, _ := status.FromError(err)
				require.Equal(t, tc.expectedCode, s.Code())
			}
		})
	}
}

func Test_CheckTokenVersionError(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)
	h := &GRPCHandler{
		store:     auth2.NewStorage(db, log),
		ds:        delegation2.NewStorage(db, log),
		log:       log,
		tp:        new(mockTicket),
		selfTvmId: 1337,
	}

	tokens := []*delegation2.Token{
		{ServiceName: "ct", SecretId: "sec-42", Token: "t42", TvmId: 1337},
	}
	for _, m := range tokens {
		require.NoError(t, db.GormDb.Create(m).Error)
	}

	testCases := []struct {
		name           string
		request        *proto.CheckTokenRequest
		yavResponse    *client.TokenError
		expectedErrMsg string
	}{
		{
			name:           "Validation Error",
			request:        &proto.CheckTokenRequest{ServiceName: "ct", SecretId: "sec-42", VersionId: "ver-1", TvmId: 1337},
			yavResponse:    &client.TokenError{Code: client.ErrCodeValidation},
			expectedErrMsg: "service 'ct' has incorrect secret version 'ver-1'",
		},
		{
			name:           "Non existent entity error",
			request:        &proto.CheckTokenRequest{ServiceName: "ct", SecretId: "sec-42", VersionId: "ver-1", TvmId: 1337},
			yavResponse:    &client.TokenError{Code: client.ErrCodeNonExistentEntity},
			expectedErrMsg: "service 'ct' has incorrect secret version 'ver-1'",
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			yav := new(mock.YavService)
			h.yavSvc = yav
			yav.On("SecretFromToken", tokens[0].Token, tc.request.ServiceName, tc.request.VersionId, "mock").Return(nil, tc.yavResponse)

			r, err := h.CheckToken(context.TODO(), tc.request)
			require.NoError(t, err)
			require.Equal(t, tc.expectedErrMsg, r.Error.Error)
		})
	}
}

func TestGRPCHandler_GetDelegationToken(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)
	h := &GRPCHandler{
		store: auth2.NewStorage(db, log),
		ds:    delegation2.NewStorage(db, log),
		log:   log,
		tp:    new(mockTicket),
	}
	db.GormDb.Create(&delegation2.Token{
		ServiceName: "get-svc",
		SecretId:    "get-id",
		TvmId:       1337,
		TokenId:     "sample-token-id",
		Token:       "sample-token",
	})
	response, err := h.GetDelegationToken(context.Background(), &proto.GetDelegationTokenRequest{
		ServiceName: "get-svc",
		SecretId:    "get-id",
		TvmId:       1337,
	})
	require.NoError(t, err)
	require.Equal(t, "sample-token", response.Token)
	require.Equal(t, "sample-token-id", response.TokenId)
}

type mockTicket struct {
}

func (m *mockTicket) Ticket() (string, error) {
	return "mock", nil
}
