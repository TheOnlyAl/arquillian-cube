package org.arquillian.cube.docker.impl.await;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectExecResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import java.util.logging.Logger;
import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.util.DockerOutputResultCallback;
import org.arquillian.cube.docker.impl.util.Ping;
import org.arquillian.cube.docker.impl.util.PingCommand;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeOutput;

public class DockerHealthAwaitStrategy extends SleepingAwaitStrategyBase {

    private static final Logger log = Logger.getLogger(DockerHealthAwaitStrategy.class.getName());

    public static final String TAG = "docker_health";

    private static final int DEFAULT_POLL_ITERATIONS = 10;

    private int pollIterations;

    private Cube<?> cube;
    private DockerClient client;
    private String[] command;

    public DockerHealthAwaitStrategy(Cube<?> cube, DockerClientExecutor dockerClientExecutor, Await params) {
        super(params.getSleepPollingTime());
        this.cube = cube;
        this.client = dockerClientExecutor.getDockerClient();

        this.pollIterations = getIterations(params);
        this.command = params.getCommand();
    }

    @Override
    public boolean await() {
        PingCommand pingCommand;
        if (command != null) {
            pingCommand = new ExecPingCommand();
        } else {
            pingCommand = new DockerHealthPingCommand();
        }
        return Ping.ping(pollIterations, getSleepTime(), getTimeUnit(), pingCommand);
    }

    private int getIterations(Await params) {
        if (params.getIterations() != null) {
            return params.getIterations();
        } else {
            return DEFAULT_POLL_ITERATIONS;
        }
    }

    private class ExecPingCommand implements PingCommand {
        @Override
        public boolean call() {
            try {
                final String execID = client
                    .execCreateCmd(cube.getId())
                    .withCmd(command)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec()
                    .getId();

                CubeOutput cubeOutput = client
                    .execStartCmd(cube.getId())
                    .withTty(true)
                    .withExecId(execID)
                    .withDetach(false)
                    .exec(new DockerOutputResultCallback()).awaitCompletion().getCubeOutput();

                final InspectExecResponse inspectExecResponse = client
                    .inspectExecCmd(execID)
                    .exec();

                log.info(() -> String.format(
                    "docker exec %s, exit status:%d, stdout:\n%s\n, stderr: /n%s\n",
                    String.join(" ", command),
                    inspectExecResponse.getExitCodeLong(),
                    cubeOutput.getStandard(),
                    cubeOutput.getError()));

                return inspectExecResponse.getExitCodeLong() == 0l;
            } catch (InterruptedException e) {
                return false;
            }
        }
    }

    private class DockerHealthPingCommand implements PingCommand {
        @Override
        public boolean call() {
            try {
                return client
                    .inspectContainerCmd(cube.getId())
                    .exec()
                    .getState()
                    .getHealth()
                    .getStatus()
                    .equals("healthy");
            } catch (NotFoundException e) {
                return false;
            }
        }
    }
}
