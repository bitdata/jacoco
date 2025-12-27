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
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

/**
 * Git仓库操作类。 封装Git仓库操作，提供增量文件识别功能。
 */
public class GitRepository {

	private Repository repository;
	private final File repositoryDir;
	private final java.util.Map<String, org.eclipse.jgit.revwalk.RevCommit> commitCache = new java.util.HashMap<String, org.eclipse.jgit.revwalk.RevCommit>();
	private final java.util.Map<org.eclipse.jgit.revwalk.RevCommit, Set<String>> changedFilesCache = new java.util.HashMap<org.eclipse.jgit.revwalk.RevCommit, Set<String>>();

	/**
	 * 创建GitRepository实例。
	 *
	 * @param repositoryDir
	 *            Git仓库目录
	 */
	public GitRepository(final File repositoryDir) {
		this.repositoryDir = repositoryDir;
		// 延迟初始化repository，允许非Git目录
	}

	/**
	 * 检查当前目录是否为Git仓库。
	 *
	 * @return true如果是Git仓库，否则false
	 */
	public boolean isGitRepository() {
		final File gitDir = new File(repositoryDir, ".git");
		return gitDir.exists() && gitDir.isDirectory();
	}

	/**
	 * 延迟初始化repository。如果repository未初始化且目录是Git仓库，则初始化它。
	 *
	 * @throws GitException
	 *             如果目录不是Git仓库或无法打开
	 */
	private void ensureRepository() throws GitException {
		if (repository == null) {
			if (!isGitRepository()) {
				throw new GitException(
						"无法打开Git仓库: " + repositoryDir.getAbsolutePath());
			}
			try {
				final RepositoryBuilder builder = new RepositoryBuilder();
				builder.setGitDir(new File(repositoryDir, ".git"));
				builder.setMustExist(true);
				this.repository = builder.build();
			} catch (final IOException e) {
				throw new GitException(
						"无法打开Git仓库: " + repositoryDir.getAbsolutePath(), e);
			}
		}
	}

	/**
	 * 解析Git提交标识（支持完整哈希、短哈希、标签、相对引用等）。 支持以下格式： - 完整哈希：40位十六进制字符串 -
	 * 短哈希：7位或更多十六进制字符 - 标签：标签名称（如"v1.0.0"） - 相对引用：HEAD~1, HEAD^, branch~n等
	 *
	 * @param commitRef
	 *            Git提交引用（如"HEAD~1", "abc1234", "v1.0"等）
	 * @return RevCommit对象，如果不存在则抛出异常
	 * @throws GitException
	 *             如果提交不存在或无法解析
	 */
	public RevCommit resolveCommit(final String commitRef) throws GitException {
		ensureRepository();
		if (commitRef == null || commitRef.trim().isEmpty()) {
			throw new GitException("提交引用不能为空");
		}
		// 检查缓存
		if (commitCache.containsKey(commitRef)) {
			return commitCache.get(commitRef);
		}
		try {
			final ObjectId commitId = repository.resolve(commitRef);
			if (commitId == null) {
				// 提供更详细的错误信息
				if (commitRef.length() < 7) {
					throw new GitException("提交引用太短（至少需要7个字符）: " + commitRef);
				}
				throw new GitException(
						"无法解析提交引用: " + commitRef + "。请检查提交是否存在，或标签/分支名称是否正确。");
			}
			final RevWalk walk = new RevWalk(repository);
			try {
				final RevCommit commit = walk.parseCommit(commitId);
				// 缓存结果
				commitCache.put(commitRef, commit);
				return commit;
			} catch (final org.eclipse.jgit.errors.IncorrectObjectTypeException e) {
				throw new GitException("引用 " + commitRef + " 不是提交对象（可能是标签或分支）",
						e);
			} finally {
				walk.close();
			}
		} catch (final IOException e) {
			throw new GitException("解析提交时出错: " + commitRef, e);
		}
	}

	/**
	 * 获取分支的第一次提交。
	 *
	 * @param branchName
	 *            分支名称
	 * @return RevCommit对象，如果分支不存在则抛出异常
	 * @throws GitException
	 *             如果分支不存在或无法获取第一次提交
	 */
	public RevCommit getFirstCommit(final String branchName)
			throws GitException {
		ensureRepository();
		if (branchName == null || branchName.trim().isEmpty()) {
			throw new GitException("分支名称不能为空");
		}
		try {
			// 尝试多种分支引用格式
			String refName = "refs/heads/" + branchName;
			ObjectId branchId = repository.resolve(refName);
			// 如果找不到，尝试默认分支（master或main）
			if (branchId == null && (branchName.equals("master")
					|| branchName.equals("main"))) {
				// 尝试另一个默认分支名
				final String altBranch = branchName.equals("master") ? "main"
						: "master";
				refName = "refs/heads/" + altBranch;
				branchId = repository.resolve(refName);
			}
			// 如果找不到，尝试直接使用分支名
			if (branchId == null) {
				branchId = repository.resolve(branchName);
			}
			if (branchId == null) {
				throw new GitException(
						"分支不存在: " + branchName + "。请检查分支名称是否正确。");
			}
			final RevWalk walk = new RevWalk(repository);
			try {
				walk.setRetainBody(false);
				walk.markStart(walk.parseCommit(branchId));
				RevCommit firstCommit = null;
				for (final RevCommit commit : walk) {
					firstCommit = commit;
				}
				if (firstCommit == null) {
					throw new GitException("分支 " + branchName + " 没有提交");
				}
				return firstCommit;
			} finally {
				walk.close();
			}
		} catch (final IOException e) {
			throw new GitException("获取分支第一次提交时出错: " + branchName, e);
		}
	}

