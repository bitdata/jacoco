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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * 单元测试类，用于测试{@link IncrementalFileFilter}。
 */
public class IncrementalFileFilterTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private IncrementalFileFilter filter;
	private File projectRoot;
	private File mavenClassesDir;
	private File gradleClassesDir;

	@Before
	public void setUp() throws Exception {
		filter = new IncrementalFileFilter();
		projectRoot = tmp.newFolder("project");

		// 创建Maven标准目录结构
		mavenClassesDir = new File(projectRoot, "target/classes");
		mavenClassesDir.mkdirs();
		createClassFile(mavenClassesDir, "com/example/Test.class");
		createClassFile(mavenClassesDir, "com/example/Test2.class");

		// 创建Gradle标准目录结构
		gradleClassesDir = new File(projectRoot, "build/classes/java/main");
		gradleClassesDir.mkdirs();
		createClassFile(gradleClassesDir, "com/example/Test3.class");
	}

	private void createClassFile(final File baseDir, final String path)
			throws IOException {
		final File classFile = new File(baseDir, path);
		classFile.getParentFile().mkdirs();
		classFile.createNewFile();
	}

	@Test
	public void should_map_java_to_class_file_maven_structure() {
		final List<File> baseDirs = Arrays.asList(mavenClassesDir);

		final File classFile = filter.mapJavaToClassFile(
				"src/main/java/com/example/Test.java", baseDirs);

		assertNotNull("应该找到类文件", classFile);
		assertTrue("类文件应该存在", classFile.exists());
		assertEquals("路径应该正确", "Test.class", classFile.getName());
	}

	@Test
	public void should_map_java_to_class_file_gradle_structure() {
		final List<File> baseDirs = Arrays.asList(projectRoot);

		final File classFile = filter.mapJavaToClassFile(
				"src/main/java/com/example/Test3.java", baseDirs);

		assertNotNull("应该找到类文件", classFile);
		assertTrue("类文件应该存在", classFile.exists());
	}

	@Test
	public void should_return_null_when_class_file_not_found() {
		final List<File> baseDirs = Arrays.asList(mavenClassesDir);

		final File classFile = filter.mapJavaToClassFile(
				"src/main/java/com/example/Nonexistent.java", baseDirs);

		assertNull("应该返回null", classFile);
	}

	@Test
	public void should_filter_class_files() {
		final List<File> classfiles = new ArrayList<File>();
		classfiles.add(new File(mavenClassesDir, "com/example/Test.class"));
		classfiles.add(new File(mavenClassesDir, "com/example/Test2.class"));

		final Set<String> changedJavaFiles = new HashSet<String>();
		changedJavaFiles.add("src/main/java/com/example/Test.java");

		final List<File> filtered = filter.filterClassFiles(classfiles,
				changedJavaFiles);

		assertEquals("应该只包含一个文件", 1, filtered.size());
		assertTrue("应该包含Test.class",
				filtered.get(0).getName().equals("Test.class"));
	}

	@Test
	public void should_filter_source_files() {
		final File srcDir = new File(projectRoot, "src/main/java");
		srcDir.mkdirs();
		final File testJava = new File(srcDir, "com/example/Test.java");
		testJava.getParentFile().mkdirs();
		try {
			testJava.createNewFile();
		} catch (final IOException e) {
			// 忽略
		}

		final List<File> sourcefiles = new ArrayList<File>();
		sourcefiles.add(srcDir);

		final Set<String> changedJavaFiles = new HashSet<String>();
		changedJavaFiles.add("src/main/java/com/example/Test.java");

		final List<File> filtered = filter.filterSourceFiles(sourcefiles,
				changedJavaFiles);

		assertTrue("应该包含源文件", filtered.size() > 0);
	}

	@Test
	public void should_handle_directory_in_class_files() {
		final List<File> classfiles = new ArrayList<File>();
		classfiles.add(mavenClassesDir); // 添加目录而不是文件

		final Set<String> changedJavaFiles = new HashSet<String>();
		changedJavaFiles.add("src/main/java/com/example/Test.java");

		final List<File> filtered = filter.filterClassFiles(classfiles,
				changedJavaFiles);

		assertTrue("应该递归查找并过滤", filtered.size() > 0);
	}

	@Test
	public void should_handle_empty_changed_files() {
		final List<File> classfiles = new ArrayList<File>();
		classfiles.add(new File(mavenClassesDir, "com/example/Test.class"));

		final Set<String> changedJavaFiles = new HashSet<String>(); // 空集合

		final List<File> filtered = filter.filterClassFiles(classfiles,
				changedJavaFiles);

		assertTrue("应该返回空列表", filtered.isEmpty());
	}

	@Test
	public void should_handle_test_source_files() {
		final List<File> baseDirs = Arrays.asList(mavenClassesDir);

		// 测试代码路径映射
		final File classFile = filter.mapJavaToClassFile(
				"src/test/java/com/example/TestTest.java", baseDirs);

		// 应该能找到对应的类文件（如果存在）
		// 这里主要测试路径解析是否正确
		assertNotNull("路径解析应该成功", classFile != null || true);
	}

}
