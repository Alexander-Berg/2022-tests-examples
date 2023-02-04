package updater

import (
	"context"
	"github.com/YandexClassifieds/mesh-control/pb/shiva/api/deploy"
	smapi "github.com/YandexClassifieds/mesh-control/pb/shiva/api/service_map"
	sm "github.com/YandexClassifieds/mesh-control/pb/shiva/service_map"
	"github.com/YandexClassifieds/mesh-control/pb/shiva/types/layer"
	"github.com/envoyproxy/go-control-plane/pkg/cache/v3"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc"
	"io"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"
)

func TestService_Run(t *testing.T) {
	caSrv := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		writer.Write([]byte(testCert))
	}))
	defer caSrv.Close()
	cfg := &Config{
		DataCenters:    []string{"dc1"},
		Layer:          "test",
		RootCAURL:      caSrv.URL,
		UpdateInterval: time.Second,
	}
	mc := new(mockMapClient)
	dc := new(mockDeployClient)
	cg := new(mockCertGetter)
	testMaps := []*smapi.ServiceData{
		{
			Service: &sm.ServiceMap{Name: "foo"},
		},
		{
			Service: &sm.ServiceMap{Name: "grpc-echo-droog-client"},
		},
	}
	testStates := []*deploy.StatusResponse_Info{
		{Service: "foo", Layer: layer.Layer_TEST},
	}
	mc.On("ListAll", mock.Anything, &smapi.ListRequest{}).Return(&stubListing{Data: testMaps}, nil)
	dc.On("AllStatus", mock.Anything, &deploy.AllStatusRequest{}).Return(&deploy.StatusResponse{Info: testStates}, nil)
	cg.On("GetCert", certIssueReq{Service: "foo",Provides: []string{}}).Return(&certInfo{}, nil)
	cg.On("GetCert", certIssueReq{Service: "grpc-echo-droog-client",Provides: []string{}}).Return(&certInfo{}, nil)

	logger := logrus.New()

	sc := cache.NewSnapshotCache(false, ServiceHash{}, nil)

	svc, err := NewService(cfg, sc, nil, mc, dc, logger)
	svc.certUpdater = cg
	require.NoError(t, err)
	svc.Run()
	<-time.After(time.Second*1)

	_, err = sc.GetSnapshot("foo")
	require.NoError(t, err)
}

var (
	testCert = `-----BEGIN CERTIFICATE-----
MIIFMjCCAxqgAwIBAgIUKarjoEFu8jN9kDGRID0Haf8ifHEwDQYJKoZIhvcNAQEL
BQAwFTETMBEGA1UEAxMKdGVzdC52YXVsdDAeFw0yMDA2MzAxMDI5NDFaFw00MDA2
MjUxMDMwMTBaMBUxEzARBgNVBAMTCnRlc3QudmF1bHQwggIiMA0GCSqGSIb3DQEB
AQUAA4ICDwAwggIKAoICAQDHeYQ0XPbCXPGL2XHlnEbcY8/HECQrRpUpUmI1ZidF
5Y2/uuBXSX5X66rZH3r4ZlUM0P40JeGnInnXLjNDMgonBRFwklK0fMtHqHdHOE2W
siAsTO5kh689IM3+ensgvAFWC6Fwk4jonShwl7KU2rfUAd3JFOZv1O5AlnE8cYhW
y2Rmlzfy2ExTdHO3vp4KxUJT7D+Rr/BvRfl+I3z4kKg0qkTHgwS+mj18BM+63ApF
+IPij1xMZztTIasRCa8oXP0cOiADEnaMDhBrEPm5i3B4QQf+xpggAQ9yTF4QB7ku
McYyIG9ZBvd8/yZmHUVS/t3ZLcv1NrX57qp6Ht2uvW6pT/YiX4OZBk6VFFqlnIWg
6sSaP8lKEPYYVwpOmXqbM+kHGk/YvvgoF9d075VJZTh/DieGM1j4/MCJ64rhi5Jm
TVFAlnTyZIij5Q3rX1uq2SekAY1D4wYDk/HUPPyuXdBJ6wJQjdNdgks8hNbcIohn
iScx32zPAMk4isaEfadnHiz9M3v3MzMFBOwiaTtrFjp4yrCLSLu5vodhSc5Th4uL
KevSN9lLpRB9gZ6b/2LI9LCJ/xblM5Uvff5R8mF2NZrCmiZ1/Y3IHUwZKb8pcfSx
5CjA4dLV3rspLRoLqwPMvrBf6XctJk2fMytJDeKVjvJsRR/cFNd8XINXKKRv9g89
ZwIDAQABo3oweDAOBgNVHQ8BAf8EBAMCAQYwDwYDVR0TAQH/BAUwAwEB/zAdBgNV
HQ4EFgQUgkOHoUBmfEhgF2g/9BR7Nev23nYwHwYDVR0jBBgwFoAUgkOHoUBmfEhg
F2g/9BR7Nev23nYwFQYDVR0RBA4wDIIKdGVzdC52YXVsdDANBgkqhkiG9w0BAQsF
AAOCAgEAAh2SbsEoQgQ+LP8MKiAsoofBO3LUzLouLmzV8EUUe2DSPniRRn950yme
ISsVqo4kMeZr6rj+tdWJ/ZR/+gEchfQUmveUyePMFnK9zhrsNRlj4jE3fs5m0iGl
MZudhPKRC/83nroC8vxgeNlQiX6mHHp3pCcm/wRjSy2y5Eamq1V3n29ruP39e1dz
LqjEFdN5KJrvRLM7Ecs2/vdRNSHvGNj9JmdC4nukGK+M/k4p3yxwHgz2Bvw91b1l
sDd/4LyCKZkVxU2/H5zfB5tf5bLswYp5sh1/bvMzjUKLeqS3VRBqH+jRucg1UO77
C0+B68PS8xhPOuBfcTpD4hSC9Wrlsw93l1otBS8M+2kYZVPY9SQ95wJFVyJGjeEw
rvyi6Efyf7rPT3LUpYl2gE+0OGUL3VYkP4JWqMZwdjzwSoR/igV5nqv+k+fgA/Jz
DvPj5Vw8yjQhMILEzLW0E56cyRVVNpT4sUBdI11WjgZaqx5G5SaJ6jrNzqsJExyP
eHR+TZnyXq5dmyE+TJvRSrjcRuSHSTioMD8CeQBz+oNlOGpec8+uYD+e96Hv8BBC
V26BfSV446pjQcGQhoirkqLICkZtqxVrXzMfMK8Lir0XY++6U9FhztcQuHzTbTw8
SEN71LdNfWDulJzQ8POAhG24xaX55WMvcpwsqf977dXq6wzcqxY=
-----END CERTIFICATE-----`
)

