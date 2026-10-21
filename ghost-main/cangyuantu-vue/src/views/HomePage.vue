<template>
  <div class="home" ref="root">
    <!-- 氛围：流光粒子 -->
    <div class="embers">
      <i v-for="(e, i) in embers" :key="i" :style="e"></i>
    </div>

    <!-- ===== 顶部导航栏 ===== -->
    <header class="nav">
      <div class="nav-inner">
        <!-- 品牌 -->
        <div class="brand" @click="goHome">
          <div class="seal">沧</div>
          <div class="brand-txt">
            <b>沧元图</b>
            <span>CANG YUAN TU · 元界</span>
          </div>
        </div>

        <!-- 导航项 -->
        <nav class="menu">
          <a v-for="m in menus" :key="m.key"
             :class="{ active: m.key === activeMenu }"
             @click="onMenu(m)">{{ m.label }}</a>
        </nav>

        <!-- 右侧：检索 + 用户 -->
        <div class="nav-right">
          <div class="nav-search" :class="{ focus: navFocus }">
            <span class="s-ico">⌕</span>
            <input v-model="keyword" type="text" maxlength="40"
                   placeholder="检索神尊 / 功法 / 妖魔 / 秘境…"
                   @focus="navFocus = true" @blur="navFocus = false"
                   @keyup.enter="doSearch" />
            <button class="s-btn" @click="doSearch">检索</button>
          </div>

          <div class="user" v-if="avatar">
            <div class="avatar">{{ avatarInitial }}</div>
            <span class="uname" :title="avatar.deity_name">{{ entryName }}</span>
            <span class="guest-tag" v-if="avatar.isGuest">游客</span>
            <button class="logout" @click="$emit('logout')">退位</button>
          </div>
        </div>
      </div>
    </header>

    <!-- ===== 英雄区 + 主检索栏 ===== -->
    <section class="hero">
      <div class="hero-badge">元界 · 神尊归位</div>
      <h1 class="hero-title">沧元界 · 检索万物</h1>
      <p class="hero-sub">刀斩妖邪，笔绘众生。于元界之中，寻你的神尊、功法与秘境。</p>

      <div class="hero-search">
        <span class="hs-ico">⌕</span>
        <input v-model="keyword" type="text" maxlength="40"
               placeholder="输入神尊名号、功法、妖魔或秘境…"
               @keyup.enter="doSearch" />
        <button class="hs-btn" :disabled="searching" @click="doSearch">
          {{ searching ? '检索中…' : '入元界检索' }}
        </button>
      </div>

      <div class="hot">
        <span class="hot-label">热门：</span>
        <button v-for="t in hotTags" :key="t" class="hot-tag" @click="quickSearch(t)">{{ t }}</button>
      </div>
    </section>

    <!-- ===== 首页展示框：四大核心界域（4 个展示框） ===== -->
    <section class="showcase">
      <div class="showcase-head">
        <span class="sh-ico">❖</span>
        <h2>核心界域</h2>
        <span class="sh-line"></span>
        <span class="sh-tip">轻触展示框，直入对应界域</span>
      </div>
      <div class="frames">
        <article v-for="f in showcase" :key="f.type" class="frame"
                 :style="{ '--c': f.color }" @click="goSection(f.type)">
          <div class="frame-ico">{{ f.icon }}</div>
          <h3 class="frame-title">{{ f.title }}</h3>
          <p class="frame-desc">{{ f.desc }}</p>
          <div class="frame-foot">
            <span class="frame-count">收录 {{ counts[f.type] || 0 }}</span>
            <span class="frame-go">进入 ›</span>
          </div>
          <span class="frame-glow"></span>
        </article>
      </div>
    </section>

    <!-- ===== 检索结果 / 内容区 ===== -->
    <main class="main">
      <!-- 检索态提示 -->
      <div class="search-head" v-if="hasSearched">
        <div class="sh-left">
          <span class="sh-kw">「{{ lastKeyword }}」</span>
          <span class="sh-count">共寻得 {{ catalog.length }} 条元界记载</span>
          <span class="sh-src" v-if="apiResults">· 后端数据源</span>
          <span class="sh-src local" v-else>· 本地兜底</span>
        </div>
        <button class="sh-clear" @click="clearSearch">清除检索</button>
      </div>

      <!-- 空态 -->
      <div class="empty" v-if="hasSearched && catalog.length === 0">
        <div class="empty-mark">◇</div>
        <p>未寻得相关元界记载，换个名号再试？</p>
      </div>

      <!-- 分类板块（神尊 / 功法 / 妖魔 / 秘境） -->
      <section v-for="sec in sections" :key="sec.type" class="block"
               :id="'sec-' + sec.type"
               :class="{ flash: flashType === sec.type }"
               v-show="itemsOf(sec.type).length">
        <div class="block-head">
          <span class="bh-icon">{{ sec.icon }}</span>
          <h2>{{ sec.title }}</h2>
          <span class="bh-line"></span>
          <button class="bh-top" @click="goHome">回到顶界 ↑</button>
        </div>
        <div class="grid">
          <article v-for="(it, idx) in itemsOf(sec.type)" :key="sec.type + idx" class="card"
                   :style="{ '--c': it.color || sec.color }" @click="onOpen(it)">
            <div class="card-top">
              <span class="card-tag">{{ it.tag || sec.tag }}</span>
              <span class="card-type">{{ it.type }}</span>
            </div>
            <h3 class="card-name">{{ it.name }}</h3>
            <p class="card-desc">{{ it.desc }}</p>
            <button class="card-more" @click.stop="onOpen(it)">探 寻 ›</button>
          </article>
        </div>
      </section>

      <!-- 元界动态滚动条 -->
      <section class="news" v-show="newsItems.length" id="sec-动态">
        <div class="news-head"><span class="nh-ico">❖</span><h2>元界动态</h2></div>
        <div class="news-track">
          <button v-for="(n, i) in newsItems" :key="i" class="news-item"
                  :style="{ '--c': n.color }" @click="quickSearch(n.kw)">
            <span class="news-tag">{{ n.tag }}</span>
            <span class="news-text">{{ n.text }}</span>
          </button>
        </div>
      </section>
    </main>

    <footer class="foot">
      © 沧元图主题演示 · 仅作展示用途，非官方站点 · 首页图鉴与检索已接入后端（/api/codex · /api/search）
    </footer>

    <!-- 详情弹窗 -->
    <transition name="fade">
      <div class="modal" v-if="activeItem" @click.self="closeModal">
        <div class="modal-card" :style="{ '--c': activeItem.color }">
          <button class="modal-x" @click="closeModal">✕</button>
          <span class="modal-tag">{{ activeItem.tag }}</span>
          <h3 class="modal-name">{{ activeItem.name }}</h3>
          <span class="modal-type">{{ activeItem.type }} · 元界记载</span>
          <p class="modal-desc">{{ activeItem.desc }}</p>
          <div class="modal-flavor" v-if="activeItem.flavor">「 {{ activeItem.flavor }} 」</div>
          <button class="modal-close" @click="closeModal">收 卷</button>
        </div>
      </div>
    </transition>

    <!-- 提示 toast -->
    <div class="toast" :class="{ show: toastShow }">{{ toastMsg }}</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { searchCangyuan, fetchCodex } from '../api'

