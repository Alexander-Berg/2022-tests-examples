package lib

import (
	"bufio"
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"os"
	"os/exec"
	"strconv"
	"strings"

	"a.yandex-team.ru/library/go/test/recipe"
)

const (
	DisableCgroupsChecksPath        = "/home/qemu/cgroups_checks.disable"
	DisableCgroupsExcessCheckPath   = "/home/qemu/cgroups_excess_check.disable"
	DisableCgroupsShortageCheckPath = "/home/qemu/cgroups_shortage_check.disable"

	CustomAllowedCgroupsExcessPath   = "/home/qemu/allowed_cgroups_excess.conf"
	CustomAllowedCgroupsShortagePath = "/home/qemu/allowed_cgroups_shortage.conf"
)

var EnableCgroupsChecks, EnableCgroupsExcessCheck, EnableCgroupsShortageCheck = true, true, true
var AllowedCgroupsExcess, AllowedCgroupsShortage = 100, 100

type CmdWrapper struct {
	Cmd *exec.Cmd
}

type Recipe struct {
	wrapper CmdWrapper
}

type CgSubsys struct {
	Name      string `json:"subsys_name"`
	Hierarchy int    `json:"hierarchy"`
	Count     int    `json:"num_cgroups"`
	Enabled   bool   `json:"enabled"`
}

func (c *CmdWrapper) Command(name string, arg ...string) *exec.Cmd {
	if c.Cmd != nil {
		return &exec.Cmd{
			Path: c.Cmd.Path,
			Args: append(c.Cmd.Args, append([]string{name}, arg...)...),
		}

	}
	return exec.Command(name, arg...)
}

func (c *CmdWrapper) String() string {
	if c.Cmd != nil {
		return c.Cmd.String()
	}
	return ""
}

func (r *Recipe) initWrapper() {
	tw := os.Getenv("TEST_ENV_WRAPPER")
	c := strings.Fields(tw)

	if len(c) > 0 {
		r.wrapper.Cmd = exec.Command(c[0], c[1:]...)
		// fmt.Printf("CGLEAK_CHECK: set wrapper to %s\n", r.wrapper.Cmd.String())
	}
}

func ParseProcCgroups(r io.Reader) ([]CgSubsys, error) {
	subsystems := []CgSubsys{}

	s := bufio.NewScanner(r)
	for s.Scan() {
		text := s.Text()
		if text[0] != '#' {
			parts := strings.Fields(text)
			if len(parts) >= 4 && parts[3] != "0" {
				h, err := strconv.Atoi(parts[1])
				if err != nil {
					return nil, err
				}
				cnt, err := strconv.Atoi(parts[2])
				if err != nil {
					return nil, err
				}
				cgs := CgSubsys{
					Name:      parts[0],
					Hierarchy: h,
					Count:     cnt,
					Enabled:   true,
				}
				subsystems = append(subsystems, cgs)
			}
		}
	}
	if err := s.Err(); err != nil {
		return nil, err
	}
	return subsystems, nil
}

func (r Recipe) Start() error {
	r.initWrapper()
	fmt.Printf("CGLEAK_CHECK: Start with wrapper '%s'\n", r.wrapper.String())

	outBuf, err := catRemoteFile("/proc/cgroups")
	if err != nil {
		return fmt.Errorf("CGLEAK_CHECK: failed to 'cat' remote \"/proc/cgroups\": %v", err)
	}

	reader := bytes.NewReader([]byte(outBuf))
	grp, err := ParseProcCgroups(reader)
	if err != nil {
		return fmt.Errorf("CGLEAK_CHECK: cgroups parse fail: %v", err)
	}
	jData, err := json.Marshal(grp)
	if err != nil {
		return err
	}
	recipe.SetEnv("CGLEAK_CHECK_STATE", string(jData))
	return err
}

