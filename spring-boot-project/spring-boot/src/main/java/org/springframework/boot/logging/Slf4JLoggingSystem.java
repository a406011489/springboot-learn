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

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.slf4j.bridge.SLF4JBridgeHandler;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 继承 AbstractLoggingSystem 抽象类，基于 Slf4J 的 LoggingSystem 的抽象基类。
 */
public abstract class Slf4JLoggingSystem extends AbstractLoggingSystem {

	private static final String BRIDGE_HANDLER = "org.slf4j.bridge.SLF4JBridgeHandler";

	public Slf4JLoggingSystem(ClassLoader classLoader) {
		super(classLoader);
	}

	@Override
	public void beforeInitialize() {
		super.beforeInitialize();

		// <1> 配置 JUL 的桥接处理器
		configureJdkLoggingBridgeHandler();
	}

	@Override
	public void cleanUp() {
		if (isBridgeHandlerAvailable()) {
			removeJdkLoggingBridgeHandler();
		}
	}

	@Override
	protected void loadConfiguration(LoggingInitializationContext initializationContext, String location,
			LogFile logFile) {
		Assert.notNull(location, "Location must not be null");
		if (initializationContext != null) {
			applySystemProperties(initializationContext.getEnvironment(), logFile);
		}
	}

	private void configureJdkLoggingBridgeHandler() {
		try {
			// <1> 判断 JUL 是否桥接到 SLF4J 了
			if (isBridgeJulIntoSlf4j()) {

				// <2> 移除 JUL 桥接处理器
				removeJdkLoggingBridgeHandler();

				// <3> 重新安装 SLF4JBridgeHandler
				SLF4JBridgeHandler.install();
			}
		}
		catch (Throwable ex) {
			// Ignore. No java.util.logging bridge is installed.
		}
	}

	/**
	 * Return whether bridging JUL into SLF4J or not.
	 * @return whether bridging JUL into SLF4J or not
	 * @since 2.0.4
	 */
	protected final boolean isBridgeJulIntoSlf4j() {
		return isBridgeHandlerAvailable() && isJulUsingASingleConsoleHandlerAtMost();
	}

	protected final boolean isBridgeHandlerAvailable() {
		return ClassUtils.isPresent(BRIDGE_HANDLER, getClassLoader());
	}

	private boolean isJulUsingASingleConsoleHandlerAtMost() {
		Logger rootLogger = LogManager.getLogManager().getLogger("");
		Handler[] handlers = rootLogger.getHandlers();
		return handlers.length == 0 || (handlers.length == 1 && handlers[0] instanceof ConsoleHandler);
	}

	private void removeJdkLoggingBridgeHandler() {
		try {
			// 移除
			removeDefaultRootHandler();

			// 卸载
			SLF4JBridgeHandler.uninstall();
		}
		catch (Throwable ex) {
			// Ignore and continue
		}
	}

	private void removeDefaultRootHandler() {
		try {
			Logger rootLogger = LogManager.getLogManager().getLogger("");
			Handler[] handlers = rootLogger.getHandlers();
			if (handlers.length == 1 && handlers[0] instanceof ConsoleHandler) {
				rootLogger.removeHandler(handlers[0]);
			}
		}
		catch (Throwable ex) {
			// Ignore and continue
		}
	}

}