const props = defineProps({ avatar: { type: Object, default: null } })
defineEmits(['logout'])

const root = ref(null)
function scrollTop() { if (root.value) root.value.scrollTo({ top: 0, behavior: 'smooth' }) }
function getSecEl(type) { return root.value ? root.value.querySelector('#sec-' + type) : null }

/* ===== 氛围粒子 ===== */
const palette = ['#2dd4bf', '#e9c46a', '#ef4454', '#5eead4', '#ffffff']
const embers = Array.from({ length: 24 }, () => {
  const c = palette[Math.floor(Math.random() * palette.length)]
  const size = 2 + Math.random() * 3
  return {
    left: (Math.random() * 100).toFixed(2) + '%',
    background: c,
    boxShadow: `0 0 8px ${c}`,
    width: size + 'px',
    height: size + 'px',
    animation: `rise linear infinite ${(7 + Math.random() * 8).toFixed(2)}s ${(-Math.random() * 10).toFixed(2)}s`
  }
})

/* ===== 导航 ===== */
const activeMenu = ref('home')
const menus = [
  { key: 'home', label: '首页' },
  { key: 'deity', label: '神尊殿', type: '神尊' },
  { key: 'gongfa', label: '修炼功法', type: '功法' },
  { key: 'demon', label: '妖魔图鉴', type: '妖魔' },
  { key: 'realm', label: '元界秘境', type: '秘境' },
  { key: 'forum', label: '论道论坛' }
]
function onMenu(m) {
  if (m.key === 'home') { clearSearch(); activeMenu.value = 'home'; scrollTop(); return }
  if (m.key === 'forum') { toast('论道论坛尚在开辟中，敬请期待'); return }
  activeMenu.value = m.key
  goSection(m.type)
}
function goHome() { clearSearch(); activeMenu.value = 'home'; scrollTop() }

/* ===== 用户 ===== */
// 元界之名（踏入沧元界的化身名）：注册神尊取神尊原型 myth_name（如 雷声普化天尊 → 孟川），
// 游客取其生成的随机名号；作为右上角身份与头像首字。
const entryName = computed(() => {
  const a = props.avatar || {}
  if (a.isGuest) return a.deity_name || '游客'
  return a.myth_name || a.deity_name || '道友'
})
const avatarInitial = computed(() => entryName.value.slice(0, 1))

/* ===== 检索 ===== */
const keyword = ref('')
const lastKeyword = ref('')
const navFocus = ref(false)
const searching = ref(false)
const hasSearched = ref(false)
const apiResults = ref(null)

// 后端图鉴数据（首页展示框 + 分类卡片的数据源）；加载失败则回退本地 rawCatalog
const codexItems = ref(null)

// 数据基座：优先用后端图鉴，否则用本地兜底
function baseSource() { return codexItems.value || rawCatalog }

// 进入首页即向后端拉取图鉴（首页展示功能后端化）
onMounted(async () => {
  try {
    const d = await fetchCodex()
    if (d && d.ok && Array.isArray(d.items)) codexItems.value = d.items
  } catch (e) {
    codexItems.value = null
  }
})

