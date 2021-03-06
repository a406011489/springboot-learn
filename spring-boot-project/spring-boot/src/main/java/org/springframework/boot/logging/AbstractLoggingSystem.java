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

package org.springframework.boot.logging;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.SystemPropertyUtils;

/**
 * 继承 LoggingSystem 抽象类，是 LoggingSystem 的抽象基类。
 */
public abstract class AbstractLoggingSystem extends LoggingSystem {

	protected static final Comparator<LoggerConfiguration> CONFIGURATION_COMPARATOR = new LoggerConfigurationComparator(
			ROOT_LOGGER_NAME);

	private final ClassLoader classLoader;

	public AbstractLoggingSystem(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void beforeInitialize() {
	}

	// 提供模板化的初始化逻辑。
	@Override
	public void initialize(LoggingInitializationContext initializationContext, String configLocation, LogFile logFile) {

		// <1> 有自定义的配置文件，则使用指定配置文件进行初始化
		if (StringUtils.hasLength(configLocation)) {
			initializeWithSpecificConfig(initializationContext, configLocation, logFile);
			return;
		}

		// <2> 无自定义的配置文件，则使用约定配置文件进行初始化
		initializeWithConventions(initializationContext, logFile);
	}

	// 使用指定配置文件进行初始化。
	private void initializeWithSpecificConfig(LoggingInitializationContext initializationContext, String configLocation,
			LogFile logFile) {

		// <1> 获得配置文件（可能有占位符）
		configLocation = SystemPropertyUtils.resolvePlaceholders(configLocation);

		// <2> 加载配置文件
		loadConfiguration(initializationContext, configLocation, logFile);
	}

	// 使用约定配置文件进行初始化。
	private void initializeWithConventions(LoggingInitializationContext initializationContext, LogFile logFile) {

		// <1> 获得约定配置文件
		String config = getSelfInitializationConfig();

		// <2> 如果获取到，结果 logFile 为空，则重新初始化
		if (config != null && logFile == null) {
			// self initialization has occurred, reinitialize in case of property changes
			reinitialize(initializationContext);
			return;
		}

		// <3> 如果获取不到，则尝试获得约定配置文件（带 spring 后缀）
		if (config == null) {
			config = getSpringInitializationConfig();
		}

		// <4> 如果获取到，则加载配置文件
		if (config != null) {
			loadConfiguration(initializationContext, config, logFile);
			return;
		}

		// <5> 如果获取不到，则加载默认配置
		loadDefaults(initializationContext, logFile);
	}

	/**
	 * 获得约定配置文件。
	 */
	protected String getSelfInitializationConfig() {
		return findConfig(getStandardConfigLocations());
	}

	/**
	 * 尝试获得约定配置文件（带 -spring 后缀）。
	 */
	protected String getSpringInitializationConfig() {
		return findConfig(getSpringConfigLocations());
	}

	private String findConfig(String[] locations) {

		// 遍历 locations 数组，逐个判断是否存在。若存在，则返回
		for (String location : locations) {
			ClassPathResource resource = new ClassPathResource(location, this.classLoader);
			if (resource.exists()) {
				return "classpath:" + location;
			}
		}
		return null;
	}

	/**
	 * 获得约定的配置文件。
	 * 例如说：LogbackLoggingSystem 返回的是 "logback-test.groovy"、"logback-test.xml"、 "logback.groovy"、"logback.xml" 。
	 */
	protected abstract String[] getStandardConfigLocations();

	/**
	 * Return the spring config locations for this system. By default this method returns
	 * a set of locations based on {@link #getStandardConfigLocations()}.
	 * @return the spring config locations
	 * @see #getSpringInitializationConfig()
	 */
	protected String[] getSpringConfigLocations() {
		String[] locations = getStandardConfigLocations();
		for (int i = 0; i < locations.length; i++) {
			String extension = StringUtils.getFilenameExtension(locations[i]);
			locations[i] = locations[i].substring(0, locations[i].length() - extension.length() - 1) + "-spring."
					+ extension;
		}
		return locations;
	}

	/**
	 * Load sensible defaults for the logging system.
	 * @param initializationContext the logging initialization context
	 * @param logFile the file to load or {@code null} if no log file is to be written
	 */
	protected abstract void loadDefaults(LoggingInitializationContext initializationContext, LogFile logFile);

	/**
	 * Load a specific configuration.
	 * @param initializationContext the logging initialization context
	 * @param location the location of the configuration to load (never {@code null})
	 * @param logFile the file to load or {@code null} if no log file is to be written
	 */
	protected abstract void loadConfiguration(LoggingInitializationContext initializationContext, String location,
			LogFile logFile);

	/**
	 * Reinitialize the logging system if required. Called when
	 * {@link #getSelfInitializationConfig()} is used and the log file hasn't changed. May
	 * be used to reload configuration (for example to pick up additional System
	 * properties).
	 * @param initializationContext the logging initialization context
	 */
	protected void reinitialize(LoggingInitializationContext initializationContext) {
	}

	protected final ClassLoader getClassLoader() {
		return this.classLoader;
	}

	protected final String getPackagedConfigFile(String fileName) {
		String defaultPath = ClassUtils.getPackageName(getClass());
		defaultPath = defaultPath.replace('.', '/');
		defaultPath = defaultPath + "/" + fileName;
		defaultPath = "classpath:" + defaultPath;
		return defaultPath;
	}

	protected final void applySystemProperties(Environment environment, LogFile logFile) {
		new LoggingSystemProperties(environment).apply(logFile);
	}

	/**
	 * 是 AbstractLoggingSystem 的内部静态类，用于 Spring Boot LogLevel 和日志框架的 LogLevel 做映射。
	 */
	protected static class LogLevels<T> {

		private final Map<LogLevel, T> systemToNative;

		private final Map<T, LogLevel> nativeToSystem;

		public LogLevels() {
			this.systemToNative = new EnumMap<>(LogLevel.class);
			this.nativeToSystem = new HashMap<>();
		}

		public void map(LogLevel system, T nativeLevel) {
			this.systemToNative.putIfAbsent(system, nativeLevel);
			this.nativeToSystem.putIfAbsent(nativeLevel, system);
		}

		public LogLevel convertNativeToSystem(T level) {
			return this.nativeToSystem.get(level);
		}

		public T convertSystemToNative(LogLevel level) {
			return this.systemToNative.get(level);
		}

		public Set<LogLevel> getSupported() {
			return new LinkedHashSet<>(this.nativeToSystem.values());
		}

	}

}