	/**
	 * 获取自指定提交以来的变更文件列表（仅Java源文件）。
	 *
	 * @param commit
	 *            起始提交（包含该提交）
	 * @return 变更的Java源文件路径集合（相对于仓库根目录）
	 * @throws GitException
	 *             如果Git操作失败
	 */
	public Set<String> getChangedJavaFiles(final RevCommit commit)
			throws GitException {
		ensureRepository();
		// 检查缓存
		if (changedFilesCache.containsKey(commit)) {
			return new HashSet<String>(changedFilesCache.get(commit));
		}
		final Set<String> changedFiles = new HashSet<String>();
		try {
			final Git git = new Git(repository);
			try {
				// 获取HEAD提交
				final ObjectId headId = repository.resolve("HEAD");
				if (headId == null) {
					return changedFiles; // 空仓库，没有变更
				}
				final RevWalk walk = new RevWalk(repository);
				try {
					final RevCommit headCommit = walk.parseCommit(headId);

					// 准备树迭代器
					final AbstractTreeIterator oldTree = prepareTreeParser(
							repository, commit.getName());
					final AbstractTreeIterator newTree = prepareTreeParser(
							repository, headCommit.getName());

					// 获取差异，启用重命名检测
					final java.util.List<DiffEntry> diffs = git.diff()
							.setOldTree(oldTree).setNewTree(newTree)
							.setShowNameAndStatusOnly(true).call();
					// 注意：JGit默认会检测重命名，无需额外设置

					// 过滤Java文件（新增和修改的文件）
					for (final DiffEntry entry : diffs) {
						final String path = getChangedPath(entry);
						if (path != null && path.endsWith(".java")) {
							// 只包含新增、修改和重命名的文件，排除删除的文件
							final DiffEntry.ChangeType changeType = entry
									.getChangeType();
							if (changeType != DiffEntry.ChangeType.DELETE) {
								changedFiles.add(path);
								// 对于重命名，也添加旧路径（如果存在）
								if (changeType == DiffEntry.ChangeType.RENAME
										&& entry.getOldPath() != null
										&& entry.getOldPath()
												.endsWith(".java")) {
									changedFiles.add(entry.getOldPath());
								}
							}
						}
					}
				} finally {
					walk.close();
				}
			} finally {
				git.close();
			}
		} catch (final IOException e) {
			throw new GitException("获取变更文件列表时出错", e);
		} catch (final GitAPIException e) {
			throw new GitException("Git操作失败", e);
		}
		// 缓存结果
		changedFilesCache.put(commit, new HashSet<String>(changedFiles));
		return changedFiles;
	}

	/**
	 * 准备树解析器。
	 *
	 * @param repository
	 *            Git仓库
	 * @param objectId
	 *            对象ID
	 * @return 树迭代器
	 * @throws IOException
	 *             如果读取失败
	 */
	private AbstractTreeIterator prepareTreeParser(final Repository repository,
			final String objectId) throws IOException {
		try (RevWalk walk = new RevWalk(repository)) {
			final RevCommit commit = walk
					.parseCommit(repository.resolve(objectId));
			final CanonicalTreeParser treeParser = new CanonicalTreeParser();
			try (org.eclipse.jgit.lib.ObjectReader reader = repository
					.newObjectReader()) {
				treeParser.reset(reader, commit.getTree().getId());
			}
			walk.dispose();
			return treeParser;
		}
	}

	/**
	 * 获取变更文件的路径。
	 *
	 * @param entry
	 *            差异条目
	 * @return 文件路径，如果无法确定则返回null
	 */
	private String getChangedPath(final DiffEntry entry) {
		switch (entry.getChangeType()) {
		case ADD:
		case MODIFY:
		case COPY:
			return entry.getNewPath();
		case RENAME:
			// 对于重命名，返回新路径
			return entry.getNewPath();
		case DELETE:
			return entry.getOldPath();
		default:
			return null;
		}
	}

	/**
	 * 关闭Git仓库。
	 */
	public void close() {
		if (repository != null) {
			repository.close();
		}
	}

}
