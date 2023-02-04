package migrations

import (
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"sync"
	"sync/atomic"
	"testing"

	"github.com/stretchr/testify/suite"
)

func TestInitTestSuite(t *testing.T) {
	suite.Run(t, new(MigrationsTestSuite))
}

type MigrationsTestSuite struct {
	suite.Suite
}

var fakeFS map[string]string
var fakeFSMutex = sync.RWMutex{}

// сброс моков
func initOSFuncs() {
	fakeFSMutex.Lock()
	fakeFS = make(map[string]string)
	fakeFSMutex.Unlock()

	writeFile = func(path, body string) {
		fakeFSMutex.Lock()
		fakeFS[path] = body
		fakeFSMutex.Unlock()
	}

	fileExists = func(path string) bool {
		fakeFSMutex.RLock()
		_, exists := fakeFS[path]
		fakeFSMutex.RUnlock()
		return exists
	}
	exitWithCode = func(int) {}
}

func (s *MigrationsTestSuite) reqireFail() {
	fakeFSMutex.RLock()
	data, exists := fakeFS[semaforeFile]
	fakeFSMutex.RUnlock()
	s.Require().True(exists)
	s.Require().NotEqual(successSemaforeMsg, data)
}

// TestMigrationsSuccess тест механизма миграций при успешном сценарии
func (s *MigrationsTestSuite) TestMigrationsSuccess() {
	initOSFuncs()

	user := "user"
	password := "password"
	workDir := "/postgre"
	hosts := []string{"host1", "host2"}
	additionalParams := []string{"param3", "param4"}
	connStringSet := map[string]bool{}
	for _, host := range hosts {
		connStringSet[fmt.Sprintf(connectString, host, user, password)] = true
	}
	hostsJSON, err := json.Marshal(hosts)
	s.Require().NoError(err)

	_ = os.Setenv(hostsEnv, string(hostsJSON))
	_ = os.Setenv(userEnv, user)
	_ = os.Setenv(passwordEnv, password)
	_ = os.Setenv(workDirEnv, workDir)

	requireLenChan := make(chan int, len(hosts))
	requireTrueChan := make(chan bool, len(hosts))
	requireArgsChan := make(chan []string, len(hosts))

	var workerCount int32
	execWithParams = func(binary, workDir string, args []string) (combined string, err error) {
		_ = atomic.AddInt32(&workerCount, 1)
		// На этом месте должны быть закомментированные Require, но!
		// при несработавшем require прерывается горутина и не выполняется wg.Done, из-за чего имеем лок.
		// поэтому данные через каналы выкидываем наружу и там проверяем
		//s.Require().GreaterOrEqual(len(args), 2)
		requireLenChan <- len(args)
		//s.Require().True(connStringSet[args[1]])
		requireTrueChan <- connStringSet[args[1]]

		requireArgsChan <- args
		return "ok", nil
	}

	s.Require().Equal(1, TestReady())

	exitWithCode = func(code int) {
		s.Require().Equal(0, code)
	}

	// нужно добавить args[0] - имя собственного бинаря из os.Args
	Run(append([]string{"migration"}, additionalParams...))

	close(requireLenChan)
	close(requireTrueChan)
	close(requireArgsChan)
	for l := range requireLenChan {
		s.Require().GreaterOrEqual(l, 2)
	}
	for b := range requireTrueChan {
		s.Require().True(b)
	}
	for args := range requireArgsChan {
		for i := range additionalParams {
			s.Require().Equal(additionalParams[i], args[i+2])
		}
	}

	s.Require().Equal(int32(len(hosts)), workerCount)

	s.Require().Equal(0, TestReady())
}

// TestMigrationsSuccess тест различных неуспешных сценариев
func (s *MigrationsTestSuite) TestMigrationsFail() {
	initOSFuncs()
	_ = os.Unsetenv(hostsEnv)
	_ = os.Unsetenv(userEnv)
	_ = os.Unsetenv(passwordEnv)
	_ = os.Unsetenv(workDirEnv)

	execWithParams = func(binary, workDir string, args []string) (combined string, err error) {
		return "ok", nil
	}

	exitWithCode = func(code int) {
		s.Require().NotEqual(0, code) // ещё одна проверка на fail
	}
	Run([]string{"migration"})
	s.reqireFail()

	initOSFuncs()
	_ = os.Setenv(hostsEnv, `["host1","host2","host3","host4","host5","host6","host7","host8"]`)
	Run([]string{"migration"})
	s.reqireFail()

	initOSFuncs()
	_ = os.Setenv(userEnv, "user")
	Run([]string{"migration"})
	s.reqireFail()

	initOSFuncs()
	_ = os.Setenv(passwordEnv, "password")
	Run([]string{"migration"})
	s.reqireFail()

	initOSFuncs()
	_ = os.Setenv(workDirEnv, "/postgre")
	mu := sync.Mutex{}
	count := 0
	execWithParams = func(binary, workDir string, args []string) (combined string, err error) {
		mu.Lock()
		if count == 3 { // проверка, что хотя бы при одной ошибке получаем fail
			err = errors.New("")
		}
		count++
		mu.Unlock()
		return
	}
	Run([]string{"migration"})
	s.reqireFail()
}

// TestMigrationsSuccess тест различных неуспешных сценариев
func (s *MigrationsTestSuite) TestReadiness() {
	initOSFuncs()
	execWithParams = func(binary, workDir string, args []string) (combined string, err error) {
		return "ok", nil
	}
	s.Require().NotEqual(0, TestReady()) // проверяем, что не отработавший не готов
	Run([]string{"migration"})
	s.Require().Equal(0, TestReady()) // проверяем, что отработавший с ошибкой готов

	initOSFuncs()
	_ = os.Setenv(hostsEnv, `["host1"]`)
	_ = os.Setenv(userEnv, "user")
	_ = os.Setenv(passwordEnv, "password")
	_ = os.Setenv(workDirEnv, "/postgre")
	execWithParams = func(binary, workDir string, args []string) (combined string, err error) {
		return "ok", nil
	}
	Run([]string{"migration"})
	s.Require().Equal(0, TestReady()) // проверяем, что отработавший без ошибки готов
}
