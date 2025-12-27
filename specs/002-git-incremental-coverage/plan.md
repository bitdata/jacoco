# Implementation Plan: Git增量代码覆盖率分析

**Branch**: `002-git-incremental-coverage` | **Date**: 2025-12-25 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/002-git-incremental-coverage/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

为JaCoCo CLI添加基于Git的增量代码覆盖率分析功能。核心需求是在`report`命令中增加`--branch`和`--commit`参数，使覆盖率分析仅针对指定提交之后的代码变更。技术实现方案：在`Report`命令中添加Git操作支持，使用JGit库解析Git提交和识别增量文件，然后过滤`Analyzer`只分析增量代码文件，保持与原有报告格式的完全兼容。

## Technical Context

**Language/Version**: Java 8 (JDK路径: D:\bin\lib\openlogic-openjdk-8u432-b06-windows-x64)  
**Primary Dependencies**: 
- args4j 2.0.28 (CLI参数解析，已存在)
- ASM 9.9 (字节码分析，已存在)
- JGit (新增，用于Git操作)
- org.jacoco.core (覆盖率分析核心，已存在)
- org.jacoco.report (报告生成，已存在)

**Storage**: 文件系统（Git仓库、.exec文件、报告文件）  
**Testing**: JUnit 4.13.2 (项目已有测试框架)  
**Target Platform**: 跨平台（Windows/Linux/macOS），Java 8+  
**Project Type**: 单项目（Java库和CLI工具）  
**Performance Goals**: 
- 增量分析性能与完整分析相当（时间差异<10%）
- 能够处理1000+文件的代码库
- Git操作响应时间<2秒

**Constraints**: 
- 必须保持向后兼容（不指定--branch/--commit时行为不变）
- 报告格式必须与原有格式完全一致
- 技术栈和命名约定必须与原代码兼容
- 代码注释必须使用中文（遵循项目宪法）

**Scale/Scope**: 
- 修改1个命令类（Report.java）
- 新增1个Git工具类（GitRepository.java）
- 新增1个增量文件过滤器类（IncrementalFileFilter.java）
- 修改pom.xml添加JGit依赖
- 预计新增代码量：~500-800行

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **中文编写原则**: 所有文档、代码注释和提交信息必须使用中文编写（技术术语、API 名称等保持英文）

## Project Structure

### Documentation (this feature)

```text
specs/002-git-incremental-coverage/
├── plan.md              # This file (/speckit.plan command output)
├── spec.md              # Feature specification
├── checklists/          # Quality checklists
│   └── requirements.md
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
org.jacoco.cli/
├── pom.xml                                    # 修改：添加JGit依赖
└── src/
    └── org/
        └── jacoco/
            └── cli/
                └── internal/
                    ├── commands/
                    │   └── Report.java        # 修改：添加--branch和--commit参数，集成增量分析逻辑
                    └── git/                   # 新增包：Git相关工具类
                        ├── GitRepository.java     # Git仓库操作：解析提交、获取增量文件列表
                        └── IncrementalFileFilter.java  # 增量文件过滤器：过滤classfiles和sourcefiles
```

**Structure Decision**: 
- 在`org.jacoco.cli.internal`包下新增`git`子包，遵循JaCoCo的包命名约定
- 新增的类使用与原代码相同的命名风格（PascalCase类名，camelCase方法名）
- 保持与现有命令类相同的代码结构和风格
- 所有新增代码的注释使用中文（遵循项目宪法原则）

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

无需填写（无违反宪法的情况）

