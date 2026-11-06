<template>
  <section class="adm-card">
    <div class="adm-h2">
      <span class="ico">✉</span>元界动态
      <span class="tip">首页「元界动态」横向滚动条内容 · 共 {{ items.length }} 条</span>
    </div>

    <div class="adm-toolbar">
      <div class="adm-help">点击动态会以「跳转关键词」发起检索，建议填写图鉴中已有的名号。</div>
      <div style="display:flex;gap:10px">
        <button class="adm-btn" :disabled="loading" @click="load">{{ loading ? '加载中…' : '刷新' }}</button>
        <button class="adm-btn primary" @click="openCreate">＋ 新增动态</button>
      </div>
    </div>

    <div class="adm-table-wrap">
      <table class="adm-table">
        <thead>
          <tr>
            <th style="width:52px">ID</th>
            <th style="width:96px">标签</th>
            <th>文案</th>
            <th style="width:130px">跳转关键词</th>
            <th style="width:96px">主色</th>
            <th style="width:66px">排序</th>
            <th style="width:86px">状态</th>
            <th style="width:170px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading && !items.length"><td colspan="8" class="adm-empty">正在读取动态…</td></tr>
          <tr v-else-if="!items.length"><td colspan="8" class="adm-empty">暂无动态，点击「新增动态」开始录入</td></tr>
          <tr v-for="n in items" :key="n.id">
            <td class="num">{{ n.id }}</td>
            <td><span class="adm-tag" :style="{ color: n.color, borderColor: n.color + '88' }">{{ n.tag }}</span></td>
            <td>{{ n.content }}</td>
            <td class="muted">{{ n.keyword || '—' }}</td>
            <td class="num">{{ n.color }}</td>
            <td class="num">{{ n.sort_no || 0 }}</td>
            <td>
              <span class="adm-tag" :class="n.status === 1 ? 'on' : 'off'">
                {{ n.status === 1 ? '展示中' : '已隐藏' }}
              </span>
            </td>
            <td class="ops">
              <button class="adm-btn sm" @click="openEdit(n)">编辑</button>
              <button class="adm-btn sm gold" @click="toggle(n)">{{ n.status === 1 ? '隐藏' : '展示' }}</button>
              <button class="adm-btn sm danger" @click="pending = n">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <transition name="adm-fade">
      <div class="adm-modal" v-if="form" @click.self="form = null">
        <div class="adm-modal-card" style="max-width:560px">
          <button class="adm-modal-x" @click="form = null">✕</button>
          <h3>{{ form.id ? '编辑元界动态' : '新增元界动态' }}</h3>
          <p>动态将出现在首页「元界动态」滚动条中。</p>

          <div class="adm-row">
            <div class="adm-field">
              <label>标签 <b>*</b></label>
              <select v-model="form.tag" class="adm-select">
                <option v-for="t in tags" :key="t" :value="t">{{ t }}</option>
              </select>
            </div>
            <div class="adm-field">
              <label>主色</label>
              <div style="display:flex;gap:10px;align-items:center">
                <input v-model="form.color" class="adm-input" placeholder="#e9c46a" maxlength="16" />
                <input type="color" v-model="form.color" class="color-pick" />
              </div>
            </div>
          </div>

          <div class="adm-field">
            <label>动态文案 <b>*</b></label>
            <input v-model="form.content" class="adm-input" maxlength="255"
                   placeholder="如：妖潮预警 · 东海之滨妖气骤盛" />
          </div>

          <div class="adm-row">
            <div class="adm-field">
              <label>跳转检索关键词</label>
              <input v-model="form.keyword" class="adm-input" maxlength="64" placeholder="如：噬魂魔" />
            </div>
            <div class="adm-field">
              <label>排序（越小越前）</label>
              <input v-model.number="form.sort_no" type="number" class="adm-input" />
            </div>
          </div>

          <div class="adm-field">
            <label>状态</label>
            <select v-model.number="form.status" class="adm-select">
              <option :value="1">展示</option>
              <option :value="0">隐藏</option>
            </select>
          </div>

          <div class="adm-modal-ops">
            <button class="adm-btn" @click="form = null">取消</button>
            <button class="adm-btn primary" :disabled="busy" @click="save">{{ busy ? '保存中…' : '保存' }}</button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="adm-fade">
      <div class="adm-modal" v-if="pending" @click.self="pending = null">
        <div class="adm-modal-card" style="max-width:420px">
          <h3>确认删除</h3>
          <p>即将删除动态「{{ pending.content }}」，该操作不可恢复。</p>
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
import { ref, onMounted } from 'vue'
import { fetchAdminNews, saveAdminNews, deleteAdminNews } from '../../api'

const props = defineProps({ cred: { type: Object, required: true } })
const emit = defineEmits(['toast'])

const tags = ['公告', '预警', '活动', '历练', '战报']
const items = ref([])
const loading = ref(false)
const busy = ref(false)
const form = ref(null)
const pending = ref(null)

async function load() {
  loading.value = true
  const d = await fetchAdminNews(props.cred)
  loading.value = false
  if (d.ok) items.value = Array.isArray(d.items) ? d.items : []
  else emit('toast', d.msg || '动态加载失败', true)
}

function openCreate() {
  form.value = { id: null, tag: '公告', content: '', keyword: '', color: '#e9c46a', sort_no: 50, status: 1 }
}
function openEdit(n) { form.value = { ...n } }

async function save() {
  const f = form.value
  if (!f.content || !f.content.trim()) return emit('toast', '请填写动态文案', true)
  busy.value = true
  const d = await saveAdminNews(props.cred, {
    id: f.id, tag: f.tag, content: f.content.trim(),
    keyword: f.keyword, color: f.color, sort_no: f.sort_no, status: f.status
  })
  busy.value = false
  if (d.ok) {
    emit('toast', f.id ? '动态已更新' : '动态已新增')
    form.value = null
    load()
  } else {
    emit('toast', d.msg || '保存失败', true)
  }
}

async function toggle(n) {
  busy.value = true
  const target = n.status === 1 ? 0 : 1
  const d = await saveAdminNews(props.cred, {
    id: n.id, tag: n.tag, content: n.content, keyword: n.keyword,
    color: n.color, sort_no: n.sort_no, status: target
  })
  busy.value = false
  if (d.ok) {
    n.status = target
    emit('toast', target === 1 ? '已展示' : '已隐藏')
  } else {
    emit('toast', d.msg || '操作失败', true)
  }
}

async function doDelete() {
  const n = pending.value
  busy.value = true
  const d = await deleteAdminNews(props.cred, n.id)
  busy.value = false
  pending.value = null
  if (d.ok) {
    items.value = items.value.filter(x => x.id !== n.id)
    emit('toast', '动态已删除')
  } else {
    emit('toast', d.msg || '删除失败', true)
  }
}

onMounted(load)
</script>

<style scoped>
.color-pick {
  width: 42px; height: 38px; flex-shrink: 0;
  padding: 2px; border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, .16);
  background: rgba(7, 11, 22, .6); cursor: pointer;
}
</style>
