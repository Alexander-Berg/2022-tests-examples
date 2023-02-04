package operations

import (
	"fmt"
	"os"
	"path"
	"path/filepath"
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/hprof-courier-pusher/files/helper_test"
	"github.com/YandexClassifieds/shiva/common/config"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/require"
)

var (
	fPath, fHardlinkPath, pathPrefix, hardlinkDirPath, hostname string
	pathSuffix                                                  = "/alloc/logs"

	fName  = "my_service__0.0.0.1__my_branch__false__abc0101010101010110__1613663640.hprof"
	fName2 = "my_service__0.0.0.1____false__abc0101010101010110__1613663640.hprof"

	nameData = map[string]string{
		"serviceName":  "my_service",
		"version":      "0.0.0.1",
		"allocationId": "abc0101010101010110",
		"branch":       "my_branch",
		"canary":       "false",
		"rand":         "1613663640",
	}
	nameData2 = map[string]string{
		"serviceName":  "my_service",
		"version":      "0.0.0.1",
		"allocationId": "abc0101010101010110",
		"branch":       "",
		"canary":       "false",
		"rand":         "1613663640",
	}
)

func init() {
	test.InitTestEnv()

	pathPrefix = config.Str("PREFIX_PATH")
	hardlinkDirPath = config.Str("HARDLINK_DIRECTORY")
	hostname = config.Str("_DEPLOY_HOSTNAME")
	fPath = fmt.Sprintf("%s/%s/%s/%s/%s", pathPrefix, helper_test.ContainerID0, pathSuffix, helper_test.HprofDirNames[0], helper_test.HprofFileName)
}

func TestService_GetListOfEntries(t *testing.T) {
	err := helper_test.CreateTestData(pathPrefix, pathSuffix)
	require.NoError(t, err)
	defer helper_test.RemoveTestData(pathPrefix)

	svc := NewService(test.NewLogger(t))
	entries, err := svc.GetListOfEntries(pathPrefix)
	require.NoError(t, err)
	require.Len(t, entries, 3)
}

func TestService_GetFileSize(t *testing.T) {
	err := helper_test.CreateTestData(pathPrefix, pathSuffix)
	require.NoError(t, err)
	defer helper_test.RemoveTestData(pathPrefix)

	svc := NewService(test.NewLogger(t))
	fileSize, err := svc.GetFileSize(fPath)
	require.NoError(t, err)
	require.Equal(t, int64(9), fileSize)
}

func TestService_GetFileModifyTime(t *testing.T) {
	err := helper_test.CreateTestData(pathPrefix, pathSuffix)
	require.NoError(t, err)
	defer helper_test.RemoveTestData(pathPrefix)

	svc := NewService(test.NewLogger(t))
	_, err = svc.GetFileModifyTime(fPath)
	require.NoError(t, err)
}

func TestService_getDataFromName(t *testing.T) {
	data := getDataFromName(fName)
	require.Equal(t, nameData, data)

	data2 := getDataFromName(fName2)
	require.Equal(t, nameData2, data2)
}

func TestIsValidNewFilenameFormat(t *testing.T) {
	svc := NewService(test.NewLogger(t))
	res := svc.IsValidHprofDirFormat("my_service__0.0.0.1____false.hprof")
	require.True(t, res)
	res = svc.IsValidHprofDirFormat("my_service__0.0.0.1__mbranch__false.hprof")
	require.True(t, res)
	res = svc.IsValidHprofDirFormat("autoru-api-task.stderr.0")
	require.False(t, res)
}

func Test_CleanData(t *testing.T) {
	err := helper_test.CreateTestData(pathPrefix, pathSuffix)
	require.NoError(t, err)
	defer helper_test.RemoveTestData(pathPrefix)

	svc := NewService(test.NewLogger(t))
	require.NoError(t, svc.CreateHardlinksDirectory(hardlinkDirPath))

	newFilePath, err := renameFile(fPath, helper_test.ContainerID0)
	require.NoError(t, err)

	filename := filepath.Base(newFilePath)
	hardlinkPath := path.Join(hardlinkDirPath, filename)
	require.NoError(t, os.Link(newFilePath, hardlinkPath))
	data, err := svc.PrepareSvcData(newFilePath, hardlinkPath, hostname)
	require.NoError(t, err)
	svc.Hprofs[filename] = data
	require.Len(t, svc.Hprofs, 1)

	err = svc.CleanData(data)
	require.NoError(t, err)
	require.Len(t, svc.Hprofs, 0)
}

func Test_CleanData_EmptyOriginalPath(t *testing.T) {
	err := helper_test.CreateTestData(pathPrefix, pathSuffix)
	require.NoError(t, err)
	defer helper_test.RemoveTestData(pathPrefix)

	svc := NewService(test.NewLogger(t))
	require.NoError(t, svc.CreateHardlinksDirectory(hardlinkDirPath))

	newFilePath, err := renameFile(fPath, helper_test.ContainerID0)
	require.NoError(t, err)

	filename := filepath.Base(newFilePath)
	hardlinkPath := path.Join(hardlinkDirPath, filename)
	require.NoError(t, os.Link(newFilePath, hardlinkPath))
	data, err := svc.PrepareSvcData("", hardlinkPath, hostname)
	require.NoError(t, err)
	svc.Hprofs[filename] = data
	require.Len(t, svc.Hprofs, 1)

	err = svc.CleanData(data)
	require.NoError(t, err)
	require.Len(t, svc.Hprofs, 0)
}
