package com.github.jad.jobmanager;

/**
 *
 * @author Illia Krokhmalov <jad7kii@gmail.com>
 * @since 12/2018
 */
public class PausingJobException extends RuntimeException {
    public PausingJobException(String message) {
        super(message);
    }
}
