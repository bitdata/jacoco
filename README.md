JaCoCo Java Code Coverage Library
=================================

[![Build Status](https://dev.azure.com/jacoco-org/JaCoCo/_apis/build/status/JaCoCo?branchName=master)](https://dev.azure.com/jacoco-org/JaCoCo/_build/latest?definitionId=1&branchName=master)
[![Maven Central](https://img.shields.io/maven-central/v/org.jacoco/jacoco.svg)](https://central.sonatype.com/namespace/org.jacoco)

JaCoCo is a free Java code coverage library distributed under the Eclipse Public
License.

## Starting Points

*   I want to use JaCoCo → [Download](https://www.jacoco.org/jacoco/), [Maven](https://www.jacoco.org/jacoco/trunk/doc/maven.html), [Ant](https://www.jacoco.org/jacoco/trunk/doc/ant.html), [CLI](https://www.jacoco.org/jacoco/trunk/doc/cli.html), [Other](https://www.jacoco.org/jacoco/trunk/doc/integrations.html)
*   I want to know how JaCoCo works → [Documentation](http://www.jacoco.org/jacoco/trunk/doc/)
*   I have a question → [FAQ](http://www.jacoco.org/jacoco/trunk/doc/faq.html), [Documentation](http://www.jacoco.org/jacoco/trunk/doc/), [User Forum](https://groups.google.com/forum/?fromgroups=#!forum/jacoco)
*   I found a bug → [Bug Report](https://github.com/jacoco/jacoco/issues/new/choose)
*   I have an idea → [User Forum](https://groups.google.com/forum/?fromgroups=#!forum/jacoco), [Feature Request](https://github.com/jacoco/jacoco/issues/new/choose)

## Git增量代码覆盖率分析功能

本版本新增了基于Git的增量代码覆盖率分析功能，可以仅分析自指定提交以来的代码变更的测试覆盖率。

### 功能概述

通过指定`--branch`和`--commit`参数，JaCoCo CLI的`report`命令现在支持增量代码覆盖率分析：
- 仅分析自指定提交以来变更的Java文件
- 支持所有Git标准提交引用格式（哈希、标签、相对引用等）
- 完全向后兼容：不指定增量参数时行为与原有JaCoCo完全一致

### 使用方法

#### 基本语法

```bash
java -jar jacococli.jar report \
  --classfiles <classfiles路径> \
  --sourcefiles <sourcefiles路径> \
  [--branch <分支名>] \
  [--commit <提交标识>] \
  [--html <输出目录>] \
  [--xml <输出文件>] \
  [--csv <输出文件>] \
  <exec文件>
```

#### 参数说明

- `--branch <分支名>`: 指定Git分支名称，用于增量分析
- `--commit <提交标识>`: 指定起始提交（格式与`git checkout`兼容），如果省略则从该分支的第一次提交开始
- 其他参数与原有JaCoCo CLI完全一致

#### 提交标识格式

`--commit`参数支持所有Git标准提交引用格式：
- **完整哈希**: `abc1234567890abcdef1234567890abcdef1234`
- **短哈希**: `abc1234` (至少7位)
- **标签**: `v1.0.0`, `release-1.0`
- **相对引用**: `HEAD~1`, `HEAD^`, `master~5`
- **分支名**: `master`, `develop` (解析为分支的最新提交)

### 使用示例

#### 示例1: 分析自特定提交以来的代码

```bash
java -jar jacococli.jar report \
  --classfiles target/classes \
  --sourcefiles src/main/java \
  --html report \
  --branch master \
  --commit HEAD~10 \
  jacoco.exec
```

**说明**: 从HEAD往前10个提交开始分析，只分析这10个提交中修改或新增的Java文件的覆盖率。

#### 示例2: 分析分支的所有代码

```bash
java -jar jacococli.jar report \
  --classfiles target/classes \
  --sourcefiles src/main/java \
  --html report \
  --branch feature/new-feature \
  jacoco.exec
```

**说明**: 从`feature/new-feature`分支的第一次提交开始分析所有代码。

#### 示例3: 使用提交哈希

```bash
java -jar jacococli.jar report \
  --classfiles target/classes \
  --sourcefiles src/main/java \
  --xml report.xml \
  --branch master \
  --commit abc1234567890abcdef1234567890abcdef1234 \
  jacoco.exec
```

#### 示例4: 使用Git标签

```bash
java -jar jacococli.jar report \
  --classfiles target/classes \
  --sourcefiles src/main/java \
  --html report \
  --branch master \
  --commit v1.0.0 \
  jacoco.exec
```

#### 示例5: Maven项目

```bash
mvn clean test
java -jar jacococli.jar report \
  --classfiles target/classes \
  --sourcefiles src/main/java \
  --html target/site/jacoco \
  --branch master \
  --commit HEAD~5 \
  target/jacoco.exec
```

#### 示例6: Gradle项目

```bash
./gradlew test jacocoTestReport
java -jar jacococli.jar report \
  --classfiles build/classes/java/main \
  --sourcefiles src/main/java \
  --html build/reports/jacoco \
  --branch develop \
  --commit HEAD~3 \
  build/jacoco/test.exec
```

### 向后兼容性

**重要**: 如果不指定`--branch`和`--commit`参数，命令行为与原有JaCoCo完全一致：

```bash
# 原有用法仍然有效，行为不变
java -jar jacococli.jar report \
  --classfiles target/classes \
  --sourcefiles src/main/java \
  --html report \
  jacoco.exec
```

### 工作原理

1. **识别增量文件**: 使用`git diff`获取自指定提交以来变更的Java源文件
2. **路径映射**: 将Java源文件路径映射到对应的类文件路径（支持Maven/Gradle标准目录结构）
3. **文件过滤**: 只分析变更文件对应的类文件
4. **生成报告**: 使用过滤后的文件列表生成覆盖率报告（格式与原有报告完全一致）

### 注意事项

1. **Git仓库要求**: 必须在Git仓库目录中运行命令，或确保可以访问Git仓库
2. **分支存在性**: 指定的分支必须存在于Git仓库中
3. **提交存在性**: 指定的提交必须存在于指定分支的历史中
4. **文件匹配**: 系统会自动匹配Java源文件和类文件，但需要确保目录结构符合标准（Maven/Gradle）或可映射

### 错误处理

如果出现错误，命令会输出明确的错误信息：

```bash
# 分支不存在
[ERROR] Branch 'nonexistent-branch' does not exist.

# 提交不存在
[ERROR] Commit 'invalid-commit' does not exist or cannot be resolved.

# 不是Git仓库
[ERROR] Current directory is not a Git repository.
```

### 性能

- Git操作通常<2秒
- 文件过滤开销可忽略不计
- 增量分析可以显著减少分析时间（仅分析变更文件）