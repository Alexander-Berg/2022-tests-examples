package client

import (
	"encoding/json"
	"io"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/YandexClassifieds/shiva/common"
	"github.com/stretchr/testify/require"
)

func TestClient_GetSecret(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		_, err := io.Copy(w, strings.NewReader(getSecretResponse))
		require.Equal(t, "OAuth test-token", r.Header.Get("Authorization"))
		require.NoError(t, err)
	}))
	defer server.Close()

	cli := &client{
		conf: newConf(server.URL),
		http: *server.Client(),
	}
	response, err := cli.GetSecret("sec-0000000000000000000000ygj0")
	require.NoError(t, err)

	expectedResponse := &SecretInfo{
		CreatedAt:    1445385600,
		CreatedBy:    100,
		CreatorLogin: "vault-test-100",
		Versions: []SecretInfoVersion{
			{CreatedAt: 1445385603, CreatedBy: 100, CreatorLogin: "vault-test-100", VersionId: "ver-0000000000000000000000ygj4", Keys: []string{"password"}},
			{CreatedAt: 1445385602, CreatedBy: 100, CreatorLogin: "vault-test-100", VersionId: "ver-0000000000000000000000ygj3", Keys: []string{"password"}},
			{CreatedAt: 1445385601, CreatedBy: 100, CreatorLogin: "vault-test-100", VersionId: "ver-0000000000000000000000ygj2", Keys: []string{"password"}},
			{CreatedAt: 1445385600, CreatedBy: 100, CreatorLogin: "vault-test-100", VersionId: "ver-0000000000000000000000nbk1", Keys: []string{"cert", "password"}},
		},
	}
	require.Equal(t, expectedResponse, response)
}

func TestClient_GetSecretByVersion(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		_, err := io.Copy(w, strings.NewReader(getVersionResponse))
		require.Equal(t, "OAuth test-token", r.Header.Get("Authorization"))
		require.NoError(t, err)
	}))
	defer server.Close()

	cli := &client{
		conf: newConf(server.URL),
		http: *server.Client(),
	}
	response, err := cli.GetSecretByVersionOrSecretId("ver-0000000000000000000000nbk1")
	require.NoError(t, err)

	expectedResponse := &VersionInfo{
		CreatedAt:  1445385600,
		SecretName: "secret_1",
		SecretId:   "sec-0000000000000000000000ygj0",
		VersionId:  "ver-0000000000000000000000nbk1",
		Values:     map[string]string{"cert": "U2VjcmV0IGZpbGU=", "password": "123"},
	}
	require.Equal(t, expectedResponse, response)
}

func TestClient_CanRead(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		responseStr := `{"access":"allowed","status":"ok"}`
		_, err := io.Copy(w, strings.NewReader(responseStr))
		require.NoError(t, err)
	}))
	defer server.Close()
	cli := &client{
		conf: newConf(server.URL),
		http: *server.Client(),
	}
	can, err := cli.CanRead("sec-xyz", "11200000001076600")
	require.NoError(t, err)

	require.True(t, can)
}

func TestClient_CreateDelegationToken(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		var err error
		require.Equal(t, "POST", r.Method)
		require.Equal(t, "OAuth test-token", r.Header.Get("Authorization"))
		reqBody, err := ioutil.ReadAll(r.Body)
		require.NoError(t, err)

		require.Equal(t, `{"signature":"svc","tvm_client_id":1337}`, string(reqBody))
		w.WriteHeader(http.StatusOK)
		responseStr := `{"token":"some-token-value","token_uuid":"some-token-id","status":"ok"}`
		_, err = io.Copy(w, strings.NewReader(responseStr))
		require.NoError(t, err)
	}))
	defer server.Close()
	cli := &client{
		conf: newConf(server.URL),
		http: *server.Client(),
	}
	info, err := cli.CreateDelegationToken("test-secret", "svc", 1337)
	require.NoError(t, err)

	require.Equal(t, "some-token-id", info.TokenID)
	require.Equal(t, "some-token-value", info.Token)
}

func TestClient_RevokeDelegationToken(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		require.Equal(t, "POST", r.Method)
		require.Equal(t, "OAuth test-token", r.Header.Get("Authorization"))
		require.Equal(t, "/1/tokens/some-token-id/revoke/", r.URL.Path)

		w.WriteHeader(http.StatusOK)
		responseStr := `{"status":"ok"}`
		_, err := io.Copy(w, strings.NewReader(responseStr))
		require.NoError(t, err)
	}))
	defer server.Close()
	cli := &client{
		conf: newConf(server.URL),
		http: *server.Client(),
	}
	err := cli.RevokeDelegationToken("some-token-id")
	require.NoError(t, err)
}

