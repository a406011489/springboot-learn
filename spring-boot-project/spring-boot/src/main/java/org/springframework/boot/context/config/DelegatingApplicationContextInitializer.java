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

package org.springframework.boot.context.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * 实现 ApplicationContextInitializer、Ordered 接口，
 * 根据环境变量配置的 context.initializer.classes 配置的 ApplicationContextInitializer 类们，交给它们进行初始化。
 */
public class DelegatingApplicationContextInitializer
		implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

	/**
	 * 环境变量配置的属性
	 */
	private static final String PROPERTY_NAME = "context.initializer.classes";

	/**
	 * 默认优先级
	 */
	private int order = 0;

	@Override
	public void initialize(ConfigurableApplicationContext context) {

		// <1> 获得环境变量配置的 ApplicationContextInitializer 集合们
		ConfigurableEnvironment environment = context.getEnvironment();
		List<Class<?>> initializerClasses = getInitializerClasses(environment);

		// 如果非空，则进行初始化
		if (!initializerClasses.isEmpty()) {
			applyInitializerClasses(context, initializerClasses);
		}
	}

	private List<Class<?>> getInitializerClasses(ConfigurableEnvironment env) {

		// 获得环境变量配置的属性
		String classNames = env.getProperty(PROPERTY_NAME);

		// 拼装成数组，按照 ，分隔
		List<Class<?>> classes = new ArrayList<>();
		if (StringUtils.hasLength(classNames)) {
			for (String className : StringUtils.tokenizeToStringArray(classNames, ",")) {
				classes.add(getInitializerClass(className));
			}
		}
		return classes;
	}

	private Class<?> getInitializerClass(String className) throws LinkageError {
		try {
			Class<?> initializerClass = ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
			Assert.isAssignable(ApplicationContextInitializer.class, initializerClass);
			return initializerClass;
		}
		catch (ClassNotFoundException ex) {
			throw new ApplicationContextException("Failed to load context initializer class [" + className + "]", ex);
		}
	}

	private void applyInitializerClasses(ConfigurableApplicationContext context, List<Class<?>> initializerClasses) {
		Class<?> contextClass = context.getClass();
		List<ApplicationContextInitializer<?>> initializers = new ArrayList<>();
		for (Class<?> initializerClass : initializerClasses) {
			initializers.add(instantiateInitializer(contextClass, initializerClass));
		}
		applyInitializers(context, initializers);
	}

	private ApplicationContextInitializer<?> instantiateInitializer(Class<?> contextClass, Class<?> initializerClass) {
		Class<?> requireContextClass = GenericTypeResolver.resolveTypeArgument(initializerClass,
				ApplicationContextInitializer.class);
		Assert.isAssignable(requireContextClass, contextClass,
				String.format(
						"Could not add context initializer [%s] as its generic parameter [%s] is not assignable "
								+ "from the type of application context used by this context loader [%s]: ",
						initializerClass.getName(), requireContextClass.getName(), contextClass.getName()));
		return (ApplicationContextInitializer<?>) BeanUtils.instantiateClass(initializerClass);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void applyInitializers(ConfigurableApplicationContext context,
			List<ApplicationContextInitializer<?>> initializers) {
		initializers.sort(new AnnotationAwareOrderComparator());
		for (ApplicationContextInitializer initializer : initializers) {
			initializer.initialize(context);
		}
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

}
