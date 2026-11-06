<template>
  <section class="adm-card">
    <div class="adm-h2">
      <span class="ico">✧</span>操作日志
      <span class="tip">记录后台对首页数据的每一次增删改（最近 {{ items.length }} 条）</span>
      <span style="flex:1"></span>
      <button class="adm-btn sm" :disabled="loading" @click="load">{{ loading ? '加载中…' : '刷新' }}</button>
    </div>

    <div class="adm-table-wrap" style="margin-top:14px">
      <table class="adm-table">
        <thead>
          <tr>
            <th style="width:52px">ID</th>
            <th style="width:170px">时间</th>
            <th style="width:150px">管理员</th>
            <th style="width:90px">动作</th>
            <th style="width:100px">模块</th>
            <th style="width:80px">目标</th>
            <th>摘要</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!items.length"><td colspan="7" class="adm-empty">暂无操作记录</td></tr>
          <tr v-for="l in items" :key="l.id">
            <td class="num">{{ l.id }}</td>
            <td class="muted">{{ fmt(l.created_at) }}</td>
            <td class="strong">{{ l.admin_name || '—' }}</td>
            <td><span class="adm-tag" :class="cls(l.action)">{{ l.action }}</span></td>
            <td class="muted">{{ l.target_type || '—' }}</td>
            <td class="num">{{ l.target_id || '—' }}</td>
            <td class="muted">{{ l.detail || '—' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { fetchAdminLogs } from '../../api'

const props = defineProps({ cred: { type: Object, required: true } })
const emit = defineEmits(['toast'])

const items = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  const d = await fetchAdminLogs(props.cred)
  loading.value = false
  if (d.ok) items.value = Array.isArray(d.items) ? d.items : []
  else emit('toast', d.msg || '日志加载失败', true)
}

function fmt(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

function cls(a) {
  if (a === '删除') return 'red'
  if (a === '新增' || a === '上架') return 'on'
  return 'gold'
}

onMounted(load)
</script>
