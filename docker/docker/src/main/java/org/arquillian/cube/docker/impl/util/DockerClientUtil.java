package org.arquillian.cube.docker.impl.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig.Builder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

public class DockerClientUtil {
    private DockerClientUtil() {
    }

    public static DockerClient createDefaultDockerClient(String dockerHost) {
        final DockerClientConfig config = createDefaultDockerClientConfig(dockerHost);
        return createDefaultDockerClient(config);
    }

    public static DockerClient createDefaultDockerClient() {
        final DockerClientConfig config = createDefaultDockerClientConfig(null);
        return createDefaultDockerClient(config);
    }

    private static DockerClientConfig createDefaultDockerClientConfig(String dockerHost) {
        final Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();

        if (dockerHost != null) {
            configBuilder.withDockerHost(dockerHost);
        }
        return configBuilder.build();
    }

    public static DockerClient createDefaultDockerClient(DockerClientConfig config) {
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder().dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }
}