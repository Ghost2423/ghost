<template>
  <section class="adm-card">
    <div class="adm-h2">
      <span class="ico">✦</span>核心界域展示框
      <span class="tip">首页「核心界域」四个展示框的标题 / 图标 / 主色 / 简介 / 排序 / 显隐</span>
    </div>

    <div class="adm-toolbar">
      <div class="adm-help">收录数由对应界域的图鉴条目（上架）实时统计，此处无需维护。</div>
      <div style="display:flex;gap:10px">
        <button class="adm-btn" :disabled="loading" @click="load">{{ loading ? '加载中…' : '刷新' }}</button>
        <button class="adm-btn primary" v-if="missing.length" @click="openCreate">＋ 补齐缺失界域</button>
      </div>
    </div>

    <div class="adm-frames" v-if="items.length">
      <div v-for="s in items" :key="s.id" class="adm-frame" :style="{ '--c': s.color }">
        <div class="fh">
          <span class="fi">{{ s.icon }}</span>
          <span class="adm-tag" :class="s.status === 1 ? 'on' : 'off'">
            {{ s.status === 1 ? '展示中' : '已隐藏' }}
          </span>
        </div>
        <div class="ft">{{ s.title }}</div>
        <div class="fd">{{ s.description || '（暂无简介）' }}</div>
        <div class="fm">
          <span class="adm-tag gold">{{ s.section_type }} · 收录 {{ (counts[s.section_type] || 0) }}</span>
          <span style="display:flex;gap:8px">
            <button class="adm-btn sm" @click="openEdit(s)">编辑</button>
            <button class="adm-btn sm gold" @click="toggle(s)">{{ s.status === 1 ? '隐藏' : '展示' }}</button>
          </span>
        </div>
      </div>
    </div>
    <div v-else class="adm-empty">暂无展示框配置</div>

    <!-- 编辑弹窗 -->
    <transition name="adm-fade">
      <div class="adm-modal" v-if="form" @click.self="form = null">
        <div class="adm-modal-card" style="width:820px;max-width:92vw">
          <button class="adm-modal-x" @click="form = null">✕</button>
          <h3>{{ form.id ? '编辑展示框' : '新增展示框' }}</h3>
          <p>展示框对应首页四大界域，点击后跳转到该界域的图鉴列表。</p>

          <div class="sec-2col">
            <div>

          <div class="adm-field">
            <label>界域类型 <b>*</b></label>
            <select v-model="form.section_type" class="adm-select" :disabled="!!form.id"
                    @change="form.color = colorOf(form.section_type)">
              <option v-for="c in options" :key="c" :value="c">{{ c }}</option>
            </select>
            <div class="adm-help" v-if="form.id">界域类型创建后不可修改</div>
          </div>

          <div class="adm-row">
            <div class="adm-field">
              <label>标题 <b>*</b></label>
              <input v-model="form.title" class="adm-input" placeholder="如：神尊殿" maxlength="32" />
            </div>
            <div class="adm-field">
              <label>图标</label>
              <input v-model="form.icon" class="adm-input" placeholder="如：✦" maxlength="8" />
            </div>
          </div>

          <div class="adm-row">
            <div class="adm-field">
              <label>主色</label>
              <div style="display:flex;gap:10px;align-items:center">
                <input v-model="form.color" class="adm-input" placeholder="#e9c46a" maxlength="16" />
                <input type="color" v-model="form.color" class="color-pick" />
              </div>
            </div>
            <div class="adm-field">
              <label>排序（越小越前）</label>
              <input v-model.number="form.sort_no" type="number" class="adm-input" />
            </div>
          </div>

          <div class="adm-field">
            <label>简介</label>
            <textarea v-model="form.description" class="adm-textarea" maxlength="255"
                      placeholder="展示框上的说明文字"></textarea>
          </div>

          <div class="adm-field" style="margin-bottom:6px">
            <label>状态</label>
            <select v-model.number="form.status" class="adm-select">
              <option :value="1">展示</option>
              <option :value="0">隐藏</option>
            </select>
          </div>

            </div>

            <!-- 右：首页卡片实时预览 -->
            <aside class="sec-pv-side">
              <div class="sec-pv-label">
                首页卡片 · 实时预览
                <span class="sec-live">LIVE</span>
              </div>

              <div class="sec-pv" :style="{ '--c': form.color || '#e9c46a' }">
                <span class="sec-pv-off" v-if="form.status === 0">已隐藏 · 首页不显示</span>
                <div class="sec-pv-ico">{{ form.icon || '❖' }}</div>
                <h4 class="sec-pv-title">{{ form.title || '未命名界域' }}</h4>
                <p class="sec-pv-desc">{{ form.description || '（暂无简介，首页该处会留白）' }}</p>
                <div class="sec-pv-foot">
                  <span class="sec-pv-count">{{ form.section_type }} · 收录 {{ counts[form.section_type] || 0 }}</span>
                  <span class="sec-pv-go">进入 ›</span>
                </div>
                <span class="sec-pv-glow"></span>
              </div>

              <div class="adm-help" style="margin-top:10px">
                按首页卡片样式等比缩小；输入即时反映，保存后首页生效。收录数取自该界域「上架」的图鉴条目。
              </div>
            </aside>
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
          <h3>确认删除展示框</h3>
          <p>即将删除「{{ pending.title }}」，首页将不再显示该展示框（对应图鉴条目不受影响）。</p>
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
import { fetchAdminSections, saveAdminSection, deleteAdminSection } from '../../api'

