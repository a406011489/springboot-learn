/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.context;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

/**
 * 监听 EmbeddedServletContainerInitializedEvent 类型的事件，然后将内嵌的 Web 服务器使用的端口给设置到 ApplicationContext 中。
 */
public class ServerPortInfoApplicationContextInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext>, ApplicationListener<WebServerInitializedEvent> {

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		applicationContext.addApplicationListener(this);
	}

	// 将自身作为一个 ApplicationListener 监听器，添加到 Spring 容器中。
	@Override
	public void onApplicationEvent(WebServerInitializedEvent event) {

		// <1> 获得属性名
		String propertyName = "local." + getName(event.getApplicationContext()) + ".port";

		// <2> 设置端口到 environment 的 propertyName 中
		setPortProperty(event.getApplicationContext(), propertyName, event.getWebServer().getPort());
	}

	private String getName(WebServerApplicationContext context) {
		String name = context.getServerNamespace();
		return StringUtils.hasText(name) ? name : "server";
	}

	private void setPortProperty(ApplicationContext context, String propertyName, int port) {

		// 设置端口到 environment 的 propertyName 中
		if (context instanceof ConfigurableApplicationContext) {
			setPortProperty(((ConfigurableApplicationContext) context).getEnvironment(), propertyName, port);
		}

		// 如果有父容器，则继续设置
		if (context.getParent() != null) {
			setPortProperty(context.getParent(), propertyName, port);
		}
	}

	@SuppressWarnings("unchecked")
	private void setPortProperty(ConfigurableEnvironment environment, String propertyName, int port) {
		MutablePropertySources sources = environment.getPropertySources();

		// 获得 "server.ports" 属性对应的值
		PropertySource<?> source = sources.get("server.ports");
		if (source == null) {
			source = new MapPropertySource("server.ports", new HashMap<>());
			sources.addFirst(source);
		}

		// 添加到 source 中
		((Map<String, Object>) source.getSource()).put(propertyName, port);
	}

}
