# AIPlanner 前端设计文档

## 1. 项目概述

**项目名称**：AIPlanner 前端

**目标**：为 AIPlanner 后端提供完整的 Web 界面，覆盖用户从注册登录、创建学习目标、查看 AI 生成的学习路线、管理任务、接收实时提醒到查看掌握度分析的全流程。

**设计原则**：组件化、状态驱动、实时响应、渐进增强。

---

## 2. 技术栈

| 层 | 选型 | 版本 | 说明 |
|---|------|------|------|
| 框架 | Vue 3 | 3.5+ | Composition API + `<script setup>` |
| UI 库 | Element Plus | 2.9+ | 企业级 Vue3 组件库 |
| 路由 | Vue Router | 4.x | 嵌套路由 + 导航守卫 |
| 状态管理 | Pinia | 2.x | Vue3 官方推荐 |
| HTTP | Axios | 1.x | 拦截器统一 Token 注入 |
| WebSocket | 原生 API | — | 轻量，无需额外依赖 |
| 图表 | ECharts | 5.x | 掌握度仪表盘、学习报告 |
| 构建 | Vite | 6.x | 快速开发与构建 |
| CSS | SCSS + Element Plus 变量 | — | Element Plus 主题定制 |

---

## 3. 项目结构

```
ai-planner-frontend/
├── public/
├── src/
│   ├── api/                        # API 请求封装
│   │   ├── request.js              # Axios 实例 + 拦截器
│   │   ├── auth.js                 # 登录/注册/用户信息
│   │   ├── goals.js                # 目标 CRUD + AI 结果
│   │   ├── phases.js               # 阶段查询
│   │   ├── tasks.js                # 任务 CRUD + 状态更新
│   │   ├── reminders.js            # 提醒查询 + 标记已发送
│   │   └── mastery.js              # 掌握度评估
│   ├── stores/                     # Pinia 状态管理
│   │   ├── userStore.js            # 用户 + Token
│   │   ├── goalStore.js            # 目标 + AI 进度
│   │   ├── taskStore.js            # 任务列表
│   │   └── websocketStore.js       # WebSocket 连接 + 消息
│   ├── router/
│   │   └── index.js                # 路由配置 + 导航守卫
│   ├── composables/                # 组合式函数
│   │   ├── useWebSocket.js         # WebSocket 连接管理
│   │   └── useAIProgress.js        # AI 进度状态
│   ├── components/                 # 公共组件
│   │   ├── MainLayout.vue          # 主布局 (侧栏+顶栏+内容)
│   │   ├── SideMenu.vue            # 侧栏导航
│   │   ├── TopHeader.vue           # 顶栏 (用户头像、退出)
│   │   ├── StatsCard.vue           # 统计卡片
│   │   ├── TaskItem.vue            # 任务行
│   │   ├── GoalCard.vue            # 目标卡片
│   │   ├── AIProgressPanel.vue     # AI 分析进度面板
│   │   ├── PhaseTimeline.vue       # 阶段时间线 (Element Plus Timeline)
│   │   └── MasteryGauge.vue        # 掌握度仪表盘 (ECharts)
│   ├── views/                      # 页面组件
│   │   ├── LoginPage.vue
│   │   ├── RegisterPage.vue
│   │   ├── DashboardPage.vue       # 仪表盘首页
│   │   ├── GoalListPage.vue        # 目标列表
│   │   ├── GoalCreatePage.vue      # 创建目标 + AI 进度
│   │   ├── GoalDetailPage.vue      # 目标详情 / 学习路线
│   │   ├── TaskPage.vue            # 任务列表
│   │   ├── ReminderPage.vue        # 提醒记录
│   │   ├── MasteryPage.vue         # 掌握度分析
│   │   ├── ReportPage.vue          # 学习报告
│   │   └── ProfilePage.vue         # 个人信息
│   ├── styles/
│   │   ├── variables.scss          # Element Plus 主题变量
│   │   └── global.scss             # 全局样式
│   ├── App.vue
│   └── main.js
├── index.html
├── vite.config.js
├── package.json
└── .env.development
```

---

## 4. 路由设计

