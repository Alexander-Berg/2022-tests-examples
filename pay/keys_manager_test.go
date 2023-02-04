package mastercard

import (
	"io/ioutil"
	"math"
	"net/http"
	"path/filepath"
	"sync"
	"testing"
	"time"

	"github.com/go-resty/resty/v2"
	"github.com/jarcoal/httpmock"
	"github.com/stretchr/testify/require"
	zaplog "go.uber.org/zap"

	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
)

const mockURL = "http://127.0.0.1/keys"

var validKeysResp3 = `{"keys":[{"kty":"RSA","e":"AQAB","use":"enc","kid":"149123-src-fpan-encryption","key_ops":["wrapKey","encrypt"],"alg":"RSA-OAEP-256","n":"vt4nDSPStTlM1NNcycvIqUf4x14I4jiTqMTKPjGtay0yfa1vByNChmuppDwET5gGGlpL8ccj3YVsBi9_bWoe_appkPwhxd7wR9RywV3zmWuMIhMwlk0lnHAML65nsHVM3oEpEvCfAPs1NXltTyfjnkgFENI3tHqtwdtM8eP02pp0jvW69fybvyVhLzXwSOgJntjtjRV7hQr5led_jWb5zzXI48OVTT_F9iinDdtX5y3E-if5WtGZUFETb_tZFZYnMLaLlHwvb6Zkr84RSwwsMf2nAL_4zP2UahMwzamhBoOSaqyxGxEq67Hr1U8zAC5hl9D8NbgSwpWxsODUrHx9rw"},{"kty":"RSA","e":"AQAB","use":"sig","kid":"149126-src-payload-verification","key_ops":["verify"],"alg":"RS256","n":"16F6ufTElkOD84pc1qfZD7znjfYj1h77GYLjhc8EjiB_dCTm0s7G9hfhVxn-w3YnFpRT69ZGLmvUmj240rnZdkCktLKP4x_u12ISUb4UEvnMD6ED2X1woPH4tRalSm8ZCKpl8kL9YBhekOwCjLWj6spBgN0ifdu36LxZl271HhKfq211OaFaVqZ_AL4lNOyopkLXzNdWj57D0vUzXYEfVEvSwRk2c4MbRE-bED03OhqOqRCByJK5uS9QGR2M5IE5JyQnTYkIFj9N9745Lr4U_jYRxJ-RQeZcamjI-ohxdkYXRyxxZirREhBI79tfnrdDLGs9pG2mn3oX0E8gpBGdTw"},{"kty":"RSA","e":"AQAB","use":"sig","kid":"149125-src-identity-verification","key_ops":["verify"],"alg":"RS256","n":"uvWxufMyd01QtLUmdMVFThZ3-fFnQhcmUyKqTXcnCO1-nX12Nak5mRfD66pJdtONH_fqByOlF5g3gK5Q3sge6ZGAtDckWckIqbsC9QCtI1-FgSBqtpSEkp61F3HFLAT5-H6vgORRfw_JqF4hI2ogZvBWSfOjvUDxbYjQuW1vNe9Sb7324ApE3vIThLfOaqSS3s1tk-X0QGQvNndchzcw044k1amrxDGMa-U4pRJDzMrWwZvBrXPRE0D9rrQXofytyQMq0wdH_vnfTXwnskWhJGc5eVq-3NPLLJjKG1edq-7dpKV2eMlwkRDa1jV7hQMubnSghL2WPSBYjH75sbu-oQ"}]}`
var validKeysResp2 = `{"keys": [{"kty": "RSA","e": "AQAB","use": "sig","kid": "149126-src-payload-verification","key_ops": ["verify"],"alg": "RS256","n": "16F6ufTElkOD84pc1qfZD7znjfYj1h77GYLjhc8EjiB_dCTm0s7G9hfhVxn-w3YnFpRT69ZGLmvUmj240rnZdkCktLKP4x_u12ISUb4UEvnMD6ED2X1woPH4tRalSm8ZCKpl8kL9YBhekOwCjLWj6spBgN0ifdu36LxZl271HhKfq211OaFaVqZ_AL4lNOyopkLXzNdWj57D0vUzXYEfVEvSwRk2c4MbRE-bED03OhqOqRCByJK5uS9QGR2M5IE5JyQnTYkIFj9N9745Lr4U_jYRxJ-RQeZcamjI-ohxdkYXRyxxZirREhBI79tfnrdDLGs9pG2mn3oX0E8gpBGdTw"},{"kty": "RSA","e": "AQAB","use": "sig","kid": "149125-src-identity-verification","key_ops": ["verify"],"alg": "RS256","n": "uvWxufMyd01QtLUmdMVFThZ3-fFnQhcmUyKqTXcnCO1-nX12Nak5mRfD66pJdtONH_fqByOlF5g3gK5Q3sge6ZGAtDckWckIqbsC9QCtI1-FgSBqtpSEkp61F3HFLAT5-H6vgORRfw_JqF4hI2ogZvBWSfOjvUDxbYjQuW1vNe9Sb7324ApE3vIThLfOaqSS3s1tk-X0QGQvNndchzcw044k1amrxDGMa-U4pRJDzMrWwZvBrXPRE0D9rrQXofytyQMq0wdH_vnfTXwnskWhJGc5eVq-3NPLLJjKG1edq-7dpKV2eMlwkRDa1jV7hQMubnSghL2WPSBYjH75sbu-oQ"}]}`

