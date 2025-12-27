/*******************************************************************************
 * Copyright (c) 2009, 2025 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    [Your Name] - initial implementation
 *
 *******************************************************************************/
package org.jacoco.cli.internal.git;

/**
 * Git操作相关的异常类。 用于封装Git操作过程中可能出现的各种错误。
 */
public class GitException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * 创建Git异常。
	 *
	 * @param message
	 *            错误消息
	 */
	public GitException(final String message) {
		super(message);
	}

	/**
	 * 创建Git异常。
	 *
	 * @param message
	 *            错误消息
	 * @param cause
	 *            原因异常
	 */
	public GitException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
