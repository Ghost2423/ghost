<template>
  <div>
    <!-- 统计卡 -->
    <div class="adm-stats">
      <div class="adm-stat">
        <div class="n">{{ ov.codex_total || 0 }}</div>
        <div class="l">图鉴条目总数</div>
      </div>
      <div class="adm-stat gold">
        <div class="n">{{ ov.codex_online || 0 }}</div>
        <div class="l">首页展示中（上架）</div>
      </div>
      <div class="adm-stat">
        <div class="n">{{ ov.users || 0 }}</div>
        <div class="l">注册用户</div>
      </div>
      <div class="adm-stat red">
        <div class="n">{{ ov.today_new || 0 }}</div>
        <div class="l">今日新增用户</div>
      </div>
    </div>

    <div class="adm-split" style="margin-top:18px">
      <!-- 分类收录 -->
      <section class="adm-card">
        <div class="adm-h2">
          <span class="ico">❖</span>首页四大界域收录
          <span class="tip">数据来自图鉴表 cy_codex（仅统计上架条目）</span>
        </div>
        <div class="adm-table-wrap" style="margin-top:14px">
          <table class="adm-table">
            <thead>
              <tr><th>界域</th><th>收录数</th><th>下架数</th><th>操作</th></tr>
            </thead>
            <tbody>
              <tr v-for="c in cats" :key="c">
                <td class="strong">{{ c }}</td>
                <td class="num">{{ (ov.codex_counts || {})[c] || 0 }}</td>
                <td class="num">{{ offlineOf(c) }}</td>
                <td class="ops">
                  <button class="adm-btn sm" @click="$emit('go', 'codex')">管理条目</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- 运行概览 -->
      <section class="adm-card">
        <div class="adm-h2"><span class="ico">◈</span>站点数据概览</div>
        <ul class="ov-list">
          <li><span>核心界域展示框</span><b>{{ ov.sections || 0 }}</b></li>
          <li><span>元界动态</span><b>{{ ov.news || 0 }}</b></li>
          <li><span>管理员账号</span><b>{{ ov.admins || 0 }}</b></li>
          <li><span>下架图鉴</span><b>{{ ov.codex_offline || 0 }}</b></li>
        </ul>
        <div class="adm-help" style="margin-top:14px">
          首页展示内容（展示框 / 卡片 / 动态 / 文案）均由本系统维护，修改后刷新首页即生效。
        </div>
      </section>
    </div>

    <!-- 最近操作 -->
    <section class="adm-card">
      <div class="adm-h2">
        <span class="ico">✧</span>最近操作
        <span class="tip">记录 {{ logs.length }} 条</span>
        <span style="flex:1"></span>
        <button class="adm-btn sm" :disabled="loading" @click="load">{{ loading ? '加载中…' : '刷新' }}</button>
      </div>
      <div class="adm-table-wrap" style="margin-top:14px">
        <table class="adm-table">
          <thead>
            <tr><th>时间</th><th>管理员</th><th>动作</th><th>模块</th><th>摘要</th></tr>
          </thead>
          <tbody>
            <tr v-if="!logs.length"><td colspan="5" class="adm-empty">暂无操作记录</td></tr>
            <tr v-for="l in logs" :key="l.id">
              <td class="muted">{{ fmt(l.created_at) }}</td>
              <td class="strong">{{ l.admin_name || '—' }}</td>
              <td><span class="adm-tag" :class="actClass(l.action)">{{ l.action }}</span></td>
              <td class="muted">{{ l.target_type || '—' }}</td>
              <td class="muted">{{ l.detail || '—' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { fetchAdminOverview, fetchAdminLogs } from '../../api'

const props = defineProps({ cred: { type: Object, required: true } })
const emit = defineEmits(['toast', 'go'])

const cats = ['神尊', '功法', '妖魔', '秘境']
const ov = ref({})
const logs = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  const d = await fetchAdminOverview(props.cred)
  if (d.ok) ov.value = d
  else emit('toast', d.msg || '概览加载失败', true)

  const l = await fetchAdminLogs(props.cred)
  if (l.ok && Array.isArray(l.items)) logs.value = l.items.slice(0, 10)
  loading.value = false
}

// 某分类下架数 = 该分类条目总数 - 上架数
function offlineOf(c) {
  const total = Number((ov.value.codex_counts_all || {})[c] || 0)
  const online = Number((ov.value.codex_counts || {})[c] || 0)
  return Math.max(0, total - online)
}

function fmt(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

function actClass(a) {
  if (a === '删除') return 'red'
  if (a === '新增' || a === '上架') return 'on'
  return 'gold'
}

onMounted(load)
</script>

<style scoped>
.ov-list { list-style: none; margin-top: 14px; }
.ov-list li {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 2px;
  border-bottom: 1px solid rgba(255, 255, 255, .06);
  font-size: 13px; letter-spacing: 1px;
}
.ov-list li:last-child { border-bottom: none; }
.ov-list li span { color: var(--muted, #8b9ab8); }
.ov-list li b { color: var(--jade, #2dd4bf); font-size: 16px; }
</style>
