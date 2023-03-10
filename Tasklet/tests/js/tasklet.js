const { Input, Output } = require("./proto/tasklet/tests/proto/tasks_pb");
const { JobStatement, JobInstance, Requirements, JobResult } = require("./proto/tasklet/api/tasklet_pb")
const { Any } = require('google-protobuf/google/protobuf/any_pb.js');

function unpackInput(input) {
    return input ? input.unpack(Input.deserializeBinary, input.getTypeName()) : new Input();
}

function makeResult(success, output) {
    const result = new JobResult();
    result.setSuccess(true);
    const anyOutput = new Any();
    // FIXME: Where to get output type name and type url prefix?
    anyOutput.pack(output.serializeBinary(), 'Tasklet.Test.Output');
    result.setOutput(anyOutput);
    return Buffer.from(result.serializeBinary());
}

// TODO: export message name to protoc-generated JS code
const taskletName = "CompareTask";

module.exports = {
    getTaskletName(data, callback) {
        callback(null, Buffer.from(taskletName));
    },
    getPythonProtoImportPath(data, callback) {
        // TODO: get proto class name from JS code, which could get it from generated by protoc JS code
        const pyImportPath = data.toString("utf8").split('@')[0];
        callback(null, Buffer.from(pyImportPath));
    },
    getInitialDescription(data, callback) {
        const job = JobStatement.deserializeBinary(data);
        job.setRequirements(job.getRequirements() || new Requirements());
        job.setName(taskletName);
        callback(null, Buffer.from(job.serializeBinary()));
    },
    execute(data, callback) {
        const job = JobInstance.deserializeBinary(data);
        const input = unpackInput(job.getStatement().getInput());
        const a = input.getA();
        const b = input.getB();
        const output = new Output();
        output.setC(a < b ? -1 : (a > b ? 1 : 0));
        callback(null, makeResult(true, output));
    }
};
