package main

import (
	"a.yandex-team.ru/balancer/production/x/iptables_daemon/ipset"
	"a.yandex-team.ru/balancer/production/x/iptables_daemon/mocks"
	"a.yandex-team.ru/balancer/production/x/iptables_daemon/types"
	"a.yandex-team.ru/library/go/core/log/nop"
	"bytes"
	"fmt"
	"github.com/golang/mock/gomock"
	"github.com/golang/protobuf/proto"
	"io"
	"strconv"
	"strings"
	"testing"
	"time"
)

// for Close method
type BytesWriter struct {
	impl bytes.Buffer
}

func (b *BytesWriter) Close() error {
	return nil
}

func (b *BytesWriter) String() string {
	return b.impl.String()
}

func (b *BytesWriter) Write(p []byte) (n int, err error) {
	return b.impl.Write(p)
}

type FakeCmdExecutor struct {
	cmd              FakeCmdWithPipe
	executedCommands []string
}

type FakeCmdWithPipe struct {
	pipeWrite BytesWriter
}

func (e *FakeCmdExecutor) GetExecutedPipeCommands() []string {
	return strings.Split(e.cmd.pipeWrite.String(), "\n")
}

func (e *FakeCmdExecutor) GetExecutedCommands() []string {
	return e.executedCommands
}

func (e *FakeCmdExecutor) ExecCmd(s string) ipset.CmdRunResult {
	e.executedCommands = append(e.executedCommands, s)
	return ipset.CmdRunResult{Err: nil, StdOut: "", StdErr: ""}
}

func (e *FakeCmdExecutor) NewCmdWithPipe(name string, args ...string) (ipset.ICmdWithPipe, error) {
	return &e.cmd, nil
}

func (c *FakeCmdWithPipe) Wait() ipset.CmdRunResult {
	return ipset.CmdRunResult{Err: nil, StdOut: "", StdErr: ""}
}

func (c *FakeCmdWithPipe) GetWritePipe() io.WriteCloser {
	return &c.pipeWrite
}

type BytesReader struct {
	impl bytes.Buffer
}

func (r *BytesReader) Close() error {
	return nil
}

func (r *BytesReader) DropLastByte() {
	if r.impl.Len() > 0 {
		r.impl.Truncate(r.impl.Len() - 1)
	}
}

func (r *BytesReader) Read(b []byte) (n int, err error) {
	return r.impl.Read(b)
}

func NewBytesReaderFromStr(s string) *BytesReader {
	return &BytesReader{impl: *bytes.NewBufferString(s)}
}

func GetSimpleCbbResponse(ips []string, timeout uint64, t *testing.T) (BytesReader, uint64) {
	ts := uint64(time.Now().Unix())
	numIps := uint32(len(ips))
	response := bytes.Buffer{}

	header := types.TCbbIpResponceHeader{
		LastCreatedTs: ts,
		NumIps:        numIps,
	}
	out, err := proto.Marshal(&header)
	if err != nil {
		t.Errorf("Unable to marshal header, reason: \"%v\"", err)
	}
	response.WriteByte(byte(len(out)))
	response.Write(out)

	for _, ip := range ips {
		item := types.TCbbIpResponceItem{
			Ip:        ip,
			ExpiredTs: uint32(ts + timeout),
		}
		out, err := proto.Marshal(&item)
		if err != nil {
			t.Errorf("Unable to marshal item, reason: \"%v\"", err)
		}
		response.WriteByte(byte(len(out)))
		response.Write(out)
	}

	return BytesReader{response}, ts
}

func TestUpdateIPSetWithFailedRequest(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	cmdExecutor := FakeCmdExecutor{}
	cbb := mocks.NewMockIClient(ctrl)
	metrics := mocks.NewMockIMetrics(ctrl)
	logger := nop.Logger{}

	ipset := ipset.New("v4", "v6", &cmdExecutor)

	cbb.EXPECT().Fetch(metrics, &logger).Return(nil, fmt.Errorf("error"))

	metrics.EXPECT().ReportCbbHTTPError().Times(1)
	metrics.EXPECT().ReportExecTimeMs(gomock.Any(), gomock.Any()).AnyTimes()

	UpdateIPSet(cbb, ipset, metrics, &logger)
}

func TestUpdateIPSetWithEmptyResponse(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	cmdExecutor := FakeCmdExecutor{}
	cbb := mocks.NewMockIClient(ctrl)
	metrics := mocks.NewMockIMetrics(ctrl)
	logger := nop.Logger{}

	ipset := ipset.New("v4", "v6", &cmdExecutor)

	cbb.EXPECT().Fetch(metrics, &logger).Return(&BytesReader{}, nil)
	cbb.EXPECT().UpdateLastTS(uint64(0))

	metrics.EXPECT().ReportCbbHTTPError().Times(1)
	metrics.EXPECT().ReportExecTimeMs(gomock.Any(), gomock.Any()).AnyTimes()

	UpdateIPSet(cbb, ipset, metrics, &logger)
}

