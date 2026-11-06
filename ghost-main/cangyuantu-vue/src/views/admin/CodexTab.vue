<template>
  <section class="adm-card">
    <div class="adm-h2">
      <span class="ico">❖</span>图鉴管理
      <span class="tip">
        首页四大界域卡片与检索结果的数据源 · 共 {{ total }} 条{{ pages > 1 ? ` · 第 ${page}/${pages} 页` : '' }}
      </span>
    </div>

    <!-- 分类页签 + 筛选 -->
    <div class="adm-toolbar">
      <div class="adm-tabs">
        <button v-for="t in tabs" :key="t" :class="{ on: cat === t }" @click="pickCat(t)">
          {{ t }}<span v-if="t !== '全部'">{{ countsAll[t] || 0 }}</span>
        </button>
      </div>
      <div class="adm-tb-right">
        <select v-model="statusFilter" class="adm-select adm-mini" @change="onStatusChange">
          <option value="">全部状态</option>
          <option value="1">仅上架</option>
          <option value="0">仅下架</option>
        </select>
        <input v-model="kw" class="adm-input" style="width:200px"
               placeholder="搜索名号 / 称号 / 简介…（回车）"
               @keyup.enter="resetAndLoad" @input="onKwInput" />
        <button class="adm-btn" :disabled="loading" @click="load()">{{ loading ? '加载中…' : '刷新' }}</button>
        <button class="adm-btn primary" @click="openCreate">＋ 新增条目</button>
      </div>
    </div>

    <!-- 批量操作条（有选中时才出现） -->
    <transition name="adm-fade">
      <div class="adm-batch" v-if="selected.length">
        <span class="adm-batch-txt">已选 <b>{{ selected.length }}</b> 条</span>
        <button class="adm-btn sm gold" :disabled="busy" @click="batchStatus(1)">批量上架</button>
        <button class="adm-btn sm" :disabled="busy" @click="batchStatus(0)">批量下架</button>
        <button class="adm-btn sm" :disabled="busy" @click="selected = []">取消选择</button>
        <span class="adm-batch-hint">批量操作即时作用于首页展示</span>
      </div>
    </transition>

    <div class="adm-table-wrap">
      <table class="adm-table">
        <thead>
          <tr>
            <th class="ck-col">
              <input type="checkbox" class="adm-check"
                     :checked="allChecked" :indeterminate.prop="partialChecked"
                     :disabled="!items.length" @change="toggleAll" title="全选本页" />
            </th>
            <th style="width:52px">ID</th>
            <th style="width:96px">分类</th>
            <th style="width:150px">名号</th>
            <th style="width:130px">称号 / 标签</th>
            <th>简介</th>
            <th style="width:76px">售价</th>
            <th style="width:76px">神尊力</th>
            <th style="width:66px">排序</th>
            <th style="width:86px">状态</th>
            <th style="width:170px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading && !items.length"><td colspan="11" class="adm-empty">正在读取图鉴数据…</td></tr>
          <tr v-else-if="!items.length">
            <td colspan="11" class="adm-empty">
              {{ hasFilter ? '当前筛选条件下没有条目，试试切换分类或清空搜索' : '暂无条目，点击「新增条目」开始录入' }}
            </td>
          </tr>
          <tr v-for="it in items" :key="it.id" :class="{ 'row-on': isChecked(it.id) }">
            <td class="ck-col">
              <input type="checkbox" class="adm-check" :checked="isChecked(it.id)"
                     @change="toggleOne(it.id)" />
            </td>
            <td class="num">{{ it.id }}</td>
            <td>
              <span class="adm-tag" :style="{ color: it.color, borderColor: it.color + '77' }">{{ it.category }}</span>
            </td>
            <td class="strong">
              <span class="adm-dot" :style="{ background: it.color, color: it.color }"></span>{{ it.name }}
            </td>
            <td class="muted">{{ it.title || '—' }}</td>
            <td class="desc-cell" :title="it.description">{{ it.description || '—' }}</td>
            <td class="num">{{ it.price || 0 }}</td>
            <td class="num">{{ it.power_gain || 0 }}</td>
            <td class="num">{{ it.sort_no || 0 }}</td>
            <td>
              <span class="adm-tag" :class="it.status === 1 ? 'on' : 'off'">
                {{ it.status === 1 ? '上架中' : '已下架' }}
              </span>
            </td>
            <td class="ops">
              <button class="adm-btn sm" @click="openEdit(it)">编辑</button>
              <button class="adm-btn sm gold" @click="toggle(it)">{{ it.status === 1 ? '下架' : '上架' }}</button>
              <button class="adm-btn sm danger" @click="pending = it">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页 -->
    <div class="adm-pager" v-if="total > 0">
      <div class="adm-pager-info">
        共 <b>{{ total }}</b> 条 · 每页
        <select v-model.number="pageSize" class="adm-select adm-mini" @change="onPageSizeChange">
          <option :value="10">10</option>
          <option :value="20">20</option>
          <option :value="50">50</option>
          <option :value="100">100</option>
        </select>
        条
      </div>
      <div class="adm-pager-btns">
        <button class="adm-btn sm" :disabled="page <= 1 || loading" @click="goPage(page - 1)">上一页</button>
        <button v-for="p in pageButtons" :key="p" class="adm-btn sm page-no"
                :class="{ on: p === page }" :disabled="loading" @click="goPage(p)">{{ p }}</button>
        <button class="adm-btn sm" :disabled="page >= pages || loading" @click="goPage(page + 1)">下一页</button>
      </div>
    </div>

    <!-- 新增 / 编辑弹窗 -->
    <transition name="adm-fade">
      <div class="adm-modal" v-if="form" @click.self="form = null">
        <div class="adm-modal-card">
          <button class="adm-modal-x" @click="form = null">✕</button>
          <h3>{{ form.id ? '编辑图鉴条目' : '新增图鉴条目' }}</h3>
          <p>分类决定条目出现在首页哪个界域；下架后该条目不再于首页与检索中显示。</p>

          <div class="adm-row">
            <div class="adm-field">
              <label>分类 <b>*</b></label>
              <select v-model="form.category" class="adm-select" @change="onCatChange">
                <option v-for="c in cats" :key="c" :value="c">{{ c }}</option>
              </select>
            </div>
            <div class="adm-field">
              <label>名号 <b>*</b></label>
              <input v-model="form.name" class="adm-input" placeholder="如：孟川" maxlength="64" />
            </div>
          </div>

          <div class="adm-row">
            <div class="adm-field">
              <label>称号 / 标签</label>
              <input v-model="form.title" class="adm-input" placeholder="如：三修·神魔" maxlength="64" />
            </div>
            <div class="adm-field">
              <label>卡片主色</label>
              <div style="display:flex;gap:10px;align-items:center">
                <input v-model="form.color" class="adm-input" placeholder="#e9c46a" maxlength="16" />
                <input type="color" v-model="form.color" class="color-pick" />
              </div>
            </div>
          </div>

          <div class="adm-field">
            <label>简介</label>
            <textarea v-model="form.description" class="adm-textarea" maxlength="1024"
                      placeholder="首页卡片与详情弹窗中展示的简介"></textarea>
          </div>

          <div class="adm-field">
            <label>设定台词（flavor）</label>
            <input v-model="form.flavor" class="adm-input" placeholder="如：众生是朝阳，而我，向着朝阳。" maxlength="512" />
          </div>

          <div class="adm-row-3">
            <div class="adm-field">
              <label>售价（铜币）</label>
              <input v-model.number="form.price" type="number" min="0" class="adm-input" />
            </div>
            <div class="adm-field">
              <label>购得神尊力</label>
              <input v-model.number="form.power_gain" type="number" min="0" class="adm-input" />
            </div>
            <div class="adm-field">
              <label>排序（越小越前）</label>
              <input v-model.number="form.sort_no" type="number" class="adm-input" />
            </div>
          </div>

          <div class="adm-field">
            <label>状态</label>
            <select v-model.number="form.status" class="adm-select">
              <option :value="1">上架（首页展示）</option>
              <option :value="0">下架（首页隐藏）</option>
            </select>
            <div class="adm-help">售价与神尊力仅对「功法」类有效，用于修炼殿购买。</div>
          </div>

          <div class="adm-modal-ops">
            <button class="adm-btn" @click="form = null">取消</button>
            <button class="adm-btn primary" :disabled="busy" @click="save">{{ busy ? '保存中…' : '保存' }}</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 删除确认 -->
    <transition name="adm-fade">
      <div class="adm-modal" v-if="pending" @click.self="pending = null">
        <div class="adm-modal-card" style="max-width:420px">
          <h3>确认删除</h3>
          <p>即将删除「{{ pending.category }} · {{ pending.name }}」，该操作不可恢复，首页将同步移除该条目。</p>
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
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import {
  fetchAdminCodex, saveAdminCodex, toggleAdminCodex, deleteAdminCodex, batchAdminCodexStatus
} from '../../api'

