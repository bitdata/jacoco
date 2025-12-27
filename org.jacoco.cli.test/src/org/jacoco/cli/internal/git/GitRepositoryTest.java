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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * 单元测试类，用于测试{@link GitRepository}。
 */
public class GitRepositoryTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private File repoDir;
	private Git git;
	private GitRepository gitRepo;

	@Before
	public void setUp() throws Exception {
		// 创建Git仓库
		repoDir = tmp.newFolder("test-repo");
		git = Git.init().setDirectory(repoDir).call();

		// 创建初始提交
		createTestFile("Test.java", "public class Test {}");
		git.add().addFilepattern(".").call();
		git.commit().setMessage("Initial commit").call();

		// 创建更多提交用于测试
		createTestFile("Test2.java", "public class Test2 {}");
		git.add().addFilepattern(".").call();
		final RevCommit commit2 = git.commit().setMessage("Second commit")
				.call();

		createTestFile("Test3.java", "public class Test3 {}");
		git.add().addFilepattern(".").call();
		git.commit().setMessage("Third commit").call();

		gitRepo = new GitRepository(repoDir);
	}

	@After
	public void tearDown() {
		if (gitRepo != null) {
			gitRepo.close();
		}
		if (git != null) {
			git.close();
		}
	}

	private void createTestFile(final String name, final String content)
			throws IOException {
		final File file = new File(repoDir, name);
		file.getParentFile().mkdirs();
		java.nio.file.Files.write(file.toPath(), content.getBytes());
	}

	@Test
	public void should_detect_git_repository() throws Exception {
		assertTrue("应该是Git仓库", gitRepo.isGitRepository());
	}

	@Test
	public void should_detect_non_git_repository() throws Exception {
		final File nonRepoDir = tmp.newFolder("non-repo");
		final GitRepository nonRepo = new GitRepository(nonRepoDir);
		try {
			assertFalse("不应该是Git仓库", nonRepo.isGitRepository());
		} finally {
			nonRepo.close();
		}
	}

	@Test
	public void should_resolve_commit_by_full_hash() throws Exception {
		final Repository repo = git.getRepository();
		final String fullHash = repo.resolve("HEAD").getName();

		final RevCommit commit = gitRepo.resolveCommit(fullHash);

		assertNotNull("应该解析到提交", commit);
		assertEquals("提交哈希应该匹配", fullHash, commit.getName());
	}

	@Test
	public void should_resolve_commit_by_short_hash() throws Exception {
		final Repository repo = git.getRepository();
		final String fullHash = repo.resolve("HEAD").getName();
		final String shortHash = fullHash.substring(0, 7);

		final RevCommit commit = gitRepo.resolveCommit(shortHash);

		assertNotNull("应该解析到提交", commit);
		assertTrue("提交哈希应该匹配", commit.getName().startsWith(shortHash));
	}

	@Test
	public void should_resolve_commit_by_HEAD() throws Exception {
		final RevCommit commit = gitRepo.resolveCommit("HEAD");

		assertNotNull("应该解析到提交", commit);
	}

	@Test
	public void should_resolve_commit_by_relative_ref() throws Exception {
		final RevCommit commit = gitRepo.resolveCommit("HEAD~1");

		assertNotNull("应该解析到提交", commit);
	}

	@Test
	public void should_throw_exception_for_invalid_commit() {
		try {
			gitRepo.resolveCommit("invalid-commit-hash-12345");
			fail("应该抛出GitException");
		} catch (final GitException e) {
			assertTrue("错误消息应该包含提交引用",
					e.getMessage().contains("invalid-commit-hash"));
		}
	}

	@Test
	public void should_get_first_commit_of_branch() throws Exception {
		// 获取当前分支名（可能是master或main）
		final String currentBranch = git.getRepository().getBranch();
		final RevCommit firstCommit = gitRepo.getFirstCommit(currentBranch);

		assertNotNull("应该获取到第一次提交", firstCommit);
		assertEquals("提交消息应该是Initial commit", "Initial commit",
				firstCommit.getShortMessage());
	}

	@Test
	public void should_throw_exception_for_nonexistent_branch() {
		try {
			gitRepo.getFirstCommit("nonexistent-branch");
			fail("应该抛出GitException");
		} catch (final GitException e) {
			assertTrue("错误消息应该包含分支名",
					e.getMessage().contains("nonexistent-branch"));
		}
	}

	@Test
	public void should_get_changed_java_files() throws Exception {
		// 获取HEAD~1提交
		final RevCommit startCommit = gitRepo.resolveCommit("HEAD~1");

		// 获取变更文件
		final Set<String> changedFiles = gitRepo
				.getChangedJavaFiles(startCommit);

		assertNotNull("应该返回文件集合", changedFiles);
		assertTrue("应该包含Test3.java", changedFiles.contains("Test3.java"));
	}

	@Test
	public void should_not_include_deleted_files() throws Exception {
		// 删除一个文件
		final File testFile = new File(repoDir, "Test2.java");
		testFile.delete();
		git.add().addFilepattern(".").call();
		git.commit().setMessage("Delete Test2.java").call();

		// 获取HEAD~1提交
		final RevCommit startCommit = gitRepo.resolveCommit("HEAD~1");

		// 获取变更文件
		final Set<String> changedFiles = gitRepo
				.getChangedJavaFiles(startCommit);

		// 删除的文件不应该在列表中
		assertFalse("不应该包含已删除的文件", changedFiles.contains("Test2.java"));
	}

	@Test
	public void should_handle_empty_changeset() throws Exception {
		// 使用HEAD作为起始提交（没有变更）
		final RevCommit startCommit = gitRepo.resolveCommit("HEAD");

		// 获取变更文件
		final Set<String> changedFiles = gitRepo
				.getChangedJavaFiles(startCommit);

		assertNotNull("应该返回文件集合", changedFiles);
		assertTrue("应该是空集合", changedFiles.isEmpty());
	}

}