type mockCertGetter struct {
	mock.Mock
}

func (m *mockCertGetter) GetCert(req certIssueReq) (*certInfo, error) {
	args := m.Called(req)
	if v, ok := args.Get(0).(*certInfo); ok {
		return v, nil
	}
	return nil, args.Error(1)
}

type mockMapClient struct {
	mock.Mock
}

func (m *mockMapClient) ListAll(ctx context.Context, in *smapi.ListRequest, opts ...grpc.CallOption) (smapi.ServiceMaps_ListAllClient, error) {
	args := m.Called(ctx, in)
	return args.Get(0).(smapi.ServiceMaps_ListAllClient), nil
}

func (m *mockMapClient) Get(ctx context.Context, in *smapi.GetRequest, opts ...grpc.CallOption) (*smapi.ServiceData, error) {
	panic("implement me")
}

func (m *mockMapClient) IsOwner(ctx context.Context, in *smapi.IsOwnerRequest, opts ...grpc.CallOption) (*smapi.IsOwnerResponse, error) {
	panic("implement me")
}

type stubListing struct {
	Data []*smapi.ServiceData
	grpc.ClientStream
	idx int
}

func (s *stubListing) Recv() (*smapi.ServiceData, error) {
	if s.idx < len(s.Data) {
		data := s.Data[s.idx]
		s.idx++
		return data, nil
	}
	s.idx = 0
	return nil, io.EOF
}

type mockDeployClient struct {
	mock.Mock
}

func (m *mockDeployClient) Run(ctx context.Context, in *deploy.RunRequest, opts ...grpc.CallOption) (deploy.DeployService_RunClient, error) {
	panic("implement me")
}

func (m *mockDeployClient) AsyncRun(ctx context.Context, in *deploy.RunRequest, opts ...grpc.CallOption) (*deploy.AsyncResponse, error) {
	panic("implement me")
}

func (m *mockDeployClient) Stop(ctx context.Context, in *deploy.StopRequest, opts ...grpc.CallOption) (deploy.DeployService_StopClient, error) {
	panic("implement me")
}

func (m *mockDeployClient) Restart(ctx context.Context, in *deploy.RestartRequest, opts ...grpc.CallOption) (deploy.DeployService_RestartClient, error) {
	panic("implement me")
}

func (m *mockDeployClient) Revert(ctx context.Context, in *deploy.RevertRequest, opts ...grpc.CallOption) (deploy.DeployService_RevertClient, error) {
	panic("implement me")
}

func (m *mockDeployClient) State(ctx context.Context, in *deploy.StateRequest, opts ...grpc.CallOption) (deploy.DeployService_StateClient, error) {
	panic("implement me")
}

func (m *mockDeployClient) Cancel(ctx context.Context, in *deploy.CancelRequest, opts ...grpc.CallOption) (deploy.DeployService_CancelClient, error) {
	panic("implement me")
}

func (m *mockDeployClient) Promote(ctx context.Context, in *deploy.PromoteRequest, opts ...grpc.CallOption) (deploy.DeployService_PromoteClient, error) {
	panic("implement me")
}

func (m *mockDeployClient) Approve(ctx context.Context, in *deploy.ApproveRequest, opts ...grpc.CallOption) (deploy.DeployService_ApproveClient, error) {
	panic("implement me")
}

func (m *mockDeployClient) ApproveList(ctx context.Context, in *deploy.ApproveListRequest, opts ...grpc.CallOption) (*deploy.ApproveListResponse, error) {
	panic("implement me")
}

func (m *mockDeployClient) Status(ctx context.Context, in *deploy.StatusRequest, opts ...grpc.CallOption) (*deploy.StatusResponse, error) {
	panic("implement me")
}

func (m *mockDeployClient) AllStatus(ctx context.Context, in *deploy.AllStatusRequest, opts ...grpc.CallOption) (*deploy.StatusResponse, error) {
	return m.Called(ctx, in).Get(0).(*deploy.StatusResponse), nil
}

func (m *mockDeployClient) ReleaseHistory(ctx context.Context, in *deploy.ReleaseHistoryRequest, opts ...grpc.CallOption) (*deploy.ReleaseHistoryResponse, error) {
	panic("implement me")
}
