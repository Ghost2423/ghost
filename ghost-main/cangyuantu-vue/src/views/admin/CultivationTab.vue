<template>
  <section class="adm-card">
    <div class="adm-h2">
      <span class="ico">⚡</span>修炼数据
      <span class="tip">境界 / 神尊力 / 铜币 / 称号 · 可改指定账号或全部账号，保存即落库</span>
      <span style="flex:1"></span>
      <button class="adm-btn primary solid sm" @click="openBatch">批量编辑全部账号</button>
      <button class="adm-btn sm" :disabled="loading" @click="load">{{ loading ? '加载中…' : '刷新' }}</button>
    </div>

    <div class="adm-toolbar">
      <div style="display:flex;gap:8px;flex-wrap:wrap">
        <span class="adm-tag gold">账号 {{ items.length }}</span>
        <span class="adm-tag on">已开辟 {{ initializedCount }}</span>
        <span class="adm-tag">未开辟 {{ items.length - initializedCount }}</span>
      </div>
      <div style="display:flex;gap:10px;align-items:center">
        <input v-model="kw" class="adm-input" style="width:220px" placeholder="搜索账号 / 境界 / 称号…" />
      </div>
    </div>

    <div class="adm-table-wrap">
      <table class="adm-table">
        <thead>
          <tr>
            <th style="width:52px">ID</th>
            <th>账号</th>
            <th style="width:120px">当前境界</th>
            <th style="width:100px">神尊力</th>
            <th style="width:100px">铜币</th>
            <th style="width:120px">称号</th>
            <th style="width:100px">状态</th>
            <th style="width:90px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading && !items.length"><td colspan="8" class="adm-empty">正在读取修炼数据…</td></tr>
          <tr v-else-if="!filtered.length"><td colspan="8" class="adm-empty">暂无账号</td></tr>
          <tr v-for="r in filtered" :key="r.user_id">
            <td class="num">{{ r.user_id }}</td>
            <td class="strong">{{ r.deity_name }}</td>
            <td>
              <span class="adm-tag" :class="isDeity(r.realm) ? 'gold' : 'on'">{{ r.realm }}</span>
            </td>
            <td class="num jade-text">⚡ {{ r.power }}</td>
            <td class="num gold-text">◉ {{ r.copper }}</td>
            <td class="muted">{{ r.title || '—' }}</td>
            <td>
              <span class="adm-tag" :class="r.initialized ? 'on' : ''">
                {{ r.initialized ? '已开辟' : '未开辟' }}
              </span>
            </td>
            <td class="ops">
              <button class="adm-btn sm" :disabled="busy" @click="openOne(r)">编辑</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- ===== 编辑弹窗：单个账号 或 全部账号 ===== -->
    <transition name="adm-fade">
      <div class="adm-modal" v-if="editing" @click.self="editing = null">
        <div class="adm-modal-card" style="max-width:600px">
          <h3>{{ batch ? '批量编辑 · 全部账号' : '编辑修炼数据 · ' + editing.deity_name }}</h3>
          <p style="font-size:12px;color:#9fb0c8;line-height:1.7;margin:6px 0 14px">
            保存写入 <b>user_cultivation</b> 并立即生效。<b>只改你填写的字段</b>，留空的保持原值；
            改境界而未填称号时，按规则自动授予对应最高称号（暗星→封侯神尊，无间及以上→封王神尊）。
            <span v-if="batch" style="color:#e9c46a">批量模式将影响全部 {{ items.length }} 个账号，请谨慎操作。</span>
          </p>

          <div class="edit-scope" v-if="!batch">
            目标账号：<b>{{ editing.deity_name }}</b>（ID {{ editing.user_id }}）
          </div>

          <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px 16px">
            <label class="ef" style="grid-column:1/-1">
              <span>境界（选填，不改请留「保持原值」）</span>
              <select class="adm-input" v-model="f.realm">
                <option value="">— 保持原值 —</option>
                <option v-for="r in ladder" :key="r" :value="r">{{ r }}</option>
              </select>
            </label>
            <label class="ef"><span>神尊力（选填）</span>
              <input class="adm-input" type="number" min="0" v-model="f.power" placeholder="留空不改" /></label>
            <label class="ef"><span>铜币（选填）</span>
              <input class="adm-input" type="number" min="0" v-model="f.copper" placeholder="留空不改" /></label>
            <label class="ef" style="grid-column:1/-1"><span>称号（选填，改境界时留空则自动授予）</span>
              <input class="adm-input" v-model="f.title" placeholder="留空 = 自动/保持原值" /></label>
          </div>

          <div class="adm-modal-ops">
            <button class="adm-btn" @click="editing = null">取消</button>
            <button class="adm-btn primary solid" :disabled="busy || !hasChange" @click="save">
              {{ busy ? '保存中…' : (batch ? '应用到全部账号' : '保存到数据库') }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { fetchAdminCultivations, updateAdminCultivation } from '../../api'

const props = defineProps({ cred: { type: Object, required: true } })
const emit = defineEmits(['toast'])

// 与后端 CultivationRules.LADDER 保持一致
const ladder = ['筑基', '内炼', '洗髓', '脱胎', '无漏', '丹云', '不灭', '大日', '暗星', '无间',
  '造化尊者', '混洞', '帝君', '原初', '一劫', '二劫', '三劫', '四劫', '五劫', '六劫', '七劫', '八劫', '永恒']

const items = ref([])
const loading = ref(false)
const busy = ref(false)
const kw = ref('')
const editing = ref(null)
const batch = ref(false)
const f = ref({ realm: '', power: '', copper: '', title: '' })

const initializedCount = computed(() => items.value.filter(r => r.initialized).length)
const filtered = computed(() => {
  const k = kw.value.trim().toLowerCase()
  if (!k) return items.value
  return items.value.filter(r =>
    (r.deity_name || '').toLowerCase().includes(k) ||
    (r.realm || '').toLowerCase().includes(k) ||
    (r.title || '').toLowerCase().includes(k))
})
const hasChange = computed(() =>
  f.value.realm !== '' || String(f.value.power).trim() !== '' ||
  String(f.value.copper).trim() !== '' || String(f.value.title).trim() !== '')

function isDeity(realm) {
  return !['筑基', '内炼', '洗髓', '脱胎', '无漏'].includes(realm)
}

async function load() {
  loading.value = true
  const d = await fetchAdminCultivations(props.cred)
  loading.value = false
  if (d.ok && Array.isArray(d.list)) {
    items.value = d.list
  } else {
    emit('toast', d.msg || '修炼数据加载失败', true)
  }
}

function resetForm() {
  f.value = { realm: '', power: '', copper: '', title: '' }
}

function openOne(r) {
  batch.value = false
  editing.value = r
  resetForm()
}

function openBatch() {
  batch.value = true
  editing.value = { deity_name: '全部账号' }
  resetForm()
}

async function save() {
  busy.value = true
  const item = {}
  if (f.value.realm !== '') item.realm = f.value.realm
  if (String(f.value.power).trim() !== '') {
    const v = Number(f.value.power)
    if (!Number.isInteger(v) || v < 0) { busy.value = false; return emit('toast', '神尊力须为非负整数', true) }
    item.power = v
  }
  if (String(f.value.copper).trim() !== '') {
    const v = Number(f.value.copper)
    if (!Number.isInteger(v) || v < 0) { busy.value = false; return emit('toast', '铜币须为非负整数', true) }
    item.copper = v
  }
  if (String(f.value.title).trim() !== '') item.title = String(f.value.title).trim()

  const d = await updateAdminCultivation(props.cred, editing.value && editing.value.user_id, batch.value, item)
  busy.value = false
  if (!d.ok) return emit('toast', d.msg || '保存失败', true)
  editing.value = null
  emit('toast', `已保存并落库，影响 ${d.updated || 0} 个账号`)
  await load()
}

onMounted(load)
</script>

<style scoped>
.edit-scope {
  font-size: 12px; color: #c7d4e8; background: rgba(255, 255, 255, .04);
  border: 1px solid rgba(255, 255, 255, .08); border-radius: 8px;
  padding: 8px 12px; margin-bottom: 14px;
}
.jade-text { color: #2dd4bf; }
.gold-text { color: #e9c46a; }
</style>
