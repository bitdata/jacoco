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
package org.jacoco.cli.internal.commands;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.jacoco.cli.internal.CommandTestBase;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * 集成测试类，用于测试Report命令的增量分析功能。
 */
public class ReportIncrementalTest extends CommandTestBase {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private File repoDir;
	private Git git;
	private File classDir;
	private File sourceDir;
	private File execFile;

	@Before
	public void setUp() throws Exception {
		// 创建Git仓库
		repoDir = tmp.newFolder("test-repo");
		git = Git.init().setDirectory(repoDir).call();

		// 创建目录结构
		classDir = new File(repoDir, "target/classes");
		classDir.mkdirs();
		sourceDir = new File(repoDir, "src/main/java");
		sourceDir.mkdirs();

		// 创建初始提交
		createJavaFile("com/example/Base.java",
				"package com.example; public class Base {}");
		createClassFile("com/example/Base.class");
		git.add().addFilepattern(".").call();
		git.commit().setMessage("Initial commit").call();

		// 创建第二个提交（增量代码）
		createJavaFile("com/example/Incremental.java",
				"package com.example; public class Incremental {}");
		createClassFile("com/example/Incremental.class");
		git.add().addFilepattern(".").call();
		git.commit().setMessage("Add Incremental class").call();

		// 创建exec文件
		execFile = new File(repoDir, "jacoco.exec");
		createExecFile(execFile);
	}

	@After
	public void tearDown() {
		if (git != null) {
			git.close();
		}
	}

	private void createJavaFile(final String path, final String content)
			throws IOException {
		final File javaFile = new File(sourceDir, path);
		javaFile.getParentFile().mkdirs();
		java.nio.file.Files.write(javaFile.toPath(), content.getBytes());
	}

	private void createClassFile(final String path) throws IOException {
		final File classFile = new File(classDir, path);
		classFile.getParentFile().mkdirs();
		classFile.createNewFile();
	}

	private void createExecFile(final File execFile) throws IOException {
		final FileOutputStream execout = new FileOutputStream(execFile);
		final ExecutionDataWriter writer = new ExecutionDataWriter(execout);
		// 添加执行数据
		writer.visitClassExecution(new ExecutionData(0x1234567890ABCDEFL,
				"com/example/Incremental",
				new boolean[] { true, false, true }));
		execout.close();
	}

	@Test
	public void should_analyze_incremental_files_when_branch_and_commit_specified()
			throws Exception {
		// 使用绝对路径，GitRepository会自动查找.git目录
		execute("report", execFile.getAbsolutePath(), "--classfiles",
				classDir.getAbsolutePath(), "--sourcefiles",
				sourceDir.getAbsolutePath(), "--html",
				new File(repoDir, "report").getAbsolutePath(), "--branch",
				"master", "--commit", "HEAD~1");

		assertOk();
		assertContains("[INFO] 发现", out);
		assertContains("个变更的Java文件", out);
	}

	@Test
	public void should_analyze_all_files_when_only_branch_specified()
			throws Exception {
		execute("report", execFile.getAbsolutePath(), "--classfiles",
				classDir.getAbsolutePath(), "--sourcefiles",
				sourceDir.getAbsolutePath(), "--html",
				new File(repoDir, "report").getAbsolutePath(), "--branch",
				"master");

		assertOk();
		assertContains("[INFO] 使用分支", out);
	}

	@Test
	public void should_maintain_backward_compatibility_when_no_incremental_params()
			throws Exception {
		// 不指定--branch和--commit，应该与原有行为一致
		execute("report", execFile.getAbsolutePath(), "--classfiles",
				classDir.getAbsolutePath(), "--html",
				new File(repoDir, "report").getAbsolutePath());

		assertOk();
		// 不应该包含增量分析相关的消息
		assertContainsNot("[INFO] 使用提交", out);
		assertContainsNot("[INFO] 发现", out);
	}

	@Test
	public void should_handle_file_modification_scenario() throws Exception {
		// 修改文件
		createJavaFile("com/example/Base.java",
				"package com.example; public class Base { public void newMethod() {} }");
		createClassFile("com/example/Base.class");
		git.add().addFilepattern(".").call();
		git.commit().setMessage("Modify Base class").call();

		execute("report", execFile.getAbsolutePath(), "--classfiles",
				classDir.getAbsolutePath(), "--sourcefiles",
				sourceDir.getAbsolutePath(), "--html",
				new File(repoDir, "report").getAbsolutePath(), "--branch",
				"master", "--commit", "HEAD~1");

		assertOk();
		assertContains("[INFO] 发现", out);
	}

	@Test
	public void should_handle_file_addition_scenario() throws Exception {
		// 添加新文件
		createJavaFile("com/example/NewClass.java",
				"package com.example; public class NewClass {}");
		createClassFile("com/example/NewClass.class");
		git.add().addFilepattern(".").call();
		git.commit().setMessage("Add NewClass").call();

		execute("report", execFile.getAbsolutePath(), "--classfiles",
				classDir.getAbsolutePath(), "--sourcefiles",
				sourceDir.getAbsolutePath(), "--html",
				new File(repoDir, "report").getAbsolutePath(), "--branch",
				"master", "--commit", "HEAD~1");

		assertOk();
		assertContains("[INFO] 发现", out);
	}

	@Test
	public void should_handle_file_deletion_scenario() throws Exception {
		// 删除文件
		new File(sourceDir, "com/example/Base.java").delete();
		new File(classDir, "com/example/Base.class").delete();
		git.add().addFilepattern(".").call();
		git.commit().setMessage("Delete Base class").call();

		execute("report", execFile.getAbsolutePath(), "--classfiles",
				classDir.getAbsolutePath(), "--sourcefiles",
				sourceDir.getAbsolutePath(), "--html",
				new File(repoDir, "report").getAbsolutePath(), "--branch",
				"master", "--commit", "HEAD~1");

		assertOk();
		// 删除的文件不应该在变更列表中
	}

	@Test
	public void should_handle_empty_changeset() throws Exception {
		// 使用HEAD作为起始提交（没有变更）
		execute("report", execFile.getAbsolutePath(), "--classfiles",
				classDir.getAbsolutePath(), "--sourcefiles",
				sourceDir.getAbsolutePath(), "--html",
				new File(repoDir, "report").getAbsolutePath(), "--branch",
				"master", "--commit", "HEAD");

		assertOk();
		assertContains("[INFO] 发现 0 个变更的Java文件", out);
	}

}