func TestClient_RevokeDelegationToken_Error(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		require.Equal(t, "POST", r.Method)
		require.Equal(t, "OAuth test-token", r.Header.Get("Authorization"))
		w.WriteHeader(http.StatusInternalServerError)
	}))
	defer server.Close()
	cli := &client{
		conf: newConf(server.URL),
		http: *server.Client(),
	}
	err := cli.RevokeDelegationToken("some-token-id")
	require.Error(t, err)
	require.IsType(t, &common.HttpStatusError{}, err)
	e := err.(*common.HttpStatusError)
	require.Equal(t, e.Code, http.StatusInternalServerError)
}

func TestClient_AddUserRole(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		require.Equal(t, "POST", r.Method)
		require.Equal(t, "OAuth test-token", r.Header.Get("Authorization"))
		require.Equal(t, "/1/secrets/sec-id/roles/?role=OWNER&login=login", r.RequestURI)

		w.WriteHeader(http.StatusOK)
		responseStr := `{"status":"ok"}`
		_, err := io.Copy(w, strings.NewReader(responseStr))
		require.NoError(t, err)
	}))
	defer server.Close()
	cli := &client{
		conf: newConf(server.URL),
		http: *server.Client(),
	}
	err := cli.AddUserRole("sec-id", "login", "OWNER")
	require.NoError(t, err)
}

func TestClient_SecretFromToken(t *testing.T) {
	t.Run("without_version_id", testTokenNoVersion)
	t.Run("with_version_id", testTokenWithVersion)
}

func TestClient_Readers(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		_, err := io.Copy(w, strings.NewReader(getReadersResponse))
		require.NoError(t, err)
		require.Equal(t, "GET", r.Method)
		require.Equal(t, "/1/secrets/sec-0000000000000000000000ygj0/readers/", r.URL.Path)
	}))
	defer server.Close()

	cli := &client{
		conf: newConf(server.URL),
		http: *server.Client(),
	}

	readersInfo, err := cli.Readers("sec-0000000000000000000000ygj0")
	require.NoError(t, err)
	require.Len(t, readersInfo.Readers, 1)
	expectedInfo := &ReadersInfo{
		Name: "secret_1",
		Readers: []*RoleHolderInfo{
			{
				Login:     "vault-test-100",
				RoleSlug:  "OWNER",
				StaffID:   0,
				StaffSlug: "",
				StaffUrl:  "",
			},
		},
	}

	require.Equal(t, expectedInfo, readersInfo)
}

func TestClient_Owners(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		_, err := io.Copy(w, strings.NewReader(getOwnersResponse))
		require.NoError(t, err)
		require.Equal(t, "GET", r.Method)
		require.Equal(t, "/1/secrets/sec-0000000000000000000000ygj0/owners/", r.URL.Path)
	}))
	defer server.Close()

	cli := &client{
		conf: newConf(server.URL),
		http: *server.Client(),
	}

	ownersInfo, err := cli.Owners("sec-0000000000000000000000ygj0")
	require.NoError(t, err)
	require.Len(t, ownersInfo.Owners, 1)
	expectedInfo := &OwnersInfo{
		Name: "secret_1",
		Owners: []*RoleHolderInfo{
			{
				Login:     "vault-test-100",
				RoleSlug:  "OWNER",
				StaffID:   0,
				StaffSlug: "",
				StaffUrl:  "",
			},
		},
	}
	require.Equal(t, expectedInfo, ownersInfo)
}

func TestClient_CreateSecret(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		responseStr := `{"status":"ok", "uuid":"sec-100"}`
		_, err := io.Copy(w, strings.NewReader(responseStr))
		require.NoError(t, err)
		require.Equal(t, "POST", r.Method)
		require.Equal(t, "OAuth test-token", r.Header.Get("Authorization"))
		require.Equal(t, "/1/secrets", r.RequestURI)

		reqBody, err := ioutil.ReadAll(r.Body)
		require.NoError(t, err)

		require.Equal(t, `{"name":"test-name"}`, string(reqBody))
	}))
	defer server.Close()

	cli := &client{
		conf: newConf(server.URL),
		http: *server.Client(),
	}
	secId, err := cli.CreateSecret("test-name")
	require.NoError(t, err)
	require.Equal(t, "sec-100", secId)
}

