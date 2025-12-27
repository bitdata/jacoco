# Research: Git增量代码覆盖率分析

**Date**: 2025-12-25  
**Feature**: Git增量代码覆盖率分析

## 技术调研

### 1. Git操作库选择

**选项A: JGit (Eclipse JGit)**
- 优点：
  - 纯Java实现，无需外部git命令
  - 与JaCoCo的Eclipse许可证兼容（EPL-2.0）
  - API丰富，支持所有Git操作
  - 跨平台，不依赖系统PATH
- 缺点：
  - 增加依赖库大小
  - 学习曲线
- 结论：**推荐使用JGit**

**选项B: 调用git命令行**
- 优点：
  - 无需添加依赖
  - 直接使用系统git命令
- 缺点：
  - 依赖系统PATH中的git命令
  - 跨平台兼容性问题（Windows/Linux/macOS）
  - 需要解析命令行输出
  - 错误处理复杂
- 结论：不推荐

**最终选择**: JGit库

### 2. 增量文件识别策略

**方案A: 使用git diff获取变更文件列表**
```bash
git diff --name-only <commit>..HEAD
git diff --name-only --diff-filter=AM <commit>..HEAD  # 只包含新增和修改
```
- 优点：简单直接
- 缺点：需要处理文件重命名（--find-renames）

**方案B: 使用git log获取提交中的文件**
```bash
git log --name-only --pretty=format: <commit>..HEAD
```
- 优点：可以获取每次提交的变更
- 缺点：需要去重，处理更复杂

**最终选择**: 方案A（git diff），使用JGit的DiffCommand API

### 3. 文件过滤时机

**方案A: 在Analyzer.analyzeAll()调用前过滤File列表**
- 位置：Report.analyze()方法中，遍历classfiles之前
- 优点：简单，不影响Analyzer内部逻辑
- 缺点：需要处理目录递归的情况

**方案B: 创建自定义Analyzer包装器**
- 优点：更灵活，可以精细控制
- 缺点：需要修改更多代码，可能影响性能

**最终选择**: 方案A，在Report命令中过滤文件列表

### 4. Java源文件与类文件匹配

**问题**: Git diff返回的是.java源文件路径，但Analyzer需要.class文件

**解决方案**:
1. 将.java路径转换为.class路径（替换扩展名）
2. 将相对路径转换为相对于classfiles目录的路径
3. 处理包结构：src/main/java/com/example/Test.java -> target/classes/com/example/Test.class

**实现方式**:
- 创建路径映射工具类
- 支持Maven/Gradle标准目录结构
- 支持自定义目录结构

### 5. 提交标识解析

**Git提交标识格式**:
- 完整哈希：40位十六进制字符串
- 短哈希：7位或更多
- 标签：tag名称
- 相对引用：HEAD~1, HEAD^, branch~2等

**JGit支持**:
- `Repository.resolve(String)` 可以解析所有格式
- 自动处理短哈希歧义（如果存在）
- 支持所有Git标准引用格式

### 6. 分支第一次提交识别

**问题**: 当未指定commit时，需要找到分支的第一次提交

**解决方案**:
- 使用`git rev-list --reverse <branch> | head -1`
- JGit: `RevWalk`遍历提交历史，找到第一个提交

### 7. 性能考虑

**潜在性能瓶颈**:
1. Git操作（解析提交、diff）：预计<2秒
2. 文件过滤：O(n)复杂度，n为文件数量
3. 路径匹配：需要高效算法

**优化策略**:
- 缓存Git操作结果（在同一命令执行期间）
- 使用Set进行快速文件路径查找
- 避免重复的文件系统操作

### 8. 错误处理

**需要处理的错误场景**:
1. 分支不存在：抛出明确的异常
2. 提交不存在：抛出明确的异常
3. 提交不在分支历史中：抛出明确的异常
4. 不是Git仓库：检测.git目录存在性
5. Git操作失败：捕获并转换为用户友好的错误信息

### 9. 向后兼容性

**关键点**:
- 不指定--branch和--commit时，行为必须与原有Report命令完全一致
- 所有现有参数和选项保持不变
- 报告格式完全一致

**实现方式**:
- 使用Optional或null检查
- 仅在指定了--branch或--commit时才启用增量分析
- 默认情况下，classfiles和sourcefiles列表不变

## 技术决策总结

1. **Git库**: JGit (org.eclipse.jgit)
2. **增量识别**: git diff获取变更文件列表
3. **过滤时机**: 在Analyzer调用前过滤文件列表
4. **路径匹配**: 自定义工具类处理.java到.class的路径转换
5. **提交解析**: 使用JGit的Repository.resolve()
6. **错误处理**: 明确的异常和错误消息

## 参考资料

- JGit Documentation: https://www.eclipse.org/jgit/
- JGit API: https://download.eclipse.org/jgit/docs/latest/apidocs/
- Git Diff Documentation: https://git-scm.com/docs/git-diff
- JaCoCo Analyzer API: org.jacoco.core.analysis.Analyzer