```javascript
const routes = [
  { path: '/login',              component: LoginPage,      meta: { guest: true } },
  { path: '/register',           component: RegisterPage,   meta: { guest: true } },
  {
    path: '/',
    component: MainLayout,
    meta: { requiresAuth: true },
    redirect: '/dashboard',
    children: [
      { path: '/dashboard',      component: DashboardPage },
      { path: '/goals',          component: GoalListPage },
      { path: '/goals/create',   component: GoalCreatePage },
      { path: '/goals/:id',      component: GoalDetailPage, props: true },
      { path: '/tasks',          component: TaskPage },
      { path: '/reminders',      component: ReminderPage },
      { path: '/mastery/:goalId',component: MasteryPage,    props: true },
      { path: '/reports',        component: ReportPage },
      { path: '/profile',        component: ProfilePage },
    ],
  },
  { path: '/:pathMatch(.*)*',    redirect: '/dashboard' },
];
```

### 导航守卫

```javascript
router.beforeEach((to, from, next) => {
  const userStore = useUserStore();
  if (to.meta.requiresAuth && !userStore.token) {
    next('/login');
  } else if (to.meta.guest && userStore.token) {
    next('/dashboard');
  } else {
    next();
  }
});
```

---

## 5. 状态管理 (Pinia)

### userStore
```javascript
// state: { token, userInfo }
// actions: login(), register(), fetchUserInfo(), logout()
// persist: localStorage
```

### goalStore
```javascript
// state: { goals, currentGoal, phases, aiProgress }
// actions: fetchGoals(), createGoal(), fetchGoalDetail(), fetchPhases()
// aiProgress: { step, content } — WebSocket 实时更新
```

### taskStore
```javascript
// state: { tasks, currentTask }
// actions: fetchTasks(), updateStatus(), updateProgress(), fetchOverdue()
```

### websocketStore
```javascript
// state: { connected, messages }
// actions: connect(), disconnect(), send()
// 自动连接：GoalCreatePage 打开时连接，离开时断开
```

---

## 6. HTTP 请求封装

```javascript
// api/request.js
const instance = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 30000,
});

// 请求拦截器 — 注入 Token
instance.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = token;
  return config;
});

// 响应拦截器 — 统一错误处理
instance.interceptors.response.use(
  res => res.data,          // 解包 Result<T>
  err => {
    if (err.response?.status === 401) {
      // Token 过期 → 跳转登录
      router.push('/login');
    }
    ElMessage.error(err.response?.data?.message || '请求失败');
    return Promise.reject(err);
  }
);
```

---

## 7. API 接口映射

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| auth | POST | /api/user/register | 注册 |
| auth | POST | /api/user/login | 登录 |
| auth | GET | /api/user/info | 用户信息 |
| goals | POST | /api/planner/goals | 创建目标 (触发 AI) |
| goals | GET | /api/planner/goals | 目标列表 (分页) |
| goals | GET | /api/planner/goals/:id | 目标详情 |
| goals | PUT | /api/planner/goals/:id | 更新目标 |
| goals | DELETE | /api/planner/goals/:id | 删除目标 |
| phases | GET | /api/planner/phases/:goalId | 阶段列表 (分页) |
| tasks | POST | /api/task | 创建任务 |
| tasks | GET | /api/task?phaseId=&status= | 任务列表 |
| tasks | PUT | /api/task/:id/status | 更新任务状态 |
| tasks | PUT | /api/task/:id/progress | 更新任务进度 |
| tasks | GET | /api/task/overdue | 超时任务 |
| reminders | POST | /api/reminders | 创建提醒 |
| reminders | GET | /api/reminders | 用户提醒列表 |
| reminders | PUT | /api/reminders/:id/send | 标记已发送 |
| mastery | POST | /api/ai/evaluate | 触发掌握度分析 |

---

## 8. WebSocket 集成

```javascript
// composables/useWebSocket.js
export function useWebSocket() {
  const ws = ref(null);
  const messages = ref([]);
  const connected = ref(false);

  function connect() {
    ws.value = new WebSocket('ws://localhost:8086/ws/ai-stream');
    ws.value.onopen = () => { connected.value = true; };
    ws.value.onmessage = (event) => {
      const msg = JSON.parse(event.data);
      messages.value.push(msg);
      // 路由到对应 goal 的 AIProgressPanel
    };
    ws.value.onclose = () => { connected.value = false; };
  }

  function disconnect() {
    ws.value?.close();
  }

  onUnmounted(disconnect);
  return { connected, messages, connect, disconnect };
}
```

**消息格式**（与后端约定）：
```json
{
  "goalId": 1,
  "type": "ANALYSIS_DONE | ROADMAP_DONE | TASKS_DONE | COMPLETED",
  "content": "Goal analysis complete"
}
```

---

## 9. 页面详细说明