func TestClient_AddGroupRole(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		require.Equal(t, "POST", r.Method)
		require.Equal(t, "OAuth test-token", r.Header.Get("Authorization"))
		require.Equal(t, "/1/secrets/sec-id/roles/?role=OWNER&staff_id=766", r.RequestURI)

		w.WriteHeader(http.StatusOK)
		responseStr := `{"status":"ok"}`
		_, err := io.Copy(w, strings.NewReader(responseStr))
		require.NoError(t, err)
	}))
	defer server.Close()
	cli := &client{
		conf: newConf(server.URL),
		http: *server.Client(),
	}
	err := cli.AddGroupRole("sec-id", 766, "OWNER")
	require.NoError(t, err)
}

func testTokenNoVersion(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		var err error
		dec := json.NewDecoder(r.Body)
		req := tokenRequest{}
		err = dec.Decode(&req)
		require.NoError(t, err)

		require.True(t, len(req.TokenizedRequests) > 0, "tokenized requests are empty")
		require.Equal(t, "my-token", req.TokenizedRequests[0].Token)
		require.Equal(t, "some-ticket", req.TokenizedRequests[0].ServiceTicket)
		require.Equal(t, "tst-service", req.TokenizedRequests[0].Signature)
		enc := json.NewEncoder(w)
		err = enc.Encode(tokenResponse{
			Status: "ok",
			Secrets: []tokenResponseSecret{
				{
					Status: "ok",
					Values: []secretValue{
						{Key: "foo", Value: "bar"},
						{Key: "k2", Value: "v2"},
					},
				},
			},
		})
		require.NoError(t, err)
	}))
	defer server.Close()
	cli := &client{
		conf: newConf(server.URL),
		http: *server.Client(),
	}
	data, err := cli.SecretFromToken("my-token", "tst-service", "", "some-ticket")
	require.NoError(t, err)

	require.Len(t, data, 2)
	require.Equal(t, "bar", data["foo"])
	require.Equal(t, "v2", data["k2"])
}

func TestClient_DeleteUserRole(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		require.Equal(t, "DELETE", r.Method)
		require.Equal(t, "OAuth test-token", r.Header.Get("Authorization"))
		require.Equal(t, "/1/secrets/sec-id/roles/?role=OWNER&login=login", r.RequestURI)

		w.WriteHeader(http.StatusOK)
		responseStr := `{"status":"ok"}`
		_, err := io.Copy(w, strings.NewReader(responseStr))
		require.NoError(t, err)
	}))
	defer server.Close()
	cli := &client{
		conf: newConf(server.URL),
		http: *server.Client(),
	}
	err := cli.DeleteUserRole("sec-id", "login", "OWNER")
	require.NoError(t, err)
}

func TestClient_DeleteGroupRole(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		require.Equal(t, "DELETE", r.Method)
		require.Equal(t, "OAuth test-token", r.Header.Get("Authorization"))
		require.Equal(t, "/1/secrets/sec-id/roles/?role=OWNER&staff_id=766", r.RequestURI)

		w.WriteHeader(http.StatusOK)
		responseStr := `{"status":"ok"}`
		_, err := io.Copy(w, strings.NewReader(responseStr))
		require.NoError(t, err)
	}))
	defer server.Close()
	cli := &client{
		conf: newConf(server.URL),
		http: *server.Client(),
	}
	err := cli.DeleteGroupRole("sec-id", 766, "OWNER")
	require.NoError(t, err)
}

func testTokenWithVersion(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		var err error
		dec := json.NewDecoder(r.Body)
		req := tokenRequest{}
		err = dec.Decode(&req)
		require.NoError(t, err)

		require.True(t, len(req.TokenizedRequests) > 0, "tokenized requests are empty")
		require.Equal(t, "ver-42", req.TokenizedRequests[0].VersionId)
		require.Equal(t, "my-token", req.TokenizedRequests[0].Token)
		require.Equal(t, "some-ticket", req.TokenizedRequests[0].ServiceTicket)
		require.Equal(t, "tst-service", req.TokenizedRequests[0].Signature)
		enc := json.NewEncoder(w)
		err = enc.Encode(tokenResponse{
			Status: "ok",
			Secrets: []tokenResponseSecret{
				{
					Status: "ok",
					Values: []secretValue{
						{Key: "foo", Value: "bar"},
						{Key: "k2", Value: "v2"},
					},
				},
			},
		})
		require.NoError(t, err)
	}))
	defer server.Close()
	cli := &client{
		conf: newConf(server.URL),
		http: *server.Client(),
	}
	data, err := cli.SecretFromToken("my-token", "tst-service", "ver-42", "some-ticket")
	require.NoError(t, err)

	require.Len(t, data, 2)
	require.Equal(t, "bar", data["foo"])
	require.Equal(t, "v2", data["k2"])
}

