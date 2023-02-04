package app

import (
	"fmt"
	"io/ioutil"
	"net"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/common/config"
	"github.com/YandexClassifieds/shiva/pb/shiva/api/service_map"
	"github.com/YandexClassifieds/shiva/pkg/auth/blackbox"
	"github.com/YandexClassifieds/shiva/test"
	mocks "github.com/YandexClassifieds/shiva/test/mock"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/aws/aws-sdk-go/aws/credentials"
	v4 "github.com/aws/aws-sdk-go/aws/signer/v4"
	"github.com/stretchr/testify/assert"
	mock2 "github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"golang.org/x/oauth2"
)

func TestProxy_Success(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(Dump{}))

	s3Server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		require.Empty(t, r.Header.Get("cookie"))
		require.Empty(t, r.Header.Get("x-real-ip"))
		require.Empty(t, r.Header.Get("content-length"))
		require.NotEmpty(t, r.Header.Get("authorization"))
		require.NotEmpty(t, r.Header.Get("x-amz-date"))
		require.NotEmpty(t, r.Header.Get("x-amz-content-sha256"))
		require.Equal(t, "bytes=0-", r.Header.Get("range"))
		w.Header().Set("Content-Type", "text/csv")
		w.WriteHeader(200)
		_, err := w.Write([]byte("abcd"))
		require.NoError(t, err)
	}))
	defer s3Server.Close()

	serviceName := "dev-service"
	db.GormDb.Save(&Dump{
		ServiceName: serviceName,
		DumpUrl:     s3Server.URL,
		ExpireAt:    time.Now().Add(time.Minute),
	})
	require.NoError(t, db.GormDb.Error)

	bbClient := &blackbox.MockChecker{}
	login := "dev-user"
	bbClient.On("CheckSessionId", "sess-id", "sess-id", "localhost", net.ParseIP("127.0.0.1")).
		Return(&blackbox.UserInfo{Login: login}, nil)
	sMapClient := &mocks.ServiceMapsClient{}
	sMapClient.On("IsOwner", mock2.Anything, &service_map.IsOwnerRequest{
		Service: serviceName,
		Login:   login,
	}).Return(&service_map.IsOwnerResponse{IsOwner: true}, nil)
	storage := NewDumpStorage(db, test.NewLogger(t))
	signer := v4.NewSigner(credentials.NewStaticCredentials("a", "b", ""))
	proxy := RunProxyService(storage, test.NewLogger(t), "localhost:0", sMapClient, bbClient, "localhost", signer)
	require.Eventually(t, func() bool {
		return proxy.lis != nil
	}, time.Second, time.Second/10)

	url := fmt.Sprintf("http://%s/hprof/1", proxy.lis.Addr().String())
	req, err := http.NewRequest("GET", url, nil)
	require.NoError(t, err)
	req.Header.Add("x-forwarded-for", "127.0.0.1")
	req.Header.Add("x-real-ip", "127.0.0.1")
	req.Header.Add("content-length", "")
	req.Header.Add("range", "bytes=0-")
	req.AddCookie(&http.Cookie{
		Name:  "Session_id",
		Value: "sess-id",
	})
	req.AddCookie(&http.Cookie{
		Name:  "sessionid2",
		Value: "sess-id",
	})
	resp, err := http.DefaultClient.Do(req)
	require.NoError(t, err)
	defer func() {
		require.NoError(t, resp.Body.Close())
	}()

	buf, err := ioutil.ReadAll(resp.Body)
	require.NoError(t, err)

	assert.Equal(t, "abcd", string(buf))
	require.Equal(t, 200, resp.StatusCode)
	require.Equal(t, "text/csv", resp.Header.Get("Content-Type"))
}

