/*
  Copyright (c) 2019 Liveramp.com, All Rights Reserved
  <p>
  Developed by Grid Dynamics International, Inc. for the customer Art.com.
  http://www.griddynamics.com
  <p>
  Classification level: Confidential
  <p>
  EXCEPT EXPRESSED BY WRITTEN WRITING, THIS CODE AND INFORMATION ARE PROVIDED "AS IS"
  WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED
  TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR PURPOSE.
  <p>
  For information about the licensing and copyright of this document please
  contact Grid Dynamics at info@griddynamics.com.
 */
package com.github.jad.jobmanager;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.core.env.Environment;


/**
 *
 * @author Illia Krokhmalov <jad7kii@gmail.com>
 * @since 12/2018
 */
@Slf4j
public class Config {
    private final String prefix;
    private final Environment environment;
    @Setter
    private Logger logger = log;

    public Config(String prefix, Environment environment) {
        this.prefix = prefix;
        this.environment = environment;
    }

    public <T> T getProp(String parameter, Class<T> clazz) {
        String key = prefix + parameter;
        T property = environment.getProperty(key, clazz);
        if (property == null) {
            throw new IllegalArgumentException("Property:\"" + key + "\" is not defined in config");
        }
        logger.debug("For key: {}, fetching property value: {}", key, property);
        return property;
    }

    public <T> T getProp(String parameter, Class<T> clazz, T defaultValue) {
        return environment.getProperty(prefix + parameter, clazz, defaultValue);
    }
}
