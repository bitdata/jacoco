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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 增量文件过滤器。 根据增量文件列表过滤classfiles和sourcefiles。
 */
public class IncrementalFileFilter {

	/**
	 * 过滤类文件列表，只保留增量代码对应的类文件。
	 *
	 * @param classfiles
	 *            原始类文件列表
	 * @param changedJavaFiles
	 *            变更的Java源文件路径集合
	 * @return 过滤后的类文件列表
	 */
	public List<File> filterClassFiles(final List<File> classfiles,
			final Set<String> changedJavaFiles) {
		final List<File> filtered = new ArrayList<File>();
		for (final File classfile : classfiles) {
			if (classfile.isDirectory()) {
				// 递归处理目录
				filtered.addAll(filterClassFilesInDirectory(classfile,
						changedJavaFiles, classfiles));
			} else {
				// 检查文件是否对应变更的Java源文件
				if (isIncrementalClassFile(classfile, changedJavaFiles,
						classfiles)) {
					filtered.add(classfile);
				}
			}
		}
		return filtered;
	}

	/**
	 * 递归过滤目录中的类文件。
	 *
	 * @param dir
	 *            目录
	 * @param changedJavaFiles
	 *            变更的Java源文件路径集合
	 * @param classfilesBaseDirs
	 *            类文件基础目录列表
	 * @return 过滤后的类文件列表
	 */
	private List<File> filterClassFilesInDirectory(final File dir,
			final Set<String> changedJavaFiles,
			final List<File> classfilesBaseDirs) {
		final List<File> filtered = new ArrayList<File>();
		final File[] files = dir.listFiles();
		if (files != null) {
			for (final File file : files) {
				if (file.isDirectory()) {
					filtered.addAll(filterClassFilesInDirectory(file,
							changedJavaFiles, classfilesBaseDirs));
				} else if (file.getName().endsWith(".class")) {
					if (isIncrementalClassFile(file, changedJavaFiles,
							classfilesBaseDirs)) {
						filtered.add(file);
					}
				}
			}
		}
		return filtered;
	}

