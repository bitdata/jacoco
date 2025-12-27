/*******************************************************************************
 * Copyright (c) 2009, 2025 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    John Keeping - initial implementation
 *    Marc R. Hoffmann - rework
 *
 *******************************************************************************/
package org.jacoco.cli.internal.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jacoco.cli.internal.Command;
import org.jacoco.cli.internal.git.GitException;
import org.jacoco.cli.internal.git.GitRepository;
import org.jacoco.cli.internal.git.IncrementalFileFilter;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.ISourceFileLocator;
import org.jacoco.report.MultiReportVisitor;
import org.jacoco.report.MultiSourceFileLocator;
import org.jacoco.report.csv.CSVFormatter;
import org.jacoco.report.html.HTMLFormatter;
import org.jacoco.report.xml.XMLFormatter;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 * The <code>report</code> command.
 */
public class Report extends Command {

	@Argument(usage = "list of JaCoCo *.exec files to read", metaVar = "<execfiles>")
	List<File> execfiles = new ArrayList<File>();

	@Option(name = "--classfiles", usage = "location of Java class files", metaVar = "<path>", required = true)
	List<File> classfiles = new ArrayList<File>();

	@Option(name = "--sourcefiles", usage = "location of the source files", metaVar = "<path>")
	List<File> sourcefiles = new ArrayList<File>();

	@Option(name = "--tabwith", usage = "tab stop width for the source pages (default 4)", metaVar = "<n>")
	int tabwidth = 4;

	@Option(name = "--name", usage = "name used for this report", metaVar = "<name>")
	String name = "JaCoCo Coverage Report";

	@Option(name = "--encoding", usage = "source file encoding (by default platform encoding is used)", metaVar = "<charset>")
	String encoding;

	@Option(name = "--xml", usage = "output file for the XML report", metaVar = "<file>")
	File xml;

	@Option(name = "--csv", usage = "output file for the CSV report", metaVar = "<file>")
	File csv;

	@Option(name = "--html", usage = "output directory for the HTML report", metaVar = "<dir>")
	File html;

	@Option(name = "--branch", usage = "Git分支名称，用于增量分析", metaVar = "<branch>")
	String branch;

	@Option(name = "--commit", usage = "Git提交标识（格式与git checkout兼容），用于增量分析起始点", metaVar = "<commit>")
	String commit;

	@Override
	public String description() {
		return "Generate reports in different formats by reading exec and Java class files. "
				+ "支持增量分析：使用--branch和--commit参数可以仅分析指定提交之后的代码变更。";
	}

	@Override
	public int execute(final PrintWriter out, final PrintWriter err)
			throws IOException {
		// 如果指定了增量分析参数，进行文件过滤
		if (branch != null || commit != null) {
			try {
				filterIncrementalFiles(out);
			} catch (final GitException e) {
				err.println("[ERROR] " + e.getMessage());
				if (e.getCause() != null) {
					err.println("[ERROR] 原因: " + e.getCause().getMessage());
				}
				return -1;
			}
		}

		final ExecFileLoader loader = loadExecutionData(out);
		final IBundleCoverage bundle = analyze(loader.getExecutionDataStore(),
				out);
		writeReports(bundle, loader, out);
		return 0;
	}

	/**
	 * 过滤增量文件。 如果指定了--branch或--commit参数，则只分析增量代码。
	 *
	 * @param out
	 *            输出流
	 * @throws GitException
	 *             如果Git操作失败
	 */
	private void filterIncrementalFiles(final PrintWriter out)
			throws GitException {
		// 确定Git仓库目录
		// 策略：从classfiles或sourcefiles目录向上查找.git目录
		File repoDir = findGitRepositoryRoot();
		if (repoDir == null) {
			// 如果找不到，使用当前工作目录
			repoDir = new File(System.getProperty("user.dir", "."));
		}

		final GitRepository gitRepo = new GitRepository(repoDir);
		try {
			if (!gitRepo.isGitRepository()) {
				throw new GitException(
						"当前目录不是Git仓库: " + repoDir.getAbsolutePath()
								+ "。请确保在Git仓库目录中运行命令，或指定正确的classfiles路径。");
			}

			// 解析提交
			org.eclipse.jgit.revwalk.RevCommit startCommit;
			if (commit != null) {
				// 如果指定了commit，解析它
				try {
					startCommit = gitRepo.resolveCommit(commit);
					out.printf("[INFO] 使用提交: %s (%.7s)%n",
							startCommit.getName(), startCommit.getName());
				} catch (final GitException e) {
					throw new GitException(
							"无法解析提交 '" + commit + "': " + e.getMessage(), e);
				}
			} else if (branch != null) {
				// 如果只指定了branch，获取分支的第一次提交
				try {
					startCommit = gitRepo.getFirstCommit(branch);
					out.printf("[INFO] 使用分支 %s 的第一次提交: %s (%.7s)%n", branch,
							startCommit.getName(), startCommit.getName());
				} catch (final GitException e) {
					throw new GitException(
							"无法获取分支 '" + branch + "' 的第一次提交: " + e.getMessage(),
							e);
				}
			} else {
				// 不应该到达这里，但为了安全起见
				return;
			}

			// 获取变更的Java文件列表
			final java.util.Set<String> changedJavaFiles = gitRepo
					.getChangedJavaFiles(startCommit);
			out.printf("[INFO] 发现 %d 个变更的Java文件%n",
					Integer.valueOf(changedJavaFiles.size()));

			// 过滤文件列表
			final IncrementalFileFilter filter = new IncrementalFileFilter();
			final List<File> filteredClassfiles = filter
					.filterClassFiles(classfiles, changedJavaFiles);
			final List<File> filteredSourcefiles = filter
					.filterSourceFiles(sourcefiles, changedJavaFiles);

			// 检查是否有文件无法映射（记录警告）
			int unmappedCount = 0;
			for (final String javaPath : changedJavaFiles) {
				final File mappedClassFile = filter.mapJavaToClassFile(javaPath,
						classfiles);
				if (mappedClassFile == null) {
					unmappedCount++;
					if (unmappedCount <= 5) { // 只显示前5个警告
						out.printf("[WARN] 无法找到Java源文件对应的类文件: %s%n", javaPath);
					}
				}
			}
			if (unmappedCount > 5) {
				out.printf("[WARN] 还有 %d 个文件无法映射到类文件（已省略详细信息）%n",
						Integer.valueOf(unmappedCount - 5));
			}

			// 更新文件列表
			classfiles.clear();
			classfiles.addAll(filteredClassfiles);
			sourcefiles.clear();
			sourcefiles.addAll(filteredSourcefiles);

			out.printf("[INFO] 过滤后: %d 个类文件, %d 个源文件%n",
					Integer.valueOf(classfiles.size()),
					Integer.valueOf(sourcefiles.size()));
		} finally {
			gitRepo.close();
		}
	}