async function doSearch() {
  const kw = keyword.value.trim()
  if (!kw) { toast('请输入检索词'); return }
  lastKeyword.value = kw
  activeMenu.value = 'home'
  searching.value = true
  try {
    const d = await searchCangyuan(kw)
    if (d && d.ok && Array.isArray(d.results)) apiResults.value = d.results
    else apiResults.value = null
  } catch (e) {
    apiResults.value = null
  }
  hasSearched.value = true
  searching.value = false
}
function quickSearch(t) { keyword.value = t; doSearch() }
function clearSearch() {
  keyword.value = ''
  lastKeyword.value = ''
  hasSearched.value = false
  apiResults.value = null
}

/* ===== 首页展示框：四大核心界域 ===== */
const showcase = [
  { type: '神尊', title: '神尊殿', icon: '✦', color: '#e9c46a',
    desc: '人族神尊列传，孟川、柳七月、秦五……元界守护者的名号与传说。' },
  { type: '功法', title: '修炼功法', icon: '✧', color: '#2dd4bf',
    desc: '元神淬体、刀道雷法，淬肉身、凝神魂的修行法门总纲。' },
  { type: '妖魔', title: '妖魔图鉴', icon: '☠', color: '#ef4454',
    desc: '妖族谱录：噬魂魔、赤焰妖将、妖王……识其形，方能斩其势。' },
  { type: '秘境', title: '元界秘境', icon: '❖', color: '#5eead4',
    desc: '不周遗墟、元初山、暗星界，藏上古传承的凶险界域。' }
]

const flashType = ref('')
function goSection(type) {
  clearSearch()
  activeMenu.value = { '神尊': 'deity', '功法': 'gongfa', '妖魔': 'demon', '秘境': 'realm' }[type] || 'home'
  // 等待 DOM 解除 v-show 后滚动
  requestAnimationFrame(() => {
    const el = getSecEl(type)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' })
      flashType.value = type
      setTimeout(() => { if (flashType.value === type) flashType.value = '' }, 1300)
    } else scrollTop()
  })
}

/* ===== 分类内容（神尊 / 功法 / 妖魔 / 秘境），依《沧元图》设定填充 ===== */
const sections = [
  { type: '神尊', title: '神尊殿', icon: '✦', tag: '神尊', color: '#e9c46a' },
  { type: '功法', title: '修炼功法', icon: '✧', tag: '功法', color: '#2dd4bf' },
  { type: '妖魔', title: '妖魔图鉴', icon: '☠', tag: '妖魔', color: '#ef4454' },
  { type: '秘境', title: '元界秘境', icon: '❖', tag: '秘境', color: '#5eead4' }
]

const rawCatalog = [
  // —— 神尊 ——
  { type: '神尊', name: '孟川', tag: '主角·刀道', color: '#e9c46a',
    desc: '东宁府孟家弟子，绘画天才，年幼时母亲被妖族当面杀死，在那次经历中意外获得元神，开始向成为神尊努力，成为神尊后初心不变，以刀斩妖邪，以笔绘众生',
    flavor: '刀过处，妖邪尽伏；吾以此身，护这一城烟火。' },
  { type: '神尊', name: '柳七月', tag: '神尊·水脉', color: '#e9c46a',
    desc: '水神一脉传人，神尊境强者。灵动缥缈，以柔克刚，与孟川并肩守护人族。',
    flavor: '水无常形，柔亦可断金刚。' },
  { type: '神尊', name: '秦五', tag: '神尊·引路', color: '#e9c46a',
    desc: '孟川的引路与庇护者，神尊境前辈，镇守一方，点化无数人族天骄。',
    flavor: '神尊之路无尽，且看后来者登顶。' },
  { type: '神尊', name: '晏烬', tag: '天骄·同辈', color: '#e9c46a',
    desc: '与孟川同辈的人族天骄，剑意凌厉，于妖潮中淬炼成锋。',
    flavor: '剑锋所指，妖潮退散。' },

  // —— 功法 ——
  { type: '功法', name: '元神淬体诀', tag: '淬体', color: '#2dd4bf',
    desc: '凝元淬体，熔炼神魂，奠定肉身成圣之不灭根基，元界修行之入门根本。',
    flavor: '炼体如炼心，不破不立。' },
  { type: '功法', name: '水墨刀意', tag: '刀道', color: '#2dd4bf',
    desc: '孟川主修之刀道极致，以意为锋，一刀山河动，斩妖于无形。',
    flavor: '一笔山河，一刀断生死。' },
  { type: '功法', name: '九霄雷法', tag: '雷系', color: '#2dd4bf',
    desc: '引动九天雷霆，焚邪破障，天威浩荡，专克妖族阴晦之气。',
    flavor: '天命雷霆，代天罚恶。' },
  { type: '功法', name: '不灭身', tag: '肉身', color: '#2dd4bf',
    desc: '肉身境之极致，滴血可重生，妖王难伤其分毫，护道之铁壁。',
    flavor: '肉身不灭，则道心不坠。' },
  { type: '功法', name: '心意刀', tag: '刀道', color: '#2dd4bf',
    desc: '天地游龙，心意无尽',
    flavor: '人说谎言，刀说真相' },

  // —— 妖魔 ——
  { type: '妖魔', name: '噬魂魔', tag: '妖魔·魂', color: '#ef4454',
    desc: '潜于暗影吞噬神魂，最善惑人心智，杀人于无形，妖族中的诡谲一支。',
    flavor: '你的恐惧，便是我的食粮。' },
  { type: '妖魔', name: '赤焰妖将', tag: '火妖', color: '#ef4454',
    desc: '焚天赤焰，所过寸草不生，凶悍异常的大妖战将，统御一方妖兵。',
    flavor: '焚尽八荒，方显吾之凶威。' },
  { type: '妖魔', name: '蚀骨蛛皇', tag: '虫妖', color: '#ef4454',
    desc: '盘踞幽渊，吐丝成阵，困敌于无形之网，猎物难逃其噬。',
    flavor: '落入吾网，便再无归途。' },
  { type: '妖魔', name: '黑铁妖王', tag: '妖王', color: '#ef4454',
    desc: '统御一方妖族之妖王，气息如渊，人族神尊亦需忌惮三分。',
    flavor: '人族神尊？不过尔尔。' },

  // —— 秘境 ——
  { type: '秘境', name: '不周遗墟', tag: '遗迹', color: '#5eead4',
    desc: '上古不周山倾塌后遗留的墟境，藏有开天传承与上古兵器，探秘者众。',
    flavor: '开天之力，尽藏于此墟之中。' },
  { type: '秘境', name: '元初山', tag: '圣地', color: '#5eead4',
    desc: '人族神尊修炼圣地，灵气如海，禁制重重，乃问道之至高所在。',
    flavor: '问道元初，方知神尊之浩瀚。' },
  { type: '秘境', name: '暗星界', tag: '秘界', color: '#5eead4',
    desc: '游离于元界之外的暗星之界，妖族据守，星光尽灭，凶险万分。',
    flavor: '星光尽灭处，妖族称王。' },
  { type: '秘境', name: '归海秘境', tag: '水境', color: '#5eead4',
    desc: '柳七月一脉守护的归海秘境，水脉本源孕养万物，藏有水神传承。',
    flavor: '海纳百川，孕养一方生灵。' }
]

