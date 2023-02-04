package helper_test

import (
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"
)

var (
	ServiceName0 = "my_service1"
	Version0     = "0.0.0.1"
	Branch0      = "my_branch"
	ContainerID0 = "00c8b435-db9b-8a62-df7b-69864cecd6e0"

	containerIDs = []string{
		ContainerID0,
		"00c8b435-db9b-8a62-df7b-69864cecd6e1",
		"00c8b435-db9b-8a62-df7b-69864cecd6e2",
	}

	HprofDirNames = []string{
		fmt.Sprintf("%s__%s__%s__false", ServiceName0, Version0, Branch0),
		"my_service2__0.0.0.2__my_branch__false",
		"my_service3__0.0.0.3__my_branch__true",
	}

	HprofFileName     = "java_pid1.hprof"
	logStdoutFileName = "autoru-api-task.stdout.0"
	logStderrFileName = "autoru-api-task.stderr.0"
)

func CreateTestData(pathPrefix, pathSuffix string) error {
	for i, containerID := range containerIDs {
		logDirPath := filepath.Join(pathPrefix, containerID, pathSuffix)
		hprofDirPath := filepath.Join(logDirPath, HprofDirNames[i])
		err := os.MkdirAll(hprofDirPath, 0755)
		if err != nil {
			return err
		}

		wrongTypeFile := filepath.Join(hprofDirPath, "f2.hprof")
		err = os.Mkdir(wrongTypeFile, 0755)
		if err != nil {
			return err
		}

		data := []byte("hello\ngo\n")
		filePaths := []string{
			// valid hprof file
			filepath.Join(hprofDirPath, HprofFileName),
			// invalid files (stdout, stderr logs)
			filepath.Join(logDirPath, logStdoutFileName),
			filepath.Join(logDirPath, logStderrFileName),
		}

		for _, f := range filePaths {
			err = ioutil.WriteFile(f, data, 0644)
			if err != nil {
				return err
			}
		}
	}

	return nil
}

func RemoveTestData(pathPrefix string) {
	err := os.RemoveAll(filepath.Dir(pathPrefix))
	if err != nil {
		panic(err)
	}
}
