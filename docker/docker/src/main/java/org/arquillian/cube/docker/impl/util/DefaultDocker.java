package org.arquillian.cube.docker.impl.util;

import com.github.dockerjava.api.DockerClient;

public class DefaultDocker {

    public DockerClient getDefaultDockerClient(String defaultPath) {
          return DockerClientUtil.createDefaultDockerClient(defaultPath);
    }
}