// 动态（独立滚动条）
const newsItems = [
  { tag: '公告', text: '神尊归位大典 · 新晋神尊觉醒仪式将启', kw: '神尊', color: '#e9c46a' },
  { tag: '预警', text: '妖潮预警 · 东海之滨妖气骤盛', kw: '妖潮', color: '#ef4454' },
  { tag: '活动', text: '不周遗墟开放 · 探秘可得上古传承', kw: '不周遗墟', color: '#5eead4' },
  { tag: '历练', text: '元初山论道 · 同辈天骄切磋在即', kw: '元初山', color: '#2dd4bf' }
]

const counts = computed(() => {
  const m = {}
  baseSource().forEach(it => { m[it.type] = (m[it.type] || 0) + 1 })
  return m
})

// 当前展示数据集：后端结果 / 本地过滤 / 全量
const catalog = computed(() => {
  const src = baseSource()
  if (!hasSearched.value) return src
  if (apiResults.value) return apiResults.value
  const kw = lastKeyword.value.trim().toLowerCase()
  if (!kw) return src
  return src.filter(it =>
    (it.name || '').toLowerCase().includes(kw) ||
    (it.desc || '').toLowerCase().includes(kw) ||
    (it.tag || '').toLowerCase().includes(kw) ||
    (it.type || '').toLowerCase().includes(kw)
  )
})

// 各分类下条目
function itemsOf(type) { return catalog.value.filter(it => (it.type || '') === type) }

function onOpen(it) { activeItem.value = it }
const activeItem = ref(null)
function closeModal() { activeItem.value = null }

const hotTags = ['孟川', '柳七月', '元神淬体诀', '水墨刀意', '噬魂魔', '不周遗墟']

/* ===== toast ===== */
const toastMsg = ref('')
const toastShow = ref(false)
let toastTimer = null
function toast(msg) {
  toastMsg.value = msg
  toastShow.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastShow.value = false }, 2200)
}
</script>

<style scoped>
.home{position:fixed;inset:0;overflow-y:auto;overflow-x:hidden;
  background:
    radial-gradient(120% 80% at 80% 0%,rgba(45,212,191,.10),transparent 55%),
    radial-gradient(100% 90% at 10% 100%,rgba(239,68,84,.10),transparent 55%),
    linear-gradient(180deg,var(--ink2),var(--ink) 70%);
  color:var(--text)}
.embers{position:fixed;inset:0;pointer-events:none;z-index:0;overflow:hidden}
.embers i{position:absolute;bottom:-10px;border-radius:50%;opacity:0}
@keyframes rise{
  0%{transform:translateY(0) scale(1);opacity:0}
  10%{opacity:.9}90%{opacity:.5}
  100%{transform:translateY(-105vh) scale(.4);opacity:0}}