	/**
	 * 查找Git仓库根目录。 从classfiles或sourcefiles目录向上查找，直到找到包含.git的目录。
	 *
	 * @return Git仓库根目录，如果找不到则返回null
	 */
	private File findGitRepositoryRoot() {
		// 尝试从classfiles目录查找
		if (!classfiles.isEmpty()) {
			final File repoDir = findGitRootFromFile(classfiles.get(0));
			if (repoDir != null) {
				return repoDir;
			}
		}
		// 尝试从sourcefiles目录查找
		if (!sourcefiles.isEmpty()) {
			final File repoDir = findGitRootFromFile(sourcefiles.get(0));
			if (repoDir != null) {
				return repoDir;
			}
		}
		// 尝试从当前工作目录查找
		return findGitRootFromFile(
				new File(System.getProperty("user.dir", ".")));
	}

	/**
	 * 从指定文件/目录向上查找Git仓库根目录。
	 *
	 * @param startFile
	 *            起始文件或目录
	 * @return Git仓库根目录，如果找不到则返回null
	 */
	private File findGitRootFromFile(final File startFile) {
		File current = startFile.isFile() ? startFile.getParentFile()
				: startFile;
		while (current != null) {
			final File gitDir = new File(current, ".git");
			if (gitDir.exists() && (gitDir.isDirectory() || gitDir.isFile())) {
				return current;
			}
			final File parent = current.getParentFile();
			if (parent == null || parent.equals(current)) {
				break;
			}
			current = parent;
		}
		return null;
	}

	private ExecFileLoader loadExecutionData(final PrintWriter out)
			throws IOException {
		final ExecFileLoader loader = new ExecFileLoader();
		if (execfiles.isEmpty()) {
			out.println("[WARN] No execution data files provided.");
		} else {
			for (final File file : execfiles) {
				out.printf("[INFO] Loading execution data file %s.%n",
						file.getAbsolutePath());
				loader.load(file);
			}
		}
		return loader;
	}

	private IBundleCoverage analyze(final ExecutionDataStore data,
			final PrintWriter out) throws IOException {
		final CoverageBuilder builder = new CoverageBuilder();
		final Analyzer analyzer = new Analyzer(data, builder);
		for (final File f : classfiles) {
			analyzer.analyzeAll(f);
		}
		printNoMatchWarning(builder.getNoMatchClasses(), out);
		return builder.getBundle(name);
	}

	private void printNoMatchWarning(final Collection<IClassCoverage> nomatch,
			final PrintWriter out) {
		if (!nomatch.isEmpty()) {
			out.println(
					"[WARN] Some classes do not match with execution data.");
			out.println(
					"[WARN] For report generation the same class files must be used as at runtime.");
			for (final IClassCoverage c : nomatch) {
				out.printf(
						"[WARN] Execution data for class %s does not match.%n",
						c.getName());
			}
		}
	}

	private void writeReports(final IBundleCoverage bundle,
			final ExecFileLoader loader, final PrintWriter out)
			throws IOException {
		out.printf("[INFO] Analyzing %s classes.%n",
				Integer.valueOf(bundle.getClassCounter().getTotalCount()));
		final IReportVisitor visitor = createReportVisitor();
		visitor.visitInfo(loader.getSessionInfoStore().getInfos(),
				loader.getExecutionDataStore().getContents());
		visitor.visitBundle(bundle, getSourceLocator());
		visitor.visitEnd();
	}

	private IReportVisitor createReportVisitor() throws IOException {
		final List<IReportVisitor> visitors = new ArrayList<IReportVisitor>();

		if (xml != null) {
			final XMLFormatter formatter = new XMLFormatter();
			visitors.add(formatter.createVisitor(new FileOutputStream(xml)));
		}

		if (csv != null) {
			final CSVFormatter formatter = new CSVFormatter();
			visitors.add(formatter.createVisitor(new FileOutputStream(csv)));
		}

		if (html != null) {
			final HTMLFormatter formatter = new HTMLFormatter();
			visitors.add(
					formatter.createVisitor(new FileMultiReportOutput(html)));
		}

		return new MultiReportVisitor(visitors);
	}

	private ISourceFileLocator getSourceLocator() {
		final MultiSourceFileLocator multi = new MultiSourceFileLocator(
				tabwidth);
		for (final File f : sourcefiles) {
			multi.add(new DirectorySourceFileLocator(f, encoding, tabwidth));
		}
		return multi;
	}

}
