package listener

import (
	mocks2 "github.com/YandexClassifieds/shiva/test/mock"
	"github.com/stretchr/testify/mock"
	"path/filepath"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/hprof-courier-pusher/files/helper_test"
	"github.com/YandexClassifieds/shiva/cmd/hprof-courier-pusher/files/listener/mocks"
	"github.com/YandexClassifieds/shiva/cmd/hprof-courier-pusher/files/operations"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/require"
)

func Test_searchAndProcessHprofs(t *testing.T) {
	test.InitTestEnv()
	log := test.NewLogger(t)
	tmpDir := t.TempDir()
	conf := NewConf()
	conf.pathPrefix = tmpDir + "/hprof"
	conf.hardlinkDirectory = tmpDir + "/hardlinks"

	oSvc := operations.NewService(test.NewLogger(t))
	hprofApiSvc := mocks.NewMockAPIService(log)
	s3Svc := mocks.NewMockS3Service(log)
	rateLimiter := mocks2.RateLimiter{}
	rateLimiter.On("Check", mock.Anything).Return(nil)
	svc := newService(log, conf, oSvc, hprofApiSvc, s3Svc, &rateLimiter)

	err := helper_test.CreateTestData(conf.pathPrefix, pathSuffix)
	require.NoError(t, err)

	err = svc.filesSvc.CreateHardlinksDirectory(conf.hardlinkDirectory)
	require.NoError(t, err)

	svc.searchAndProcessNewHprofs()
	require.Len(t, svc.filesSvc.Hprofs, 3)

	hLinks, err := oSvc.GetListOfEntries(conf.hardlinkDirectory)
	require.NoError(t, err)

	fName0 := filepath.Base(hLinks[0])
	hprofSvcData := svc.filesSvc.Hprofs[fName0]
	require.NotEmpty(t, hprofSvcData.OriginalPath)
	require.NotEmpty(t, hprofSvcData.HardlinkPath)
	require.NotEmpty(t, hprofSvcData.PreviousSize)
	require.NotEmpty(t, hprofSvcData.LastCheckSizeTs)
	require.Equal(t, helper_test.ServiceName0, hprofSvcData.HprofApiData.ServiceName)
	require.Equal(t, layer.Layer_TEST, hprofSvcData.HprofApiData.Layer)
	require.Equal(t, helper_test.Version0, hprofSvcData.HprofApiData.Version)
	require.Equal(t, false, hprofSvcData.HprofApiData.Canary)
	require.Equal(t, helper_test.Branch0, hprofSvcData.HprofApiData.Branch)
	require.Equal(t, helper_test.ContainerID0, hprofSvcData.HprofApiData.AllocationId)
	require.Equal(t, conf.hostname, hprofSvcData.HprofApiData.Host)
	require.Empty(t, hprofSvcData.HprofApiData.Created)
	require.Equal(t, operations.Unknown, hprofSvcData.S3Upload)
}

func Test_processHardlinkHprofs(t *testing.T) {
	test.InitTestEnv()
	log := test.NewLogger(t)
	tmpDir := t.TempDir()
	conf := NewConf()
	conf.pathPrefix = tmpDir + "/hprof"
	conf.hardlinkDirectory = tmpDir + "/hardlinks"

	oSvc := operations.NewService(test.NewLogger(t))
	hprofApiSvc := mocks.NewMockAPIService(log)
	s3Svc := mocks.NewMockS3Service(log)
	rateLimiter := mocks2.RateLimiter{}
	rateLimiter.On("Check", mock.Anything).Return(nil)
	svc := newService(log, conf, oSvc, hprofApiSvc, s3Svc, &rateLimiter)

	err := helper_test.CreateTestData(conf.pathPrefix, pathSuffix)
	require.NoError(t, err)

	err = svc.filesSvc.CreateHardlinksDirectory(conf.hardlinkDirectory)
	require.NoError(t, err)

	svc.searchAndProcessNewHprofs()
	hLinks, err := oSvc.GetListOfEntries(conf.hardlinkDirectory)
	require.NoError(t, err)
	fName0 := filepath.Base(hLinks[0])
	hprofSvcData := svc.filesSvc.Hprofs[fName0]

	time.Sleep(time.Duration(conf.checkSizeTimeLimit+1) * time.Second)
	svc.processHardlinkHprofs()
	require.Equal(t, operations.NeedUpload, hprofSvcData.S3Upload)
	require.Len(t, svc.filesSvc.Hprofs, 3)
	require.NotEmpty(t, hprofSvcData.HprofApiData.Created)

	svc.processHardlinkHprofs()
	require.Eventually(t, func() bool { return hprofSvcData.S3Upload == operations.Uploaded }, 3*time.Second, 10*time.Millisecond)
	require.Len(t, svc.filesSvc.Hprofs, 3)

	svc.processHardlinkHprofs()
	require.Len(t, svc.filesSvc.Hprofs, 0)
}