### 9.1 登录页 (LoginPage)
- Element Plus 表单：用户名 + 密码
- 登录成功 → localStorage 存 Token → 跳转 Dashboard
- 底部链接跳转注册页

### 9.2 注册页 (RegisterPage)
- Element Plus 表单：用户名 + 密码 + 确认密码
- 注册成功 → 跳转登录页
- 底部链接跳转登录页

### 9.3 仪表盘 (DashboardPage)
- 统计卡片行 (StatsCard × 3)：进行中目标数、完成任务数、总进度百分比
- 今日任务列表 (TodayTasks)：从 Reminder API 获取
- 学习进度趋势图 (ECharts 折线图)：最近 7 天完成任务数

### 9.4 目标列表 (GoalListPage)
- GoalCard 网格：目标名称、状态标签、创建时间、进度条
- 每张卡片：点击进入详情、右键菜单 (编辑/删除)
- 顶部按钮："创建新目标" → GoalCreatePage
- 状态筛选：全部 / ANALYZING / ACTIVE / COMPLETED

### 9.5 创建目标 (GoalCreatePage)
- GoalForm：目标名称、目标描述、预估时长
- 提交后 → AIProgressPanel 实时展示：
  - Element Plus Steps 组件显示当前步骤
  - 日志区域滚动显示 AI 推送的进度文本
  - 全部完成后自动跳转到 GoalDetailPage

### 9.6 目标详情 (GoalDetailPage)
- 目标基础信息卡片
- PhaseTimeline：Element Plus Timeline 组件
  - 每个 Phase 可展开/折叠
  - 展开后显示 Task 列表
- TaskItem：复选框 + 任务名 + 进度条 + 状态标签
  - 勾选完成 → PUT /api/task/:id/status (COMPLETED)
  - 进度滑块 → PUT /api/task/:id/progress
- 底部按钮："评估掌握度" → MasteryPage

### 9.7 任务列表 (TaskPage)
- Element Plus Table：任务名、所属阶段、状态、优先级、截止日期
- 列筛选：按状态(PENDING / IN_PROGRESS / COMPLETED)、按优先级
- 行内操作：更新状态、更新进度

### 9.8 提醒记录 (ReminderPage)
- Element Plus Table：任务名、提醒时间、提醒类型、状态
- 筛选：全部 / PENDING / SENT

### 9.9 掌握度分析 (MasteryPage)
- MasteryGauge：ECharts 仪表盘 (0-100 分)
- 薄弱点标签列表 (Element Plus Tag)
- 学习建议卡片列表

### 9.10 学习报告 (ReportPage)
- 本周统计：完成的任务数、新增的目标数
- ECharts 柱状图：每日完成任务数
- ECharts 饼图：技能领域分布

### 9.11 个人信息 (ProfilePage)
- 头像、用户名展示
- 修改密码表单

---

## 10. 错误处理 & 边界状态

| 场景 | 处理方式 |
|------|---------|
| 网络断开 | Axios 拦截器统一 `ElMessage.error` |
| Token 过期 | 响应 401 → 跳转登录页 |
| AI 分析超时 | GoalCreatePage 显示 "分析超时，请重试" |
| WebSocket 断开 | 自动重连 (3 次，间隔 2s) |
| 空列表 | Element Plus Empty 组件 + 引导文案 |
| 加载中 | Element Plus Skeleton / v-loading |
| 表单校验失败 | Element Plus Form 内置校验提示 |

---

## 11. 实施阶段

| 阶段 | 内容 | 页面 |
|------|------|------|
| **P1: 基础框架** | Vite 项目初始化、路由、Axios、Pinia、MainLayout | Login, Register, Dashboard |
| **P2: 核心业务** | 目标 CRUD、AI 创建流程、WebSocket 进度 | GoalList, GoalCreate, GoalDetail |
| **P3: 任务管理** | 任务列表、状态流转、进度更新 | TaskPage |
| **P4: 提醒 & 掌握度** | 提醒记录、掌握度仪表盘 | Reminder, Mastery |
| **P5: 报告 & 个人中心** | 学习报告图表、个人信息 | Report, Profile |
| **P6: 打磨上线** | 全局 Loading、错误边界、响应式适配、构建优化 | 全局 |

---

## 12. 非功能需求

- 首屏加载 < 2s (Vite 分包 + 懒加载路由)
- WebSocket 断线自动重连 (最多 3 次)
- Token 持久化到 localStorage
- 支持 Chrome/Firefox/Edge 最新 2 个版本
- 响应式适配 (桌面为主，平板可用)