func TestProxy_RealBucket(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(Dump{}))

	serviceName := "dev-service"
	db.GormDb.Save(&Dump{
		ServiceName: serviceName,
		DumpUrl:     "http://s3.mdst.yandex.net/hprof-courier/test/hello.txt",
		ExpireAt:    time.Now().Add(time.Minute),
	})
	require.NoError(t, db.GormDb.Error)

	bbClient := &blackbox.MockChecker{}
	login := "dev-user"
	bbClient.On("CheckSessionId", "sess-id", "sess-id", "localhost", mock2.Anything).
		Return(&blackbox.UserInfo{Login: login}, nil)
	sMapClient := &mocks.ServiceMapsClient{}
	sMapClient.On("IsOwner", mock2.Anything, &service_map.IsOwnerRequest{
		Service: serviceName,
		Login:   login,
	}).Return(&service_map.IsOwnerResponse{IsOwner: true}, nil)
	storage := NewDumpStorage(db, test.NewLogger(t))
	staticCredentials := credentials.NewStaticCredentials(
		config.Str("S3_ACCESS_KEY_ID"),
		config.Str("S3_ACCESS_SECRET_KEY"),
		"",
	)
	signer := v4.NewSigner(staticCredentials)
	proxy := RunProxyService(storage, test.NewLogger(t), "localhost:0", sMapClient, bbClient, "localhost", signer)
	require.Eventually(t, func() bool {
		return proxy.lis != nil
	}, time.Second, time.Second/10)

	url := fmt.Sprintf("http://%s/hprof/1", proxy.lis.Addr().String())
	req, err := http.NewRequest("GET", url, nil)
	require.NoError(t, err)
	req.Header.Set("content-length", "")
	req.Header.Set("upgrade-insecure-requests", "1")
	req.Header.Set("X-Forwarded-For", "127.0.0.1, 127.0.0.1")
	req.Header.Set("x-forwarded-host", "hprof.test.vertis.yandex-team.ru")
	req.Header.Set("x-forwarded-proto", "https")
	req.Header.Set("x-real-ip", "2a02:6b8:c0e:500:1:d:a57:8")
	req.Header.Set("x-request-host", "hprof.test.vertis.yandex-team.ru")
	req.Header.Set("x-request-id", "e4d98655e0ee32cf73d45b8cab2eb8e8")

	req.AddCookie(&http.Cookie{
		Name:  "Session_id",
		Value: "sess-id",
	})
	req.AddCookie(&http.Cookie{
		Name:  "sessionid2",
		Value: "sess-id",
	})
	resp, err := http.DefaultClient.Do(req)
	require.NoError(t, err)
	defer func() {
		require.NoError(t, resp.Body.Close())
	}()

	buf, err := ioutil.ReadAll(resp.Body)
	require.NoError(t, err)

	assert.Equal(t, "Hello from bucket!\n", string(buf))
	require.Equal(t, 200, resp.StatusCode)
	require.Equal(t, "text/plain", resp.Header.Get("Content-Type"))
}

func TestProxy_404(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	db.GormDb.AutoMigrate(Dump{})

	bbClient := &blackbox.MockChecker{}
	bbClient.On("CheckOAuthToken", &oauth2.Token{AccessToken: "12345"}, net.ParseIP("127.0.0.1")).
		Return(&blackbox.UserInfo{Login: "dev-user"}, nil)

	storage := NewDumpStorage(db, test.NewLogger(t))
	signer := v4.NewSigner(credentials.NewStaticCredentials("", "", ""))
	proxy := RunProxyService(storage, test.NewLogger(t), "localhost:0", nil, bbClient, "localhost", signer)
	require.Eventually(t, func() bool {
		return proxy.lis != nil
	}, time.Second, time.Second/10)

	url := fmt.Sprintf("http://%s/hprof/1", proxy.lis.Addr().String())
	req, err := http.NewRequest("GET", url, nil)
	require.NoError(t, err)
	req.Header.Set("Authorization", "Bearer 12345")
	resp, err := http.DefaultClient.Do(req)
	require.NoError(t, err)
	defer func() {
		require.NoError(t, resp.Body.Close())
	}()

	require.Equal(t, 404, resp.StatusCode)
}

func TestProxy_WrongMethod(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(Dump{}))

	bbClient := &blackbox.MockChecker{}
	bbClient.On("CheckOAuthToken", &oauth2.Token{AccessToken: "12345"}, net.ParseIP("127.0.0.1")).
		Return(&blackbox.UserInfo{Login: "dev-user"}, nil)

	storage := NewDumpStorage(db, test.NewLogger(t))
	signer := v4.NewSigner(credentials.NewStaticCredentials("", "", ""))
	proxy := RunProxyService(storage, test.NewLogger(t), "localhost:0", nil, bbClient, "localhost", signer)
	require.Eventually(t, func() bool {
		return proxy.lis != nil
	}, time.Second, time.Second/10)

	url := fmt.Sprintf("http://%s/hprof/1", proxy.lis.Addr().String())
	req, err := http.NewRequest("POST", url, nil)
	require.NoError(t, err)
	resp, err := http.DefaultClient.Do(req)
	require.NoError(t, err)
	defer func() {
		require.NoError(t, resp.Body.Close())
	}()

	require.Equal(t, http.StatusMethodNotAllowed, resp.StatusCode)
}
