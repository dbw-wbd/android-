# WordReciteApp —— 简易背单词 App

一个用 **Java + Room 数据库** 实现的极简背单词 App，参考“不背单词”的卡片式背单词思路，
只保留课设需要的基础功能：**卡片背单词 + 生词本收藏 + 本地存储**。
没有后端、不需要联网、没有复杂动画、没有付费模块。

---

## 一、环境要求

| 项目 | 要求 |
| --- | --- |
| 开发工具 | Android Studio（建议 2022.3 及以上版本，本项目使用 AGP 8.5.2） |
| JDK | JDK 17（Android Studio 自带，无需单独安装） |
| Gradle | 8.7（项目已配置 Gradle Wrapper，会自动下载） |
| 编译 SDK | Android 14（API 34），`compileSdk = 34` |
| Build-Tools | 36.0.0（已在 `app/build.gradle` 里指定，避免重复下载） |
| 最低 SDK | Android 7.0（API 24），`minSdk = 24` |
| 目标 SDK | Android 14（API 34），`targetSdk = 34` |
| 开发语言 | Java |
| 数据库 | Room 2.6.1（SQLite 之上的一层封装） |
| 依赖库 | AppCompat、Material Components、ConstraintLayout、RecyclerView、Room |
| 网络要求 | **仅首次 Sync 需要联网下载依赖**，App 运行本身完全离线 |
| 运行设备 | 真机（Android 7.0 以上）或模拟器（API 24 以上） |

> 说明：第一次在 Android Studio 里打开项目时，需要联网下载 Gradle 和第三方依赖库，
> 下载完成后这些内容会缓存在本机，以后**断网也能正常编译运行**。

---

## 二、导入 Android Studio 步骤

1. **打开 Android Studio** → 在欢迎界面点击 **Open**（不是 New Project）。
2. 在弹出的文件选择框中，选中**本项目根目录**（即包含 `settings.gradle` 的那个文件夹，
   也就是 `beidanci` 文件夹），点击 **OK**。
3. 如果弹出 *“Trust Project?”* 之类的信任提示，选择 **Trust Project**。
4. 等待 Android Studio 自动开始 **Gradle Sync（同步）**：
   - 右下角会显示进度条，第一次同步会下载 Gradle 8.7、AGP 8.5.2 以及各个依赖库，需要几分钟；
   - 同步完成后，底部状态栏会显示 `Gradle sync finished`。
5. **检查 SDK 路径**：如果同步时报错找不到 SDK，打开根目录下的 `local.properties`，
   把 `sdk.dir` 改成你自己电脑上的 SDK 路径，例如：

   ```properties
   sdk.dir=C\:\\Users\\你的用户名\\AppData\\Local\\Android\\Sdk
   ```

   也可以直接点击报错提示中的 *Install missing SDK / Set up SDK* 让 Android Studio 自动修复。

6. 同步成功后，左侧 Project 面板能看到 `app` 模块和所有 java / res 文件，说明导入完成。

---

## 三、运行 App 步骤

1. **准备设备**（二选一）：
   - **真机**：手机打开“开发者选项 → USB 调试”，用数据线连接电脑，在手机上允许调试授权；
   - **模拟器**：点击工具栏的 *Device Manager* → *Create Device*，选一个 **API 24 以上**的
     镜像（如 Pixel 5 / API 34），创建并启动模拟器。
2. 在 Android Studio 顶部工具栏的**设备下拉框**中，选中刚刚准备好的设备。
3. 点击绿色三角按钮 **Run ▶**（快捷键 `Shift + F10`）。
4. 等待编译安装完成，手机上会自动打开 App。第一次启动时程序会把内置的 15 个单词写入
   本地数据库，卡片上随即显示第一个单词。
5. 如果想用命令行打包安装，也可以在项目根目录执行：

   ```bash
   # Windows
   gradlew.bat assembleDebug
   # macOS / Linux
   ./gradlew assembleDebug
   ```

   生成的安装包路径：`app/build/outputs/apk/debug/app-debug.apk`

---

## 四、功能清单

