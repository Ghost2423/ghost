<template>
  <section class="adm-card">
    <div class="adm-h2">
      <span class="ico">✎</span>站点文案
      <span class="tip">首页品牌名、英雄区标题、检索占位、热门检索标签与页脚</span>
    </div>

    <div class="adm-toolbar">
      <div class="adm-help">保存后刷新首页即生效；留空则首页使用内置兜底文案。</div>
      <div style="display:flex;gap:10px">
        <button class="adm-btn" :disabled="loading" @click="load">{{ loading ? '加载中…' : '重新读取' }}</button>
        <button class="adm-btn primary" :disabled="busy" @click="save">{{ busy ? '保存中…' : '保存文案' }}</button>
      </div>
    </div>

    <div class="adm-split">
      <!-- 左：编辑区 -->
      <div>
        <div class="adm-row">
          <div class="adm-field">
            <label>品牌名（导航栏）</label>
            <input v-model="f.site_title" class="adm-input" maxlength="32" placeholder="沧元图" />
          </div>
          <div class="adm-field">
            <label>品牌副标题</label>
            <input v-model="f.site_subtitle" class="adm-input" maxlength="64" placeholder="CANG YUAN TU · 元界" />
          </div>
        </div>

        <div class="adm-field">
          <label>首屏徽标</label>
          <input v-model="f.hero_badge" class="adm-input" maxlength="32" placeholder="元界 · 神尊归位" />
        </div>

        <div class="adm-field">
          <label>首屏主标题</label>
          <input v-model="f.hero_title" class="adm-input" maxlength="64" placeholder="沧元界 · 检索万物" />
        </div>

        <div class="adm-field">
          <label>首屏副标题</label>
          <textarea v-model="f.hero_subtitle" class="adm-textarea" maxlength="255"
                    placeholder="刀斩妖邪，笔绘众生。于元界之中，寻你的神尊、功法与秘境。"></textarea>
        </div>

        <div class="adm-row">
          <div class="adm-field">
            <label>检索框占位文案</label>
            <input v-model="f.search_placeholder" class="adm-input" maxlength="64"
                   placeholder="输入神尊名号、功法、妖魔或秘境…" />
          </div>
          <div class="adm-field">
            <label>热门检索标签</label>
            <input v-model="f.hot_tags" class="adm-input" placeholder="孟川,柳七月,元神淬体诀" />
            <div class="adm-help">多个标签用英文逗号 , 分隔</div>
          </div>
        </div>

        <div class="adm-field">
          <label>页脚文案</label>
          <input v-model="f.footer_text" class="adm-input" maxlength="255"
                 placeholder="© 沧元图主题演示 · 仅作展示用途，非官方站点" />
        </div>
      </div>

      <!-- 右：首页首屏实时预览 -->
      <aside class="pv-wrap">
        <div class="pv-head">
          <span class="ico">◈</span>首页首屏 · 实时预览
          <span class="pv-live">LIVE</span>
        </div>

        <div class="pv-stage">
          <div class="pv-nav">
            <span class="pv-seal">{{ brandInitial }}</span>
            <span class="pv-brand">
              <b>{{ f.site_title || '沧元图' }}</b>
              <em>{{ f.site_subtitle || 'CANG YUAN TU · 元界' }}</em>
            </span>
          </div>

          <div class="pv-hero">
            <span class="pv-badge">{{ f.hero_badge || '元界 · 神尊归位' }}</span>
            <h1 class="pv-title">{{ f.hero_title || '沧元界 · 检索万物' }}</h1>
            <p class="pv-sub">{{ f.hero_subtitle || '刀斩妖邪，笔绘众生。于元界之中，寻你的神尊、功法与秘境。' }}</p>

            <div class="pv-search">
              <span class="pv-s-ico">⌕</span>
              <em>{{ f.search_placeholder || '输入神尊名号、功法、妖魔或秘境…' }}</em>
              <b>入元界检索</b>
            </div>

            <div class="pv-hot" v-if="hotTags.length">
              <span>热门：</span>
              <i v-for="t in hotTags" :key="t">{{ t }}</i>
            </div>
            <div class="pv-hot pv-hot-empty" v-else>热门：<em>（无标签，首页该行隐藏）</em></div>
          </div>

          <div class="pv-foot">{{ f.footer_text || '© 沧元图主题演示 · 仅作展示用途，非官方站点' }}</div>
        </div>

        <div class="adm-help" style="margin-top:10px">
          按面板宽度等比缩放的效果示意；留空的字段首页会用内置兜底文案（预览中已显示兜底值）。
        </div>
      </aside>
    </div>

    <div class="adm-h2" style="margin-top:22px;font-size:15px">
      <span class="ico">◈</span>数据库中的全部配置项
    </div>
    <div class="adm-table-wrap" style="margin-top:12px">
      <table class="adm-table">
        <thead>
          <tr><th style="width:200px">配置键</th><th>当前值</th><th style="width:220px">说明</th></tr>
        </thead>
        <tbody>
          <tr v-if="!rows.length"><td colspan="3" class="adm-empty">暂无配置项</td></tr>
          <tr v-for="r in rows" :key="r.id">
            <td class="strong">{{ r.setting_key }}</td>
            <td class="muted">{{ r.setting_value }}</td>
            <td class="muted">{{ r.remark || '—' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { fetchAdminSettings, saveAdminSettings } from '../../api'

const props = defineProps({ cred: { type: Object, required: true } })
const emit = defineEmits(['toast'])

const KEYS = ['site_title', 'site_subtitle', 'hero_badge', 'hero_title', 'hero_subtitle',
  'search_placeholder', 'hot_tags', 'footer_text']

const rows = ref([])
const loading = ref(false)
const busy = ref(false)
const f = ref(Object.fromEntries(KEYS.map(k => [k, ''])))

// 预览：热门标签（中英文逗号都可分隔，与首页解析规则一致）
const hotTags = computed(() =>
  (f.value.hot_tags || '').split(/[,，]/).map(s => s.trim()).filter(Boolean))
const brandInitial = computed(() => (f.value.site_title || '沧元图').trim().slice(0, 1) || '沧')

async function load() {
  loading.value = true
  const d = await fetchAdminSettings(props.cred)
  loading.value = false
  if (d.ok) {
    rows.value = Array.isArray(d.items) ? d.items : []
    const s = d.settings || {}
    f.value = Object.fromEntries(KEYS.map(k => [k, s[k] == null ? '' : s[k]]))
  } else {
    emit('toast', d.msg || '文案加载失败', true)
  }
}

async function save() {
  busy.value = true
  const d = await saveAdminSettings(props.cred, { ...f.value })
  busy.value = false
  if (d.ok) {
    emit('toast', `已保存 ${d.updated || 0} 项文案`)
    load()
  } else {
    emit('toast', d.msg || '保存失败', true)
  }
}

onMounted(load)
</script>

<style scoped>
/* ===== 首页首屏实时预览（等比缩放示意） ===== */
.pv-wrap { position: sticky; top: 0; }

.pv-head {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: 10px;
  font-size: 12.5px; letter-spacing: 2px; font-weight: 700;
  color: var(--gold, #e9c46a);
}
.pv-head .ico { text-shadow: 0 0 12px var(--gold, #e9c46a); }
.pv-live {
  margin-left: auto; padding: 2px 8px; border-radius: 999px;
  font-size: 10px; letter-spacing: 1.5px;
  color: var(--jade, #2dd4bf);
  border: 1px solid rgba(45, 212, 191, .45);
  background: rgba(45, 212, 191, .10);
}

.pv-stage {
  position: relative; overflow: hidden;
  border-radius: 18px; padding: 14px 16px 12px;
  border: 1px solid rgba(255, 255, 255, .10);
  background:
    radial-gradient(90% 60% at 80% 0%, rgba(233, 196, 106, .12), transparent 60%),
    radial-gradient(80% 60% at 10% 100%, rgba(45, 212, 191, .12), transparent 60%),
    linear-gradient(180deg, #0b1224, #070b16 72%);
}

/* 导航栏 */
.pv-nav { display: flex; align-items: center; gap: 8px; padding-bottom: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, .07); }
.pv-seal {
  width: 26px; height: 26px; border-radius: 7px; flex-shrink: 0;
  display: grid; place-items: center;
  font-size: 13px; font-weight: 900; color: #fff;
  background: linear-gradient(135deg, #ef4454, #b91c1c);
  box-shadow: 0 4px 14px rgba(239, 68, 68, .35);
}
.pv-brand { display: flex; flex-direction: column; gap: 1px; min-width: 0; }
.pv-brand b { font-size: 12.5px; letter-spacing: 2px; color: #e8eefc; }
.pv-brand em { font-size: 8.5px; font-style: normal; letter-spacing: 1.6px; color: #8b9ab8; }

/* 英雄区 */
.pv-hero { text-align: center; padding: 16px 0 10px; }
.pv-badge {
  display: inline-block; padding: 3px 10px; border-radius: 999px;
  font-size: 9.5px; letter-spacing: 2.5px; color: var(--jade, #2dd4bf);
  border: 1px solid rgba(45, 212, 191, .4);
  background: rgba(45, 212, 191, .08);
}
.pv-title {
  margin: 10px 0 8px; font-size: 25px; line-height: 1.25; font-weight: 900;
  letter-spacing: 2px;
  font-family: "ZCOOL XiaoWei", "Noto Serif SC", serif;
  background: linear-gradient(135deg, #f7e3ad, var(--gold, #e9c46a) 45%, #b8892a);
  -webkit-background-clip: text; background-clip: text; color: transparent;
  word-break: break-word;
}
.pv-sub {
  margin: 0 auto; max-width: 300px;
  font-size: 11px; line-height: 1.9; letter-spacing: .5px; color: #c7d4e8;
  word-break: break-word;
}

/* 检索框 */
.pv-search {
  display: flex; align-items: center; gap: 6px;
  margin: 14px auto 0; padding: 4px 4px 4px 10px;
  border-radius: 999px;
  border: 1px solid rgba(45, 212, 191, .35);
  background: rgba(7, 11, 22, .6);
  box-shadow: 0 0 0 3px rgba(45, 212, 191, .06);
}
.pv-s-ico { color: #8b9ab8; font-size: 12px; }
.pv-search em {
  flex: 1; min-width: 0; font-style: normal;
  font-size: 10.5px; color: #8b9ab8; letter-spacing: .5px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.pv-search b {
  flex-shrink: 0; padding: 6px 11px; border-radius: 999px;
  font-size: 10px; letter-spacing: 1px; color: #04130f;
  background: linear-gradient(135deg, var(--jade, #2dd4bf), #5eead4);
}

/* 热门标签 */
.pv-hot {
  display: flex; align-items: center; justify-content: center; flex-wrap: wrap;
  gap: 5px; margin-top: 14px;
  font-size: 9.5px; color: #8b9ab8;
}
.pv-hot i {
  font-style: normal; padding: 2px 8px; border-radius: 999px;
  color: var(--jade, #2dd4bf);
  border: 1px solid rgba(45, 212, 191, .3);
  background: rgba(45, 212, 191, .07);
  font-size: 9.5px;
}
.pv-hot-empty em { font-style: normal; color: #6b7893; }

/* 页脚 */
.pv-foot {
  margin-top: 14px; padding-top: 9px;
  border-top: 1px solid rgba(255, 255, 255, .07);
  text-align: center;
  font-size: 9px; letter-spacing: .5px; color: #6b7893;
  word-break: break-word;
}
</style>
