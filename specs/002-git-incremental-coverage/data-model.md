# Data Model: Git增量代码覆盖率分析

**Date**: 2025-12-25  
**Feature**: Git增量代码覆盖率分析

## 核心数据实体

### 1. GitRepository (Git仓库操作类)

**职责**: 封装Git仓库操作，提供增量文件识别功能

**主要方法**:
```java
/**
 * 解析Git提交标识（支持完整哈希、短哈希、标签、相对引用等）
 * @param commitRef Git提交引用（如"HEAD~1", "abc1234", "v1.0"等）
 * @return RevCommit对象，如果不存在则抛出异常
 */
RevCommit resolveCommit(String commitRef) throws IOException

/**
 * 获取分支的第一次提交
 * @param branchName 分支名称
 * @return RevCommit对象，如果分支不存在则抛出异常
 */
RevCommit getFirstCommit(String branchName) throws IOException

/**
 * 获取自指定提交以来的变更文件列表（仅Java源文件）
 * @param commit 起始提交（包含该提交）
 * @return Set<String> 变更的Java源文件路径集合（相对于仓库根目录）
 */
Set<String> getChangedJavaFiles(RevCommit commit) throws IOException

/**
 * 检查当前目录是否为Git仓库
 * @return true如果是Git仓库，否则false
 */
boolean isGitRepository()
```

**内部状态**:
- `Repository repository`: JGit仓库对象
- `RepositoryState state`: 仓库状态（用于错误检查）

### 2. IncrementalFileFilter (增量文件过滤器)

**职责**: 根据增量文件列表过滤classfiles和sourcefiles

**主要方法**:
```java
/**
 * 过滤类文件列表，只保留增量代码对应的类文件
 * @param classfiles 原始类文件列表
 * @param changedJavaFiles 变更的Java源文件路径集合
 * @return 过滤后的类文件列表
 */
List<File> filterClassFiles(List<File> classfiles, Set<String> changedJavaFiles)

/**
 * 过滤源文件列表，只保留增量代码对应的源文件
 * @param sourcefiles 原始源文件列表
 * @param changedJavaFiles 变更的Java源文件路径集合
 * @return 过滤后的源文件列表
 */
List<File> filterSourceFiles(List<File> sourcefiles, Set<String> changedJavaFiles)

/**
 * 将Java源文件路径转换为对应的类文件路径
 * @param javaFilePath Java源文件路径（相对于仓库根目录）
 * @param classfilesBaseDirs 类文件基础目录列表
 * @return 对应的类文件路径，如果找不到则返回null
 */
File mapJavaToClassFile(String javaFilePath, List<File> classfilesBaseDirs)
```

**路径映射规则**:
- 标准Maven结构: `src/main/java/com/example/Test.java` -> `target/classes/com/example/Test.class`
- 标准Gradle结构: `src/main/java/com/example/Test.java` -> `build/classes/java/main/com/example/Test.class`
- 自定义结构: 通过相对路径匹配

### 3. Report命令扩展

**新增字段**:
```java
@Option(name = "--branch", usage = "Git分支名称，用于增量分析", metaVar = "<branch>")
String branch;

@Option(name = "--commit", usage = "Git提交标识（格式与git checkout兼容），用于增量分析起始点", metaVar = "<commit>")
String commit;
```

**修改的方法**:
- `analyze()`: 添加增量文件过滤逻辑
- `execute()`: 添加Git操作和错误处理

## 数据流

### 正常流程

```
1. 用户调用: java -jar jacococli.jar report --branch master --commit HEAD~5 ...
   ↓
2. Report.execute() 解析参数
   ↓
3. 如果指定了--branch或--commit:
   a. 创建GitRepository实例
   b. 解析commit（如果未指定则获取分支第一次提交）
   c. 获取变更的Java文件列表
   d. 创建IncrementalFileFilter
   e. 过滤classfiles和sourcefiles
   ↓
4. 调用原有的analyze()方法（使用过滤后的文件列表）
   ↓
5. 生成报告（格式与原有完全一致）
```

### 错误处理流程

```
1. Git操作失败（分支/提交不存在等）
   ↓
2. 抛出GitException（自定义异常）
   ↓
3. Report.execute()捕获异常
   ↓
4. 输出用户友好的错误信息到err PrintWriter
   ↓
5. 返回非零退出码
```

## 数据结构

### 变更文件集合
- **类型**: `Set<String>`
- **内容**: Java源文件路径（相对于Git仓库根目录）
- **示例**: `["src/main/java/com/example/Test.java", "src/test/java/com/example/TestTest.java"]`

### 文件路径映射
- **源文件路径**: `src/main/java/com/example/Test.java`
- **类文件路径**: `target/classes/com/example/Test.class`
- **映射规则**: 替换目录前缀（src/main/java -> target/classes），替换扩展名（.java -> .class）

## 边界情况处理

### 1. 文件重命名
- Git diff使用`--find-renames`选项
- 重命名的文件会被识别为变更文件
- 需要同时处理旧路径和新路径

### 2. 文件删除
- 删除的文件不在当前工作目录中
- 不需要分析已删除的文件
- 从变更列表中排除删除的文件

### 3. 目录结构不匹配
- 如果找不到对应的类文件，记录警告但不中断分析
- 允许部分文件匹配失败

### 4. 空变更集
- 如果指定提交之后没有Java文件变更
- 返回空的文件列表
- Analyzer会生成空报告（这是合理的行为）