| 功能 | 说明 | 对应文件 |
| --- | --- | --- |
| 首页背诵卡片 | 卡片显示英文单词、音标、中文释义，顶部显示进度（如 3 / 15） | `fragment_recite.xml`、`ReciteFragment.java` |
| 下一个 | 点击后切换到下一个单词，背到最后一个自动回到第一个（循环背诵） | `ReciteFragment.showNextWord()` |
| 加入生词本 | 把当前单词写入生词本（置 `inVocabBook = true`），已加入会提示 | `ReciteFragment.addCurrentWordToVocabBook()` |
| 生词本列表 | 展示所有已标记的生词，显示单词 / 音标 / 释义 | `fragment_vocab_book.xml`、`VocabBookFragment.java` |
| 移除生词 | 点击条目右侧垃圾桶图标，弹确认框后移出生词本 | `VocabBookAdapter`、`VocabBookFragment.removeWord()` |
| 底部 Tab 导航 | 两个 Tab：背诵页 / 生词本页，切换时保留页面状态 | `activity_main.xml`、`MainActivity.java` |
| 本地存储 | Room 数据库保存单词与生词标记，不联网、无后端 | `Word.java`、`WordDao.java`、`AppDatabase.java` |
| 预设词库 | 内置 15 个四六级单词作为测试数据，首次启动写入数据库 | `PresetWords.java` |
| **多词书** | 内置 CET-4 200 词 CSV，支持切换词书、导入手机上的 CSV 词书 | `assets/wordbooks/cet4_200.csv`、`CsvParser.java`、`ReciteFragment` |
| **单词列表跳转** | 顶部「单词列表」可打开全部单词的列表，支持搜索，点击任意单词直接跳转 | `dialog_word_list.xml`、`WordListAdapter.java` |
| **进度记忆** | 自动记录上次的词书和背到第几个，重开 App 继续上次的位置 | `ReciteFragment.saveProgress()` |
| 空状态 | 生词本为空时显示提示文字 | `fragment_vocab_book.xml` 中的 `tv_empty` |

---

## 五、项目结构

```
beidanci/
├── settings.gradle               # 声明工程名与包含的模块
├── build.gradle                  # 顶层构建脚本（声明 AGP 版本）
├── gradle.properties             # Gradle 参数（AndroidX、内存、编码）
├── local.properties              # 本机 Android SDK 路径
├── readme.md                     # 本文档
├── gradlew / gradlew.bat         # 命令行编译脚本
└── app/
    ├── build.gradle              # app 模块配置（包名、SDK 版本、依赖库）
    ├── proguard-rules.pro        # 混淆规则（本项目未开启混淆）
    └── src/main/
        ├── AndroidManifest.xml   # 清单文件（声明 Activity、图标、主题）
        ├── java/com/wordstudy/
        │   ├── MainActivity.java                 # 主界面 + 底部 Tab 切换
        │   ├── data/
        │   │   ├── Word.java                     # 单词实体类（数据库表）
        │   │   ├── WordDao.java                  # 数据库操作接口（增删改查）
        │   │   ├── AppDatabase.java              # 数据库单例 + 后台线程池
        │   │   └── PresetWords.java              # 内置的 15 个预设单词
        │   ├── ui/
        │   │   ├── ReciteFragment.java           # 背诵页（核心逻辑）
        │   │   └── VocabBookFragment.java        # 生词本页
        │   ├── util/
        │   │   └── CsvParser.java                # CSV 词书解析器
        │   └── adapter/
        │       ├── VocabBookAdapter.java         # 生词本列表适配器
        │       └── WordListAdapter.java          # 单词列表（搜索 + 跳转）适配器
        ├── assets/wordbooks/
        │   └── cet4_200.csv                      # 内置 CET-4 200 词词书
        └── res/
            ├── layout/           # 界面布局（主界面、两个 Fragment、单词列表弹窗、列表条目）
            ├── menu/             # 底部 Tab 菜单
            ├── drawable/         # 矢量图标（Tab 图标、垃圾桶图标、应用图标）
            ├── mipmap-*/         # 应用启动图标（PNG）
            ├── color/            # Tab 选中/未选中的颜色状态表
            └── values/           # 颜色、字符串、主题
```

---

## 六、数据库设计

**表名：`word_table`，数据库名：`word_database`**