func mustCreateTestLogger() log.Logger {
	logger, err := zap.New(zaplog.NewDevelopmentConfig())
	if err != nil {
		panic(err)
	}
	return logger
}

func TestCanUpdateKeys(t *testing.T) {
	httpClient := resty.New().GetClient()

	httpmock.ActivateNonDefault(httpClient)
	defer httpmock.DeactivateAndReset()

	resp := newMockResponder().
		setResponse(200, validKeysResp3)

	httpmock.RegisterResponder("GET", mockURL,
		func(request *http.Request) (*http.Response, error) {
			return resp.respond(request)
		})

	km := NewPublicKeysManager(
		mustCreateTestLogger(),
		mockURL,
		WithKeysUpdatePeriod(time.Millisecond),
		WithHTTPClient(httpClient),
	)
	defer km.Close()

	// should return 3 keys from validKeysResp3
	require.Equal(t, 3, len(km.Get().Keys))

	// change keys response
	resp.setResponse(200, validKeysResp2)
	time.Sleep(100 * time.Millisecond)

	require.Equal(t, 2, len(km.Get().Keys))

	require.True(t, httpmock.GetTotalCallCount() > 10)
}

func TestRetryOnInvalidJson(t *testing.T) {
	httpClient := resty.New().GetClient()

	httpmock.ActivateNonDefault(httpClient)
	defer httpmock.DeactivateAndReset()

	resp := newMockResponder().
		setResponse(200, validKeysResp3)

	httpmock.RegisterResponder("GET", mockURL,
		func(request *http.Request) (*http.Response, error) {
			return resp.respond(request)
		})

	km := NewPublicKeysManager(
		mustCreateTestLogger(),
		mockURL,
		WithKeysUpdatePeriod(time.Millisecond),
		WithRetryParams(math.MaxInt32, time.Millisecond, time.Millisecond),
		WithHTTPClient(httpClient),
	)
	defer km.Close()

	require.Equal(t, 3, len(km.Get().Keys))

	// start return invalid json
	resp.setResponse(200, "{\"keys\": [{\"kty\": \"RSA\"")
	resp.resetCounter()

	time.Sleep(100 * time.Millisecond)

	// check retries
	actualFailed := resp.getCounter()
	require.True(t, actualFailed > 0)

	// check can get cached value
	require.Equal(t, 3, len(km.Get().Keys))
}

func TestRetryOnHTTP500(t *testing.T) {
	httpClient := resty.New().GetClient()

	httpmock.ActivateNonDefault(httpClient)
	defer httpmock.DeactivateAndReset()

	resp := newMockResponder().
		setResponse(200, validKeysResp3)

	httpmock.RegisterResponder("GET", mockURL,
		func(request *http.Request) (*http.Response, error) {
			return resp.respond(request)
		})

	km := NewPublicKeysManager(
		mustCreateTestLogger(),
		mockURL,
		WithKeysUpdatePeriod(time.Millisecond),
		WithRetryParams(math.MaxInt32, time.Millisecond, time.Millisecond),
		WithHTTPClient(httpClient),
	)
	defer km.Close()

	require.Equal(t, 3, len(km.Get().Keys))

	// start return errors
	resp.setResponse(500, validKeysResp3)
	resp.resetCounter()

	time.Sleep(100 * time.Millisecond)

	// check retries
	actualFailed := resp.getCounter()
	require.True(t, actualFailed > 20)

	// check can get cached value
	require.Equal(t, 3, len(km.Get().Keys))
}

