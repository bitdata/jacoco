# Quick Start: Git增量代码覆盖率分析

**Date**: 2025-12-25  
**Feature**: Git增量代码覆盖率分析

## 功能概述

JaCoCo CLI的`report`命令现在支持增量代码覆盖率分析。通过指定`--branch`和`--commit`参数，可以仅分析自指定提交以来的代码变更的测试覆盖率。

## 基本用法

### 示例1: 分析自特定提交以来的代码

```bash
java -jar jacococli.jar report \
  --classfiles target/classes \
  --sourcefiles src/main/java \
  --html report \
  --branch master \
  --commit HEAD~10 \
  jacoco.exec
```

**说明**:
- `--branch master`: 指定分支为master
- `--commit HEAD~10`: 从HEAD往前10个提交开始分析
- 只分析这10个提交中修改或新增的Java文件的覆盖率

### 示例2: 分析分支的所有代码

```bash
java -jar jacococli.jar report \
  --classfiles target/classes \
  --sourcefiles src/main/java \
  --html report \
  --branch feature/new-feature \
  jacoco.exec
```

**说明**:
- `--branch feature/new-feature`: 指定分支
- 未指定`--commit`: 从该分支的第一次提交开始分析所有代码

### 示例3: 使用提交哈希

```bash
java -jar jacococli.jar report \
  --classfiles target/classes \
  --sourcefiles src/main/java \
  --xml report.xml \
  --branch master \
  --commit abc1234567890abcdef1234567890abcdef1234 \
  jacoco.exec
```

**说明**:
- `--commit`支持完整哈希（40位）或短哈希（7位或更多）

### 示例4: 使用Git标签

```bash
java -jar jacococli.jar report \
  --classfiles target/classes \
  --sourcefiles src/main/java \
  --html report \
  --branch master \
  --commit v1.0.0 \
  jacoco.exec
```

**说明**:
- `--commit`支持Git标签名称

## 向后兼容性

**重要**: 如果不指定`--branch`和`--commit`参数，命令行为与原有JaCoCo完全一致：

```bash
# 原有用法仍然有效，行为不变
java -jar jacococli.jar report \
  --classfiles target/classes \
  --sourcefiles src/main/java \
  --html report \
  jacoco.exec
```

## 提交标识格式

`--commit`参数支持所有Git标准提交引用格式：

- **完整哈希**: `abc1234567890abcdef1234567890abcdef1234`
- **短哈希**: `abc1234` (至少7位)
- **标签**: `v1.0.0`, `release-1.0`
- **相对引用**: `HEAD~1`, `HEAD^`, `master~5`
- **分支名**: `master`, `develop` (解析为分支的最新提交)

## 工作原理

1. **识别增量文件**: 使用`git diff`获取自指定提交以来变更的Java源文件
2. **路径映射**: 将Java源文件路径映射到对应的类文件路径
3. **文件过滤**: 只分析变更文件对应的类文件
4. **生成报告**: 使用过滤后的文件列表生成覆盖率报告（格式与原有报告完全一致）

## 注意事项

1. **Git仓库要求**: 必须在Git仓库目录中运行命令，或确保可以访问Git仓库
2. **分支存在性**: 指定的分支必须存在于Git仓库中
3. **提交存在性**: 指定的提交必须存在于指定分支的历史中
4. **文件匹配**: 系统会自动匹配Java源文件和类文件，但需要确保目录结构符合标准（Maven/Gradle）或可映射

## 错误处理

如果出现错误，命令会输出明确的错误信息：

```bash
# 分支不存在
[ERROR] Branch 'nonexistent-branch' does not exist.

# 提交不存在
[ERROR] Commit 'invalid-commit' does not exist or cannot be resolved.

# 不是Git仓库
[ERROR] Current directory is not a Git repository.
```

## 性能

- Git操作通常<2秒
- 文件过滤开销可忽略不计
- 增量分析可以显著减少分析时间（仅分析变更文件）

## 更多示例

### Maven项目

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

### Gradle项目

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

