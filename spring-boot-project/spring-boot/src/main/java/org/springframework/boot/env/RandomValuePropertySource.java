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

package org.springframework.boot.env;

import java.util.Random;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

/**
 * 提供随机值的 PropertySource 实现类。
 */
public class RandomValuePropertySource extends PropertySource<Random> {

	/**
	 * Name of the random {@link PropertySource}.
	 */
	public static final String RANDOM_PROPERTY_SOURCE_NAME = "random";

	private static final String PREFIX = "random.";

	private static final Log logger = LogFactory.getLog(RandomValuePropertySource.class);

	public RandomValuePropertySource(String name) {
		super(name, new Random());
	}

	public RandomValuePropertySource() {
		this(RANDOM_PROPERTY_SOURCE_NAME);
	}

	@Override
	public Object getProperty(String name) {

		// <1> 必须以 random. 前缀
		if (!name.startsWith(PREFIX)) {
			return null;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Generating random property for '" + name + "'");
		}

		// <2> 根据类型，获得随机值
		return getRandomValue(name.substring(PREFIX.length()));
	}

	private Object getRandomValue(String type) {
		if (type.equals("int")) {
			return getSource().nextInt();
		}
		if (type.equals("long")) {
			return getSource().nextLong();
		}
		String range = getRange(type, "int");
		if (range != null) {
			return getNextIntInRange(range);
		}
		range = getRange(type, "long");
		if (range != null) {
			return getNextLongInRange(range);
		}
		if (type.equals("uuid")) {
			return UUID.randomUUID().toString();
		}
		return getRandomBytes();
	}

	private String getRange(String type, String prefix) {
		if (type.startsWith(prefix)) {
			int startIndex = prefix.length() + 1;
			if (type.length() > startIndex) {
				return type.substring(startIndex, type.length() - 1);
			}
		}
		return null;
	}

	private int getNextIntInRange(String range) {
		String[] tokens = StringUtils.commaDelimitedListToStringArray(range);
		int start = Integer.parseInt(tokens[0]);
		if (tokens.length == 1) {
			return getSource().nextInt(start);
		}
		return start + getSource().nextInt(Integer.parseInt(tokens[1]) - start);
	}

	private long getNextLongInRange(String range) {
		String[] tokens = StringUtils.commaDelimitedListToStringArray(range);
		if (tokens.length == 1) {
			return Math.abs(getSource().nextLong() % Long.parseLong(tokens[0]));
		}
		long lowerBound = Long.parseLong(tokens[0]);
		long upperBound = Long.parseLong(tokens[1]) - lowerBound;
		return lowerBound + Math.abs(getSource().nextLong() % upperBound);
	}

	private Object getRandomBytes() {
		byte[] bytes = new byte[32];
		getSource().nextBytes(bytes);
		return DigestUtils.md5DigestAsHex(bytes);
	}

	public static void addToEnvironment(ConfigurableEnvironment environment) {
		environment.getPropertySources().addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
				new RandomValuePropertySource(RANDOM_PROPERTY_SOURCE_NAME));
		logger.trace("RandomValuePropertySource add to Environment");
	}

}