const props = defineProps({ cred: { type: Object, required: true } })
const emit = defineEmits(['toast'])

const cats = ['神尊', '功法', '妖魔', '秘境']
const items = ref([])
const counts = ref({})
const loading = ref(false)
const busy = ref(false)
const form = ref(null)
const pending = ref(null)

// 尚未配置的界域（可补齐）
const missing = computed(() => cats.filter(c => !items.value.some(s => s.section_type === c)))
const options = computed(() => (form.value && form.value.id) ? cats : (missing.value.length ? missing.value : cats))

function colorOf(c) {
  return { 神尊: '#e9c46a', 功法: '#2dd4bf', 妖魔: '#ef4454', 秘境: '#5eead4' }[c] || '#e9c46a'
}

async function load() {
  loading.value = true
  const d = await fetchAdminSections(props.cred)
  loading.value = false
  if (d.ok) {
    items.value = Array.isArray(d.items) ? d.items : []
    counts.value = d.counts || {}
  } else {
    emit('toast', d.msg || '展示框加载失败', true)
  }
}

function openCreate() {
  const t = missing.value[0] || '神尊'
  form.value = {
    id: null, section_type: t, title: { 神尊: '神尊殿', 功法: '修炼功法', 妖魔: '妖魔图鉴', 秘境: '元界秘境' }[t],
    icon: { 神尊: '✦', 功法: '✧', 妖魔: '☠', 秘境: '❖' }[t],
    color: colorOf(t), description: '', sort_no: 50, status: 1
  }
}
function openEdit(s) { form.value = { ...s } }

async function save() {
  const f = form.value
  if (!f.title || !f.title.trim()) return emit('toast', '请填写标题', true)
  busy.value = true
  const d = await saveAdminSection(props.cred, {
    id: f.id,
    section_type: f.section_type,
    title: f.title.trim(),
    icon: f.icon,
    color: f.color,
    description: f.description,
    sort_no: f.sort_no,
    status: f.status
  })
  busy.value = false
  if (d.ok) {
    emit('toast', f.id ? '展示框已更新' : '展示框已创建')
    form.value = null
    load()
  } else {
    emit('toast', d.msg || '保存失败', true)
  }
}

async function toggle(s) {
  busy.value = true
  const target = s.status === 1 ? 0 : 1
  const d = await saveAdminSection(props.cred, {
    id: s.id, section_type: s.section_type, title: s.title, icon: s.icon,
    color: s.color, description: s.description, sort_no: s.sort_no, status: target
  })
  busy.value = false
  if (d.ok) {
    s.status = target
    emit('toast', target === 1 ? '展示框已展示' : '展示框已隐藏')
  } else {
    emit('toast', d.msg || '操作失败', true)
  }
}

