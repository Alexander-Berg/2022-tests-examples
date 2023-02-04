package yacloud

import (
	"testing"
	"time"

	"github.com/YandexClassifieds/cms/cmd/server/clusters"
	"github.com/YandexClassifieds/cms/cmd/server/hosts"
	"github.com/YandexClassifieds/cms/cmd/server/hostsStatus"
	pbDC "github.com/YandexClassifieds/cms/pb/cms/domains/datacenters"
	pbHostStatuses "github.com/YandexClassifieds/cms/pb/cms/domains/host_statuses"
	pbHostTypes "github.com/YandexClassifieds/cms/pb/cms/domains/host_types"
	"github.com/YandexClassifieds/cms/test"
	mYacloud "github.com/YandexClassifieds/cms/test/mocks/mockery/server/yacloud"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/stretchr/testify/require"
	"gorm.io/gorm"
)

var (
	tHosts = []string{
		"docker-cloud-111-vla.test.vertis.yandex.net",
		"docker-cloud-222-vla.test.vertis.yandex.net",
	}
	tHostForRemove = "docker-cloud-333-vla.test.vertis.yandex.net"
	tHostType      = pbHostTypes.HostType_VIRTUAL_MACHINE
	tDC            = pbDC.DC_VLA
)

func TestService_syncFromCloudToDB(t *testing.T) {
	test.RunUp(t)
	db := test.NewSeparatedGorm(t)
	log := logrus.New()

	clusterStorage := clusters.NewStorage(db, log)
	hostStorage := hosts.NewStorage(db, log)
	hostsStatusStorage := hostsStatus.NewStorage(db, log)
	yacloudClient := newYaCloudClientMock()
	svc := newService(clusterStorage, hostStorage, hostsStatusStorage, yacloudClient, log)

	// create cluster
	err := svc.clusters.Save(tHostType, tDC, true, 1)
	require.NoError(t, err)
	clusterList, err := svc.clusters.ListByHostType(tHostType)
	require.NoError(t, err)
	clusterID := clusterList[0].ID

	// add host manually for checking further that syncFromCloudToDB() remove it
	require.NoError(t, hostStorage.UpsertByName(tHostForRemove, clusterID))
	host, err := hostStorage.GetByName(tHostForRemove)
	require.NoError(t, err)
	require.NoError(t, hostsStatusStorage.Save(&hostsStatus.HostsStatus{
		HostID: host.ID,
		Status: pbHostStatuses.Status_READY,
	}))

	go svc.syncFromCloudToDB()
	require.Eventually(t, func() bool { return len(getHosts(t, svc)) == len(tHosts) }, 10*time.Second, 100*time.Millisecond)

	rHosts := getHosts(t, svc)
	for _, host := range rHosts {
		require.NotEmpty(t, host.Name)
		require.Equal(t, clusterID, host.ClusterID)
		require.True(t, host.AutorepairEnable)
		require.False(t, host.Removed)
	}

	removedHost, err := svc.hosts.GetByName(tHostForRemove)
	require.NoError(t, err)
	require.True(t, removedHost.Removed)

	_, err = svc.hostsStatus.GetByHostID(removedHost.ID)
	require.Error(t, err, gorm.ErrRecordNotFound)
}

func getHosts(t *testing.T, svc *Service) map[string]*hosts.Host {
	rHosts, err := svc.hosts.GetNotRemovedByType(tHostType)
	require.NoError(t, err)

	return rHosts
}

func newYaCloudClientMock() *mYacloud.IClient {
	result := make(map[string]struct{})
	for _, host := range tHosts {
		result[host] = struct{}{}
	}

	yaMock := &mYacloud.IClient{}
	yaMock.On("GetHosts").Return(result, nil)

	return yaMock
}
