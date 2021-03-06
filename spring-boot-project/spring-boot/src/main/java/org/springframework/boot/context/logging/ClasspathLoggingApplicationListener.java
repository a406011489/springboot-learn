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

package org.springframework.boot.context.logging;

import java.net.URLClassLoader;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.ResolvableType;

/**
 * 程序启动时，将 classpath 打印到 debug 日志，启动失败时 classpath 打印到 debug 日志。
 */
public final class ClasspathLoggingApplicationListener implements GenericApplicationListener {

	/**
	 * 顺序
	 */
	private static final int ORDER = LoggingApplicationListener.DEFAULT_ORDER + 1;

	private static final Log logger = LogFactory.getLog(ClasspathLoggingApplicationListener.class);

	@Override
	public void onApplicationEvent(ApplicationEvent event) {

		// 如果是 ApplicationEnvironmentPreparedEvent 事件，说明启动成功，打印成功到 debug 日志中
		if (logger.isDebugEnabled()) {
			if (event instanceof ApplicationEnvironmentPreparedEvent) {
				logger.debug("Application started with classpath: " + getClasspath());
			}
			else if (event instanceof ApplicationFailedEvent) { // 如果是 ApplicationFailedEvent 事件，说明启动失败，打印失败到 debug 日志中
				logger.debug("Application failed to start with classpath: " + getClasspath());
			}
		}
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

	@Override
	public boolean supportsEventType(ResolvableType resolvableType) {
		Class<?> type = resolvableType.getRawClass();
		if (type == null) {
			return false;
		}
		return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(type)
				|| ApplicationFailedEvent.class.isAssignableFrom(type);
	}

	private String getClasspath() {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (classLoader instanceof URLClassLoader) {
			return Arrays.toString(((URLClassLoader) classLoader).getURLs());
		}
		return "unknown";
	}

}