func newConf(url string) Conf {
	return Conf{
		URL:   url,
		Token: "test-token",
	}
}

var (
	getSecretResponse = `{
  "page": 0,
  "page_size": 50,
  "secret": {
    "acl": [
      {
        "created_at": 1445385600,
        "created_by": 100,
        "creator_login": "vault-test-100",
        "login": "vault-test-100",
        "role_slug": "OWNER",
        "uid": 100
      }
    ],
    "created_at": 1445385600,
    "created_by": 100,
    "creator_login": "vault-test-100",
    "effective_role": "OWNER",
    "name": "secret_1",
    "secret_roles": [
      {
        "created_at": 1445385600,
        "created_by": 100,
        "creator_login": "vault-test-100",
        "login": "vault-test-100",
        "role_slug": "OWNER",
        "uid": 100
      }
    ],
    "secret_versions": [
      {
        "created_at": 1445385603,
        "created_by": 100,
        "creator_login": "vault-test-100",
        "keys": [
          "password"
        ],
        "version": "ver-0000000000000000000000ygj4"
      },
      {
        "created_at": 1445385602,
        "created_by": 100,
        "creator_login": "vault-test-100",
        "keys": [
          "password"
        ],
        "version": "ver-0000000000000000000000ygj3"
      },
      {
        "created_at": 1445385601,
        "created_by": 100,
        "creator_login": "vault-test-100",
        "keys": [
          "password"
        ],
        "version": "ver-0000000000000000000000ygj2"
      },
      {
        "created_at": 1445385600,
        "created_by": 100,
        "creator_login": "vault-test-100",
        "keys": [
          "cert",
          "password"
        ],
        "version": "ver-0000000000000000000000nbk1"
      }
    ],
    "tokens": [
      {
        "created_at": 1445385600,
        "created_by": 100,
        "creator_login": "vault-test-100",
        "signature": "123",
        "state_name": "normal",
        "token_uuid": "tid-0000000000000000000000nbk0",
        "tvm_app": {
          "abc_department": {
            "display_name": "Паспорт",
            "id": 14,
            "unique_name": "passp"
          },
          "abc_state": "granted",
          "abc_state_display_name": "Выдан",
          "name": "social api (dev)",
          "tvm_client_id": 2000367
        },
        "tvm_client_id": 2000367
      }
    ],
    "updated_at": 1445385600,
    "updated_by": 100,
    "uuid": "sec-0000000000000000000000ygj0"
  },
  "status": "ok"
}`

	getVersionResponse = `{
  "status": "ok",
  "version": {
    "created_at": 1445385600,
    "created_by": 100,
    "creator_login": "vault-test-100",
    "secret_name": "secret_1",
    "secret_uuid": "sec-0000000000000000000000ygj0",
    "value": [
      {
        "key": "password",
        "value": "123"
      },
      {
        "encoding": "base64",
        "key": "cert",
        "value": "U2VjcmV0IGZpbGU="
      }
    ],
    "version": "ver-0000000000000000000000nbk1"
  }
}`

	getReadersResponse = `{
  "name": "secret_1",
  "readers": [
    {
      "created_at": 1445385600,
      "created_by": 100,
      "creator_login": "vault-test-100",
      "login": "vault-test-100",
      "role_slug": "OWNER",
      "uid": 100
    }
  ],
  "secret_uuid": "sec-0000000000000000000000ygj0",
  "status": "ok"
}`

	getOwnersResponse = `
{
  "name": "secret_1",
  "owners": [
    {
      "created_at": 1445385600,
      "created_by": 100,
      "creator_login": "vault-test-100",
      "login": "vault-test-100",
      "role_slug": "OWNER",
      "uid": 100
    }
  ],
  "secret_uuid": "sec-0000000000000000000000ygj0",
  "status": "ok"
}`
)