func (r Recipe) Stop() error {
	r.initWrapper()
	fmt.Printf("CGLEAK_CHECK: Stop with wrapper %s\n", r.wrapper.String())

	var grp1 []CgSubsys

	jData := os.Getenv("CGLEAK_CHECK_STATE")
	if jData == "" {
		return fmt.Errorf("CGLEAK_CHECK: CGLEAK_CHECK_STATE env is empty")
	}

	err := json.Unmarshal([]byte(jData), &grp1)
	if err != nil {
		return err
	}

	outBuf, err := catRemoteFile("/proc/cgroups")
	if err != nil {
		return fmt.Errorf("CGLEAK_CHECK: failed to 'cat' remote \"/proc/cgroups\": %v", err)
	}

	reader := bytes.NewReader([]byte(outBuf))
	grp2, err := ParseProcCgroups(reader)
	if err != nil {
		return fmt.Errorf("CGLEAK_CHECK: cgroups parse fail: %v", err)
	}
	cgMap := map[string]CgSubsys{}
	for _, g := range grp2 {
		cgMap[g.Name] = g
	}

	// Compare usage
	if EnableCgroupsShortageCheck {
		if RemoteFileExists(CustomAllowedCgroupsShortagePath) {
			numStr, err := catRemoteFile(CustomAllowedCgroupsShortagePath)
			if err != nil {
				fmt.Printf("CGLEAK_CHECK: failed to get CustomAllowedCgroupsShortage, fallback to default, err: %v\n", err)
			} else {
				num, err := strconv.Atoi(numStr)
				if err != nil {
					fmt.Printf("CGLEAK_CHECK: failed to convert CustomAllowedCgroupsShortage, fallback to default, err: %v\n", err)
				} else {
					AllowedCgroupsShortage = num
					fmt.Printf("CGLEAK_CHECK: AllowedCgroupsShortage is set to %v\n", AllowedCgroupsShortage)
				}
			}
		}
	} else {
		fmt.Println("CGLEAK_CHECK: Cgroups shortage check is disabled")
	}

	if EnableCgroupsExcessCheck {
		if RemoteFileExists(CustomAllowedCgroupsExcessPath) {
			numStr, err := catRemoteFile(CustomAllowedCgroupsExcessPath)
			if err != nil {
				fmt.Printf("CGLEAK_CHECK: failed to get CustomAllowedCgroupsExcess, fallback to default, err: %v\n", err)
			} else {
				num, err := strconv.Atoi(numStr)
				if err != nil {
					fmt.Printf("CGLEAK_CHECK: failed to convert CustomAllowedCgroupsExcess, fallback to default, err: %v\n", err)
				} else {
					AllowedCgroupsExcess = num
					fmt.Printf("CGLEAK_CHECK: AllowedCgroupsExcess is set to %v\n", AllowedCgroupsExcess)
				}
			}
		}
	} else {
		fmt.Println("CGLEAK_CHECK: Cgroups excess check is disabled")
	}

	for _, g1 := range grp1 {
		g2 := cgMap[g1.Name]
		fmt.Printf("CGLEAK_CHECK: Compare old:%v new:%v\n", g1, g2)
		if g2.Hierarchy != g1.Hierarchy {
			return fmt.Errorf("CGLEAK_CHECK: error detected, name: %s hierarchy: %d -> %d", g1.Name, g1.Hierarchy, g2.Hierarchy)
		}

		if EnableCgroupsShortageCheck {
			if g2.Count < (g1.Count - AllowedCgroupsShortage) {
				return fmt.Errorf("CGLEAK_CHECK: too much cgroups are gone: controller name: '%s', count change: %d -> %d (diff = %d, threshold = %d)", g1.Name, g1.Count, g2.Count, g2.Count-g1.Count, AllowedCgroupsShortage)
			}
		}

		if EnableCgroupsExcessCheck {
			if (g1.Count + AllowedCgroupsExcess) < g2.Count {
				return fmt.Errorf("CGLEAK_CHECK: cgroups leakage detected: controller name: '%s', count change: %d -> %d (diff = %d, threshold = %d)", g1.Name, g1.Count, g2.Count, g2.Count-g1.Count, AllowedCgroupsExcess)
			}
		}
	}
	return err
}

func RemoteFileExists(filename string) bool {
	r := Recipe{}
	r.initWrapper()
	c := r.wrapper.Command("test", "-f", filename)

	c.Stdout = nil
	c.Stderr = nil

	err := c.Run()

	return err == nil
}

func catRemoteFile(filename string) (string, error) {
	r := Recipe{}
	r.initWrapper()
	c := r.wrapper.Command("cat", filename)

	var stdout bytes.Buffer

	c.Stdout = &stdout
	c.Stderr = os.Stderr

	err := c.Run()
	if err != nil {
		return "", err
	}

	return stdout.String(), nil
}
