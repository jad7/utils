package com.github.jad.jobmanager;

/**
 *
 * @author Illia Krokhmalov <jad7kii@gmail.com>
 * @since 12/2018
 */
public interface PausableFlow extends Flow {
    void pause();

    void resume();
}
