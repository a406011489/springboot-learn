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

package org.springframework.boot.liquibase;

import liquibase.servicelocator.CustomResolverServiceLocator;
import liquibase.servicelocator.ServiceLocator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.util.ClassUtils;

/**
 * 实现 ApplicationListener 接口，初始化 Liquibase 的 ServiceLocator 对象。
 */
public class LiquibaseServiceLocatorApplicationListener implements ApplicationListener<ApplicationStartingEvent> {

	private static final Log logger = LogFactory.getLog(LiquibaseServiceLocatorApplicationListener.class);

	@Override
	public void onApplicationEvent(ApplicationStartingEvent event) {
		if (ClassUtils.isPresent("liquibase.servicelocator.CustomResolverServiceLocator",
				event.getSpringApplication().getClassLoader())) {
			new LiquibasePresent().replaceServiceLocator();
		}
	}

	/**
	 * Inner class to prevent class not found issues.
	 */
	private static class LiquibasePresent {

		void replaceServiceLocator() {
			CustomResolverServiceLocator customResolverServiceLocator = new CustomResolverServiceLocator(
					new SpringPackageScanClassResolver(logger));
			ServiceLocator.setInstance(customResolverServiceLocator);
		}

	}

}
