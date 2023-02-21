package org.arquillian.cube.docker.impl.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.arquillian.cube.spi.CubeOutput;

import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;

/**
 * Result callback which is used to capture the standard output and error of a docker command.
 * 
 * @author Alexander Wienzek (alexanderwienzek[at]adtelligence.de)
 */
public class DockerOutputResultCallback extends ResultCallbackTemplate<DockerOutputResultCallback, Frame> {
    private static final Logger LOGGER = Logger.getLogger(DockerOutputResultCallback.class.getName());

    ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
    ByteArrayOutputStream stdErr = new ByteArrayOutputStream();

    @Override
    public void onNext(Frame frame) {
        if (frame == null) {
            return;
        }

        try {
            StreamType streamType = frame.getStreamType();
            byte[] payload = frame.getPayload();

            switch (streamType) {
                case STDOUT:
                case RAW:
                    stdOut.write(payload);
                    break;
                case STDERR:
                    stdErr.write(payload);
                    break;
                case STDIN:
                default:
                    if (LOGGER.isLoggable(Level.SEVERE)) {
                        LOGGER.log(Level.SEVERE,
                            String.format("Unhandled stream type %s for CubeOutputResultCallback", streamType));
                    }
            }
        } catch (IOException ioException) {
            onError(ioException);
        }
    }

    /**
     * Awaits the cube output from the container.
     * 
     * @throws DockerClientException
     *             if the wait operation fails
     * @return the cube output
     */
    public CubeOutput awaitCubeOutput() {
        try {
            awaitCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Error while waiting for completion.", e);
            return new CubeOutput("", "");
        }
        return getCubeOutput();
    }

    /**
     * Return the current cube output of the callback.
     * 
     * @return the cube output
     */
    public CubeOutput getCubeOutput() {
        return new CubeOutput(stdOut.toString(), stdErr.toString());
    }
}