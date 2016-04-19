/*
 * Copyright (C) 2015 Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.kubeflix;

import java.util.HashMap;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.configuration.EnvironmentConfiguration;
import org.apache.commons.configuration.MapConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.turbine.init.TurbineInit;
import com.netflix.turbine.plugins.PluginsFactory;

import io.fabric8.kubeflix.discovery.KubernetesDiscovery;

public class StartTurbineServer implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(StartTurbineServer.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("Initing Turbine server");
		EnvironmentConfiguration configFromSystemProperties = new EnvironmentConfiguration();
		MapConfiguration mapConfig=new MapConfiguration(new HashMap<String, Object>());
		// hack to workaround kubernetes yaml file limitation on property name (Must be a C identifier)
		mapConfig.addProperty("turbine.instanceUrlSuffix", configFromSystemProperties.getProperty("turbineInstanceUrlSuffix"));
		ConcurrentCompositeConfiguration finalConfig = new ConcurrentCompositeConfiguration();
		finalConfig.addConfiguration(mapConfig, "mapConfig");
		finalConfig.addConfiguration(configFromSystemProperties, "systemConfig");
		ConfigurationManager.install(finalConfig);
		DynamicStringProperty prop = DynamicPropertyFactory.getInstance().getStringProperty("turbine.instanceUrlSuffix",null);
		logger.debug("using turbine.instanceUrlSuffix:" + prop.getValue());         
        PluginsFactory.setInstanceDiscovery(new KubernetesDiscovery());
        TurbineInit.init();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("Stopping Turbine server");
        TurbineInit.stop();
    }
}