func TestUpdateIPSetWithCorruptedResponse(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	cmdExecutor := FakeCmdExecutor{}
	cbb := mocks.NewMockIClient(ctrl)
	metrics := mocks.NewMockIMetrics(ctrl)
	logger := nop.Logger{}

	ipset := ipset.New("v4", "v6", &cmdExecutor)

	cbb.EXPECT().Fetch(metrics, &logger).Return(NewBytesReaderFromStr(" this is clearly not valid response"), nil)
	cbb.EXPECT().UpdateLastTS(uint64(0))

	metrics.EXPECT().ReportCbbBadResponse().Times(1)
	metrics.EXPECT().ReportExecTimeMs(gomock.Any(), gomock.Any()).AnyTimes()

	UpdateIPSet(cbb, ipset, metrics, &logger)
}

func TestUpdateIPSetWithCroppedResponse(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	cmdExecutor := FakeCmdExecutor{}
	cbb := mocks.NewMockIClient(ctrl)
	metrics := mocks.NewMockIMetrics(ctrl)
	logger := nop.Logger{}

	ipset := ipset.New("v4", "v6", &cmdExecutor)
	ips := []string{"111.111.0.0", "7e57:7e57::0", "111.111.0.1", "7e57:7e57::1"}
	numIps := uint32(len(ips))
	timeout := uint64(2000)
	response, ts := GetSimpleCbbResponse(ips, timeout, t)
	response.DropLastByte()
	_ = ts

	cbb.EXPECT().Fetch(metrics, &logger).Return(&response, nil)
	cbb.EXPECT().UpdateLastTS(uint64(0))

	metrics.EXPECT().ReportCbbHTTPError().Times(1)
	metrics.EXPECT().ReportCbbIPAdded().Times(int(numIps) - 1)
	metrics.EXPECT().ReportExecTimeMs(gomock.Any(), gomock.Any()).AnyTimes()

	UpdateIPSet(cbb, ipset, metrics, &logger)
}

func TestUpdateIPSetHappyPath(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	cmdExecutor := FakeCmdExecutor{}
	cbb := mocks.NewMockIClient(ctrl)
	metrics := mocks.NewMockIMetrics(ctrl)
	logger := nop.Logger{}

	ipset := ipset.New("v4", "v6", &cmdExecutor)
	ips := []string{"111.111.0.0", "7e57:7e57::0", "111.111.0.1", "7e57:7e57::1"}
	setNames := []string{"v4", "v6", "v4", "v6"}
	numIps := uint32(len(ips))
	timeout := uint64(2000)
	response, ts := GetSimpleCbbResponse(ips, timeout, t)

	cbb.EXPECT().Fetch(metrics, &logger).Return(&response, nil)
	cbb.EXPECT().UpdateLastTS(ts)

	metrics.EXPECT().ReportCbbIPAdded().Times(int(numIps))
	metrics.EXPECT().ReportExecTimeMs(gomock.Any(), gomock.Any()).AnyTimes()

	UpdateIPSet(cbb, ipset, metrics, &logger)

	executedCommands := cmdExecutor.GetExecutedPipeCommands()
	commandsNum := uint32(len(executedCommands))
	if commandsNum != numIps+1 {
		t.Errorf("expected \"%d\" commands, got \"%d\", commands: \"%v\"", numIps+1, commandsNum, executedCommands)
	}

	for i, cmd := range executedCommands[:numIps] {
		cmdArgs := strings.Split(cmd, " ")
		cmdArgsNum := uint32(len(cmdArgs))
		if cmdArgsNum != uint32(6) {
			t.Errorf("expected %d command args, got %d, cmd: \"%s\"", 6, cmdArgsNum, cmd)
		}
		if cmdArgs[0] != "add" || cmdArgs[1] != setNames[i] || cmdArgs[2] != ips[i] || cmdArgs[3] != "timeout" || cmdArgs[5] != "-exist" {
			t.Errorf("expected command \"add %s %s timeout %d -exist\" but got \"%s\"", setNames[i], ips[i], timeout, cmd)
		}
		cmdTimeout, err := strconv.ParseInt(cmdArgs[4], 10, 64)
		if err != nil {
			t.Errorf("cannot parse timeout from cmd: \"%s\"", cmd)
		}
		if float64(cmdTimeout) < 0.5*float64(timeout) || float64(cmdTimeout) > 2.0*float64(timeout) {
			t.Errorf("timeout parse from cmd is too high(too low), got %d, expected %d, cmd \"%s\"", cmdTimeout, timeout, cmd)
		}
		if executedCommands[numIps] != "quit" {
			t.Errorf("missing the quit command, commands: \"%v\"", executedCommands)
		}
	}
}
