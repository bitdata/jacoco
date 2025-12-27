<!--
Sync Impact Report:
Version: 1.0.0 (initial)
Ratification Date: 2025-12-25
Last Amended: 2025-12-25
Modified Principles: N/A (initial creation)
Added Sections: 中文编写原则 (Chinese Writing Principle)
Removed Sections: N/A
Templates Requiring Updates:
  - ✅ updated: .specify/templates/plan-template.md (添加中文编写原则检查项)
  - ✅ updated: .specify/templates/spec-template.md (添加中文编写原则说明)
  - ✅ updated: .specify/templates/tasks-template.md (添加中文编写原则说明)
  - ✅ updated: .specify/templates/agent-file-template.md (添加中文编写原则说明)
  - ✅ updated: .specify/templates/checklist-template.md (添加中文编写原则说明)
Follow-up TODOs: None
-->

# 项目宪法 (Project Constitution)

**项目名称**: JaCoCo  
**版本**: 1.0.0  
**批准日期**: 2025-12-25  
**最后修订日期**: 2025-12-25

## 概述

本文档定义了 JaCoCo 项目的核心治理原则和开发规范。所有项目参与者必须遵守这些原则，以确保项目的一致性、可维护性和协作效率。

## 核心原则

### 原则 1: 中文编写要求

**名称**: 中文编写原则

**规则**:
- 所有项目文档必须使用中文编写，包括但不限于 README、设计文档、用户手册、API 文档等。
- 所有代码注释必须使用中文编写，包括类注释、方法注释、行内注释等。
- 所有提交信息（commit messages）必须使用中文编写。

**理由**:
- 确保项目文档和代码的可读性，特别是对于中文开发者。
- 保持项目文档和代码风格的一致性。
- 降低语言切换带来的认知负担，提高开发效率。
- 便于团队内部沟通和知识传承。

**例外情况**:
- 技术术语、API 名称、类名、方法名等保持英文（遵循编程语言惯例）。
- 与外部系统集成的接口文档，如需要可提供英文版本。
- 国际化相关的配置文件可使用英文。

## 治理

### 修订程序

1. **提案**: 任何项目成员可以提出宪法修订提案，需说明修订理由和具体内容。
2. **审查**: 提案需经过项目维护者审查，确保与项目目标一致。
3. **批准**: 修订需获得项目维护者的批准。
4. **版本更新**: 修订后必须更新版本号，遵循语义化版本规则：
   - **主版本号 (MAJOR)**: 向后不兼容的治理原则移除或重新定义。
   - **次版本号 (MINOR)**: 新增原则或实质性扩展指导。
   - **补丁版本号 (PATCH)**: 澄清、措辞修正、拼写错误修复、非语义性改进。
5. **同步更新**: 修订后必须同步更新所有依赖的模板和文档。

### 版本管理

- 版本号遵循语义化版本规范 (Semantic Versioning)。
- 每次修订必须更新 `LAST_AMENDED_DATE`。
- 重大修订应记录在变更日志中。

### 合规审查

- 所有代码提交和文档更新应自动或定期审查是否符合宪法原则。
- 不符合原则的提交应被拒绝或要求修正。
- 项目维护者负责确保宪法原则的执行。

## 变更历史

### 1.0.0 (2025-12-25)
- 初始版本
- 建立中文编写原则

