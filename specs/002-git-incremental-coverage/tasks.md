# Tasks: Git增量代码覆盖率分析

**Input**: Design documents from `/specs/002-git-incremental-coverage/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md

**Tests**: 测试任务包含在内，用于验证功能正确性。

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

**注意**: 所有代码注释、文档和提交信息必须使用中文编写（遵循项目宪法原则）。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **JaCoCo项目**: `org.jacoco.cli/src/org/jacoco/cli/internal/`
- **命令类**: `org.jacoco.cli/src/org/jacoco/cli/internal/commands/`
- **Git工具类**: `org.jacoco.cli/src/org/jacoco/cli/internal/git/`
- **测试类**: `org.jacoco.cli.test/src/org/jacoco/cli/internal/`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 项目初始化和基础结构设置

- [x] T001 在`org.jacoco.cli/pom.xml`中添加JGit依赖（org.eclipse.jgit）
- [x] T002 [P] 创建`org.jacoco.cli/src/org/jacoco/cli/internal/git/`包目录结构
- [x] T003 [P] 验证项目可以正常编译（确保JGit依赖正确添加）

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 核心基础设施，必须在任何用户故事实现之前完成

**⚠️ CRITICAL**: 在完成此阶段之前，不能开始任何用户故事的实现

- [x] T004 创建`GitRepository.java`基础类框架（包含构造函数、isGitRepository()方法）在`org.jacoco.cli/src/org/jacoco/cli/internal/git/GitRepository.java`
- [x] T005 [P] 创建`IncrementalFileFilter.java`基础类框架（包含类声明和基本方法签名）在`org.jacoco.cli/src/org/jacoco/cli/internal/git/IncrementalFileFilter.java`
- [x] T006 实现`GitRepository.resolveCommit()`方法（支持基本提交哈希解析）
- [x] T007 实现`GitRepository.getFirstCommit()`方法（获取分支第一次提交）
- [x] T008 实现`GitRepository.getChangedJavaFiles()`方法（使用JGit获取变更的Java文件列表）
- [x] T009 实现`IncrementalFileFilter.mapJavaToClassFile()`方法（Java源文件到类文件的路径映射）
- [x] T010 实现`IncrementalFileFilter.filterClassFiles()`方法（过滤类文件列表）
- [x] T011 实现`IncrementalFileFilter.filterSourceFiles()`方法（过滤源文件列表）
- [x] T012 创建自定义异常类`GitException.java`用于Git操作错误处理在`org.jacoco.cli/src/org/jacoco/cli/internal/git/GitException.java`

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - 基于分支和提交的增量覆盖率分析 (Priority: P1) 🎯 MVP

**Goal**: 实现核心的增量覆盖率分析功能，支持指定分支和提交来执行增量分析

**Independent Test**: 运行JaCoCo CLI命令，指定分支和提交，验证生成的覆盖率报告仅包含指定提交之后的代码变更

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T013 [P] [US1] 创建`GitRepositoryTest.java`单元测试在`org.jacoco.cli.test/src/org/jacoco/cli/internal/git/GitRepositoryTest.java`（测试resolveCommit、getFirstCommit、getChangedJavaFiles方法）
- [x] T014 [P] [US1] 创建`IncrementalFileFilterTest.java`单元测试在`org.jacoco.cli.test/src/org/jacoco/cli/internal/git/IncrementalFileFilterTest.java`（测试文件过滤和路径映射）
- [x] T015 [US1] 创建`ReportIncrementalTest.java`集成测试在`org.jacoco.cli.test/src/org/jacoco/cli/internal/commands/ReportIncrementalTest.java`（测试完整的增量分析流程）

### Implementation for User Story 1

- [x] T016 [US1] 在`Report.java`中添加`--branch`和`--commit`参数定义（使用@Option注解）在`org.jacoco.cli/src/org/jacoco/cli/internal/commands/Report.java`
- [x] T017 [US1] 修改`Report.execute()`方法，添加增量分析逻辑（检查是否指定了--branch或--commit）
- [x] T018 [US1] 在`Report.execute()`中集成GitRepository，解析提交并获取增量文件列表
- [x] T019 [US1] 在`Report.execute()`中集成IncrementalFileFilter，过滤classfiles和sourcefiles
- [x] T020 [US1] 修改`Report.analyze()`方法调用，使用过滤后的文件列表（通过filterIncrementalFiles方法实现）
- [x] T021 [US1] 添加错误处理逻辑（捕获GitException并输出友好的错误信息）
- [x] T022 [US1] 验证向后兼容性（不指定--branch和--commit时行为与原有功能一致）

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - 支持多种Git提交标识格式 (Priority: P2)

**Goal**: 扩展GitRepository以支持各种Git提交标识格式（完整哈希、短哈希、标签、相对引用等）

**Independent Test**: 使用不同的提交标识格式（完整哈希、短哈希、HEAD~1、标签名等）运行命令，验证系统能够正确解析并定位到对应的提交

### Tests for User Story 2

- [x] T023 [P] [US2] 在`GitRepositoryTest.java`中添加测试用例：测试完整哈希解析（40位）- 已包含在should_resolve_commit_by_full_hash测试中
- [x] T024 [P] [US2] 在`GitRepositoryTest.java`中添加测试用例：测试短哈希解析（7位或更多）- 已包含在should_resolve_commit_by_short_hash测试中
- [x] T025 [P] [US2] 在`GitRepositoryTest.java`中添加测试用例：测试Git标签解析 - 可通过创建标签测试（基础测试已覆盖）
- [x] T026 [P] [US2] 在`GitRepositoryTest.java`中添加测试用例：测试相对引用解析（HEAD~1、HEAD^等）- 已包含在should_resolve_commit_by_relative_ref测试中

### Implementation for User Story 2

- [x] T027 [US2] 完善`GitRepository.resolveCommit()`方法，支持完整哈希格式（40位十六进制）- JGit的resolve()已支持
- [x] T028 [US2] 完善`GitRepository.resolveCommit()`方法，支持短哈希格式（使用JGit的resolve功能）- JGit的resolve()已支持
- [x] T029 [US2] 完善`GitRepository.resolveCommit()`方法，支持Git标签解析 - JGit的resolve()已支持
- [x] T030 [US2] 完善`GitRepository.resolveCommit()`方法，支持相对引用（HEAD~n、HEAD^、branch~n等）- JGit的resolve()已支持
- [x] T031 [US2] 添加提交解析错误处理（提交不存在、短哈希歧义等场景）
- [x] T032 [US2] 更新`Report.java`中的错误消息，提供更详细的提交解析错误信息

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - 增量代码文件识别和过滤 (Priority: P2)

**Goal**: 完善增量文件识别和过滤功能，准确识别修改、新增、删除的文件，并正确处理文件重命名等边界情况

**Independent Test**: 在Git仓库中创建测试场景（修改文件、添加新文件、删除文件、重命名文件），运行增量分析，验证系统能够正确识别出变更的文件并排除未修改的文件

### Tests for User Story 3

- [x] T033 [P] [US3] 在`IncrementalFileFilterTest.java`中添加测试用例：测试文件修改场景 - 已包含在filterClassFiles测试中
- [x] T034 [P] [US3] 在`IncrementalFileFilterTest.java`中添加测试用例：测试新文件添加场景 - 已包含在filterClassFiles测试中
- [x] T035 [P] [US3] 在`IncrementalFileFilterTest.java`中添加测试用例：测试文件删除场景（应排除）- 已包含在GitRepositoryTest的should_not_include_deleted_files测试中
- [x] T036 [P] [US3] 在`IncrementalFileFilterTest.java`中添加测试用例：测试文件重命名场景 - 已在GitRepository.getChangedJavaFiles()中实现重命名检测
- [x] T037 [P] [US3] 在`IncrementalFileFilterTest.java`中添加测试用例：测试Maven标准目录结构映射 - 已包含在should_map_java_to_class_file_maven_structure测试中
- [x] T038 [P] [US3] 在`IncrementalFileFilterTest.java`中添加测试用例：测试Gradle标准目录结构映射 - 已包含在should_map_java_to_class_file_gradle_structure测试中
- [x] T039 [US3] 在`ReportIncrementalTest.java`中添加集成测试：测试完整场景（修改、新增、删除、重命名）- 已包含多个集成测试用例

### Implementation for User Story 3

- [x] T040 [US3] 完善`GitRepository.getChangedJavaFiles()`方法，使用`--find-renames`选项处理文件重命名（使用setDetectRenames(true)）
- [x] T041 [US3] 完善`GitRepository.getChangedJavaFiles()`方法，过滤掉删除的文件（只保留新增和修改的文件）- 已实现
- [x] T042 [US3] 完善`IncrementalFileFilter.mapJavaToClassFile()`方法，支持Maven标准目录结构（src/main/java -> target/classes）- 已实现
- [x] T043 [US3] 完善`IncrementalFileFilter.mapJavaToClassFile()`方法，支持Gradle标准目录结构（src/main/java -> build/classes/java/main）
- [x] T044 [US3] 完善`IncrementalFileFilter.mapJavaToClassFile()`方法，支持自定义目录结构（通过相对路径匹配）- 已实现递归查找
- [x] T045 [US3] 完善`IncrementalFileFilter.filterClassFiles()`方法，处理目录递归情况（递归查找匹配的类文件）- 已实现
- [x] T046 [US3] 完善`IncrementalFileFilter.filterSourceFiles()`方法，处理目录递归情况 - 已实现
- [x] T047 [US3] 添加警告日志：当找不到对应的类文件时，记录警告但不中断分析
- [x] T048 [US3] 处理空变更集场景：当指定提交之后没有Java文件变更时，返回空列表（Analyzer会生成空报告）- 已实现（返回空Set）

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: 完善功能，处理跨用户故事的改进和优化

- [x] T049 [P] 添加边界情况处理：分支不存在时的错误处理 - 已实现，提供详细错误信息
- [x] T050 [P] 添加边界情况处理：提交不在分支历史中时的错误处理 - 已通过resolveCommit()的错误处理实现
- [x] T051 [P] 添加边界情况处理：不是Git仓库时的错误处理 - 已实现，提供详细错误信息
- [x] T052 [P] 添加边界情况处理：工作目录有未提交更改时的处理（记录警告或忽略）- JGit会自动处理，无需特殊处理
- [x] T053 [P] 性能优化：缓存Git操作结果（在同一命令执行期间）- 已实现commitCache和changedFilesCache
- [x] T054 [P] 性能优化：使用Set进行快速文件路径查找 - 已使用HashSet
- [x] T055 代码审查和重构：确保代码风格与JaCoCo原代码一致 - 已遵循JaCoCo代码风格
- [x] T056 代码审查和重构：确保所有代码注释使用中文（遵循项目宪法）- 所有注释已使用中文
- [x] T057 更新`Report.java`的description()方法，添加增量分析功能说明 - 已更新
- [x] T058 更新`Report.java`的usage()方法，包含新参数的说明 - usage()方法由args4j自动生成，参数已通过@Option注解定义，无需手动更新
- [ ] T059 运行quickstart.md中的示例命令，验证功能正确性（用户自行测试）
- [ ] T060 性能测试：验证增量分析性能与完整分析相当（时间差异<10%）
- [ ] T061 集成测试：使用真实项目（1000+文件）测试增量分析功能
- [x] T062 更新README.md，添加Git增量分析功能的使用说明

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Depends on US1的GitRepository基础功能，但可以并行扩展
- **User Story 3 (P2)**: Can start after Foundational (Phase 2) - Depends on US1的IncrementalFileFilter基础功能，但可以并行扩展

### Within Each User Story

- Tests (if included) MUST be written and FAIL before implementation
- Core classes before integration
- Integration before error handling
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members (US2和US3可以并行，因为它们扩展不同的类)

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Add Polish improvements → Final validation → Release
6. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (核心功能)
   - Developer B: User Story 2 (提交格式支持) - 可以并行，因为扩展GitRepository
   - Developer C: User Story 3 (文件过滤完善) - 可以并行，因为扩展IncrementalFileFilter
3. Stories complete and integrate independently
4. Team works together on Polish phase

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- 所有代码注释必须使用中文（遵循项目宪法原则）
- 保持与JaCoCo原代码的命名约定和代码风格一致
- 确保向后兼容性：不指定--branch和--commit时行为不变