| 字段名 | 类型 | 说明 | 约束 |
| --- | --- | --- | --- |
| id | int | 主键，自增 | `@PrimaryKey(autoGenerate = true)` |
| word | String | 英文单词 | 例如 `abandon` |
| phonetic | String | 音标 | 例如 `/əˈbændən/` |
| meaning | String | 中文释义 | 例如 `vt. 放弃，抛弃；遗弃` |
| inVocabBook | boolean | 是否加入生词本 | SQLite 中存为 1 / 0 |

主要 SQL 操作（写在 `WordDao.java` 的注解里）：

- 查询全部单词：`SELECT * FROM word_table ORDER BY id ASC`
- 查询生词本单词：`SELECT * FROM word_table WHERE inVocabBook = 1 ORDER BY id ASC`（返回 LiveData）
- 加入/移出生词本：`UPDATE word_table SET inVocabBook = :inBook WHERE id = :id`

**设计说明（可直接写进报告）**：

1. "移出生词本"采用**修改标记位**而不是删除数据行的方式，
   这样单词仍然保留在词库中，背诵时还能再次出现，避免误删。
2. 生词本列表返回类型使用 `LiveData`，数据一旦变化界面自动刷新，
   不需要手动调用刷新代码，这是 Google 推荐的架构写法。
3. 数据库读写全部通过 `ExecutorService` 放到子线程执行，避免阻塞 UI 线程导致界面卡顿。

---

## 七、关键实现说明

1. **第一次启动写入预设单词**：在 `ReciteFragment.loadWordsFromDatabase()` 里，
   先 `countAllWords()` 判断数据库是否为空，为空就把 `PresetWords.getPresetWords()` 一次性插入。
   所以想换词库，只要改 `PresetWords.java` 即可。

2. **背诵顺序**：按单词 id 升序。切换单词用的是取余运算
   `currentIndex = (currentIndex + 1) % wordList.size()`，实现“背完自动回到第一个”的循环效果。

3. **判断是否已在生词本**：点击“加入生词本”时会实时查询数据库
   （`isInVocabBook()`），而不是只依赖内存中的标记，保证状态永远准确。

4. **底部 Tab 切换**：`MainActivity` 用 `add + show/hide` 而不是 `replace`，
   这样从生词本切回背诵页时，仍然停留在原来那个单词上，体验更好。

5. **卡片样式**：`MaterialCardView` 设置 `cardCornerRadius = 20dp`、`cardElevation = 0dp`、
   `strokeColor` 浅灰描边，形成“白底 + 圆角 + 细边框”的简约卡片风格，没有多余特效。

---

## 八、常见问题

**Q1：Gradle Sync 失败，提示下载超时 / 连接不上？**
A：首次同步需要联网下载 Gradle 和依赖库。
本项目的 `gradle/wrapper/gradle-wrapper.properties` 中，`distributionUrl` 默认使用国内镜像
`https://repo.huaweicloud.com/gradle/gradle-8.7-bin.zip`，下载更稳定；
如果想换回官方地址，改成 `https://services.gradle.org/distributions/gradle-8.7-bin.zip` 即可。
如果依赖库（AppCompat / Material / Room 等）下载失败，可在 `settings.gradle` 的
`repositories` 中把 `google()`、`mavenCentral()` 换成国内镜像，例如：

```groovy
maven { url 'https://maven.aliyun.com/repository/google' }
maven { url 'https://maven.aliyun.com/repository/public' }
```

**Q2：提示 “SDK location not found”？**
A：修改根目录 `local.properties` 中的 `sdk.dir` 为自己电脑的 SDK 路径（参考第二节第 5 步）。

**Q3：提示 AGP 版本过低/过高需要升级？**
A：按 Android Studio 弹窗提示点击 *Upgrade* 自动升级插件版本即可，
项目代码本身不需要修改。

**Q4：为什么生词本里移除后，背诵页还能背到这个单词？**
A：这是**有意设计**的。“移除”只是取消收藏标记，并没有删除单词数据，
这样词库不会被越用越少（详见第六节设计说明 1）。

**Q5：想增加常用单词 / 换成自己的词库？**
A：编辑 `app/src/main/java/com/wordstudy/data/PresetWords.java`，
按 `add(list, "单词", "音标", "中文释义")` 的格式增加即可。
如果是已经运行过的 App，需要先卸载 App 再重新运行（或清空应用数据），
因为预设单词只在数据库为空时写入一次。