async function doDelete() {
  const s = pending.value
  busy.value = true
  const d = await deleteAdminSection(props.cred, s.id)
  busy.value = false
  pending.value = null
  if (d.ok) {
    items.value = items.value.filter(x => x.id !== s.id)
    emit('toast', '展示框已删除')
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

/* ===== 左表单 / 右首页卡片预览 ===== */
.sec-2col {
  display: grid;
  grid-template-columns: 1.3fr .7fr;
  gap: 22px;
  align-items: start;
}
@media (max-width: 860px) {
  .sec-2col { grid-template-columns: 1fr; }
}

.sec-pv-side { position: sticky; top: 0; }

.sec-pv-label {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: 10px;
  font-size: 12px; letter-spacing: 2px; font-weight: 700;
  color: var(--gold, #e9c46a);
}
.sec-live {
  margin-left: auto; padding: 2px 8px; border-radius: 999px;
  font-size: 9.5px; letter-spacing: 1.5px;
  color: var(--jade, #2dd4bf);
  border: 1px solid rgba(45, 212, 191, .45);
  background: rgba(45, 212, 191, .10);
}

/* 首页 .frame 卡片的等比缩小版 */
.sec-pv {
  position: relative; overflow: hidden;
  padding: 20px 18px 16px; border-radius: 18px;
  border: 1px solid color-mix(in srgb, var(--c) 38%, transparent);
  background:
    radial-gradient(90% 70% at 100% 0%, color-mix(in srgb, var(--c) 18%, transparent), transparent 62%),
    linear-gradient(160deg, rgba(14, 22, 42, .78), rgba(7, 11, 22, .6));
  box-shadow: 0 18px 44px rgba(0, 0, 0, .42), inset 0 1px 0 rgba(255, 255, 255, .06);
}
.sec-pv::before {
  content: ""; position: absolute; left: 0; top: 0; bottom: 0; width: 4px;
  background: linear-gradient(180deg, var(--c), transparent); opacity: .9;
}
.sec-pv-off {
  position: absolute; right: 12px; top: 12px; z-index: 2;
  padding: 2px 8px; border-radius: 999px;
  font-size: 9.5px; letter-spacing: 1px; color: #9aa6bd;
  border: 1px solid rgba(255, 255, 255, .18);
  background: rgba(7, 11, 22, .78);
}
.sec-pv-ico {
  width: 44px; height: 44px; border-radius: 12px;
  display: grid; place-items: center;
  font-size: 22px; color: var(--c);
  background: color-mix(in srgb, var(--c) 14%, transparent);
  border: 1px solid color-mix(in srgb, var(--c) 36%, transparent);
  box-shadow: 0 0 18px color-mix(in srgb, var(--c) 26%, transparent);
}
.sec-pv-title {
  margin: 14px 0 8px;
  font-size: 18px; font-weight: 900; letter-spacing: 2px;
  color: #e8eefc;
  font-family: "ZCOOL XiaoWei", "Noto Serif SC", serif;
  word-break: break-word;
}
.sec-pv-desc {
  margin: 0; min-height: 64px;
  font-size: 12px; line-height: 1.85; letter-spacing: .4px; color: #b9c6dd;
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
  word-break: break-word;
}
.sec-pv-foot {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 12px; padding-top: 10px;
  border-top: 1px solid rgba(255, 255, 255, .07);
}
.sec-pv-count { font-size: 11px; letter-spacing: 1px; color: var(--c); }
.sec-pv-go { font-size: 12px; letter-spacing: 2px; color: #e8eefc; }
.sec-pv-glow {
  position: absolute; right: -40px; top: -40px; width: 120px; height: 120px;
  border-radius: 50%; pointer-events: none; opacity: .5;
  background: radial-gradient(circle, color-mix(in srgb, var(--c) 30%, transparent), transparent 70%);
}
</style>
