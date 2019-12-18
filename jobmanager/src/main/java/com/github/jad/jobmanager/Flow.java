package com.github.jad.jobmanager;


import com.github.jad.utils.CommonUtils;


/**
 *
 * @author Illia Krokhmalov <jad7kii@gmail.com>
 * @since 12/2018
 */

public interface Flow {

    default String getName() {
        return CommonUtils.uncapitalize(this.getClass().getSimpleName());
    }

    void run(Context context);

    default boolean isPausable() {
        return this instanceof PausableFlow;
    }

    boolean stop();
}