	/**
	 * 检查类文件是否对应增量Java源文件。
	 *
	 * @param classFile
	 *            类文件
	 * @param changedJavaFiles
	 *            变更的Java源文件路径集合
	 * @param classfilesBaseDirs
	 *            类文件基础目录列表
	 * @return true如果对应增量文件，否则false
	 */
	private boolean isIncrementalClassFile(final File classFile,
			final Set<String> changedJavaFiles,
			final List<File> classfilesBaseDirs) {
		// 尝试将类文件路径映射回Java源文件路径
		for (final String javaPath : changedJavaFiles) {
			final File mappedClassFile = mapJavaToClassFile(javaPath,
					classfilesBaseDirs);
			if (mappedClassFile != null && mappedClassFile.equals(classFile)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 过滤源文件列表，只保留增量代码对应的源文件。
	 *
	 * @param sourcefiles
	 *            原始源文件列表
	 * @param changedJavaFiles
	 *            变更的Java源文件路径集合
	 * @return 过滤后的源文件列表
	 */
	public List<File> filterSourceFiles(final List<File> sourcefiles,
			final Set<String> changedJavaFiles) {
		final List<File> filtered = new ArrayList<File>();
		final Set<String> normalizedChangedPaths = normalizePaths(
				changedJavaFiles);
		for (final File sourcefile : sourcefiles) {
			if (sourcefile.isDirectory()) {
				// 递归处理目录
				filtered.addAll(filterSourceFilesInDirectory(sourcefile,
						normalizedChangedPaths));
			} else if (sourcefile.getName().endsWith(".java")) {
				// 检查文件是否在变更列表中
				if (isIncrementalSourceFile(sourcefile,
						normalizedChangedPaths)) {
					filtered.add(sourcefile);
				}
			}
		}
		return filtered;
	}

	/**
	 * 递归过滤目录中的源文件。
	 *
	 * @param dir
	 *            目录
	 * @param normalizedChangedPaths
	 *            规范化的变更文件路径集合
	 * @return 过滤后的源文件列表
	 */
	private List<File> filterSourceFilesInDirectory(final File dir,
			final Set<String> normalizedChangedPaths) {
		final List<File> filtered = new ArrayList<File>();
		final File[] files = dir.listFiles();
		if (files != null) {
			for (final File file : files) {
				if (file.isDirectory()) {
					filtered.addAll(filterSourceFilesInDirectory(file,
							normalizedChangedPaths));
				} else if (file.getName().endsWith(".java")) {
					if (isIncrementalSourceFile(file, normalizedChangedPaths)) {
						filtered.add(file);
					}
				}
			}
		}
		return filtered;
	}

	/**
	 * 检查源文件是否在变更列表中。
	 *
	 * @param sourceFile
	 *            源文件
	 * @param normalizedChangedPaths
	 *            规范化的变更文件路径集合
	 * @return true如果在变更列表中，否则false
	 */
	private boolean isIncrementalSourceFile(final File sourceFile,
			final Set<String> normalizedChangedPaths) {
		final String normalizedPath = normalizePath(sourceFile.getPath());
		return normalizedChangedPaths.contains(normalizedPath);
	}

	/**
	 * 规范化路径集合（统一使用正斜杠）。
	 *
	 * @param paths
	 *            路径集合
	 * @return 规范化后的路径集合
	 */
	private Set<String> normalizePaths(final Set<String> paths) {
		final Set<String> normalized = new HashSet<String>();
		for (final String path : paths) {
			normalized.add(normalizePath(path));
		}
		return normalized;
	}

	/**
	 * 规范化路径（统一使用正斜杠）。
	 *
	 * @param path
	 *            路径
	 * @return 规范化后的路径
	 */
	private String normalizePath(final String path) {
		return path.replace('\\', '/');
	}

	/**
	 * 将Java源文件路径转换为对应的类文件路径。
	 * 支持Maven标准结构（target/classes）和Gradle标准结构（build/classes/java/main）。
	 *
	 * @param javaFilePath
	 *            Java源文件路径（相对于仓库根目录）
	 * @param classfilesBaseDirs
	 *            类文件基础目录列表
	 * @return 对应的类文件路径，如果找不到则返回null
	 */
	public File mapJavaToClassFile(final String javaFilePath,
			final List<File> classfilesBaseDirs) {
		// 规范化路径
		final String normalizedJavaPath = normalizePath(javaFilePath);

		// 提取包路径（去掉src/main/java等前缀和.java后缀）
		String packagePath = extractPackagePath(normalizedJavaPath);
		if (packagePath == null) {
			return null;
		}

		// 转换为类文件路径
		final String classFileName = packagePath + ".class";

		// 在每个基础目录中查找
		for (final File baseDir : classfilesBaseDirs) {
			// 首先尝试直接匹配（Maven标准结构：target/classes）
			File classFile = new File(baseDir, classFileName);
			if (classFile.exists()) {
				return classFile;
			}

			// 如果baseDir是项目根目录，尝试Gradle结构
			if (baseDir.isDirectory()) {
				// 尝试Gradle标准结构
				final File gradleClassFile = findGradleClassFile(baseDir,
						packagePath);
				if (gradleClassFile != null) {
					return gradleClassFile;
				}
			}

			// 尝试递归查找（处理自定义目录结构）
			classFile = findClassFileRecursive(baseDir, classFileName);
			if (classFile != null) {
				return classFile;
			}
		}

		return null;
	}

	/**
	 * 递归查找类文件。
	 *
	 * @param dir
	 *            起始目录
	 * @param classFileName
	 *            类文件名（包含相对路径，如com/example/Test.class）
	 * @return 找到的类文件，如果找不到则返回null
	 */
	private File findClassFileRecursive(final File dir,
			final String classFileName) {
		if (!dir.isDirectory()) {
			return null;
		}

		// 直接查找
		final File directFile = new File(dir, classFileName);
		if (directFile.exists()) {
			return directFile;
		}

		// 递归查找（限制深度以避免性能问题）
		return findClassFileRecursive(dir, classFileName, 0, 5);
	}

	/**
	 * 递归查找类文件（带深度限制）。
	 *
	 * @param dir
	 *            当前目录
	 * @param classFileName
	 *            类文件名
	 * @param currentDepth
	 *            当前深度
	 * @param maxDepth
	 *            最大深度
	 * @return 找到的类文件，如果找不到则返回null
	 */
	private File findClassFileRecursive(final File dir,
			final String classFileName, final int currentDepth,
			final int maxDepth) {
		if (currentDepth >= maxDepth) {
			return null;
		}

		final File[] files = dir.listFiles();
		if (files == null) {
			return null;
		}

		for (final File file : files) {
			if (file.isDirectory()) {
				final File found = findClassFileRecursive(file, classFileName,
						currentDepth + 1, maxDepth);
				if (found != null) {
					return found;
				}
			} else if (file.getName().endsWith(".class")) {
				// 检查相对路径是否匹配
				// 这里简化处理，只检查文件名是否匹配
				if (classFileName.endsWith(file.getName())) {
					// 进一步验证路径结构
					final String relativePath = getRelativePath(dir, file);
					if (relativePath != null && normalizePath(relativePath)
							.equals(normalizePath(classFileName))) {
						return file;
					}
				}
			}
		}

		return null;
	}

	/**
	 * 获取相对路径。
	 *
	 * @param baseDir
	 *            基础目录
	 * @param file
	 *            文件
	 * @return 相对路径，如果无法计算则返回null
	 */
	private String getRelativePath(final File baseDir, final File file) {
		try {
			return baseDir.toPath().relativize(file.toPath()).toString();
		} catch (final Exception e) {
			return null;
		}
	}

	/**
	 * 从Java源文件路径中提取包路径。 支持标准Maven/Gradle目录结构。
	 *
	 * @param javaFilePath
	 *            Java源文件路径
	 * @return 包路径（如com/example/Test），如果无法提取则返回null
	 */
	private String extractPackagePath(final String javaFilePath) {
		// 移除.java后缀
		if (!javaFilePath.endsWith(".java")) {
			return null;
		}
		String path = javaFilePath.substring(0, javaFilePath.length() - 5); // 移除".java"

		// 尝试匹配标准Maven结构: src/main/java/...
		final String mavenPrefix = "src/main/java/";
		if (path.startsWith(mavenPrefix)) {
			return path.substring(mavenPrefix.length());
		}

		// 尝试匹配标准Gradle结构: src/main/java/...
		// (Gradle源文件结构与Maven相同)
		final String gradlePrefix = "src/main/java/";
		if (path.startsWith(gradlePrefix)) {
			return path.substring(gradlePrefix.length());
		}

		// 尝试匹配测试代码: src/test/java/...
		final String testPrefix = "src/test/java/";
		if (path.startsWith(testPrefix)) {
			return path.substring(testPrefix.length());
		}

		// 尝试匹配Gradle测试代码: src/test/java/...
		final String gradleTestPrefix = "src/test/java/";
		if (path.startsWith(gradleTestPrefix)) {
			return path.substring(gradleTestPrefix.length());
		}

		// 如果路径不包含标准前缀，尝试直接使用（可能是自定义结构）
		// 查找最后一个"src"或"java"目录
		final int srcIndex = path.lastIndexOf("/src/");
		if (srcIndex >= 0) {
			final int javaIndex = path.indexOf("/java/", srcIndex);
			if (javaIndex >= 0) {
				return path.substring(javaIndex + 6); // 跳过"/java/"
			}
		}

		// 如果都不匹配，返回原始路径（去掉可能的目录前缀）
		// 假设路径直接是包路径
		return path;
	}

	/**
	 * 查找Gradle类文件目录。
	 * Gradle的类文件可能在build/classes/java/main/或build/classes/java/test/目录下。
	 *
	 * @param baseDir
	 *            基础目录
	 * @param packagePath
	 *            包路径
	 * @return 类文件，如果找不到则返回null
	 */
	private File findGradleClassFile(final File baseDir,
			final String packagePath) {
		// 尝试标准Gradle主类目录
		final File mainClassFile = new File(baseDir,
				"build/classes/java/main/" + packagePath + ".class");
		if (mainClassFile.exists()) {
			return mainClassFile;
		}
		// 尝试Gradle测试类目录
		final File testClassFile = new File(baseDir,
				"build/classes/java/test/" + packagePath + ".class");
		if (testClassFile.exists()) {
			return testClassFile;
		}
		return null;
	}

}
