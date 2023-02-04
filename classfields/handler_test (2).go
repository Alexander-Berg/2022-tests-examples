package api

import (
	"context"
	"testing"

	"github.com/YandexClassifieds/cms/cmd/server/checks"
	"github.com/YandexClassifieds/cms/cmd/server/clusters"
	"github.com/YandexClassifieds/cms/cmd/server/hosts"
	"github.com/YandexClassifieds/cms/cmd/server/hostsStatus"
	proto "github.com/YandexClassifieds/cms/pb/cms/api/server"
	pbDC "github.com/YandexClassifieds/cms/pb/cms/domains/datacenters"
	pbHostStatus "github.com/YandexClassifieds/cms/pb/cms/domains/host_statuses"
	pbHostTypes "github.com/YandexClassifieds/cms/pb/cms/domains/host_types"
	"github.com/YandexClassifieds/cms/test"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/stretchr/testify/require"
)

var (
	hostType   = pbHostTypes.HostType_BAREMETAL
	dc1        = pbDC.DC_SAS
	hostname   = "docker-111-sas.prod.vertis.yandex.net"
	clusterID  uint
	autorepair = false
)

func TestHandler_SetClusterAutorepair(t *testing.T) {
	test.RunUp(t)

	handler := newHandler(t)

	err := handler.clusters.Save(hostType, dc1, autorepair, 1)
	require.NoError(t, err)

	req := &proto.SetClusterAutorepairEnableRequest{
		HostType:   pbHostTypes.HostType_BAREMETAL,
		Dc:         pbDC.DC_SAS,
		Autorepair: true,
	}

	_, err = handler.SetClusterAutorepair(context.Background(), req)
	require.NoError(t, err)
}

func TestHandler_SetHostAutorepair_NoErr(t *testing.T) {
	test.RunUp(t)

	handler := newHandler(t)

	err := handler.hosts.Save(hostname, clusterID)
	require.NoError(t, err)

	req := &proto.SetHostAutorepairEnableRequest{
		Name:       hostname,
		Autorepair: false,
	}

	_, err = handler.SetHostAutorepair(context.Background(), req)
	require.NoError(t, err)
}

func TestHandler_SetHostAutorepair_EmptyName(t *testing.T) {
	test.RunUp(t)

	handler := newHandler(t)

	req := &proto.SetHostAutorepairEnableRequest{
		Name:       "",
		Autorepair: true,
	}

	_, err := handler.SetHostAutorepair(context.Background(), req)
	require.Error(t, err)
}

func TestHandler_GetHostStatus(t *testing.T) {
	test.RunUp(t)

	handler := newHandler(t)

	err := handler.hosts.Save(hostname, clusterID)
	require.NoError(t, err)
	id, err := handler.hosts.GetIDByName(hostname)
	require.NoError(t, err)
	err = handler.hostsStatus.Save(&hostsStatus.HostsStatus{
		HostID: id,
		Status: pbHostStatus.Status_READY,
	})
	require.NoError(t, err)

	req := &proto.GetHostStatusRequest{
		Name: hostname,
	}

	_, err = handler.GetHostStatus(context.Background(), req)
	require.NoError(t, err)
}

func TestHandler_GetClusterStatus(t *testing.T) {
	test.RunUp(t)

	handler := newHandler(t)

	err := handler.clusters.Save(hostType, dc1, autorepair, 1)
	require.NoError(t, err)
	cluster, err := handler.clusters.GetByTypeAndDC(hostType, dc1)
	require.NoError(t, err)

	readyHost := "docker-901-sas.prod.vertis.yandex.net"
	addHost(t, handler, readyHost, cluster.ID, pbHostStatus.Status_READY, true, false)

	notReadyHost := "docker-902-sas.prod.vertis.yandex.net"
	addHost(t, handler, notReadyHost, cluster.ID, pbHostStatus.Status_NEW, true, false)

	autorepairDisabledHost := "docker-903-sas.prod.vertis.yandex.net"
	addHost(t, handler, autorepairDisabledHost, cluster.ID, pbHostStatus.Status_READY, false, false)

	removedHost := "docker-904-sas.prod.vertis.yandex.net"
	addHost(t, handler, removedHost, cluster.ID, pbHostStatus.Status_READY, false, true)

	req := &proto.GetClusterStatusRequest{
		HostType: hostType,
		Dc:       dc1,
	}
	result, err := handler.GetClusterStatus(context.Background(), req)
	require.NoError(t, err)

	require.Equal(t, hostType, result.HostType)
	require.Equal(t, dc1, result.Dc)
	require.Equal(t, false, result.Autorepair)
	require.Equal(t, false, result.Removed)
	require.Equal(t, int64(1), result.NotReadyLimit)
	require.Equal(t, int64(3), result.TotalHostsNumber)
	require.Equal(t, int64(2), result.ReadyHostsNumber)
	require.Equal(t, int64(1), result.NotReadyHostsNumber)

	require.Len(t, result.Hosts, 3)
}

func newHandler(t *testing.T) *Handler {
	db := test.NewGorm(t)
	log := logrus.New()

	clusterStorage := clusters.NewStorage(db, log)
	hostStorage := hosts.NewStorage(db, log)
	hostStatus := hostsStatus.NewStorage(db, log)
	checks := checks.NewStorage(db, log)

	return NewHandler(clusterStorage, hostStorage, hostStatus, checks, log)
}

func addHost(t *testing.T, handler *Handler, hostname string, clusterID uint, status pbHostStatus.Status, autorepairEnable, removed bool) {
	err := handler.hosts.Save(hostname, clusterID)
	require.NoError(t, err)

	id, err := handler.hosts.GetIDByName(hostname)
	require.NoError(t, err)

	err = handler.hosts.SetAutorepairEnableByName(hostname, autorepairEnable)
	require.NoError(t, err)

	err = handler.hosts.SetRemovedByName(hostname, removed)
	require.NoError(t, err)

	err = handler.hostsStatus.Save(&hostsStatus.HostsStatus{
		HostID: id,
		Status: status,
	})
	require.NoError(t, err)
}