const props = defineProps({ cred: { type: Object, required: true } })
const emit = defineEmits(['toast'])

const cats = ['神尊', '功法', '妖魔', '秘境']
const tabs = ['全部', ...cats]

const cat = ref('全部')
const kw = ref('')
const statusFilter = ref('')          // ''=不限，'1'=仅上架，'0'=仅下架
const items = ref([])
const countsAll = ref({})             // 各分类总数（页签角标）
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const pages = ref(0)
const loading = ref(false)
const busy = ref(false)
const form = ref(null)
const pending = ref(null)
const selected = ref([])              // 跨页保留的选中 id

const hasFilter = computed(() => cat.value !== '全部' || !!kw.value.trim() || statusFilter.value !== '')

/* ---------------- 数据加载 ---------------- */

async function load(p) {
  if (typeof p === 'number') page.value = p
  loading.value = true
  const d = await fetchAdminCodex(props.cred, {
    category: cat.value === '全部' ? '' : cat.value,
    keyword: kw.value.trim(),
    status: statusFilter.value === '' ? null : Number(statusFilter.value),
    page: page.value,
    pageSize: pageSize.value
  })
  loading.value = false
  if (!d.ok) {
    emit('toast', d.msg || '图鉴加载失败', true)
    return
  }
  items.value = Array.isArray(d.items) ? d.items : []
  countsAll.value = d.counts_all || {}
  total.value = Number(d.total || 0)
  page.value = Number(d.page || 1)     // 后端会纠正越界页码（如删完最后一页）
  pageSize.value = Number(d.page_size || pageSize.value)
  pages.value = Number(d.pages || 0)
}

