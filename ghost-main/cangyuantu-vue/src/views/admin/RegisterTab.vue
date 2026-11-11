<template>
  <section class="adm-card">
    <div class="adm-h2">
      <span class="ico">✦</span>注册记录
      <span class="tip">每次注册自动落库 · 用户被删除后记录仍保留（最近 {{ items.length }} 条）</span>
      <span style="flex:1"></span>
      <button class="adm-btn sm" :disabled="loading" @click="load">{{ loading ? '加载中…' : '刷新' }}</button>
    </div>

    <div class="adm-toolbar">
      <div style="display:flex;gap:8px;flex-wrap:wrap">
        <span class="adm-tag gold">累计注册 {{ items.length }} 条</span>
        <span class="adm-tag on">前端注册 {{ forgeCount }} 条</span>
        <span class="adm-tag">历史回填 {{ migrateCount }} 条</span>
      </div>
      <div style="display:flex;gap:10px;align-items:center">
        <input v-model="kw" class="adm-input" style="width:220px" placeholder="搜索账号 / 地域 / IP…" />
      </div>
    </div>

    <div class="adm-table-wrap">
      <table class="adm-table">
        <thead>
          <tr>
            <th style="width:52px">ID</th>
            <th style="width:165px">注册时间</th>
            <th>账号</th>
            <th style="width:150px">密码</th>
            <th style="width:110px">地域</th>
            <th style="width:130px">神尊名号</th>
            <th style="width:100px">权限</th>
            <th style="width:125px">来源 IP</th>
            <th style="width:80px">根基 ID</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading && !items.length"><td colspan="9" class="adm-empty">正在读取注册记录…</td></tr>
          <tr v-else-if="!filtered.length"><td colspan="9" class="adm-empty">暂无注册记录</td></tr>
          <tr v-for="r in filtered" :key="r.id">
            <td class="num">{{ r.id }}</td>
            <td class="muted">{{ fmt(r.created_at) }}</td>
            <td class="strong">{{ r.account }}</td>
            <td class="muted">{{ r.password }}</td>
            <td class="muted">{{ r.region || '—' }}</td>
            <td class="muted">{{ r.myth_name || '—' }}</td>
            <td>
              <span class="adm-tag" :class="Number(r.permission) === 0 ? 'gold' : 'on'">
                {{ Number(r.permission) === 0 ? '0 · 管理员' : '1 · 默认用户' }}
              </span>
            </td>
            <td class="muted">{{ r.register_ip || '—' }}</td>
            <td class="num">{{ r.avatar_id || '—' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { fetchRegisterRecords } from '../../api'

const props = defineProps({ cred: { type: Object, required: true } })
const emit = defineEmits(['toast'])

const items = ref([])
const loading = ref(false)
const kw = ref('')

const filtered = computed(() => {
  const k = kw.value.trim().toLowerCase()
  if (!k) return items.value
  return items.value.filter(r =>
    (r.account || '').toLowerCase().includes(k) ||
    (r.region || '').toLowerCase().includes(k) ||
    (r.register_ip || '').toLowerCase().includes(k) ||
    (r.myth_name || '').toLowerCase().includes(k)
  )
})
const forgeCount = computed(() => items.value.filter(r => r.source === 'forge').length)
const migrateCount = computed(() => items.value.filter(r => r.source === 'migrate').length)

async function load() {
  loading.value = true
  const d = await fetchRegisterRecords(props.cred)
  loading.value = false
  if (d.ok) items.value = Array.isArray(d.records) ? d.records : []
  else emit('toast', d.msg || '注册记录加载失败', true)
}

function fmt(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

onMounted(load)
</script>
