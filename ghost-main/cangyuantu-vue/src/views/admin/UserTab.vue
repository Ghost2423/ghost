<template>
  <section class="adm-card">
    <div class="adm-h2">
      <span class="ico">☰</span>用户管理
      <span class="tip">权限：1 = 默认用户（注册默认） · 0 = 管理员（可登录后台）</span>
    </div>

    <div class="adm-toolbar">
      <div style="display:flex;gap:8px;flex-wrap:wrap">
        <span class="adm-tag gold">管理员 {{ adminCount }}</span>
        <span class="adm-tag on">默认用户 {{ userCount }}</span>
        <span class="adm-tag">共 {{ users.length }} 人</span>
      </div>
      <div style="display:flex;gap:10px;align-items:center">
        <input v-model="kw" class="adm-input" style="width:220px" placeholder="搜索账号 / 地域 / 原型…" />
        <button class="adm-btn" :disabled="loading" @click="load">{{ loading ? '加载中…' : '刷新' }}</button>
      </div>
    </div>

    <div class="adm-table-wrap">
      <table class="adm-table">
        <thead>
          <tr>
            <th style="width:52px">ID</th>
            <th>账号（神尊相）</th>
            <th style="width:150px">密码（召唤语）</th>
            <th style="width:110px">地域</th>
            <th style="width:120px">权限</th>
            <th style="width:170px">注册时间</th>
            <th style="width:170px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading && !users.length"><td colspan="7" class="adm-empty">正在读取用户数据…</td></tr>
          <tr v-else-if="!filtered.length"><td colspan="7" class="adm-empty">暂无匹配用户</td></tr>
          <tr v-for="u in filtered" :key="u.id">
            <td class="num">{{ u.id }}</td>
            <td class="strong">
              <span class="adm-dot" :class="Number(u.permission) === 0 ? 'gold' : 'jade'"
                    :style="{ background: Number(u.permission) === 0 ? '#e9c46a' : '#2dd4bf',
                              color: Number(u.permission) === 0 ? '#e9c46a' : '#2dd4bf' }"></span>
              {{ u.deity_name }}
              <span class="adm-tag gold" v-if="u.id === adminId" style="margin-left:8px">当前登录</span>
            </td>
            <td class="muted">{{ u.spell }}</td>
            <td class="muted">{{ u.region || '—' }}</td>
            <td>
              <span class="adm-tag" :class="Number(u.permission) === 0 ? 'gold' : 'on'">
                {{ Number(u.permission) === 0 ? '0 · 管理员' : '1 · 默认用户' }}
              </span>
            </td>
            <td class="muted">{{ fmt(u.created_at) }}</td>
            <td class="ops">
              <template v-if="u.id !== adminId">
                <button class="adm-btn sm" :disabled="busy" @click="togglePerm(u)">
                  {{ Number(u.permission) === 0 ? '取消管理员' : '设为管理员' }}
                </button>
                <button class="adm-btn sm danger" :disabled="busy" @click="pending = u">删除</button>
              </template>
              <span v-else class="muted">—</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <transition name="adm-fade">
      <div class="adm-modal" v-if="pending" @click.self="pending = null">
        <div class="adm-modal-card" style="max-width:420px">
          <h3>确认删除用户</h3>
          <p>即将删除用户「{{ pending.deity_name }}」，其神尊根基与修炼数据将一并消失，该操作不可恢复。</p>
          <div class="adm-modal-ops">
            <button class="adm-btn" @click="pending = null">取消</button>
            <button class="adm-btn danger solid" :disabled="busy" @click="doDelete">确认删除</button>
          </div>
        </div>
      </div>
    </transition>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { fetchAdminUsers, updatePermission, deleteAdminUser } from '../../api'

const props = defineProps({
  cred: { type: Object, required: true },
  adminId: { type: [Number, String], default: 0 }
})
const emit = defineEmits(['toast'])

const users = ref([])
const loading = ref(false)
const busy = ref(false)
const kw = ref('')
const pending = ref(null)

const filtered = computed(() => {
  const k = kw.value.trim().toLowerCase()
  if (!k) return users.value
  return users.value.filter(u =>
    (u.deity_name || '').toLowerCase().includes(k) ||
    (u.region || '').toLowerCase().includes(k) ||
    (u.myth_name || '').toLowerCase().includes(k)
  )
})
const adminCount = computed(() => users.value.filter(u => Number(u.permission) === 0).length)
const userCount = computed(() => users.value.filter(u => Number(u.permission) !== 0).length)

async function load() {
  loading.value = true
  const d = await fetchAdminUsers(props.cred)
  loading.value = false
  if (d.ok && Array.isArray(d.users)) {
    users.value = d.users
  } else {
    emit('toast', d.msg || '用户列表获取失败', true)
    if (d.msg && d.msg.includes('无权')) emit('toast', '登录状态已失效，请重新登录', true)
  }
}

async function togglePerm(u) {
  const target = Number(u.permission) === 0 ? 1 : 0
  busy.value = true
  const d = await updatePermission(props.cred, u.id, target)
  busy.value = false
  if (d.ok) {
    u.permission = target
    emit('toast', target === 0 ? `已将「${u.deity_name}」提升为管理员` : `已将「${u.deity_name}」恢复为默认用户`)
  } else {
    emit('toast', d.msg || '权限变更失败', true)
  }
}

async function doDelete() {
  const u = pending.value
  busy.value = true
  const d = await deleteAdminUser(props.cred, u.id)
  busy.value = false
  pending.value = null
  if (d.ok) {
    users.value = users.value.filter(x => x.id !== u.id)
    emit('toast', `已删除用户「${u.deity_name}」`)
  } else {
    emit('toast', d.msg || '删除失败', true)
  }
}

function fmt(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

onMounted(load)
</script>
