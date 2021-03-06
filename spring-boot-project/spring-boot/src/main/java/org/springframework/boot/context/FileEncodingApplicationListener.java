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

package org.springframework.boot.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 在Spring Boot环境准备完成以后运行，获取环境中的系统环境参数，
 * 检测当前系统环境的 file.encoding 和 spring.mandatory-file-encoding 设置的值是否一样，如果不一样则抛出异常；
 * 如果不配置 spring.mandatory-file-encoding 则不检查。
 */
public class FileEncodingApplicationListener
		implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

	private static final Log logger = LogFactory.getLog(FileEncodingApplicationListener.class);

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		ConfigurableEnvironment environment = event.getEnvironment();

		// 如果未配置，则不进行检查
		if (!environment.containsProperty("spring.mandatory-file-encoding")) {
			return;
		}

		// 比对系统变量的 `file.encoding` ，和环境变量的 `spring.mandatory-file-encoding` 。
		// 如果不一致，抛出 IllegalStateException 异常
		String encoding = System.getProperty("file.encoding");
		String desired = environment.getProperty("spring.mandatory-file-encoding");
		if (encoding != null && !desired.equalsIgnoreCase(encoding)) {
			logger.error("System property 'file.encoding' is currently '" + encoding + "'. It should be '" + desired
					+ "' (as defined in 'spring.mandatoryFileEncoding').");
			logger.error("Environment variable LANG is '" + System.getenv("LANG")
					+ "'. You could use a locale setting that matches encoding='" + desired + "'.");
			logger.error("Environment variable LC_ALL is '" + System.getenv("LC_ALL")
					+ "'. You could use a locale setting that matches encoding='" + desired + "'.");
			throw new IllegalStateException("The Java Virtual Machine has not been configured to use the "
					+ "desired default character encoding (" + desired + ").");
		}
	}

}