// 条件变化：回到第一页并清空选中，避免批量操作误伤看不见的条目
function resetAndLoad() {
  selected.value = []
  load(1)
}

function pickCat(t) {
  if (cat.value === t) return
  cat.value = t
  resetAndLoad()
}

// 下拉框直接读事件源的值：不依赖 v-model 与 @change 的执行先后顺序
function onStatusChange(e) {
  statusFilter.value = e.target.value
  resetAndLoad()
}

function onPageSizeChange(e) {
  pageSize.value = Number(e.target.value) || 20
  resetAndLoad()
}

function goPage(p) {
  if (p < 1 || p > pages.value || p === page.value) return
  load(p)
}

// 搜索输入防抖（400ms），回车立即查
let kwTimer = null
function onKwInput() {
  if (kwTimer) clearTimeout(kwTimer)
  kwTimer = setTimeout(() => { selected.value = []; load(1) }, 400)
}
onBeforeUnmount(() => { if (kwTimer) clearTimeout(kwTimer) })

/* ---------------- 分页页码 ---------------- */

const pageButtons = computed(() => {
  const totalPages = pages.value
  const cur = page.value
  if (totalPages <= 7) return Array.from({ length: totalPages }, (_, i) => i + 1)
  const set = new Set([1, totalPages, cur, cur - 1, cur + 1])
  if (cur <= 3) [2, 3, 4].forEach(n => set.add(n))
  if (cur >= totalPages - 2) [totalPages - 3, totalPages - 2, totalPages - 1].forEach(n => set.add(n))
  return [...set].filter(n => n >= 1 && n <= totalPages).sort((a, b) => a - b)
})