/* ===== 导航栏 ===== */
.nav{position:sticky;top:0;z-index:30;
  background:linear-gradient(180deg,rgba(7,11,22,.92),rgba(7,11,22,.62));
  backdrop-filter:blur(14px);
  border-bottom:1px solid var(--line);
  box-shadow:0 8px 30px rgba(0,0,0,.4)}
.nav-inner{max-width:1240px;margin:0 auto;display:flex;align-items:center;gap:26px;
  padding:14px 26px}
.brand{display:flex;align-items:center;gap:12px;cursor:pointer;flex-shrink:0}
.seal{width:42px;height:42px;border-radius:11px;
  background:linear-gradient(135deg,var(--crimson),#b91c1c);
  display:grid;place-items:center;font-weight:900;color:#fff;font-size:20px;
  box-shadow:0 0 22px rgba(239,68,84,.55);animation:sealPulse 3s ease-in-out infinite}
@keyframes sealPulse{50%{box-shadow:0 0 36px rgba(239,68,84,.9)}}
.brand-txt b{font-size:18px;letter-spacing:4px;font-weight:900;display:block}
.brand-txt span{font-size:11px;color:var(--muted);letter-spacing:2px}
.menu{display:flex;gap:6px;flex:1;flex-wrap:wrap}
.menu a{position:relative;padding:8px 14px;font-size:14px;letter-spacing:2px;
  color:var(--muted);cursor:pointer;transition:.18s;border-radius:9px}
.menu a:hover{color:var(--text);background:rgba(45,212,191,.08)}
.menu a.active{color:var(--jade);
  background:linear-gradient(180deg,rgba(45,212,191,.14),rgba(45,212,191,.04))}
.menu a.active::after{content:"";position:absolute;left:14px;right:14px;bottom:2px;height:2px;
  background:linear-gradient(90deg,var(--jade),var(--gold));border-radius:2px;
  box-shadow:0 0 10px var(--jade)}

.nav-right{display:flex;align-items:center;gap:16px;flex-shrink:0}
.nav-search{display:flex;align-items:center;gap:6px;padding:6px 6px 6px 12px;
  background:rgba(7,11,22,.6);border:1px solid rgba(255,255,255,.12);border-radius:999px;
  transition:.2s}
.nav-search.focus{border-color:var(--jade);box-shadow:0 0 0 3px rgba(45,212,191,.18)}
.nav-search .s-ico{color:var(--muted);font-size:16px}
.nav-search input{width:190px;background:none;border:none;outline:none;
  color:var(--text);font-family:inherit;font-size:13px;letter-spacing:.5px}
.nav-search .s-btn{padding:6px 12px;border:none;border-radius:999px;cursor:pointer;
  font-family:inherit;font-size:12px;letter-spacing:2px;color:#04130f;
  background:linear-gradient(135deg,var(--jade),#5eead4);transition:.15s}
.nav-search .s-btn:hover{filter:brightness(1.08)}

.user{display:flex;align-items:center;gap:9px}
.avatar{width:34px;height:34px;border-radius:50%;display:grid;place-items:center;
  font-weight:900;color:#04130f;font-size:15px;
  background:linear-gradient(135deg,var(--gold),#f7f3e8);
  box-shadow:0 0 16px rgba(233,196,106,.5)}
.uname{font-size:13px;color:var(--text);letter-spacing:1px;max-width:96px;
  overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.guest-tag{padding:2px 8px;border-radius:999px;font-size:11px;letter-spacing:1px;
  color:var(--muted);background:rgba(148,163,184,.16);border:1px solid var(--line)}
.logout{padding:6px 12px;border:1px solid var(--line);border-radius:999px;
  background:none;color:var(--muted);cursor:pointer;font-family:inherit;font-size:12px;
  letter-spacing:2px;transition:.15s}
.logout:hover{color:var(--crimson);border-color:var(--crimson)}

/* ===== 英雄区 ===== */
.hero{max-width:960px;margin:0 auto;padding:74px 26px 24px;text-align:center;position:relative;z-index:2}
.hero-badge{display:inline-block;font-size:12px;letter-spacing:5px;color:var(--jade);
  border:1px solid var(--line);border-radius:999px;padding:6px 16px;
  background:rgba(45,212,191,.08);box-shadow:0 0 14px rgba(45,212,191,.12) inset}
.hero-title{font-family:"ZCOOL XiaoWei","Noto Serif SC",serif;
  font-size:62px;line-height:1.05;letter-spacing:8px;margin:22px 0 14px;
  background:linear-gradient(90deg,var(--jade),#fff,var(--gold),var(--jade));
  background-size:220% auto;-webkit-background-clip:text;background-clip:text;color:transparent;
  filter:drop-shadow(0 0 26px rgba(45,212,191,.4));animation:shine 5s linear infinite}
@keyframes shine{to{background-position:220% center}}
.hero-sub{color:#c7d4e8;font-size:16px;line-height:2;letter-spacing:1px;
  text-shadow:0 2px 14px rgba(0,0,0,.6);max-width:640px;margin:0 auto}

.hero-search{display:flex;align-items:center;gap:10px;margin:34px auto 0;max-width:680px;
  padding:9px 9px 9px 20px;background:var(--glass);
  border:1px solid rgba(255,255,255,.14);border-radius:999px;
  backdrop-filter:blur(16px);box-shadow:0 20px 60px rgba(0,0,0,.5),inset 0 1px 0 rgba(255,255,255,.08);
  position:relative;overflow:hidden}
.hero-search::before{content:"";position:absolute;inset:-1px;border-radius:999px;padding:1.5px;
  background:linear-gradient(130deg,var(--jade),transparent 42%,var(--crimson));
  -webkit-mask:linear-gradient(#000 0 0) content-box,linear-gradient(#000 0 0);
  -webkit-mask-composite:xor;mask-composite:exclude;pointer-events:none;opacity:.6}
.hs-ico{color:var(--jade);font-size:22px;flex-shrink:0}
.hero-search input{flex:1;min-width:0;background:none;border:none;outline:none;color:var(--text);
  font-family:inherit;font-size:16px;letter-spacing:.5px}
.hero-search input::placeholder{color:var(--muted)}
.hs-btn{padding:12px 24px;border:none;border-radius:999px;cursor:pointer;
  font-family:inherit;font-size:15px;font-weight:700;letter-spacing:4px;color:#04130f;
  background:linear-gradient(135deg,var(--jade),#5eead4);
  box-shadow:0 12px 30px rgba(45,212,191,.4);transition:.18s;position:relative;overflow:hidden;flex-shrink:0}
.hs-btn::after{content:"";position:absolute;top:0;left:-60%;width:50%;height:100%;
  background:linear-gradient(120deg,transparent,rgba(255,255,255,.55),transparent);
  transform:skewX(-20deg);animation:sword 3s ease-in-out infinite}
@keyframes sword{0%{left:-60%}55%,100%{left:140%}}
.hs-btn:hover{transform:translateY(-1px);box-shadow:0 16px 44px rgba(45,212,191,.55)}
.hs-btn:disabled{opacity:.6;cursor:wait}

.hot{margin-top:22px;display:flex;align-items:center;justify-content:center;gap:10px;flex-wrap:wrap}
.hot-label{font-size:13px;color:var(--muted);letter-spacing:1px}
.hot-tag{font-size:13px;padding:6px 14px;border:1px solid var(--line);border-radius:999px;
  color:var(--jade);background:rgba(45,212,191,.06);cursor:pointer;letter-spacing:1px;transition:.15s}
.hot-tag:hover{background:rgba(45,212,191,.16);box-shadow:0 0 14px rgba(45,212,191,.22)}

/* ===== 首页展示框：四大核心界域 ===== */
.showcase{max-width:1240px;margin:36px auto 6px;padding:0 26px;position:relative;z-index:2}
.showcase-head{display:flex;align-items:center;gap:14px;margin-bottom:22px}
.sh-ico{font-size:20px;color:var(--jade);text-shadow:0 0 14px var(--jade)}
.showcase-head h2{font-family:"ZCOOL XiaoWei","Noto Serif SC",serif;font-size:26px;
  letter-spacing:4px;font-weight:900}
.sh-line{flex:1;height:1px;background:linear-gradient(90deg,var(--line),transparent)}
.sh-tip{font-size:12px;color:var(--muted);letter-spacing:1px}
.frames{display:grid;grid-template-columns:repeat(4,1fr);gap:18px}
.frame{position:relative;padding:26px 22px 22px;border-radius:20px;overflow:hidden;cursor:pointer;
  background:linear-gradient(160deg,rgba(14,22,42,.7),rgba(7,11,22,.55));
  border:1px solid color-mix(in srgb,var(--c) 38%,transparent);
  box-shadow:0 18px 44px rgba(0,0,0,.42),inset 0 1px 0 rgba(255,255,255,.06);
  transition:.22s;backdrop-filter:blur(10px)}
.frame::before{content:"";position:absolute;left:0;top:0;bottom:0;width:4px;
  background:linear-gradient(180deg,var(--c),transparent);opacity:.9}
.frame:hover{transform:translateY(-6px);border-color:var(--c);
  box-shadow:0 28px 64px rgba(0,0,0,.55),0 0 30px color-mix(in srgb,var(--c) 40%,transparent)}
.frame-ico{width:52px;height:52px;border-radius:14px;display:grid;place-items:center;
  font-size:26px;color:var(--c);margin-bottom:16px;
  background:color-mix(in srgb,var(--c) 14%,transparent);
  border:1px solid color-mix(in srgb,var(--c) 36%,transparent);
  box-shadow:0 0 18px color-mix(in srgb,var(--c) 26%,transparent)}
.frame-title{font-size:21px;font-weight:900;letter-spacing:2px;margin-bottom:10px;
  font-family:"ZCOOL XiaoWei","Noto Serif SC",serif}
.frame-desc{font-size:13px;color:var(--muted);line-height:1.9;letter-spacing:.5px;
  min-height:70px;display:-webkit-box;-webkit-line-clamp:3;-webkit-box-orient:vertical;overflow:hidden}
.frame-foot{display:flex;align-items:center;justify-content:space-between;margin-top:14px}
.frame-count{font-size:12px;color:var(--c);letter-spacing:1px}
.frame-go{font-size:13px;color:var(--text);letter-spacing:2px;transition:.15s}
.frame:hover .frame-go{color:var(--c);letter-spacing:4px}
.frame-glow{position:absolute;right:-40px;top:-40px;width:120px;height:120px;border-radius:50%;
  background:radial-gradient(circle,color-mix(in srgb,var(--c) 30%,transparent),transparent 70%);
  opacity:.5;pointer-events:none}

/* ===== 内容区 ===== */
.main{max-width:1240px;margin:0 auto;padding:34px 26px 60px;position:relative;z-index:2}
.search-head{display:flex;align-items:center;justify-content:space-between;
  margin:10px 0 26px;padding-bottom:16px;border-bottom:1px solid var(--line)}
.sh-left{display:flex;align-items:baseline;gap:12px;flex-wrap:wrap}
.sh-kw{font-size:20px;font-weight:900;color:var(--text);letter-spacing:2px}
.sh-count{font-size:13px;color:var(--muted);letter-spacing:1px}
.sh-src{font-size:12px;color:var(--jade);letter-spacing:1px}
.sh-src.local{color:var(--gold)}
.sh-clear{padding:7px 16px;border:1px solid var(--line);border-radius:999px;background:none;
  color:var(--muted);cursor:pointer;font-family:inherit;font-size:13px;letter-spacing:2px;transition:.15s}
.sh-clear:hover{color:var(--jade);border-color:var(--jade)}

.empty{text-align:center;padding:60px 0;color:var(--muted)}
.empty-mark{font-size:54px;color:var(--line);margin-bottom:14px}
.empty p{letter-spacing:2px}

.block{margin-bottom:46px;scroll-margin-top:88px;border-radius:18px;transition:.3s}
.block.flash{animation:flashSec 1.3s ease}
@keyframes flashSec{
  0%{box-shadow:0 0 0 0 color-mix(in srgb,var(--jade) 0%,transparent)}
  30%{box-shadow:0 0 0 3px color-mix(in srgb,var(--jade) 55%,transparent),0 0 40px rgba(45,212,191,.5)}
  100%{box-shadow:0 0 0 0 transparent}}
.block-head{display:flex;align-items:center;gap:14px;margin-bottom:22px}
.bh-icon{font-size:22px;color:var(--jade);text-shadow:0 0 14px var(--jade)}
.block-head h2{font-family:"ZCOOL XiaoWei","Noto Serif SC",serif;font-size:26px;
  letter-spacing:4px;font-weight:900}
.bh-line{flex:1;height:1px;background:linear-gradient(90deg,var(--line),transparent)}
.bh-top{font-size:12px;color:var(--muted);border:1px solid var(--line);border-radius:999px;
  padding:6px 12px;background:none;cursor:pointer;font-family:inherit;letter-spacing:1px;transition:.15s}
.bh-top:hover{color:var(--jade);border-color:var(--jade)}

.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(248px,1fr));gap:18px}
.card{position:relative;padding:22px 20px 20px;border-radius:18px;overflow:hidden;cursor:pointer;
  background:var(--glass);border:1px solid rgba(255,255,255,.10);
  backdrop-filter:blur(12px);box-shadow:0 18px 44px rgba(0,0,0,.42),inset 0 1px 0 rgba(255,255,255,.06);
  transition:.2s}
.card::before{content:"";position:absolute;left:0;top:0;bottom:0;width:3px;
  background:linear-gradient(180deg,var(--c),transparent);opacity:.85}
.card:hover{transform:translateY(-5px);border-color:var(--c);
  box-shadow:0 26px 60px rgba(0,0,0,.55),0 0 26px color-mix(in srgb,var(--c) 35%,transparent)}
.card-top{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px}
.card-tag{font-size:11px;letter-spacing:2px;padding:4px 10px;border-radius:999px;
  color:var(--c);background:color-mix(in srgb,var(--c) 14%,transparent);
  border:1px solid color-mix(in srgb,var(--c) 40%,transparent)}
.card-type{font-size:11px;color:var(--muted);letter-spacing:2px}
.card-name{font-size:20px;font-weight:900;letter-spacing:2px;margin-bottom:10px;
  font-family:"ZCOOL XiaoWei","Noto Serif SC",serif}
.card-desc{font-size:13px;color:var(--muted);line-height:1.9;letter-spacing:.5px;
  min-height:62px;display:-webkit-box;-webkit-line-clamp:3;-webkit-box-orient:vertical;overflow:hidden}
.card-more{margin-top:12px;background:none;border:none;color:var(--c);cursor:pointer;
  font-family:inherit;font-size:13px;letter-spacing:3px;padding:0;transition:.15s}
.card-more:hover{letter-spacing:5px}

/* ===== 元界动态滚动条 ===== */
.news{max-width:1240px;margin:8px auto 0;padding:24px 26px 0}
.news-head{display:flex;align-items:center;gap:12px;margin-bottom:16px}
.nh-ico{font-size:18px;color:var(--gold);text-shadow:0 0 12px var(--gold)}
.news-head h2{font-family:"ZCOOL XiaoWei","Noto Serif SC",serif;font-size:22px;letter-spacing:4px;font-weight:900}
.news-track{display:flex;gap:14px;overflow-x:auto;padding-bottom:10px;scrollbar-width:thin}
.news-track::-webkit-scrollbar{height:6px}
.news-track::-webkit-scrollbar-thumb{background:var(--line);border-radius:999px}
.news-item{flex:0 0 auto;display:flex;align-items:center;gap:10px;padding:12px 18px;cursor:pointer;
  border-radius:14px;background:var(--glass);border:1px solid color-mix(in srgb,var(--c) 34%,transparent);
  transition:.18s}
.news-item:hover{transform:translateY(-3px);box-shadow:0 0 22px color-mix(in srgb,var(--c) 35%,transparent)}
.news-tag{font-size:11px;letter-spacing:1px;padding:3px 9px;border-radius:999px;
  color:var(--c);background:color-mix(in srgb,var(--c) 14%,transparent);border:1px solid color-mix(in srgb,var(--c) 40%,transparent)}
.news-text{font-size:13px;color:var(--text);letter-spacing:.5px;white-space:nowrap}

.foot{position:relative;z-index:2;text-align:center;padding:26px;font-size:12px;
  color:var(--muted);letter-spacing:1px;border-top:1px solid rgba(255,255,255,.06)}

/* ===== 详情弹窗 ===== */
.modal{position:fixed;inset:0;z-index:80;display:grid;place-items:center;padding:24px;
  background:rgba(4,7,14,.66);backdrop-filter:blur(6px)}
.modal-card{position:relative;width:100%;max-width:460px;padding:38px 34px 32px;border-radius:22px;
  background:linear-gradient(160deg,rgba(14,22,42,.96),rgba(7,11,22,.96));
  border:1px solid color-mix(in srgb,var(--c) 45%,transparent);
  box-shadow:0 30px 80px rgba(0,0,0,.6),0 0 40px color-mix(in srgb,var(--c) 22%,transparent);
  animation:cardIn .45s ease both}
@keyframes cardIn{from{opacity:0;transform:translateY(16px) scale(.97)}to{opacity:1;transform:none}}
.modal-card::before{content:"";position:absolute;inset:-1px;border-radius:23px;padding:1.5px;
  background:linear-gradient(130deg,var(--c),transparent 46%,color-mix(in srgb,var(--c) 60%,transparent));
  -webkit-mask:linear-gradient(#000 0 0) content-box,linear-gradient(#000 0 0);
  -webkit-mask-composite:xor;mask-composite:exclude;pointer-events:none;opacity:.7}
.modal-x{position:absolute;top:16px;right:18px;width:32px;height:32px;border-radius:50%;
  border:1px solid var(--line);background:none;color:var(--muted);cursor:pointer;font-size:14px;transition:.15s}
.modal-x:hover{color:var(--crimson);border-color:var(--crimson)}
.modal-tag{display:inline-block;font-size:11px;letter-spacing:2px;padding:4px 11px;border-radius:999px;
  color:var(--c);background:color-mix(in srgb,var(--c) 14%,transparent);
  border:1px solid color-mix(in srgb,var(--c) 40%,transparent)}
.modal-name{font-family:"ZCOOL XiaoWei","Noto Serif SC",serif;font-size:32px;letter-spacing:4px;
  font-weight:900;margin:14px 0 6px}
.modal-type{font-size:12px;color:var(--muted);letter-spacing:2px}
.modal-desc{font-size:14px;color:#cdd9ec;line-height:2;letter-spacing:.5px;margin:18px 0 16px}
.modal-flavor{font-family:"ZCOOL XiaoWei","Noto Serif SC",serif;font-size:16px;color:var(--c);
  letter-spacing:2px;line-height:1.8;text-shadow:0 0 16px color-mix(in srgb,var(--c) 40%,transparent);
  padding:14px 16px;border-left:3px solid var(--c);background:color-mix(in srgb,var(--c) 8%,transparent);border-radius:0 12px 12px 0}
.modal-close{margin-top:22px;width:100%;padding:11px;border:none;border-radius:11px;cursor:pointer;
  font-family:inherit;font-size:14px;font-weight:700;letter-spacing:6px;color:#04130f;
  background:linear-gradient(135deg,var(--c),color-mix(in srgb,var(--c) 60%,#5eead4))}
.modal-close:hover{filter:brightness(1.08)}

.fade-enter-active,.fade-leave-active{transition:opacity .25s}
.fade-enter-from,.fade-leave-to{opacity:0}

/* ===== toast ===== */
.toast{position:fixed;left:50%;top:84px;transform:translateX(-50%) translateY(-20px);
  background:linear-gradient(135deg,var(--jade-deep),var(--jade));
  color:#04130f;padding:12px 22px;border-radius:12px;font-weight:700;
  letter-spacing:1px;opacity:0;pointer-events:none;transition:.3s;z-index:60;
  box-shadow:0 14px 40px rgba(45,212,191,.45)}
.toast.show{opacity:1;transform:translateX(-50%) translateY(0)}

@media(max-width:980px){
  .frames{grid-template-columns:repeat(2,1fr)}
}
@media(max-width:880px){
  .menu{display:none}
  .nav-search input{width:120px}
  .uname{display:none}
  .hero-title{font-size:44px;letter-spacing:5px}
}
@media(max-width:560px){
  .frames{grid-template-columns:1fr}
}
</style>