func TestCanClose(t *testing.T) {
	httpClient := resty.New().GetClient()

	httpmock.ActivateNonDefault(httpClient)
	defer httpmock.DeactivateAndReset()

	resp := newMockResponder().
		setResponse(200, validKeysResp3)

	httpmock.RegisterResponder("GET", mockURL,
		func(request *http.Request) (*http.Response, error) {
			return resp.respond(request)
		})

	km := NewPublicKeysManager(
		mustCreateTestLogger(),
		mockURL,
		WithKeysUpdatePeriod(time.Millisecond),
		WithRetryParams(math.MaxInt32, time.Millisecond, time.Millisecond),
		WithHTTPClient(httpClient),
	)

	// wait for init
	_ = km.Get()
	// stop manager, wait for 100 ms and check new requests
	resp.resetCounter()
	km.Close()
	time.Sleep(100 * time.Millisecond)

	require.True(t, resp.getCounter() < 10)
}

func TestCache(t *testing.T) {
	httpClient := resty.New().GetClient()

	httpmock.ActivateNonDefault(httpClient)
	defer httpmock.DeactivateAndReset()

	resp := newMockResponder().
		setResponse(500, validKeysResp3)

	httpmock.RegisterResponder("GET", mockURL,
		func(request *http.Request) (*http.Response, error) {
			return resp.respond(request)
		})

	cachePath := filepath.Join(t.TempDir(), "cached-keys.json")
	err := ioutil.WriteFile(cachePath, []byte(validKeysResp2), 0644)
	require.NoError(t, err)

	km := NewPublicKeysManager(
		mustCreateTestLogger(),
		mockURL,
		WithKeysUpdatePeriod(time.Millisecond),
		WithRetryParams(math.MaxInt32, time.Millisecond, time.Millisecond),
		WithHTTPClient(httpClient),
		WithCachePath(cachePath),
	)

	defer km.Close()

	// initial data taken from cache
	require.Equal(t, 2, len(km.Get().Keys))

	resp.setResponse(200, validKeysResp3)
	time.Sleep(100 * time.Millisecond)

	// data updated from http request and saved to cache file
	require.Equal(t, 3, len(km.Get().Keys))
	km.Close()

	resp.setResponse(500, validKeysResp3)

	km2 := NewPublicKeysManager(
		mustCreateTestLogger(),
		mockURL,
		WithKeysUpdatePeriod(time.Millisecond),
		WithRetryParams(math.MaxInt32, time.Millisecond, time.Millisecond),
		WithHTTPClient(httpClient),
		WithCachePath(cachePath),
	)

	defer km2.Close()

	// initial data taken from last saved response
	require.Equal(t, 3, len(km2.Get().Keys))
}

type mockResponder struct {
	counter   int32
	responder httpmock.Responder
	lock      *sync.Mutex
}

func newMockResponder() *mockResponder {
	return &mockResponder{
		counter:   0,
		responder: nil,
		lock:      &sync.Mutex{},
	}
}

func (mr *mockResponder) setResponse(code int, data string) *mockResponder {
	mr.lock.Lock()
	defer mr.lock.Unlock()

	mr.responder = func(request *http.Request) (*http.Response, error) {
		mr.counter += 1
		resp := httpmock.NewStringResponse(code, data)
		resp.Header.Set("Content-Type", "application/json")
		return resp, nil
	}
	return mr
}

func (mr *mockResponder) resetCounter() {
	mr.lock.Lock()
	defer mr.lock.Unlock()
	mr.counter = 0
}

func (mr *mockResponder) respond(request *http.Request) (*http.Response, error) {
	mr.lock.Lock()
	defer mr.lock.Unlock()
	return mr.responder(request)
}

func (mr *mockResponder) getCounter() int32 {
	mr.lock.Lock()
	defer mr.lock.Unlock()
	return mr.counter
}
