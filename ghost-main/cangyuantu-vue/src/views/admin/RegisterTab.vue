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
            <th style="width:80px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading && !items.length"><td colspan="10" class="adm-empty">正在读取注册记录…</td></tr>
          <tr v-else-if="!filtered.length"><td colspan="10" class="adm-empty">暂无注册记录</td></tr>
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
            <td class="ops">
              <button class="adm-btn sm" @click="openEdit(r)">编辑</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- ===== 编辑注册记录（同时回写用户主表与论坛作者） ===== -->
    <transition name="adm-fade">
      <div class="adm-modal" v-if="editing" @click.self="editing = null">
        <div class="adm-modal-card" style="max-width:580px">
          <h3>编辑注册记录 · #{{ editing.id }}</h3>
          <p style="font-size:12px;color:#9fb0c8;line-height:1.7;margin:6px 0 14px">
            保存写入 <b>user_register_record</b>；若根基 ID 仍指向现存用户，会同时把改动写回
            <b>avatar_foundation</b>，并同步论道帖 / 回帖 / 共鸣记录里的作者账号。
          </p>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px 16px">
            <label class="ef"><span>注册账号</span>
              <input class="adm-input" v-model="f.account" /></label>
            <label class="ef"><span>注册密码</span>
              <input class="adm-input" v-model="f.password" /></label>
            <label class="ef"><span>地域</span>
              <input class="adm-input" v-model="f.region" /></label>
            <label class="ef"><span>神尊名号</span>
              <input class="adm-input" v-model="f.myth_name" /></label>
            <label class="ef"><span>权限</span>
              <select class="adm-input" v-model.number="f.permission">
                <option :value="1">1 · 默认用户</option>
                <option :value="0">0 · 管理员</option>
              </select></label>
            <label class="ef"><span>心魔誓约</span>
              <select class="adm-input" v-model.number="f.agreed">
                <option :value="1">已立誓</option>
                <option :value="0">未立誓</option>
              </select></label>
            <label class="ef" style="grid-column:1/-1"><span>神话原型简介</span>
              <textarea class="adm-input" v-model="f.myth_story" rows="3" style="resize:vertical"></textarea></label>
          </div>
          <div class="adm-modal-ops">
            <button class="adm-btn" @click="editing = null">取消</button>
            <button class="adm-btn primary solid" :disabled="busy" @click="saveEdit">
              {{ busy ? '保存中…' : '保存到数据库' }}</button>
          </div>
        </div>
      </div>
    </transition>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { fetchRegisterRecords, updateRegisterRecord } from '../../api'

const props = defineProps({ cred: { type: Object, required: true } })
const emit = defineEmits(['toast'])

const items = ref([])
const loading = ref(false)
const busy = ref(false)
const kw = ref('')
const editing = ref(null)
const f = ref({})

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

function openEdit(r) {
  editing.value = r
  f.value = {
    account: r.account || '',
    password: r.password || '',
    region: r.region || '',
    myth_name: r.myth_name || '',
    myth_story: r.myth_story || '',
    permission: Number(r.permission) === 0 ? 0 : 1,
    agreed: Number(r.agreed) === 0 ? 0 : 1
  }
}

async function saveEdit() {
  busy.value = true
  const d = await updateRegisterRecord(props.cred, editing.value.id, { ...f.value })
  busy.value = false
  if (!d.ok) return emit('toast', d.msg || '保存失败', true)
  emit('toast', '已保存，用户主表与论坛作者同步完成')
  editing.value = null
  await load()
}

function fmt(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

onMounted(load)
</script>

<style scoped>
.ef { display: flex; flex-direction: column; gap: 5px; font-size: 12px; color: #9fb0c8; letter-spacing: .5px; }
.ef .adm-input { width: 100%; box-sizing: border-box; }
</style>