/* ---------------- 多选 ---------------- */

const isChecked = (id) => selected.value.includes(id)
const allChecked = computed(() =>
  items.value.length > 0 && items.value.every(it => selected.value.includes(it.id)))
const partialChecked = computed(() =>
  !allChecked.value && items.value.some(it => selected.value.includes(it.id)))

function toggleOne(id) {
  const i = selected.value.indexOf(id)
  if (i >= 0) selected.value.splice(i, 1)
  else selected.value.push(id)
}

function toggleAll() {
  const pageIds = items.value.map(it => it.id)
  if (allChecked.value) {
    selected.value = selected.value.filter(id => !pageIds.includes(id))
  } else {
    const merged = new Set([...selected.value, ...pageIds])
    selected.value = [...merged]
  }
}

/* ---------------- 表单 ---------------- */

function blank() {
  const c = cat.value === '全部' ? '神尊' : cat.value
  return {
    id: null, category: c, name: '', title: '', description: '', flavor: '',
    color: colorOf(c), price: 0, power_gain: 0, sort_no: 0, status: 1
  }
}

function colorOf(c) {
  return { 神尊: '#e9c46a', 功法: '#2dd4bf', 妖魔: '#ef4454', 秘境: '#5eead4' }[c] || '#e9c46a'
}

function openCreate() { form.value = blank() }
function openEdit(it) { form.value = { ...it } }
function onCatChange() { form.value.color = colorOf(form.value.category) }

async function save() {
  const f = form.value
  if (!f.name || !f.name.trim()) return emit('toast', '请填写名号', true)
  busy.value = true
  const d = await saveAdminCodex(props.cred, {
    id: f.id, category: f.category, name: f.name.trim(), title: f.title,
    description: f.description, flavor: f.flavor, color: f.color,
    price: f.price, power_gain: f.power_gain, sort_no: f.sort_no, status: f.status
  })
  busy.value = false
  if (d.ok) {
    emit('toast', f.id ? '已保存修改' : '已新增条目')
    form.value = null
    load()
  } else {
    emit('toast', d.msg || '保存失败', true)
  }
}

/* ---------------- 单条 / 批量操作 ---------------- */

async function toggle(it) {
  busy.value = true
  const target = it.status === 1 ? 0 : 1
  const d = await toggleAdminCodex(props.cred, it.id, target)
  busy.value = false
  if (d.ok) {
    it.status = target
    emit('toast', target === 1 ? `「${it.name}」已上架` : `「${it.name}」已下架`)
  } else {
    emit('toast', d.msg || '操作失败', true)
  }
}

async function batchStatus(status) {
  if (!selected.value.length) return
  busy.value = true
  const d = await batchAdminCodexStatus(props.cred, selected.value, status)
  busy.value = false
  if (d.ok) {
    emit('toast', `${status === 1 ? '已上架' : '已下架'} ${d.updated} 条`)
    selected.value = []
    load()
  } else {
    emit('toast', d.msg || '批量操作失败', true)
  }
}

async function doDelete() {
  const it = pending.value
  busy.value = true
  const d = await deleteAdminCodex(props.cred, it.id)
  busy.value = false
  pending.value = null
  if (d.ok) {
    selected.value = selected.value.filter(id => id !== it.id)
    emit('toast', `已删除「${it.name}」`)
    load()
  } else {
    emit('toast', d.msg || '删除失败', true)
  }
}

onMounted(() => load(1))
</script>

<style scoped>
.desc-cell {
  max-width: 300px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  color: #b9c6dd;
}
.color-pick {
  width: 42px; height: 38px; flex-shrink: 0;
  padding: 2px; border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, .16);
  background: rgba(7, 11, 22, .6); cursor: pointer;
}
.adm-tb-right { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
</style>
