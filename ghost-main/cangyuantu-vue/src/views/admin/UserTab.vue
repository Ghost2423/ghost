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
            <th style="width:230px">操作</th>
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
              <button class="adm-btn sm" :disabled="busy" @click="openEdit(u)">编辑</button>
              <template v-if="u.id !== adminId">
                <button class="adm-btn sm" :disabled="busy" @click="togglePerm(u)">
                  {{ Number(u.permission) === 0 ? '取消管理员' : '设为管理员' }}
                </button>
                <button class="adm-btn sm danger" :disabled="busy" @click="pending = u">删除</button>
              </template>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- ===== 编辑用户资料（保存即写库，并同步所有关联表） ===== -->
    <transition name="adm-fade">
      <div class="adm-modal" v-if="editing" @click.self="editing = null">
        <div class="adm-modal-card" style="max-width:580px">
          <h3>编辑用户资料 · {{ editing.deity_name }}</h3>
          <p style="font-size:12px;color:#9fb0c8;line-height:1.7;margin:6px 0 14px">
            保存会写入 <b>avatar_foundation</b>，并同步 <b>注册记录 / 论道帖与回帖作者 / 共鸣记录 / 操作日志</b>
            中保存的相应内容；改名后论坛里的旧作者名会一起更新。
          </p>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px 16px">
            <label class="ef"><span>账号（神尊相）</span>
              <input class="adm-input" v-model="f.deity_name" /></label>
            <label class="ef"><span>密码（召唤语）</span>
              <input class="adm-input" v-model="f.spell" /></label>
            <label class="ef"><span>地域</span>
              <input class="adm-input" v-model="f.region" /></label>
            <label class="ef"><span>神尊名号</span>
              <input class="adm-input" v-model="f.myth_name" /></label>
            <label class="ef"><span>权限</span>
              <select class="adm-input" v-model.number="f.permission" :disabled="isSelf">
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
          <div v-if="isSelf" style="font-size:11.5px;color:#e9c46a;margin-top:10px">
            你正在编辑自己：改账号或密码后会立即换用新凭证；权限不可自行修改，避免锁死后台。
          </div>
          <div class="adm-modal-ops">
            <button class="adm-btn" @click="editing = null">取消</button>
            <button class="adm-btn primary solid" :disabled="busy" @click="saveEdit">
              {{ busy ? '保存中…' : '保存到数据库' }}</button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="adm-fade">
      <div class="adm-modal" v-if="pending" @click.self="pending = null">
        <div class="adm-modal-card" style="max-width:420px">
          <h3>确认删除用户</h3>
          <p>即将删除用户「{{ pending.deity_name }}」，其修炼状态、已购功法、切磋与共鸣记录会一并清理，论题与回帖保留但作者标记为「已注销神尊」；注册留痕仍保留。该操作不可恢复。</p>
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
import { fetchAdminUsers, updatePermission, deleteAdminUser, updateAdminUser } from '../../api'

const props = defineProps({
  cred: { type: Object, required: true },
  adminId: { type: [Number, String], default: 0 }
})
const emit = defineEmits(['toast', 'self-updated'])

const users = ref([])
const loading = ref(false)
const busy = ref(false)
const kw = ref('')
const pending = ref(null)
const editing = ref(null)
const f = ref({})
const isSelf = computed(() => Number(editing.value && editing.value.id) === Number(props.adminId))

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

function openEdit(u) {
  editing.value = u
  f.value = {
    deity_name: u.deity_name || '',
    spell: u.spell || '',
    region: u.region || '',
    myth_name: u.myth_name || '',
    myth_story: u.myth_story || '',
    permission: Number(u.permission) === 0 ? 0 : 1,
    agreed: Number(u.agreed) === 0 ? 0 : 1
  }
}

async function saveEdit() {
  const self = isSelf.value
  const item = { ...f.value }
  // 改自己时不动权限，避免把自己降成默认用户导致后台被锁
  if (self) delete item.permission
  busy.value = true
  const d = await updateAdminUser(props.cred, editing.value.id, item)
  busy.value = false
  if (!d.ok) return emit('toast', d.msg || '保存失败', true)
  emit('toast', '已保存，数据库关联内容同步完成')
  editing.value = null
  await load()
  if (self && d.user) emit('self-updated', d.user)
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
    emit('toast', `已删除用户「${u.deity_name}」，关联数据清理 ${d.cleaned || 0} 条`)
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

<style scoped>
.ef { display: flex; flex-direction: column; gap: 5px; font-size: 12px; color: #9fb0c8; letter-spacing: .5px; }
.ef .adm-input { width: 100%; box-sizing: border-box; }
</style>
